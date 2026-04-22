package com.normallynormal.void_water;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.minecraft.world.phys.Vec3;

@EventBusSubscriber(modid = VoidWaterMod.MODID, value = Dist.CLIENT)
public class ClientEventHandler {

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (ClientTrailData.serverHasMod) return;
        if (!(event.getLevel() instanceof Level level)) return;
        if (!level.isClientSide()) return;

        int minY = Util.getMinYForLevel(level);
        var chunkPos = event.getChunk().getPos();
        for (int x = chunkPos.getMinBlockX(); x <= chunkPos.getMaxBlockX(); x++) {
            for (int z = chunkPos.getMinBlockZ(); z <= chunkPos.getMaxBlockZ(); z++) {
                if (!level.getFluidState(new BlockPos(x, minY, z)).isEmpty()) {
                    ClientTrailData.addClientScanned(x, z, Config.trailLength);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload event) {
        if (ClientTrailData.serverHasMod) return;
        if (!event.getLevel().isClientSide()) return;
        var chunkPos = event.getChunk().getPos();
        ClientTrailData.removeChunk(chunkPos.x, chunkPos.z);
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (ClientTrailData.getAll().isEmpty()) return;
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
            Vec3 camPos = event.getCamera().getPosition();
            if (camPos.y >= Util.getMinYForLevel()) return;
            VoidTrailRenderer.render(event.getPoseStack(), bufferSource, camPos);
        } else if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES) {
            MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
            Vec3 camPos = event.getCamera().getPosition();
            if (camPos.y < Util.getMinYForLevel()) return;
            VoidTrailRenderer.render(event.getPoseStack(), bufferSource, camPos);
        }
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) {
            ClientTrailData.clear();
        }
    }
}
