package com.normallynormal.void_water.mixin;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.normallynormal.void_water.SectionCompileContext;
import net.minecraft.client.renderer.SectionBufferBuilderPack;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.RenderSectionRegion;
import net.minecraft.client.renderer.chunk.SectionCompiler;
import net.minecraft.core.SectionPos;
import com.mojang.blaze3d.vertex.VertexSorting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Map;

@Mixin(SectionCompiler.class)
public class SectionCompilerMixin {
    private static final String COMPILE_DESC =
            "compile(Lnet/minecraft/core/SectionPos;" +
            "Lnet/minecraft/client/renderer/chunk/RenderSectionRegion;" +
            "Lcom/mojang/blaze3d/vertex/VertexSorting;" +
            "Lnet/minecraft/client/renderer/SectionBufferBuilderPack;" +
            "Ljava/util/List;)" +
            "Lnet/minecraft/client/renderer/chunk/SectionCompiler$Results;";

    @Inject(method = COMPILE_DESC, at = @At("HEAD"))
    private void capturePack(
            SectionPos sectionPos, RenderSectionRegion region, VertexSorting vertexSorting,
            SectionBufferBuilderPack sectionBufferBuilderPack, List<?> additionalRenderers,
            CallbackInfoReturnable<SectionCompiler.Results> cir
    ) {
        SectionCompileContext.CURRENT_PACK.set(sectionBufferBuilderPack);
    }

    @ModifyVariable(method = COMPILE_DESC, at = @At("STORE"), ordinal = 0)
    private Map<ChunkSectionLayer, BufferBuilder> captureBufferMap(Map<ChunkSectionLayer, BufferBuilder> map) {
        SectionCompileContext.CURRENT_MAP.set(map);
        return map;
    }

    @Inject(method = COMPILE_DESC, at = @At("RETURN"))
    private void clearContext(CallbackInfoReturnable<SectionCompiler.Results> cir) {
        SectionCompileContext.clear();
    }
}
