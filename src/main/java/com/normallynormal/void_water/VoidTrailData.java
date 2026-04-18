package com.normallynormal.void_water;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VoidTrailData extends SavedData {
    private final Map<Long, Integer> trailLengths = new HashMap<>();
    private final Map<Long, Integer> growthTicks = new HashMap<>();

    private record TrailEntry(long pos, int len) {
        static final Codec<TrailEntry> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.LONG.fieldOf("pos").forGetter(TrailEntry::pos),
            Codec.INT.fieldOf("len").forGetter(TrailEntry::len)
        ).apply(inst, TrailEntry::new));
    }

    private static final Codec<VoidTrailData> CODEC = TrailEntry.CODEC.listOf().xmap(
        list -> {
            VoidTrailData data = new VoidTrailData();
            list.forEach(e -> data.trailLengths.put(e.pos(), Math.min(e.len(), Config.trailLength)));
            return data;
        },
        data -> data.trailLengths.entrySet().stream()
            .map(e -> new TrailEntry(e.getKey(), e.getValue()))
            .toList()
    );

    public static final SavedDataType<VoidTrailData> TYPE = new SavedDataType<>(
        "void_water_trails",
        VoidTrailData::new,
        CODEC,
        DataFixTypes.LEVEL
    );

    public static long pack(int x, int z) {
        return (long) x << 32 | (z & 0xFFFFFFFFL);
    }

    public static int unpackX(long packed) {
        return (int) (packed >> 32);
    }

    public static int unpackZ(long packed) {
        return (int) packed;
    }

    public Map<Long, Integer> getTrailLengths() {
        return trailLengths;
    }

    public List<Long> getTrackedPositions() {
        return new ArrayList<>(trailLengths.keySet());
    }

    public void addPosition(long packed) {
        if (!trailLengths.containsKey(packed)) {
            trailLengths.put(packed, 0);
            setDirty();
        }
    }

    /** Returns the new length if it changed this tick, or -1 if unchanged. */
    public int tick(long packed, boolean fluidPresent, int tickDelay) {
        if (!trailLengths.containsKey(packed)) return -1;
        int length = trailLengths.get(packed);

        if (fluidPresent) {
            if (length >= Config.trailLength) return -1;
            int ticks = growthTicks.getOrDefault(packed, 0) + 1;
            if (ticks >= tickDelay) {
                growthTicks.put(packed, 0);
                trailLengths.put(packed, length + 1);
                setDirty();
                return length + 1;
            }
            growthTicks.put(packed, ticks);
            return -1;
        } else {
            growthTicks.remove(packed);
            trailLengths.remove(packed);
            setDirty();
            return 0;
        }
    }

    public static VoidTrailData getOrCreate(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }
}
