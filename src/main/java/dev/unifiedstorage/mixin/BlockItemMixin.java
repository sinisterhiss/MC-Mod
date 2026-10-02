package dev.unifiedstorage.mixin;

import dev.unifiedstorage.UnifiedStorageMod;
import dev.unifiedstorage.storage.TerminalState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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

        // For ordinary placements getClickedPos is already the placed position. A few blocks can
        // update the placement context internally, so also check the adjacent face position.
        BlockPos clicked = context.getClickedPos();
        if (markIfSupported(serverLevel, clicked)) return;

        Direction face = context.getClickedFace();
        markIfSupported(serverLevel, clicked.relative(face));
    }

    private static boolean markIfSupported(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!UnifiedStorageMod.isSupportedContainer(state)) {
            return false;
        }

        TerminalState.get(level).mark(pos);
        return true;
    }
}
