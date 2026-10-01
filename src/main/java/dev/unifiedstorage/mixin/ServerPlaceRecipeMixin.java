package dev.unifiedstorage.mixin;

import dev.unifiedstorage.storage.UniversalStorageState;
import net.minecraft.core.Holder;
import net.minecraft.recipebook.ServerPlaceRecipe;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * If vanilla recipe placement cannot find an ingredient in the player's normal inventory,
 * pull the same ingredient from Universal Storage instead.
 */
@Mixin(ServerPlaceRecipe.class)
public abstract class ServerPlaceRecipeMixin {
    @Shadow @Final private Inventory inventory;

    @Inject(method = "moveItemToGrid", at = @At("RETURN"), cancellable = true)
    private void unifiedstorage$pullMissingIngredientFromStorage(
            Slot targetSlot,
            Holder<Item> itemInInventory,
            int count,
            CallbackInfoReturnable<Integer> cir
    ) {
        if (cir.getReturnValue() != -1) return;
        if (!(this.inventory.player instanceof ServerPlayer serverPlayer)) return;

        ItemStack existing = targetSlot.getItem();
        ItemStack taken = UniversalStorageState.get(serverPlayer).takeForCrafting(
                serverPlayer.getUUID(),
                itemInInventory,
                existing,
                count
        );

        if (taken.isEmpty()) return;

        int takenCount = taken.getCount();
        if (existing.isEmpty()) {
            targetSlot.set(taken);
        } else {
            existing.grow(takenCount);
            targetSlot.setChanged();
        }

        cir.setReturnValue(count - takenCount);
    }
}
