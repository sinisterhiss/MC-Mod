package dev.unifiedstorage.mixin;

import dev.unifiedstorage.storage.UniversalStorageState;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

/**
 * Vanilla gets first chance to put a pickup in the player's normal inventory.
 * Whatever remains after vanilla's attempt goes to universal storage.
 */
@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin {
    @Shadow private int pickupDelay;
    @Shadow @Nullable private UUID owner;

    @Inject(method = "onPlayerCollision", at = @At("TAIL"))
    private void unifiedstorage$overflowPickup(PlayerEntity player, CallbackInfo ci) {
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return;
        if (pickupDelay != 0) return;
        if (owner != null && !owner.equals(player.getUuid())) return;

        ItemEntity self = (ItemEntity) (Object) this;
        if (self.isRemoved()) return;

        ItemStack remainder = self.getStack();
        if (remainder.isEmpty()) return;

        UniversalStorageState storage = UniversalStorageState.get(serverPlayer);
        storage.insert(serverPlayer.getUuid(), remainder);

        if (remainder.isEmpty()) {
            // Vanilla did not remove the entity because the normal inventory was full.
            // The server now owns the items in persistent universal storage, so remove the entity.
            self.discard();
        }
    }
}
