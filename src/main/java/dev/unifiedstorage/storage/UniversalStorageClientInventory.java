package dev.unifiedstorage.storage;

import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Client-side mirror of the 54 server-owned storage slots. */
public final class UniversalStorageClientInventory implements Container {
    private final NonNullList<ItemStack> stacks;

    public UniversalStorageClientInventory(int size) {
        this.stacks = NonNullList.withSize(size, ItemStack.EMPTY);
    }

    @Override
    public int getContainerSize() {
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
    public ItemStack getItem(int slot) {
        return slot >= 0 && slot < stacks.size() ? stacks.get(slot) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (slot < 0 || slot >= stacks.size()) return ItemStack.EMPTY;
        ItemStack stack = stacks.get(slot);
        if (stack.isEmpty()) return ItemStack.EMPTY;
        ItemStack removed = stack.split(amount);
        if (stack.isEmpty()) stacks.set(slot, ItemStack.EMPTY);
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (slot < 0 || slot >= stacks.size()) return ItemStack.EMPTY;
        ItemStack removed = stacks.get(slot);
        stacks.set(slot, ItemStack.EMPTY);
        return removed;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot < 0 || slot >= stacks.size()) return;
        stacks.set(slot, stack);
    }

    @Override
    public int getMaxStackSize() {
        return UniversalStorageState.MAX_STACK_SIZE;
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return UniversalStorageState.MAX_STACK_SIZE;
    }

    @Override
    public void setChanged() {
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void clearContent() {
        stacks.clear();
    }
}
