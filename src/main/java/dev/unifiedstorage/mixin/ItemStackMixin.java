package dev.unifiedstorage.mixin;

import com.mojang.serialization.Codec;
import dev.unifiedstorage.storage.UniversalStorageState;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Makes every non-empty exact ItemStack capable of holding up to 999 items.
 *
 * ItemStack's disk codec normally caps count at 99 in 26.3. The redirect widens that codec range
 * so player inventories and Universal Storage can be saved without truncation.
 */
@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
    @Redirect(
            method = "*",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/ExtraCodecs;intRange(II)Lcom/mojang/serialization/Codec;"
            ),
            require = 1
    )
    private static Codec<Integer> unifiedstorage$widenItemStackCountCodec(int min, int max) {
        if (min == 1 && max == 99) {
            return ExtraCodecs.intRange(1, UniversalStorageState.MAX_STACK_SIZE);
        }
        return ExtraCodecs.intRange(min, max);
    }

    /** Overrides ItemInstance's inherited component-based default. */
    public int getMaxStackSize() {
        return UniversalStorageState.MAX_STACK_SIZE;
    }

    @Inject(method = "isStackable", at = @At("HEAD"), cancellable = true)
    private void unifiedstorage$allNonEmptyExactStacksMayStack(CallbackInfoReturnable<Boolean> cir) {
        ItemStack self = (ItemStack) (Object) this;
        cir.setReturnValue(!self.isEmpty());
    }
}
