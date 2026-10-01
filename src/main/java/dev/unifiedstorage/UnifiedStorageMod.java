package dev.unifiedstorage;

import dev.unifiedstorage.net.ModNetworking;
import dev.unifiedstorage.screen.UniversalStorageScreenHandler;
import dev.unifiedstorage.storage.TerminalState;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class UnifiedStorageMod implements ModInitializer {
    public static final String MOD_ID = "unifiedstorage";

    public static final MenuType<UniversalStorageScreenHandler> UNIVERSAL_STORAGE_MENU =
            Registry.register(
                    BuiltInRegistries.MENU,
                    Identifier.fromNamespaceAndPath(MOD_ID, "universal_storage"),
                    new MenuType<>(UniversalStorageScreenHandler::new, FeatureFlagSet.of())
            );

    @Override
    public void onInitialize() {
        ModNetworking.init();

        UseBlockCallback.EVENT.register((player, level, hand, hitResult) -> {
            if (level.isClientSide() || hand != InteractionHand.MAIN_HAND || !(player instanceof ServerPlayer serverPlayer)) {
                return InteractionResult.PASS;
            }

            BlockPos pos = hitResult.getBlockPos();
            BlockState blockState = level.getBlockState(pos);

            if (blockState.is(Blocks.CRAFTING_TABLE)) {
                openUnifiedCrafting(serverPlayer);
                return InteractionResult.SUCCESS_SERVER;
            }

            if (!isSupportedContainer(blockState)) {
                return InteractionResult.PASS;
            }

            ServerLevel serverLevel = (ServerLevel) level;
            if (!TerminalState.get(serverLevel).isTerminal(pos)) {
                return InteractionResult.PASS;
            }

            openUnifiedCrafting(serverPlayer);
            return InteractionResult.SUCCESS_SERVER;
        });

        PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> {
            if (level instanceof ServerLevel serverLevel && isSupportedContainer(state)) {
                TerminalState.get(serverLevel).unmark(pos);
            }
        });
    }

    public static void openUnifiedCrafting(ServerPlayer player) {
        player.openMenu(new SimpleMenuProvider(
                (containerId, inventory, openingPlayer) ->
                        new UniversalStorageScreenHandler(containerId, inventory),
                Component.literal("Universal Storage + Crafting")
        ));
    }

    public static boolean isSupportedContainer(BlockState state) {
        return state.is(Blocks.CHEST) || state.is(Blocks.TRAPPED_CHEST) || state.is(Blocks.BARREL);
    }
}
