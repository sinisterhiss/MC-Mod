package dev.unifiedstorage.client;

import dev.unifiedstorage.UnifiedStorageMod;
import dev.unifiedstorage.screen.UniversalStorageScreen;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screen.ingame.HandledScreens;

public final class UnifiedStorageClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        HandledScreens.register(UnifiedStorageMod.UNIVERSAL_STORAGE_SCREEN, UniversalStorageScreen::new);
    }
}
