package dev.unifiedstorage.net;

import dev.unifiedstorage.UnifiedStorageMod;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public final class ModNetworking {
    private ModNetworking() {
    }

    public static void init() {
        PayloadTypeRegistry.serverboundPlay().register(
                OpenUnifiedCraftingPayload.TYPE,
                OpenUnifiedCraftingPayload.CODEC
        );

        ServerPlayNetworking.registerGlobalReceiver(
                OpenUnifiedCraftingPayload.TYPE,
                (payload, context) -> UnifiedStorageMod.openUnifiedCrafting(context.player())
        );
    }
}
