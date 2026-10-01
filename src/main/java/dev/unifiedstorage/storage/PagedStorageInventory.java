package dev.unifiedstorage.storage;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.UUID;

/** A 54-slot window onto one page of the player's Universal Storage list. */
public final class PagedStorageInventory implements Container {
    public static final int PAGE_SIZE = 54;

    private final UniversalStorageState state;
    private final UUID playerId;
    private int page;

    public PagedStorageInventory(UniversalStorageState state, UUID playerId) {
        this.state = state;
        this.playerId = playerId;
    }

    public void setPage(int page) {
        this.page = Math.max(0, page);
    }

    private int logicalIndex(int slot) {
        return page * PAGE_SIZE + slot;
    }

    private List<ItemStack> items() {
        return state.items(playerId);
    }

    private void ensureIndex(int index) {
        List<ItemStack> items = items();
        while (items.size() <= index) items.add(ItemStack.EMPTY);
    }

    @Override
    public int getContainerSize() {
        return PAGE_SIZE;
    }

    @Override
    public boolean isEmpty() {
        for (int i = 0; i < PAGE_SIZE; i++) {
            if (!getItem(i).isEmpty()) return false;
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        int index = logicalIndex(slot);
        List<ItemStack> items = items();
        return index < items.size() ? items.get(index) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        int index = logicalIndex(slot);
        List<ItemStack> items = items();
        if (index >= items.size()) return ItemStack.EMPTY;

        ItemStack stack = items.get(index);
        if (stack.isEmpty()) return ItemStack.EMPTY;

        ItemStack removed = stack.split(amount);
        if (stack.isEmpty()) items.set(index, ItemStack.EMPTY);
        setChanged();
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        int index = logicalIndex(slot);
        List<ItemStack> items = items();
        if (index >= items.size()) return ItemStack.EMPTY;

        ItemStack removed = items.get(index);
        items.set(index, ItemStack.EMPTY);
        setChanged();
        return removed;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        int index = logicalIndex(slot);
        ensureIndex(index);
        if (!stack.isEmpty() && stack.getCount() > UniversalStorageState.MAX_STACK_SIZE) {
            stack.setCount(UniversalStorageState.MAX_STACK_SIZE);
        }
        items().set(index, stack);
        setChanged();
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
        state.setDirty();
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < PAGE_SIZE; i++) {
            setItem(i, ItemStack.EMPTY);
        }
    }
}
