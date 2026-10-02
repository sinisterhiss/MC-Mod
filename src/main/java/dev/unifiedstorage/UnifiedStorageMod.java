package dev.unifiedstorage;

import dev.unifiedstorage.net.ModNetworking;
import dev.unifiedstorage.screen.FoundContainerScreenHandler;
import dev.unifiedstorage.screen.StorageTerminalScreenHandler;
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
import net.minecraft.stats.Stats;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.TrappedChestBlock;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.Vec3;

public final class UnifiedStorageMod implements ModInitializer {
    public static final String MOD_ID = "unifiedstorage";

    public static final MenuType<UniversalStorageScreenHandler> UNIVERSAL_STORAGE_MENU =
            Registry.register(
                    BuiltInRegistries.MENU,
                    Identifier.fromNamespaceAndPath(MOD_ID, "universal_storage"),
                    new MenuType<>(UniversalStorageScreenHandler::new, FeatureFlagSet.of())
            );

    public static final MenuType<StorageTerminalScreenHandler> STORAGE_TERMINAL_MENU =
            Registry.register(
                    BuiltInRegistries.MENU,
                    Identifier.fromNamespaceAndPath(MOD_ID, "storage_terminal"),
                    new MenuType<>(StorageTerminalScreenHandler::new, FeatureFlagSet.of())
            );

    public static final MenuType<FoundContainerScreenHandler> FOUND_CONTAINER_3_MENU =
            Registry.register(
                    BuiltInRegistries.MENU,
                    Identifier.fromNamespaceAndPath(MOD_ID, "found_container_3"),
                    new MenuType<>(FoundContainerScreenHandler::threeRowsClient, FeatureFlagSet.of())
            );

    public static final MenuType<FoundContainerScreenHandler> FOUND_CONTAINER_6_MENU =
            Registry.register(
                    BuiltInRegistries.MENU,
                    Identifier.fromNamespaceAndPath(MOD_ID, "found_container_6"),
                    new MenuType<>(FoundContainerScreenHandler::sixRowsClient, FeatureFlagSet.of())
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

            // Player-placed chest/barrel = pure Universal Storage terminal.
            if (TerminalState.get(serverLevel).isTerminal(pos)) {
                openUniversalStorage(serverPlayer);
                return InteractionResult.SUCCESS_SERVER;
            }

            // Natural/found chest/barrel = keep its own loot, but show Universal Storage beside it.
            if (openFoundContainer(serverPlayer, serverLevel, pos, blockState)) {
                return InteractionResult.SUCCESS_SERVER;
            }

            return InteractionResult.PASS;
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
                Component.translatable("container.crafting")
        ));
    }

    public static void openUniversalStorage(ServerPlayer player) {
        player.openMenu(new SimpleMenuProvider(
                (containerId, inventory, openingPlayer) ->
                        new StorageTerminalScreenHandler(containerId, inventory),
                Component.literal("Universal Storage")
        ));
    }

    private static boolean openFoundContainer(
            ServerPlayer player,
            ServerLevel level,
            BlockPos pos,
            BlockState state
    ) {
        Container container;
        int rows;
        Component title;

        if (state.getBlock() instanceof ChestBlock chestBlock) {
            container = ChestBlock.getContainer(chestBlock, state, level, pos, false);
            if (container == null) return false;

            BlockEntity first = level.getBlockEntity(pos);
            if (first instanceof BaseContainerBlockEntity firstContainer && !firstContainer.canOpen(player)) {
                BaseContainerBlockEntity.sendChestLockedNotifications(
                        Vec3.atCenterOf(pos),
                        player,
                        firstContainer.getDisplayName()
                );
                return true;
            }

            if (first instanceof RandomizableContainer randomizable) {
                randomizable.unpackLootTable(player);
            }

            if (state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
                BlockPos secondPos = ChestBlock.getConnectedBlockPos(pos, state);
                BlockEntity second = level.getBlockEntity(secondPos);

                if (second instanceof BaseContainerBlockEntity secondContainer && !secondContainer.canOpen(player)) {
                    BaseContainerBlockEntity.sendChestLockedNotifications(
                            Vec3.atCenterOf(secondPos),
                            player,
                            secondContainer.getDisplayName()
                    );
                    return true;
                }

                if (second instanceof RandomizableContainer randomizable) {
                    randomizable.unpackLootTable(player);
                }
            }

            rows = container.getContainerSize() / 9;
            title = rows == 6
                    ? Component.translatable("container.chestDouble")
                    : first instanceof BaseContainerBlockEntity base
                    ? base.getDisplayName()
                    : Component.translatable("container.chest");

            if (state.getBlock() instanceof TrappedChestBlock) {
                player.awardStat(Stats.TRIGGER_TRAPPED_CHEST);
            } else {
                player.awardStat(Stats.OPEN_CHEST);
            }
        } else if (state.getBlock() instanceof BarrelBlock
                && level.getBlockEntity(pos) instanceof BaseContainerBlockEntity barrel) {
            if (!barrel.canOpen(player)) {
                BaseContainerBlockEntity.sendChestLockedNotifications(
                        Vec3.atCenterOf(pos),
                        player,
                        barrel.getDisplayName()
                );
                return true;
            }

            if (barrel instanceof RandomizableContainer randomizable) {
                randomizable.unpackLootTable(player);
            }

            container = barrel;
            rows = 3;
            title = barrel.getDisplayName();
            player.awardStat(Stats.OPEN_BARREL);
        } else {
            return false;
        }

        MenuType<?> menuType = rows == 6 ? FOUND_CONTAINER_6_MENU : FOUND_CONTAINER_3_MENU;
        int finalRows = rows;
        Container finalContainer = container;

        player.openMenu(new SimpleMenuProvider(
                (containerId, inventory, openingPlayer) ->
                        new FoundContainerScreenHandler(
                                menuType,
                                containerId,
                                inventory,
                                finalContainer,
                                finalRows
                        ),
                title
        ));

        PiglinAi.angerNearbyPiglins(level, player, true);
        return true;
    }

    public static boolean isSupportedContainer(BlockState state) {
        return state.is(Blocks.CHEST) || state.is(Blocks.TRAPPED_CHEST) || state.is(Blocks.BARREL);
    }
}
