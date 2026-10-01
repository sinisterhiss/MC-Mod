package dev.unifiedstorage.storage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.unifiedstorage.UnifiedStorageMod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Global, persistent, effectively unbounded storage separated by player UUID. */
public final class UniversalStorageState extends SavedData {
    public static final int MAX_STACK_SIZE = 999;

    private static final Codec<Map<String, List<ItemStack>>> PLAYERS_CODEC =
            Codec.unboundedMap(Codec.STRING, ItemStack.CODEC.listOf());

    private static final Codec<UniversalStorageState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            PLAYERS_CODEC.optionalFieldOf("players", Map.<String, List<ItemStack>>of())
                    .forGetter(UniversalStorageState::serializedPlayers)
    ).apply(instance, UniversalStorageState::new));

    private static final SavedDataType<UniversalStorageState> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(UnifiedStorageMod.MOD_ID, "items"),
            UniversalStorageState::new,
            CODEC,
            DataFixTypes.LEVEL
    );

    private final Map<UUID, List<ItemStack>> byPlayer = new HashMap<>();

    public UniversalStorageState() {
    }

    private UniversalStorageState(Map<String, List<ItemStack>> serialized) {
        for (Map.Entry<String, List<ItemStack>> entry : serialized.entrySet()) {
            try {
                UUID id = UUID.fromString(entry.getKey());
                List<ItemStack> stacks = new ArrayList<>();
                for (ItemStack stack : entry.getValue()) {
                    if (!stack.isEmpty()) {
                        stacks.add(stack.copy());
                    }
                }
                byPlayer.put(id, stacks);
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    public static UniversalStorageState get(ServerPlayer player) {
        return player.level().getServer().getDataStorage().computeIfAbsent(TYPE);
    }

    public List<ItemStack> items(UUID playerId) {
        return byPlayer.computeIfAbsent(playerId, ignored -> new ArrayList<>());
    }

    public int lastOccupiedIndex(UUID playerId) {
        List<ItemStack> items = items(playerId);
        for (int i = items.size() - 1; i >= 0; i--) {
            if (!items.get(i).isEmpty()) return i;
        }
        return -1;
    }

    public int pageCount(UUID playerId, int pageSize) {
        int last = lastOccupiedIndex(playerId);
        return Math.max(1, (last + pageSize) / pageSize);
    }

    /**
     * Exact identity is ItemStack's item + complete component set. Count is ignored.
     * Different durability, enchantments, names, potion data, etc. therefore never merge.
     */
    public boolean insert(UUID playerId, ItemStack incoming) {
        if (incoming.isEmpty()) return true;

        List<ItemStack> items = items(playerId);

        for (ItemStack stored : items) {
            if (incoming.isEmpty()) break;
            if (stored.isEmpty()) continue;
            if (!ItemStack.isSameItemSameComponents(stored, incoming)) continue;

            int room = MAX_STACK_SIZE - stored.getCount();
            if (room <= 0) continue;

            int moved = Math.min(room, incoming.getCount());
            stored.grow(moved);
            incoming.shrink(moved);
        }

        while (!incoming.isEmpty()) {
            int moved = Math.min(MAX_STACK_SIZE, incoming.getCount());
            ItemStack split = incoming.copyWithCount(moved);
            incoming.shrink(moved);

            int empty = firstEmpty(items);
            if (empty >= 0) items.set(empty, split);
            else items.add(split);
        }

        setDirty();
        return true;
    }

    public void sortAndCompact(UUID playerId) {
        List<ItemStack> merged = new ArrayList<>();

        for (ItemStack source : items(playerId)) {
            if (source.isEmpty()) continue;
            ItemStack working = source.copy();

            for (ItemStack existing : merged) {
                if (working.isEmpty()) break;
                if (!ItemStack.isSameItemSameComponents(existing, working)) continue;

                int room = MAX_STACK_SIZE - existing.getCount();
                if (room <= 0) continue;

                int moved = Math.min(room, working.getCount());
                existing.grow(moved);
                working.shrink(moved);
            }

            while (!working.isEmpty()) {
                int moved = Math.min(MAX_STACK_SIZE, working.getCount());
                merged.add(working.copyWithCount(moved));
                working.shrink(moved);
            }
        }

        merged.sort(Comparator.comparing(stack ->
                BuiltInRegistries.ITEM.getKey(stack.getItem()).toString()));
        byPlayer.put(playerId, merged);
        setDirty();
    }

    public void trimTrailingEmpty(UUID playerId) {
        List<ItemStack> items = items(playerId);
        int last = lastOccupiedIndex(playerId);
        while (items.size() > last + 1) {
            items.remove(items.size() - 1);
        }
        setDirty();
    }

    private Map<String, List<ItemStack>> serializedPlayers() {
        Map<String, List<ItemStack>> serialized = new LinkedHashMap<>();
        for (Map.Entry<UUID, List<ItemStack>> entry : byPlayer.entrySet()) {
            List<ItemStack> stacks = new ArrayList<>();
            for (ItemStack stack : entry.getValue()) {
                if (!stack.isEmpty()) stacks.add(stack.copy());
            }
            serialized.put(entry.getKey().toString(), stacks);
        }
        return serialized;
    }

    private static int firstEmpty(List<ItemStack> items) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).isEmpty()) return i;
        }
        return -1;
    }
}
