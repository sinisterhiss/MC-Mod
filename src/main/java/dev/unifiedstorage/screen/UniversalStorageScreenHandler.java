package dev.unifiedstorage.screen;

import dev.unifiedstorage.UnifiedStorageMod;
import dev.unifiedstorage.storage.PagedStorageInventory;
import dev.unifiedstorage.storage.UniversalStorageClientInventory;
import dev.unifiedstorage.storage.UniversalStorageState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;
import java.util.UUID;

public final class UniversalStorageScreenHandler extends ScreenHandler {
    public static final int BUTTON_PREVIOUS = 0;
    public static final int BUTTON_NEXT = 1;
    public static final int BUTTON_SORT = 2;
    public static final int BUTTON_DEPOSIT_ALL = 3;
    public static final int BUTTON_LOOT_ALL = 4;

    private static final int STORAGE_SLOT_COUNT = 54;

    private final PlayerInventory playerInventory;
    private final Inventory storageInventory;
    private final PropertyDelegate properties;
    private final ServerPlayerEntity serverPlayer;
    private final UniversalStorageState state;
    private final UUID playerId;

    /** Client constructor and server constructor used by the registered ScreenHandlerType. */
    public UniversalStorageScreenHandler(int syncId, PlayerInventory playerInventory) {
        super(UnifiedStorageMod.UNIVERSAL_STORAGE_SCREEN, syncId);
        this.playerInventory = playerInventory;

        if (playerInventory.player instanceof ServerPlayerEntity sp) {
            this.serverPlayer = sp;
            this.state = UniversalStorageState.get(sp);
            this.playerId = sp.getUuid();
            this.storageInventory = new PagedStorageInventory(state, playerId);
        } else {
            this.serverPlayer = null;
            this.state = null;
            this.playerId = null;
            this.storageInventory = new UniversalStorageClientInventory(STORAGE_SLOT_COUNT);
        }

        this.properties = new ArrayPropertyDelegate(2);
        this.properties.set(0, 0);
        this.properties.set(1, serverPlayer == null ? 1 : state.pageCount(playerId, STORAGE_SLOT_COUNT));
        addProperties(this.properties);

        // Universal storage: same coordinates as a vanilla 6-row chest.
        for (int row = 0; row < 6; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new UniversalStorageSlot(storageInventory, col + row * 9, 8 + col * 18, 18 + row * 18));
            }
        }

        // Player inventory uses the mod-wide 999-count stacking rules.
        int offset = (6 - 4) * 18;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 103 + offset + row * 18));
            }
        }

        // Hotbar.
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, 161 + offset));
        }
    }

    public int getPage() {
        return properties.get(0);
    }

    public int getPageCount() {
        return Math.max(1, properties.get(1));
    }

    private void refreshPageData() {
        if (serverPlayer == null) return;
        int max = state.pageCount(playerId, STORAGE_SLOT_COUNT);
        properties.set(1, max);
        int page = Math.min(properties.get(0), max - 1);
        properties.set(0, page);
        if (storageInventory instanceof PagedStorageInventory paged) {
            paged.setPage(page);
        }
        sendContentUpdates();
    }

    @Override
    public boolean onButtonClick(PlayerEntity player, int id) {
        if (!(player instanceof ServerPlayerEntity)) {
            return false;
        }

        switch (id) {
            case BUTTON_PREVIOUS -> {
                properties.set(0, Math.max(0, properties.get(0) - 1));
                refreshPageData();
                return true;
            }
            case BUTTON_NEXT -> {
                properties.set(0, Math.min(getPageCount() - 1, properties.get(0) + 1));
                refreshPageData();
                return true;
            }
            case BUTTON_SORT -> {
                state.sortAndCompact(playerId);
                properties.set(0, 0);
                refreshPageData();
                return true;
            }
            case BUTTON_DEPOSIT_ALL -> {
                depositAll();
                refreshPageData();
                return true;
            }
            case BUTTON_LOOT_ALL -> {
                lootAll();
                refreshPageData();
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    private void depositAll() {
        // Main inventory + hotbar only. Armor and offhand remain untouched.
        for (int i = 0; i < 36; i++) {
            ItemStack playerStack = playerInventory.getStack(i);
            if (playerStack.isEmpty()) continue;
            ItemStack moving = playerStack.copy();
            state.insert(playerId, moving);
            if (moving.isEmpty()) {
                playerInventory.setStack(i, ItemStack.EMPTY);
            } else {
                playerInventory.setStack(i, moving);
            }
        }
        playerInventory.markDirty();
    }

    private void lootAll() {
        List<ItemStack> stored = state.items(playerId);
        for (int i = 0; i < stored.size(); i++) {
            ItemStack source = stored.get(i);
            if (source.isEmpty()) continue;

            moveIntoPlayerInventory(source);
            if (source.isEmpty()) {
                stored.set(i, ItemStack.EMPTY);
            }
        }
        state.trimTrailingEmpty(playerId);
        playerInventory.markDirty();
    }

    /** Moves as much as possible into the player's 999-count inventory stacks. */
    private int moveIntoPlayerInventory(ItemStack source) {
        if (source.isEmpty()) return 0;
        int before = source.getCount();
        playerInventory.insertStack(source);
        return before - source.getCount();
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slotIndex) {
        if (slotIndex < 0 || slotIndex >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(slotIndex);
        if (!slot.hasStack()) return ItemStack.EMPTY;

        ItemStack source = slot.getStack();
        ItemStack original = source.copy();

        if (slotIndex < STORAGE_SLOT_COUNT) {
            int moved = moveIntoPlayerInventory(source);
            if (moved <= 0) return ItemStack.EMPTY;

            if (source.isEmpty()) slot.setStack(ItemStack.EMPTY);
            slot.markDirty();
        } else if (serverPlayer != null) {
            ItemStack moving = source.copy();
            state.insert(playerId, moving);
            if (!moving.isEmpty()) return ItemStack.EMPTY;
            slot.setStack(ItemStack.EMPTY);
            slot.markDirty();
            refreshPageData();
        }

        return original;
    }

    @Override
    public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
        // 999-count stacks are legal in both Universal Storage and the player inventory now,
        // so vanilla click/number-key behavior can handle transfers directly.
        super.onSlotClick(slotIndex, button, actionType, player);
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        // A placed chest/barrel is only an access point. Closing/breaking/moving it does not own the data.
        return true;
    }

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        if (serverPlayer != null) {
            state.trimTrailingEmpty(playerId);
        }
    }
}
