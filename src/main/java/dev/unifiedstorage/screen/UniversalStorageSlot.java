package dev.unifiedstorage.screen;

import dev.unifiedstorage.storage.UniversalStorageState;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;

/** A Universal Storage slot holds up to 999 exactly-identical items. */
public final class UniversalStorageSlot extends Slot {
    public UniversalStorageSlot(Inventory inventory, int index, int x, int y) {
        super(inventory, index, x, y);
    }

    @Override
    public int getMaxItemCount() {
        return UniversalStorageState.MAX_STACK_SIZE;
    }

    @Override
    public int getMaxItemCount(ItemStack stack) {
        return UniversalStorageState.MAX_STACK_SIZE;
    }
}
