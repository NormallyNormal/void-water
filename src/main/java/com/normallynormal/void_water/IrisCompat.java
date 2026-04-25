package com.normallynormal.void_water;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class IrisCompat {
    private static final boolean AVAILABLE;
    private static final Class<?> BSBB_CLASS;
    private static final Method BEGIN_BLOCK;
    private static final Method END_BLOCK;
    private static final Object WRS_INSTANCE;
    private static final Method GET_BLOCK_STATE_IDS;
    private static final Object IRIS_API_INSTANCE;
    private static final Method ASSIGN_PIPELINE;
    private static final Object IRIS_PROGRAM_TRANSLUCENT;
    private static final Object IRIS_PROGRAM_TERRAIN_SOLID;

    static {
        boolean ok = false;
        Class<?> bsbb = null;
        Method bb = null, eb = null, gbsi = null, ap = null;
        Object wrsi = null, apiInst = null, progTrans = null, progTerrainSolid = null;
        try {
            bsbb = Class.forName("net.irisshaders.iris.vertices.BlockSensitiveBufferBuilder");
            bb = bsbb.getMethod("beginBlock", int.class, byte.class, byte.class, int.class, int.class, int.class);
            eb = bsbb.getMethod("endBlock");
            Class<?> wrs = Class.forName("net.irisshaders.iris.shaderpack.materialmap.WorldRenderingSettings");
            Field inst = wrs.getField("INSTANCE");
            wrsi = inst.get(null);
            gbsi = wrs.getMethod("getBlockStateIds");
            ok = true;
        } catch (Throwable t) {
            // Iris not installed; stay inert.
        }
        try {
            Class<?> api = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            apiInst = api.getMethod("getInstance").invoke(null);
            Class<?> programEnum = Class.forName("net.irisshaders.iris.api.v0.IrisProgram");
            progTrans = Enum.valueOf(programEnum.asSubclass(Enum.class), "TRANSLUCENT");
            progTerrainSolid = Enum.valueOf(programEnum.asSubclass(Enum.class), "TERRAIN_SOLID");
            ap = api.getMethod("assignPipeline", RenderPipeline.class, programEnum);
        } catch (Throwable t) {
            // assignPipeline was added in IrisApi v0.3; leave null on older versions.
        }
        AVAILABLE = ok;
        BSBB_CLASS = bsbb;
        BEGIN_BLOCK = bb;
        END_BLOCK = eb;
        WRS_INSTANCE = wrsi;
        GET_BLOCK_STATE_IDS = gbsi;
        IRIS_API_INSTANCE = apiInst;
        ASSIGN_PIPELINE = ap;
        IRIS_PROGRAM_TRANSLUCENT = progTrans;
        IRIS_PROGRAM_TERRAIN_SOLID = progTerrainSolid;
    }

    public static void assignTranslucentPipeline(RenderPipeline pipeline) {
        assignPipeline(pipeline, IRIS_PROGRAM_TRANSLUCENT);
    }

    public static void assignTerrainSolidPipeline(RenderPipeline pipeline) {
        assignPipeline(pipeline, IRIS_PROGRAM_TERRAIN_SOLID);
    }

    private static void assignPipeline(RenderPipeline pipeline, Object program) {
        if (ASSIGN_PIPELINE == null || IRIS_API_INSTANCE == null || program == null) return;
        try {
            ASSIGN_PIPELINE.invoke(IRIS_API_INSTANCE, pipeline, program);
        } catch (Throwable ignored) {}
    }

    private IrisCompat() {}

    private static boolean debugLogged = false;

    public static void beginFluidBlock(VertexConsumer buffer, FluidState fluidState, BlockPos pos) {
        if (!AVAILABLE) {
            if (!debugLogged) { debugLogged = true; VoidWaterMod.LOGGER.info("[VoidWater/Iris] Iris not available"); }
            return;
        }
        if (!BSBB_CLASS.isInstance(buffer)) {
            if (!debugLogged) { debugLogged = true; VoidWaterMod.LOGGER.info("[VoidWater/Iris] Buffer is not BlockSensitiveBufferBuilder: {}", buffer.getClass().getName()); }
            return;
        }
        try {
            Object idMap = GET_BLOCK_STATE_IDS.invoke(WRS_INSTANCE);
            if (idMap == null) {
                if (!debugLogged) { debugLogged = true; VoidWaterMod.LOGGER.info("[VoidWater/Iris] blockStateIds map is null"); }
                return;
            }
            BlockState legacy = fluidState.createLegacyBlock();
            Method getOrDefault = idMap.getClass().getMethod("getOrDefault", Object.class, int.class);
            int id = (int) getOrDefault.invoke(idMap, legacy, -1);
            if (!debugLogged) {
                debugLogged = true;
                VoidWaterMod.LOGGER.info("[VoidWater/Iris] beginBlock id={} fluid={} buffer={}", id, legacy, buffer.getClass().getName());
            }
            BEGIN_BLOCK.invoke(buffer,
                    id, (byte) 1, (byte) legacy.getLightEmission(),
                    pos.getX(), pos.getY(), pos.getZ());
        } catch (Throwable t) {
            if (!debugLogged) { debugLogged = true; VoidWaterMod.LOGGER.warn("[VoidWater/Iris] beginFluidBlock threw", t); }
        }
    }

    public static void endFluidBlock(VertexConsumer buffer) {
        if (!AVAILABLE || !BSBB_CLASS.isInstance(buffer)) return;
        try {
            END_BLOCK.invoke(buffer);
        } catch (Throwable ignored) {}
    }
}
