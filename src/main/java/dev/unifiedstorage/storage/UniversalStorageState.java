package dev.unifiedstorage.storage;

import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.PersistentState;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Persistent, effectively unbounded storage, separated by player UUID. */
public final class UniversalStorageState extends PersistentState {
    public static final int MAX_STACK_SIZE = 999;

    private static final String SAVE_ID = "unifiedstorage_items";
    private static final String LARGE_COUNT_KEY = "UnifiedStorageCount";
    private final Map<UUID, List<ItemStack>> byPlayer = new HashMap<>();

    private static final Type<UniversalStorageState> TYPE = new Type<>(
            UniversalStorageState::new,
            UniversalStorageState::fromNbt,
            DataFixTypes.SAVED_DATA
    );

    public static UniversalStorageState get(ServerPlayerEntity player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            throw new IllegalStateException("Universal Storage requested without a server");
        }
        return server.getOverworld().getPersistentStateManager().getOrCreate(TYPE, SAVE_ID);
    }

    public List<ItemStack> items(UUID playerId) {
        return byPlayer.computeIfAbsent(playerId, ignored -> new ArrayList<>());
    }

    public int lastOccupiedIndex(UUID playerId) {
        List<ItemStack> items = items(playerId);
        for (int i = items.size() - 1; i >= 0; i--) {
            if (!items.get(i).isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    public int pageCount(UUID playerId, int pageSize) {
        int last = lastOccupiedIndex(playerId);
        return Math.max(1, (last + pageSize) / pageSize);
    }

    /**
     * Inserts as much as possible into Universal Storage.
     *
     * "Identical" means the same item plus the same complete component data, so two tools
     * only merge if durability, enchantments, custom name, etc. are identical. Unlike vanilla,
     * the item's normal stackability is intentionally ignored here.
     */
    public boolean insert(UUID playerId, ItemStack incoming) {
        if (incoming.isEmpty()) {
            return true;
        }

        List<ItemStack> items = items(playerId);

        // Fill identical partial stacks first, regardless of the item's vanilla stack limit.
        for (ItemStack stored : items) {
            if (incoming.isEmpty()) break;
            if (stored.isEmpty()) continue;
            if (!ItemStack.areItemsAndComponentsEqual(stored, incoming)) continue;

            int room = MAX_STACK_SIZE - stored.getCount();
            if (room <= 0) continue;

            int moved = Math.min(room, incoming.getCount());
            stored.increment(moved);
            incoming.decrement(moved);
        }

        // Reuse empty logical slots, then append new 999-count stacks as necessary.
        while (!incoming.isEmpty()) {
            int moved = Math.min(MAX_STACK_SIZE, incoming.getCount());
            ItemStack split = incoming.copyWithCount(moved);
            incoming.decrement(moved);

            int empty = firstEmpty(items);
            if (empty >= 0) {
                items.set(empty, split);
            } else {
                items.add(split);
            }
        }

        markDirty();
        return true;
    }

    public void sortAndCompact(UUID playerId) {
        List<ItemStack> old = new ArrayList<>();
        for (ItemStack stack : items(playerId)) {
            if (!stack.isEmpty()) old.add(stack.copy());
        }

        List<ItemStack> merged = new ArrayList<>();
        for (ItemStack stack : old) {
            ItemStack working = stack.copy();

            for (ItemStack existing : merged) {
                if (working.isEmpty()) break;
                if (!ItemStack.areItemsAndComponentsEqual(existing, working)) continue;

                int room = MAX_STACK_SIZE - existing.getCount();
                int moved = Math.min(room, working.getCount());
                if (moved > 0) {
                    existing.increment(moved);
                    working.decrement(moved);
                }
            }

            while (!working.isEmpty()) {
                int moved = Math.min(MAX_STACK_SIZE, working.getCount());
                merged.add(working.copyWithCount(moved));
                working.decrement(moved);
            }
        }

        merged.sort(Comparator.comparing(stack -> Registries.ITEM.getId(stack.getItem()).toString()));
        byPlayer.put(playerId, merged);
        markDirty();
    }

    public void trimTrailingEmpty(UUID playerId) {
        List<ItemStack> items = items(playerId);
        int last = lastOccupiedIndex(playerId);
        while (items.size() > last + 1) {
            items.remove(items.size() - 1);
        }
        markDirty();
    }

    private static int firstEmpty(List<ItemStack> items) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).isEmpty()) return i;
        }
        return -1;
    }

    private static UniversalStorageState fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        UniversalStorageState state = new UniversalStorageState();
        NbtCompound players = nbt.getCompound("Players");

        for (String key : players.getKeys()) {
            UUID playerId;
            try {
                playerId = UUID.fromString(key);
            } catch (IllegalArgumentException ignored) {
                continue;
            }

            NbtList list = players.getList(key, NbtElement.COMPOUND_TYPE);
            List<ItemStack> items = new ArrayList<>(list.size());
            for (int i = 0; i < list.size(); i++) {
                NbtCompound encoded = list.getCompound(i);
                if (encoded.isEmpty()) {
                    items.add(ItemStack.EMPTY);
                    continue;
                }

                int largeCount = encoded.contains(LARGE_COUNT_KEY) ? encoded.getInt(LARGE_COUNT_KEY) : -1;
                NbtCompound vanillaStackNbt = encoded.copy();
                vanillaStackNbt.remove(LARGE_COUNT_KEY);

                ItemStack stack = ItemStack.fromNbt(lookup, vanillaStackNbt).orElse(ItemStack.EMPTY);
                if (!stack.isEmpty() && largeCount > 0) {
                    stack.setCount(Math.min(largeCount, MAX_STACK_SIZE));
                }
                items.add(stack);
            }
            state.byPlayer.put(playerId, items);
        }
        return state;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        NbtCompound players = new NbtCompound();

        for (Map.Entry<UUID, List<ItemStack>> entry : byPlayer.entrySet()) {
            NbtList list = new NbtList();
            for (ItemStack stack : entry.getValue()) {
                if (stack.isEmpty()) {
                    list.add(new NbtCompound());
                    continue;
                }

                NbtCompound encoded = (NbtCompound) stack.copyWithCount(1).encode(lookup, new NbtCompound());
                encoded.putInt(LARGE_COUNT_KEY, Math.min(stack.getCount(), MAX_STACK_SIZE));
                list.add(encoded);
            }
            players.put(entry.getKey().toString(), list);
        }

        nbt.put("Players", players);
        return nbt;
    }
}
