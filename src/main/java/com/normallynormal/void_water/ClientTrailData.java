package com.normallynormal.void_water;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

import java.util.HashMap;
import java.util.Map;

public class ClientTrailData {
    private static final Map<Long, Integer> TRAIL_LENGTHS = new HashMap<>();
    public static boolean serverHasMod = false;

    public static void applyUpdate(Map<Long, Integer> updates) {
        if (!serverHasMod) {
            TRAIL_LENGTHS.clear();
            serverHasMod = true;
        }
        Minecraft mc = Minecraft.getInstance();
        for (Map.Entry<Long, Integer> entry : updates.entrySet()) {
            long packed = entry.getKey();
            if (entry.getValue() <= 0) {
                TRAIL_LENGTHS.remove(packed);
            } else {
                TRAIL_LENGTHS.put(packed, entry.getValue());
            }
            if (mc.levelRenderer != null) {
                int x = VoidTrailData.unpackX(packed);
                int z = VoidTrailData.unpackZ(packed);
                int minY = Util.getMinYForLevel();
                mc.levelRenderer.setSectionDirty(x >> 4, minY >> 4, z >> 4);
            }
        }
    }

    public static void addClientScanned(int x, int z, int length) {
        long packed = VoidTrailData.pack(x, z);
        if (TRAIL_LENGTHS.containsKey(packed)) return;
        TRAIL_LENGTHS.put(packed, length);
        Minecraft mc = Minecraft.getInstance();
        if (mc.levelRenderer != null) {
            int minY = Util.getMinYForLevel();
            mc.levelRenderer.setSectionDirty(x >> 4, minY >> 4, z >> 4);
        }
    }

    public static void removeChunk(int chunkX, int chunkZ) {
        int minX = chunkX << 4, maxX = minX + 15;
        int minZ = chunkZ << 4, maxZ = minZ + 15;
        TRAIL_LENGTHS.keySet().removeIf(packed -> {
            int x = VoidTrailData.unpackX(packed);
            int z = VoidTrailData.unpackZ(packed);
            return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
        });
    }

    public static int getTrailLength(BlockPos pos) {
        return TRAIL_LENGTHS.getOrDefault(VoidTrailData.pack(pos.getX(), pos.getZ()), 0);
    }

    public static void clear() {
        TRAIL_LENGTHS.clear();
        serverHasMod = false;
    }
}
