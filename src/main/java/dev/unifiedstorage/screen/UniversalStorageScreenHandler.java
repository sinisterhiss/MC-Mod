package dev.unifiedstorage.screen;

import dev.unifiedstorage.UnifiedStorageMod;
import dev.unifiedstorage.storage.PagedStorageInventory;
import dev.unifiedstorage.storage.UniversalStorageClientInventory;
import dev.unifiedstorage.storage.UniversalStorageState;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.inventory.AbstractCraftingMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.RecipeBookType;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class UniversalStorageScreenHandler extends AbstractCraftingMenu {
    public static final int BUTTON_PREVIOUS = 0;
    public static final int BUTTON_NEXT = 1;
    public static final int BUTTON_SORT = 2;
    public static final int BUTTON_DEPOSIT_ALL = 3;
    public static final int BUTTON_LOOT_ALL = 4;

    private static final int RESULT_SLOT = 0;
    private static final int CRAFT_SLOT_START = 1;
    private static final int CRAFT_SLOT_END = 10;
    private static final int STORAGE_SLOT_START = 10;
    private static final int STORAGE_SLOT_COUNT = 54;
    private static final int STORAGE_SLOT_END = STORAGE_SLOT_START + STORAGE_SLOT_COUNT;
    private static final int PLAYER_SLOT_START = STORAGE_SLOT_END;
    private static final int PLAYER_SLOT_END = PLAYER_SLOT_START + 36;

    private static final int STORAGE_X = 176;

    private final Inventory playerInventory;
    private final Container storageInventory;
    private final ContainerData data = new SimpleContainerData(2);

    private final ServerPlayer serverPlayer;
    private final UniversalStorageState state;
    private final UUID playerId;

    private boolean placingRecipe;

    public UniversalStorageScreenHandler(int containerId, Inventory playerInventory) {
        super(UnifiedStorageMod.UNIVERSAL_STORAGE_MENU, containerId, 3, 3);
        this.playerInventory = playerInventory;

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

        // Vanilla crafting-table layout in the normal 176px panel.
        addResultSlot(playerInventory.player, 124, 35);
        addCraftingGridSlots(30, 17);
        addStandardInventorySlots(playerInventory, 8, 84);

        // Universal Storage lives immediately to the right of the normal crafting panel.
        for (int row = 0; row < 6; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new UniversalStorageSlot(
                        storageInventory,
                        col + row * 9,
                        STORAGE_X + 8 + col * 18,
                        18 + row * 18
                ));
            }
        }
    }

    public int getPage() {
        return data.get(0);
    }

    public int getPageCount() {
        return Math.max(1, data.get(1));
    }

    @Override
    public void fillCraftSlotsStackedContents(StackedItemContents contents) {
        super.fillCraftSlotsStackedContents(contents);

        // Make the vanilla recipe book count Universal Storage as part of the available supply.
        if (state != null && playerId != null) {
            for (ItemStack stack : state.items(playerId)) {
                contents.accountSimpleStack(stack);
            }
        }
    }

    @Override
    public void slotsChanged(Container container) {
        if (!placingRecipe && playerInventory.player.level() instanceof ServerLevel level) {
            updateCraftingResult(this, level, playerInventory.player, craftSlots, resultSlots, null);
        }
    }

    private static void updateCraftingResult(
            AbstractCraftingMenu menu,
            ServerLevel level,
            Player player,
            CraftingContainer container,
            ResultContainer resultSlots,
            @Nullable RecipeHolder<CraftingRecipe> recipeHint
    ) {
        CraftingInput input = container.asCraftInput();
        ServerPlayer serverPlayer = (ServerPlayer) player;
        ItemStack result = ItemStack.EMPTY;

        Optional<RecipeHolder<CraftingRecipe>> maybeRecipe =
                level.getServer().getRecipeManager().getRecipeFor(
                        RecipeType.CRAFTING,
                        input,
                        level,
                        recipeHint
                );

        if (maybeRecipe.isPresent()) {
            RecipeHolder<CraftingRecipe> recipeHolder = maybeRecipe.get();
            CraftingRecipe recipe = recipeHolder.value();
            if (resultSlots.setRecipeUsed(serverPlayer, recipeHolder)) {
                ItemStack assembled = recipe.assemble(input);
                if (assembled.isItemEnabled(level.enabledFeatures())) {
                    result = assembled;
                }
            }
        }

        resultSlots.setItem(0, result);
        menu.setRemoteSlot(0, result);
        serverPlayer.connection.send(new ClientboundContainerSetSlotPacket(
                menu.containerId,
                menu.incrementStateId(),
                0,
                result
        ));
    }

    @Override
    protected void beginPlacingRecipe() {
        placingRecipe = true;
    }

    @Override
    protected void finishPlacingRecipe(ServerLevel level, RecipeHolder<CraftingRecipe> recipe) {
        placingRecipe = false;
        updateCraftingResult(this, level, playerInventory.player, craftSlots, resultSlots, recipe);
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
                lootAll();
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

    private void lootAll() {
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

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        if (slotIndex < 0 || slotIndex >= slots.size()) return ItemStack.EMPTY;

        Slot slot = slots.get(slotIndex);
        if (!slot.hasItem()) return ItemStack.EMPTY;

        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();

        if (slotIndex == RESULT_SLOT) {
            stack.getItem().onCraftedBy(stack, player);
            moveItemStackTo(stack, PLAYER_SLOT_START, PLAYER_SLOT_END, true);

            if (!stack.isEmpty() && serverPlayer != null) {
                state.insert(playerId, stack);
            }
            if (!stack.isEmpty()) return ItemStack.EMPTY;

            slot.onQuickCraft(stack, original);
        } else if (slotIndex >= CRAFT_SLOT_START && slotIndex < CRAFT_SLOT_END) {
            moveItemStackTo(stack, PLAYER_SLOT_START, PLAYER_SLOT_END, false);

            if (!stack.isEmpty() && serverPlayer != null) {
                state.insert(playerId, stack);
            }
            if (!stack.isEmpty()) return ItemStack.EMPTY;
        } else if (slotIndex >= STORAGE_SLOT_START && slotIndex < STORAGE_SLOT_END) {
            if (!moveItemStackTo(stack, PLAYER_SLOT_START, PLAYER_SLOT_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (slotIndex >= PLAYER_SLOT_START && slotIndex < PLAYER_SLOT_END) {
            if (serverPlayer == null) return ItemStack.EMPTY;

            ItemStack moving = stack.copy();
            state.insert(playerId, moving);
            if (!moving.isEmpty()) return ItemStack.EMPTY;

            stack.setCount(0);
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        if (stack.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }

        slot.onTake(player, stack);

        if (serverPlayer != null) {
            state.trimTrailingEmpty(playerId);
            refreshPage();
        }

        return original;
    }

    @Override
    public Slot getResultSlot() {
        return slots.get(RESULT_SLOT);
    }

    @Override
    public List<Slot> getInputGridSlots() {
        return slots.subList(CRAFT_SLOT_START, CRAFT_SLOT_END);
    }

    @Override
    public RecipeBookType getRecipeBookType() {
        return RecipeBookType.CRAFTING;
    }

    @Override
    protected Player owner() {
        return playerInventory.player;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);

        if (player instanceof ServerPlayer) {
            for (int i = 0; i < craftSlots.getContainerSize(); i++) {
                ItemStack stack = craftSlots.removeItemNoUpdate(i);
                if (stack.isEmpty()) continue;

                playerInventory.add(stack);
                if (!stack.isEmpty()) {
                    state.insert(playerId, stack);
                }
            }

            resultSlots.clearContent();
            state.trimTrailingEmpty(playerId);
            playerInventory.setChanged();
        } else {
            craftSlots.clearContent();
            resultSlots.clearContent();
        }
    }
}
