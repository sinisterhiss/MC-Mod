package dev.unifiedstorage.screen;

import dev.unifiedstorage.UnifiedStorageMod;
import dev.unifiedstorage.storage.PagedStorageInventory;
import dev.unifiedstorage.storage.UniversalStorageClientInventory;
import dev.unifiedstorage.storage.UniversalStorageState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.UUID;

/**
 * Natural/found chest or barrel on the left, Universal Storage on the right.
 * The found container keeps its own contents; it is not converted into a terminal.
 */
public final class FoundContainerScreenHandler extends AbstractContainerMenu {
    public static final int BUTTON_PREVIOUS = 0;
    public static final int BUTTON_NEXT = 1;
    public static final int BUTTON_SORT = 2;
    public static final int BUTTON_DEPOSIT_ALL = 3;
    public static final int BUTTON_LOOT_ALL = 4;
    public static final int BUTTON_STORE_CHEST = 5;

    private static final int STORAGE_SLOT_COUNT = 54;

    private final Inventory playerInventory;
    private final Container foundContainer;
    private final Container storageInventory;
    private final int rows;
    private final int foundSlotCount;
    private final int playerSlotStart;
    private final int playerSlotEnd;
    private final int storageSlotStart;
    private final int storageSlotEnd;
    private final ContainerData data = new SimpleContainerData(2);

    private final ServerPlayer serverPlayer;
    private final UniversalStorageState state;
    private final UUID playerId;

    public static FoundContainerScreenHandler threeRowsClient(int containerId, Inventory inventory) {
        return new FoundContainerScreenHandler(
                UnifiedStorageMod.FOUND_CONTAINER_3_MENU,
                containerId,
                inventory,
                new SimpleContainer(27),
                3
        );
    }

    public static FoundContainerScreenHandler sixRowsClient(int containerId, Inventory inventory) {
        return new FoundContainerScreenHandler(
                UnifiedStorageMod.FOUND_CONTAINER_6_MENU,
                containerId,
                inventory,
                new SimpleContainer(54),
                6
        );
    }

    public FoundContainerScreenHandler(
            net.minecraft.world.inventory.MenuType<?> menuType,
            int containerId,
            Inventory playerInventory,
            Container foundContainer,
            int rows
    ) {
        super(menuType, containerId);
        this.playerInventory = playerInventory;
        this.foundContainer = foundContainer;
        this.rows = rows;
        this.foundSlotCount = rows * 9;
        this.playerSlotStart = foundSlotCount;
        this.playerSlotEnd = playerSlotStart + 36;
        this.storageSlotStart = playerSlotEnd;
        this.storageSlotEnd = storageSlotStart + STORAGE_SLOT_COUNT;

        checkContainerSize(foundContainer, foundSlotCount);
        foundContainer.startOpen(playerInventory.player);

        if (playerInventory.player instanceof ServerPlayer sp) {
            this.serverPlayer = sp;
            this.state = UniversalStorageState.get(sp);
            this.playerId = sp.getUUID();
            this.storageInventory = new PagedStorageInventory(state, playerId);
            this.data.set(0, 0);
            this.data.set(1, state.pageCount(playerId, STORAGE_SLOT_COUNT));
        } else {
            this.serverPlayer = null;
            this.state = null;
            this.playerId = null;
            this.storageInventory = new UniversalStorageClientInventory(STORAGE_SLOT_COUNT);
            this.data.set(0, 0);
            this.data.set(1, 1);
        }

        addDataSlots(this.data);

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(
                        foundContainer,
                        col + row * 9,
                        8 + col * 18,
                        18 + row * 18
                ));
            }
        }

        int playerTop = 18 + rows * 18 + 13;
        addStandardInventorySlots(playerInventory, 8, playerTop);

        for (int row = 0; row < 6; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new UniversalStorageSlot(
                        storageInventory,
                        col + row * 9,
                        184 + col * 18,
                        18 + row * 18
                ));
            }
        }
    }

    public int getRows() {
        return rows;
    }

    public int getPage() {
        return data.get(0);
    }

    public int getPageCount() {
        return Math.max(1, data.get(1));
    }

    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        if (!(player instanceof ServerPlayer)) return false;

        switch (buttonId) {
            case BUTTON_PREVIOUS -> {
                data.set(0, Math.max(0, getPage() - 1));
                refreshPage();
                return true;
            }
            case BUTTON_NEXT -> {
                data.set(0, Math.min(getPageCount() - 1, getPage() + 1));
                refreshPage();
                return true;
            }
            case BUTTON_SORT -> {
                state.sortAndCompact(playerId);
                data.set(0, 0);
                refreshPage();
                return true;
            }
            case BUTTON_DEPOSIT_ALL -> {
                depositAll();
                refreshPage();
                return true;
            }
            case BUTTON_LOOT_ALL -> {
                lootAllFromStorage();
                refreshPage();
                return true;
            }
            case BUTTON_STORE_CHEST -> {
                storeFoundContainer();
                refreshPage();
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    private void refreshPage() {
        if (serverPlayer == null) return;

        int pageCount = state.pageCount(playerId, STORAGE_SLOT_COUNT);
        data.set(1, pageCount);

        int page = Math.min(data.get(0), pageCount - 1);
        data.set(0, page);

        if (storageInventory instanceof PagedStorageInventory paged) {
            paged.setPage(page);
        }

        broadcastChanges();
    }

    private void depositAll() {
        for (int i = 0; i < Inventory.INVENTORY_SIZE; i++) {
            ItemStack stack = playerInventory.getItem(i);
            if (stack.isEmpty()) continue;

            ItemStack moving = stack.copy();
            state.insert(playerId, moving);

            if (moving.isEmpty()) playerInventory.setItem(i, ItemStack.EMPTY);
            else playerInventory.setItem(i, moving);
        }
        playerInventory.setChanged();
    }

    private void lootAllFromStorage() {
        List<ItemStack> stored = state.items(playerId);

        for (int i = 0; i < stored.size(); i++) {
            ItemStack source = stored.get(i);
            if (source.isEmpty()) continue;

            playerInventory.add(source);
            if (source.isEmpty()) stored.set(i, ItemStack.EMPTY);
        }

        state.trimTrailingEmpty(playerId);
        playerInventory.setChanged();
    }

    private void storeFoundContainer() {
        for (int i = 0; i < foundContainer.getContainerSize(); i++) {
            ItemStack source = foundContainer.getItem(i);
            if (source.isEmpty()) continue;

            ItemStack moving = source.copy();
            state.insert(playerId, moving);

            if (moving.isEmpty()) foundContainer.setItem(i, ItemStack.EMPTY);
            else foundContainer.setItem(i, moving);
        }

        foundContainer.setChanged();
    }

    private boolean moveToStorage(ItemStack source) {
        if (serverPlayer == null || source.isEmpty()) return false;

        int before = source.getCount();
        ItemStack moving = source.copy();
        state.insert(playerId, moving);
        int moved = before - moving.getCount();

        if (moved <= 0) return false;
        source.shrink(moved);
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        if (slotIndex < 0 || slotIndex >= slots.size()) return ItemStack.EMPTY;

        Slot slot = slots.get(slotIndex);
        if (!slot.hasItem()) return ItemStack.EMPTY;

        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();

        if (slotIndex < foundSlotCount) {
            if (!moveToStorage(stack)) return ItemStack.EMPTY;
        } else if (slotIndex >= playerSlotStart && slotIndex < playerSlotEnd) {
            if (!moveToStorage(stack)) return ItemStack.EMPTY;
        } else if (slotIndex >= storageSlotStart && slotIndex < storageSlotEnd) {
            if (!moveItemStackTo(stack, playerSlotStart, playerSlotEnd, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();

        if (stack.getCount() == original.getCount()) return ItemStack.EMPTY;

        slot.onTake(player, stack);

        if (serverPlayer != null) {
            state.trimTrailingEmpty(playerId);
            foundContainer.setChanged();
            refreshPage();
        }

        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return foundContainer.stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        foundContainer.stopOpen(player);

        if (serverPlayer != null) {
            state.trimTrailingEmpty(playerId);
        }
    }
}
