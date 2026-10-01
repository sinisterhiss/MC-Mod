package dev.unifiedstorage.mixin;

import dev.unifiedstorage.storage.UniversalStorageState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

/**
 * Vanilla fills the player's inventory first. Any pickup remainder is moved into Universal Storage.
 */
@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin {
    @Shadow private int pickupDelay;
    @Shadow @Nullable private UUID target;

    @Inject(method = "playerTouch", at = @At("TAIL"))
    private void unifiedstorage$overflowPickup(Player player, CallbackInfo ci) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        if (this.pickupDelay != 0) return;
        if (this.target != null && !this.target.equals(player.getUUID())) return;

        ItemEntity self = (ItemEntity) (Object) this;
        if (self.isRemoved()) return;

        ItemStack remainder = self.getItem();
        if (remainder.isEmpty()) return;

        UniversalStorageState.get(serverPlayer).insert(serverPlayer.getUUID(), remainder);
        if (remainder.isEmpty()) {
            self.discard();
        }
    }
}
