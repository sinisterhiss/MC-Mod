package dev.unifiedstorage.mixin;

import dev.unifiedstorage.storage.UniversalStorageState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

/**
 * Identical damageable items may stack. Immediately before one takes durability, isolate one copy
 * so the untouched copies remain identical and keep their original durability.
 */
@Mixin(ItemStack.class)
public abstract class ItemStackDamageMixin {
    @Inject(
            method = "hurtAndBreak(ILnet/minecraft/server/level/ServerLevel;Lnet/minecraft/server/level/ServerPlayer;Ljava/util/function/Consumer;)V",
            at = @At("HEAD")
    )
    private void unifiedstorage$splitBeforeDamage(
            int amount,
            ServerLevel level,
            @Nullable ServerPlayer player,
            Consumer<Item> onBreak,
            CallbackInfo ci
    ) {
        if (player != null) {
            splitOneForDamage(player);
        }
    }

    @Inject(
            method = "hurtWithoutBreaking(ILnet/minecraft/world/entity/player/Player;)V",
            at = @At("HEAD")
    )
    private void unifiedstorage$splitBeforeNonBreakingDamage(int amount, Player player, CallbackInfo ci) {
        if (player instanceof ServerPlayer serverPlayer) {
            splitOneForDamage(serverPlayer);
        }
    }

    private void splitOneForDamage(ServerPlayer player) {
        ItemStack self = (ItemStack) (Object) this;
        if (self.getCount() <= 1 || !self.isDamageableItem()) return;

        Inventory inventory = player.getInventory();

        boolean belongsToPlayer = false;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (inventory.getItem(i) == self) {
                belongsToPlayer = true;
                break;
            }
        }
        if (!belongsToPlayer) return;

        ItemStack untouched = self.copyWithCount(self.getCount() - 1);
        self.setCount(1);

        mergeOrPlaceInMainInventory(inventory, self, untouched);

        if (!untouched.isEmpty()) {
            UniversalStorageState.get(player).insert(player.getUUID(), untouched);
        }

        inventory.setChanged();
    }

    private static void mergeOrPlaceInMainInventory(Inventory inventory, ItemStack current, ItemStack incoming) {
        for (int i = 0; i < Inventory.INVENTORY_SIZE && !incoming.isEmpty(); i++) {
            ItemStack existing = inventory.getItem(i);
            if (existing == current || existing.isEmpty()) continue;
            if (!ItemStack.isSameItemSameComponents(existing, incoming)) continue;

            int room = UniversalStorageState.MAX_STACK_SIZE - existing.getCount();
            if (room <= 0) continue;

            int moved = Math.min(room, incoming.getCount());
            existing.grow(moved);
            incoming.shrink(moved);
        }

        while (!incoming.isEmpty()) {
            int empty = inventory.getFreeSlot();
            if (empty < 0) return;

            int moved = Math.min(UniversalStorageState.MAX_STACK_SIZE, incoming.getCount());
            inventory.setItem(empty, incoming.split(moved));
        }
    }
}
