package dev.unifiedstorage.storage;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;

import java.util.List;
import java.util.UUID;

/** 54-slot view into the player's dynamic universal storage list. */
public final class PagedStorageInventory implements Inventory {
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

    public int getPage() {
        return page;
    }

    private int logicalIndex(int slot) {
        return page * PAGE_SIZE + slot;
    }

    private List<ItemStack> items() {
        return state.items(playerId);
    }

    private void ensureIndex(int index) {
        List<ItemStack> items = items();
        while (items.size() <= index) {
            items.add(ItemStack.EMPTY);
        }
    }

    @Override
    public int size() {
        return PAGE_SIZE;
    }

    @Override
    public boolean isEmpty() {
        for (int i = 0; i < PAGE_SIZE; i++) {
            if (!getStack(i).isEmpty()) return false;
        }
        return true;
    }

    @Override
    public ItemStack getStack(int slot) {
        int index = logicalIndex(slot);
        List<ItemStack> items = items();
        return index < items.size() ? items.get(index) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        int index = logicalIndex(slot);
        List<ItemStack> items = items();
        if (index >= items.size()) return ItemStack.EMPTY;

        ItemStack stack = items.get(index);
        if (stack.isEmpty()) return ItemStack.EMPTY;

        ItemStack removed = stack.split(amount);
        if (stack.isEmpty()) items.set(index, ItemStack.EMPTY);
        markDirty();
        return removed;
    }

    @Override
    public ItemStack removeStack(int slot) {
        int index = logicalIndex(slot);
        List<ItemStack> items = items();
        if (index >= items.size()) return ItemStack.EMPTY;

        ItemStack removed = items.get(index);
        items.set(index, ItemStack.EMPTY);
        markDirty();
        return removed;
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        int index = logicalIndex(slot);
        ensureIndex(index);
        if (!stack.isEmpty() && stack.getCount() > UniversalStorageState.MAX_STACK_SIZE) {
            stack.setCount(UniversalStorageState.MAX_STACK_SIZE);
        }
        items().set(index, stack);
        markDirty();
    }

    @Override
    public int getMaxCountPerStack() {
        return UniversalStorageState.MAX_STACK_SIZE;
    }

    @Override
    public void markDirty() {
        state.markDirty();
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return true;
    }

    @Override
    public void clear() {
        for (int i = 0; i < PAGE_SIZE; i++) {
            setStack(i, ItemStack.EMPTY);
        }
    }
}
