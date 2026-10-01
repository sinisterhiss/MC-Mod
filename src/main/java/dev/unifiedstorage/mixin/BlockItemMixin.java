package dev.unifiedstorage.mixin;

import dev.unifiedstorage.UnifiedStorageMod;
import dev.unifiedstorage.storage.TerminalState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Marks only containers successfully placed by a player. */
@Mixin(BlockItem.class)
public abstract class BlockItemMixin {
    @Inject(method = "place", at = @At("RETURN"))
    private void unifiedstorage$markPlacedContainer(
            BlockPlaceContext context,
            CallbackInfoReturnable<InteractionResult> cir
    ) {
        if (!cir.getReturnValue().consumesAction()) return;
        if (!(context.getLevel() instanceof ServerLevel serverLevel)) return;
        if (context.getPlayer() == null) return;

        BlockPos pos = context.getClickedPos();
        BlockState placed = serverLevel.getBlockState(pos);
        if (UnifiedStorageMod.isSupportedContainer(placed)) {
            TerminalState.get(serverLevel).mark(pos);
        }
    }
}
