package dev.unifiedstorage.storage;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;

/**
 * Client-side backing inventory for the Universal Storage screen.
 *
 * Vanilla SimpleInventory clamps stacks to the item's normal max stack size, which would
 * turn a synchronized 999-item storage stack back into 64/16/1 on the client. This tiny
 * inventory deliberately preserves the server-supplied count up to the Universal Storage cap.
 */
public final class UniversalStorageClientInventory implements Inventory {
    private final DefaultedList<ItemStack> stacks;

    public UniversalStorageClientInventory(int size) {
        this.stacks = DefaultedList.ofSize(size, ItemStack.EMPTY);
    }

    @Override
    public int size() {
        return stacks.size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : stacks) {
            if (!stack.isEmpty()) return false;
        }
        return true;
    }

    @Override
    public ItemStack getStack(int slot) {
        return slot >= 0 && slot < stacks.size() ? stacks.get(slot) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        if (slot < 0 || slot >= stacks.size()) return ItemStack.EMPTY;
        ItemStack stack = stacks.get(slot);
        if (stack.isEmpty()) return ItemStack.EMPTY;

        ItemStack removed = stack.split(amount);
        if (stack.isEmpty()) stacks.set(slot, ItemStack.EMPTY);
        return removed;
    }

    @Override
    public ItemStack removeStack(int slot) {
        if (slot < 0 || slot >= stacks.size()) return ItemStack.EMPTY;
        ItemStack removed = stacks.get(slot);
        stacks.set(slot, ItemStack.EMPTY);
        return removed;
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        if (slot < 0 || slot >= stacks.size()) return;
        if (!stack.isEmpty() && stack.getCount() > UniversalStorageState.MAX_STACK_SIZE) {
            stack.setCount(UniversalStorageState.MAX_STACK_SIZE);
        }
        stacks.set(slot, stack);
    }

    @Override
    public int getMaxCountPerStack() {
        return UniversalStorageState.MAX_STACK_SIZE;
    }

    @Override
    public void markDirty() {
        // Client mirror only; the server owns persistence.
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return true;
    }

    @Override
    public void clear() {
        stacks.clear();
    }
}
