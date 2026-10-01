package dev.unifiedstorage.mixin;

import dev.unifiedstorage.storage.UniversalStorageState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.inventory.AbstractCraftingMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Player inventory uses 999-count stacks and exact component equality, including for damaged items.
 */
@Mixin(Inventory.class)
public abstract class PlayerInventoryMixin {
    public int getMaxStackSize() {
        return UniversalStorageState.MAX_STACK_SIZE;
    }

    public int getMaxStackSize(ItemStack stack) {
        return UniversalStorageState.MAX_STACK_SIZE;
    }

    /**
     * While a vanilla crafting menu is open, recipe-book availability counts Universal Storage
     * as an ingredient source. Player inventory still gets consumed first.
     */
    @Inject(method = "fillStackedContents", at = @At("TAIL"))
    private void unifiedstorage$includeStorageInCraftingAvailability(
            StackedItemContents contents,
            CallbackInfo ci
    ) {
        Inventory inventory = (Inventory) (Object) this;
        if (!(inventory.player instanceof ServerPlayer serverPlayer)) return;
        if (!(serverPlayer.containerMenu instanceof AbstractCraftingMenu)) return;

        UniversalStorageState storage = UniversalStorageState.get(serverPlayer);
        for (ItemStack stack : storage.items(serverPlayer.getUUID())) {
            contents.accountSimpleStack(stack);
        }
    }

    @Inject(method = "add(ILnet/minecraft/world/item/ItemStack;)Z", at = @At("HEAD"), cancellable = true)
    private void unifiedstorage$insertExactStacks(
            int requestedSlot,
            ItemStack incoming,
            CallbackInfoReturnable<Boolean> cir
    ) {
        Inventory inventory = (Inventory) (Object) this;

        // Leave unusual equipment-slot targeted insertions to vanilla.
        if (requestedSlot >= Inventory.INVENTORY_SIZE) return;

        if (incoming.isEmpty()) {
            cir.setReturnValue(false);
            return;
        }

        int before = incoming.getCount();

        if (requestedSlot >= 0) {
            moveIntoSlot(inventory, requestedSlot, incoming);
        } else {
            int selected = inventory.getSelectedSlot();
            moveIntoMatchingSlot(inventory, selected, incoming);
            moveIntoMatchingSlot(inventory, Inventory.SLOT_OFFHAND, incoming);

            for (int i = 0; i < Inventory.INVENTORY_SIZE && !incoming.isEmpty(); i++) {
                if (i == selected) continue;
                moveIntoMatchingSlot(inventory, i, incoming);
            }

            while (!incoming.isEmpty()) {
                int empty = inventory.getFreeSlot();
                if (empty < 0) break;

                int moved = Math.min(UniversalStorageState.MAX_STACK_SIZE, incoming.getCount());
                inventory.setItem(empty, incoming.split(moved));
            }
        }

        boolean movedAnything = incoming.getCount() < before;
        if (movedAnything) {
            inventory.setChanged();
        } else if (inventory.player.hasInfiniteMaterials()) {
            incoming.setCount(0);
            movedAnything = true;
        }

        cir.setReturnValue(movedAnything);
    }

    private static void moveIntoMatchingSlot(Inventory inventory, int slot, ItemStack incoming) {
        if (incoming.isEmpty() || slot < 0 || slot >= inventory.getContainerSize()) return;

        ItemStack existing = inventory.getItem(slot);
        if (existing.isEmpty()) return;
        if (!ItemStack.isSameItemSameComponents(existing, incoming)) return;

        int room = UniversalStorageState.MAX_STACK_SIZE - existing.getCount();
        if (room <= 0) return;

        int moved = Math.min(room, incoming.getCount());
        existing.grow(moved);
        incoming.shrink(moved);
    }

    private static void moveIntoSlot(Inventory inventory, int slot, ItemStack incoming) {
        if (incoming.isEmpty() || slot < 0 || slot >= Inventory.INVENTORY_SIZE) return;

        ItemStack existing = inventory.getItem(slot);
        if (existing.isEmpty()) {
            int moved = Math.min(UniversalStorageState.MAX_STACK_SIZE, incoming.getCount());
            inventory.setItem(slot, incoming.split(moved));
            return;
        }

        if (!ItemStack.isSameItemSameComponents(existing, incoming)) return;

        int room = UniversalStorageState.MAX_STACK_SIZE - existing.getCount();
        if (room <= 0) return;

        int moved = Math.min(room, incoming.getCount());
        existing.grow(moved);
        incoming.shrink(moved);
    }
}
