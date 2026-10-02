package dev.unifiedstorage.client;

import dev.unifiedstorage.UnifiedStorageMod;
import dev.unifiedstorage.net.OpenUnifiedCraftingPayload;
import dev.unifiedstorage.screen.FoundContainerScreen;
import dev.unifiedstorage.screen.StorageTerminalScreen;
import dev.unifiedstorage.screen.UniversalStorageScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;

public final class UnifiedStorageClient implements ClientModInitializer {
    private boolean vanillaInventoryWasOpen;

    @Override
    public void onInitializeClient() {
        MenuScreens.register(UnifiedStorageMod.UNIVERSAL_STORAGE_MENU, UniversalStorageScreen::new);
        MenuScreens.register(UnifiedStorageMod.STORAGE_TERMINAL_MENU, StorageTerminalScreen::new);
        MenuScreens.register(UnifiedStorageMod.FOUND_CONTAINER_3_MENU, FoundContainerScreen::new);
        MenuScreens.register(UnifiedStorageMod.FOUND_CONTAINER_6_MENU, FoundContainerScreen::new);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            boolean vanillaInventoryOpen =
                    client.gui.screen() instanceof InventoryScreen
                            || client.gui.screen() instanceof CreativeModeInventoryScreen;

            if (vanillaInventoryOpen
                    && !vanillaInventoryWasOpen
                    && client.getConnection() != null
                    && ClientPlayNetworking.canSend(OpenUnifiedCraftingPayload.TYPE)) {
                ClientPlayNetworking.send(new OpenUnifiedCraftingPayload());
            }

            vanillaInventoryWasOpen = vanillaInventoryOpen;
        });
    }
}
