package com.normallynormal.void_water;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ChunkWatchEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@EventBusSubscriber(modid = VoidWaterMod.MODID)
public class VoidTrailEventHandler {

    @SubscribeEvent
    public static void onNeighborNotify(BlockEvent.NeighborNotifyEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        BlockPos pos = event.getPos();
        if (pos.getY() != Util.getMinYForLevel(level)) return;

        if (!level.getFluidState(pos).isEmpty()) {
            VoidTrailData.getOrCreate(level).addPosition(VoidTrailData.pack(pos.getX(), pos.getZ()));
        }
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        int minY = Util.getMinYForLevel(level);
        VoidTrailData data = VoidTrailData.getOrCreate(level);
        List<Long> positions = data.getTrackedPositions();
        if (positions.isEmpty()) return;

        Map<Long, Integer> changed = new HashMap<>();
        for (long packed : positions) {
            BlockPos pos = new BlockPos(VoidTrailData.unpackX(packed), minY, VoidTrailData.unpackZ(packed));
            boolean fluidPresent = !level.getFluidState(pos).isEmpty();
            int tickDelay = fluidPresent ? level.getFluidState(pos).getType().getTickDelay(level) : 5;
            int newLength = data.tick(packed, fluidPresent, tickDelay);
            if (newLength >= 0) {
                changed.put(packed, newLength);
            }
        }

        if (!changed.isEmpty()) {
            PacketDistributor.sendToPlayersInDimension(level, new VoidTrailSyncPacket(changed));
        }
    }

    @SubscribeEvent
    public static void onChunkWatch(ChunkWatchEvent.Watch event) {
        ServerLevel level = event.getLevel();
        ChunkPos chunkPos = event.getPos();
        int minY = Util.getMinYForLevel(level);
        VoidTrailData data = VoidTrailData.getOrCreate(level);

        // Scan for fluids not yet tracked (handles worlds predating this feature)
        for (int x = chunkPos.getMinBlockX(); x <= chunkPos.getMaxBlockX(); x++) {
            for (int z = chunkPos.getMinBlockZ(); z <= chunkPos.getMaxBlockZ(); z++) {
                if (!level.getFluidState(new BlockPos(x, minY, z)).isEmpty()) {
                    data.addPosition(VoidTrailData.pack(x, z));
                }
            }
        }

        // Send all trail lengths within this chunk to the watching player
        Map<Long, Integer> relevant = new HashMap<>();
        int minX = chunkPos.getMinBlockX(), maxX = chunkPos.getMaxBlockX();
        int minZ = chunkPos.getMinBlockZ(), maxZ = chunkPos.getMaxBlockZ();
        for (Map.Entry<Long, Integer> entry : data.getTrailLengths().entrySet()) {
            int x = VoidTrailData.unpackX(entry.getKey());
            int z = VoidTrailData.unpackZ(entry.getKey());
            if (x >= minX && x <= maxX && z >= minZ && z <= maxZ) {
                relevant.put(entry.getKey(), entry.getValue());
            }
        }
        if (!relevant.isEmpty()) {
            PacketDistributor.sendToPlayer(event.getPlayer(), new VoidTrailSyncPacket(relevant));
        }
    }
}
