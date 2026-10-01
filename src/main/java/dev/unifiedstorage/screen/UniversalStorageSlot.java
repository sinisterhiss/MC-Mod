package dev.unifiedstorage.screen;

import dev.unifiedstorage.storage.UniversalStorageState;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class UniversalStorageSlot extends Slot {
    public UniversalStorageSlot(Container container, int slot, int x, int y) {
        super(container, slot, x, y);
    }

    @Override
    public int getMaxStackSize() {
        return UniversalStorageState.MAX_STACK_SIZE;
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return UniversalStorageState.MAX_STACK_SIZE;
    }
}
