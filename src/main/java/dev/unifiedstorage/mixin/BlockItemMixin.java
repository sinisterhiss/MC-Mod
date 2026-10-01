package dev.unifiedstorage.mixin;

import dev.unifiedstorage.UnifiedStorageMod;
import dev.unifiedstorage.storage.TerminalState;
import net.minecraft.block.BlockState;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Marks only successful player placements as universal-storage access points. */
@Mixin(BlockItem.class)
public abstract class BlockItemMixin {
    @Inject(method = "place(Lnet/minecraft/item/ItemPlacementContext;)Lnet/minecraft/util/ActionResult;", at = @At("RETURN"))
    private void unifiedstorage$markPlacedContainer(ItemPlacementContext context,
                                                    CallbackInfoReturnable<ActionResult> cir) {
        if (!cir.getReturnValue().isAccepted() || context.getWorld().isClient) return;
        if (!(context.getWorld() instanceof ServerWorld serverWorld)) return;
        if (context.getPlayer() == null) return;

        BlockPos pos = context.getBlockPos();
        BlockState placed = serverWorld.getBlockState(pos);
        if (UnifiedStorageMod.isSupportedContainer(placed)) {
            TerminalState.get(serverWorld).mark(pos);
        }
    }
}
