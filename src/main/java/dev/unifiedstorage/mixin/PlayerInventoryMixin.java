package dev.unifiedstorage.mixin;

import dev.unifiedstorage.storage.UniversalStorageState;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Makes the player's main inventory/hotbar use the same 999-count exact-item stacking rules
 * as Universal Storage. Armor/special GUI slots can still impose their own one-item limits.
 */
@Mixin(PlayerInventory.class)
public abstract class PlayerInventoryMixin {
    /** Overrides Inventory's inherited default of 99 for PlayerInventory. */
    public int getMaxCountPerStack() {
        return UniversalStorageState.MAX_STACK_SIZE;
    }

    /**
     * Vanilla deliberately refuses to merge damaged stacks. We replace insertion so any two
     * ItemStacks with the same item and exactly the same components (including DAMAGE,
     * enchantments, names, potion data, etc.) can combine up to 999.
     */
    @Inject(
            method = "insertStack(ILnet/minecraft/item/ItemStack;)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private void unifiedstorage$insertExactStacks(int requestedSlot,
                                                   ItemStack incoming,
                                                   CallbackInfoReturnable<Boolean> cir) {
        PlayerInventory inventory = (PlayerInventory) (Object) this;

        if (incoming.isEmpty()) {
            cir.setReturnValue(false);
            return;
        }

        int before = incoming.getCount();

        if (requestedSlot >= 0) {
            moveIntoSlot(inventory, requestedSlot, incoming);
        } else {
            // Preserve vanilla's useful preference order: selected hotbar, offhand, then main.
            moveIntoMatchingSlot(inventory, inventory.selectedSlot, incoming);
            moveIntoMatchingSlot(inventory, PlayerInventory.OFF_HAND_SLOT, incoming);

            for (int i = 0; i < PlayerInventory.MAIN_SIZE && !incoming.isEmpty(); i++) {
                if (i == inventory.selectedSlot) continue;
                moveIntoMatchingSlot(inventory, i, incoming);
            }

            // Only main inventory/hotbar slots are valid automatic destinations for new stacks.
            while (!incoming.isEmpty()) {
                int empty = inventory.getEmptySlot();
                if (empty < 0) break;

                int moved = Math.min(UniversalStorageState.MAX_STACK_SIZE, incoming.getCount());
                inventory.setStack(empty, incoming.split(moved));
            }
        }

        boolean movedAnything = incoming.getCount() < before;
        if (movedAnything) {
            inventory.markDirty();
        } else if (inventory.player.isInCreativeMode()) {
            incoming.setCount(0);
            movedAnything = true;
        }

        cir.setReturnValue(movedAnything);
    }

    private static void moveIntoMatchingSlot(PlayerInventory inventory, int slot, ItemStack incoming) {
        if (incoming.isEmpty() || slot < 0 || slot >= inventory.size()) return;

        ItemStack existing = inventory.getStack(slot);
        if (existing.isEmpty()) return;
        if (!ItemStack.areItemsAndComponentsEqual(existing, incoming)) return;

        int room = UniversalStorageState.MAX_STACK_SIZE - existing.getCount();
        if (room <= 0) return;

        int moved = Math.min(room, incoming.getCount());
        existing.increment(moved);
        incoming.decrement(moved);
    }

    private static void moveIntoSlot(PlayerInventory inventory, int slot, ItemStack incoming) {
        if (incoming.isEmpty() || slot < 0 || slot >= inventory.size()) return;

        ItemStack existing = inventory.getStack(slot);
        if (existing.isEmpty()) {
            int moved = Math.min(UniversalStorageState.MAX_STACK_SIZE, incoming.getCount());
            inventory.setStack(slot, incoming.split(moved));
            return;
        }

        if (!ItemStack.areItemsAndComponentsEqual(existing, incoming)) return;

        int room = UniversalStorageState.MAX_STACK_SIZE - existing.getCount();
        if (room <= 0) return;

        int moved = Math.min(room, incoming.getCount());
        existing.increment(moved);
        incoming.decrement(moved);
    }
}
