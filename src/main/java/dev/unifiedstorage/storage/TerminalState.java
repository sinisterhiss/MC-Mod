package dev.unifiedstorage.storage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.unifiedstorage.UnifiedStorageMod;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Tracks player-placed storage terminals for one dimension. */
public final class TerminalState extends SavedData {
    private static final Codec<TerminalState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.LONG.listOf().optionalFieldOf("positions", List.of())
                    .forGetter(state -> new ArrayList<>(state.terminalPositions))
    ).apply(instance, TerminalState::new));

    private static final SavedDataType<TerminalState> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(UnifiedStorageMod.MOD_ID, "terminals"),
            TerminalState::new,
            CODEC,
            DataFixTypes.LEVEL
    );

    private final Set<Long> terminalPositions = new HashSet<>();

    public TerminalState() {
    }

    private TerminalState(List<Long> positions) {
        this.terminalPositions.addAll(positions);
    }

    public static TerminalState get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    public void mark(BlockPos pos) {
        if (terminalPositions.add(pos.asLong())) {
            setDirty();
        }
    }

    public void unmark(BlockPos pos) {
        if (terminalPositions.remove(pos.asLong())) {
            setDirty();
        }
    }

    public boolean isTerminal(BlockPos pos) {
        return terminalPositions.contains(pos.asLong());
    }
}
