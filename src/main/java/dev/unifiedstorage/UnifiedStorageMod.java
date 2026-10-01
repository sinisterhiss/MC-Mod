package dev.unifiedstorage;

import dev.unifiedstorage.screen.UniversalStorageScreenHandler;
import dev.unifiedstorage.storage.TerminalState;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public final class UnifiedStorageMod implements ModInitializer {
    public static final String MOD_ID = "unifiedstorage";

    public static final ScreenHandlerType<UniversalStorageScreenHandler> UNIVERSAL_STORAGE_SCREEN =
            Registry.register(
                    Registries.SCREEN_HANDLER,
                    Identifier.of(MOD_ID, "universal_storage"),
                    new ScreenHandlerType<>(UniversalStorageScreenHandler::new, FeatureSet.empty())
            );

    @Override
    public void onInitialize() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClient || hand != Hand.MAIN_HAND || !(player instanceof ServerPlayerEntity serverPlayer)) {
                return ActionResult.PASS;
            }

            BlockPos pos = hitResult.getBlockPos();
            BlockState state = world.getBlockState(pos);
            if (!isSupportedContainer(state)) {
                return ActionResult.PASS;
            }

            ServerWorld serverWorld = (ServerWorld) world;
            if (!TerminalState.get(serverWorld).isTerminal(pos)) {
                return ActionResult.PASS;
            }

            serverPlayer.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                    (syncId, playerInventory, openingPlayer) -> new UniversalStorageScreenHandler(syncId, playerInventory),
                    Text.literal("Universal Storage")
            ));
            return ActionResult.SUCCESS;
        });

        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (isSupportedContainer(state) && world instanceof ServerWorld serverWorld) {
                TerminalState.get(serverWorld).unmark(pos);
            }
        });
    }

    public static boolean isSupportedContainer(BlockState state) {
        return state.isOf(Blocks.CHEST) || state.isOf(Blocks.TRAPPED_CHEST) || state.isOf(Blocks.BARREL);
    }

    public static boolean isSupportedContainerItem(net.minecraft.item.ItemStack stack) {
        return stack.isOf(Blocks.CHEST.asItem()) || stack.isOf(Blocks.TRAPPED_CHEST.asItem()) || stack.isOf(Blocks.BARREL.asItem());
    }
}
