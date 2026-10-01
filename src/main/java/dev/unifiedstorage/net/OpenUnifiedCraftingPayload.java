package dev.unifiedstorage.net;

import dev.unifiedstorage.UnifiedStorageMod;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record OpenUnifiedCraftingPayload() implements CustomPacketPayload {
    public static final Type<OpenUnifiedCraftingPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(UnifiedStorageMod.MOD_ID, "open_unified_crafting"));

    public static final StreamCodec<Object, OpenUnifiedCraftingPayload> CODEC =
            StreamCodec.unit(new OpenUnifiedCraftingPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
