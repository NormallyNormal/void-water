package com.normallynormal.void_water;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.Identifier;

public final class VoidFluidRenderTypes {

    public static final RenderPipeline TRANSLUCENT_VOID_FLUID_PIPELINE = RenderPipeline
            .builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(VoidWaterMod.MODID, "pipeline/translucent_void_fluid"))
            .withVertexShader("core/rendertype_translucent_moving_block")
            .withFragmentShader("core/rendertype_translucent_moving_block")
            .withSampler("Sampler0")
            .withSampler("Sampler2")
            .withBlend(BlendFunction.TRANSLUCENT)
            .withVertexFormat(DefaultVertexFormat.BLOCK, VertexFormat.Mode.QUADS)
            .build();

    public static final RenderPipeline SOLID_VOID_FLUID_PIPELINE = RenderPipeline
            .builder(RenderPipelines.BLOCK_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(VoidWaterMod.MODID, "pipeline/solid_void_fluid"))
            .build();

    public static final RenderType TRANSLUCENT_VOID_FLUID = RenderType.create(
            "translucent_void_fluid",
            RenderSetup.builder(TRANSLUCENT_VOID_FLUID_PIPELINE)
                    .useLightmap()
                    .withTexture("Sampler0", TextureAtlas.LOCATION_BLOCKS)
                    .sortOnUpload()
                    .bufferSize(786432)
                    .createRenderSetup()
    );

    public static final RenderType SOLID_VOID_FLUID = RenderType.create(
            "solid_void_fluid",
            RenderSetup.builder(SOLID_VOID_FLUID_PIPELINE)
                    .useLightmap()
                    .withTexture("Sampler0", TextureAtlas.LOCATION_BLOCKS)
                    .bufferSize(786432)
                    .createRenderSetup()
    );

    private VoidFluidRenderTypes() {}
}
