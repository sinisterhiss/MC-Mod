package dev.unifiedstorage.mixin;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keeps vanilla damageable-item enchanting behavior after ItemStack#getMaxCount becomes 999. */
@Mixin(Item.class)
public abstract class ItemMixin {
    @Inject(method = "isEnchantable", at = @At("HEAD"), cancellable = true)
    private void unifiedstorage$preserveEnchantability(ItemStack stack,
                                                        CallbackInfoReturnable<Boolean> cir) {
        // This reproduces vanilla's default intent without depending on max-stack-size == 1.
        cir.setReturnValue(stack.contains(DataComponentTypes.MAX_DAMAGE));
    }
}
