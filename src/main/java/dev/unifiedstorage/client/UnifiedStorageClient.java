package dev.unifiedstorage.client;

import dev.unifiedstorage.UnifiedStorageMod;
import dev.unifiedstorage.screen.UniversalStorageScreen;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screens.MenuScreens;

public final class UnifiedStorageClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        MenuScreens.register(UnifiedStorageMod.UNIVERSAL_STORAGE_MENU, UniversalStorageScreen::new);
    }
}
