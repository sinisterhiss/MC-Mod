package dev.unifiedstorage.mixin;

import dev.unifiedstorage.storage.UniversalStorageState;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

/**
 * A stack of identical tools is allowed, but once one tool takes durability damage it is no
 * longer identical to the untouched copies. Before vanilla applies durability, isolate one
 * physical item and move the untouched remainder elsewhere in the player's inventory; if the
 * inventory has no room, the untouched remainder goes to Universal Storage.
 */
@Mixin(ItemStack.class)
public abstract class ItemStackDamageMixin {
    @Inject(
            method = "damage(ILnet/minecraft/entity/LivingEntity;Lnet/minecraft/entity/EquipmentSlot;)V",
            at = @At("HEAD")
    )
    private void unifiedstorage$splitBeforeEntityDamage(int amount,
                                                         LivingEntity entity,
                                                         EquipmentSlot slot,
                                                         CallbackInfo ci) {
        if (entity instanceof ServerPlayerEntity player) {
            splitOneForDamage(player);
        }
    }

    @Inject(
            method = "damage(ILnet/minecraft/server/world/ServerWorld;Lnet/minecraft/server/network/ServerPlayerEntity;Ljava/util/function/Consumer;)V",
            at = @At("HEAD")
    )
    private void unifiedstorage$splitBeforeServerDamage(int amount,
                                                         ServerWorld world,
                                                         @Nullable ServerPlayerEntity player,
                                                         Consumer<Item> breakCallback,
                                                         CallbackInfo ci) {
        if (player != null) {
            splitOneForDamage(player);
        }
    }

    private void splitOneForDamage(ServerPlayerEntity player) {
        ItemStack self = (ItemStack) (Object) this;
        if (self.getCount() <= 1 || !self.isDamageable()) return;

        int untouchedCount = self.getCount() - 1;
        ItemStack untouched = self.copyWithCount(untouchedCount);
        self.setCount(1);

        PlayerInventory inventory = player.getInventory();
        insertAwayFromCurrentStack(inventory, self, untouched);

        if (!untouched.isEmpty()) {
            UniversalStorageState.get(player).insert(player.getUuid(), untouched);
        }

        inventory.markDirty();
    }

    private static void insertAwayFromCurrentStack(PlayerInventory inventory,
                                                    ItemStack current,
                                                    ItemStack incoming) {
        // Merge into another exact stack first, but never merge back into the stack about to
        // take damage.
        for (int i = 0; i < PlayerInventory.MAIN_SIZE && !incoming.isEmpty(); i++) {
            ItemStack existing = inventory.getStack(i);
            if (existing == current || existing.isEmpty()) continue;
            if (!ItemStack.areItemsAndComponentsEqual(existing, incoming)) continue;

            int room = UniversalStorageState.MAX_STACK_SIZE - existing.getCount();
            if (room <= 0) continue;

            int moved = Math.min(room, incoming.getCount());
            existing.increment(moved);
            incoming.decrement(moved);
        }

        // Then use empty main/hotbar slots. Armor/offhand are intentionally not used as
        // automatic overflow destinations.
        while (!incoming.isEmpty()) {
            int empty = findEmptyMainSlotExcluding(inventory, current);
            if (empty < 0) return;

            int moved = Math.min(UniversalStorageState.MAX_STACK_SIZE, incoming.getCount());
            inventory.setStack(empty, incoming.split(moved));
        }
    }

    private static int findEmptyMainSlotExcluding(PlayerInventory inventory, ItemStack current) {
        for (int i = 0; i < PlayerInventory.MAIN_SIZE; i++) {
            ItemStack existing = inventory.getStack(i);
            if (existing == current) continue;
            if (existing.isEmpty()) return i;
        }
        return -1;
    }
}
