package dev.unifiedstorage.storage;

import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;

import java.util.HashSet;
import java.util.Set;

/**
 * Tracks only containers placed by players. Naturally generated/found containers never enter this set.
 * The state is dimension-local, so identical coordinates in different dimensions are independent.
 */
public final class TerminalState extends PersistentState {
    private static final String SAVE_ID = "unifiedstorage_terminals";
    private final Set<Long> terminalPositions = new HashSet<>();

    private static final Type<TerminalState> TYPE = new Type<>(
            TerminalState::new,
            TerminalState::fromNbt,
            DataFixTypes.SAVED_DATA
    );

    public static TerminalState get(ServerWorld world) {
        return world.getPersistentStateManager().getOrCreate(TYPE, SAVE_ID);
    }

    public void mark(BlockPos pos) {
        if (terminalPositions.add(pos.asLong())) {
            markDirty();
        }
    }

    public void unmark(BlockPos pos) {
        if (terminalPositions.remove(pos.asLong())) {
            markDirty();
        }
    }

    public boolean isTerminal(BlockPos pos) {
        return terminalPositions.contains(pos.asLong());
    }

    private static TerminalState fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        TerminalState state = new TerminalState();
        for (long packed : nbt.getLongArray("Positions")) {
            state.terminalPositions.add(packed);
        }
        return state;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        long[] positions = new long[terminalPositions.size()];
        int i = 0;
        for (long packed : terminalPositions) {
            positions[i++] = packed;
        }
        nbt.putLongArray("Positions", positions);
        return nbt;
    }
}
