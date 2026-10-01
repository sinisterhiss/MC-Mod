package dev.unifiedstorage.mixin;

import dev.unifiedstorage.storage.UniversalStorageState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.registry.RegistryWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

/**
 * Core large-stack rules.
 *
 * Minecraft 1.21.1's normal ItemStack NBT codec only accepts counts through 99. We keep the
 * vanilla-safe encoded count at 1 and write the real count to a mod-owned integer whenever a
 * stack is larger than 99. Packet synchronization already uses a VarInt count, so 999 is safe
 * on the wire once getMaxCount() reports the larger limit.
 */
@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
    private static final String UNIFIEDSTORAGE_LARGE_COUNT_KEY = "UnifiedStorageCount";

    @Inject(method = "getMaxCount", at = @At("HEAD"), cancellable = true)
    private void unifiedstorage$maxStackSize(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(UniversalStorageState.MAX_STACK_SIZE);
    }

    @Inject(method = "isStackable", at = @At("HEAD"), cancellable = true)
    private void unifiedstorage$allExactItemsAreStackable(CallbackInfoReturnable<Boolean> cir) {
        ItemStack self = (ItemStack) (Object) this;
        cir.setReturnValue(!self.isEmpty());
    }

    @Inject(
            method = "encode(Lnet/minecraft/registry/RegistryWrapper$WrapperLookup;Lnet/minecraft/nbt/NbtElement;)Lnet/minecraft/nbt/NbtElement;",
            at = @At("HEAD"),
            cancellable = true
    )
    private void unifiedstorage$encodeLargeCount(RegistryWrapper.WrapperLookup registries,
                                                  NbtElement prefix,
                                                  CallbackInfoReturnable<NbtElement> cir) {
        ItemStack self = (ItemStack) (Object) this;
        int count = self.getCount();
        if (count <= 99) return;

        // Re-enter vanilla encoding with a codec-safe count. The recursive call does not hit
        // this branch because the copy's count is 1.
        NbtElement encoded = self.copyWithCount(1).encode(registries, prefix);
        if (encoded instanceof NbtCompound compound) {
            compound.putInt(UNIFIEDSTORAGE_LARGE_COUNT_KEY,
                    Math.min(count, UniversalStorageState.MAX_STACK_SIZE));
            cir.setReturnValue(compound);
        }
    }

    @Inject(method = "fromNbt", at = @At("HEAD"), cancellable = true)
    private static void unifiedstorage$decodeLargeCount(RegistryWrapper.WrapperLookup registries,
                                                         NbtElement nbt,
                                                         CallbackInfoReturnable<Optional<ItemStack>> cir) {
        if (!(nbt instanceof NbtCompound compound)) return;
        if (!compound.contains(UNIFIEDSTORAGE_LARGE_COUNT_KEY)) return;

        int realCount = compound.getInt(UNIFIEDSTORAGE_LARGE_COUNT_KEY);
        NbtCompound vanillaCopy = compound.copy();
        vanillaCopy.remove(UNIFIEDSTORAGE_LARGE_COUNT_KEY);

        Optional<ItemStack> decoded = ItemStack.fromNbt(registries, vanillaCopy);
        decoded.ifPresent(stack -> {
            if (!stack.isEmpty()) {
                stack.setCount(Math.max(1,
                        Math.min(realCount, UniversalStorageState.MAX_STACK_SIZE)));
            }
        });
        cir.setReturnValue(decoded);
    }
}
