package com.normallynormal.void_water;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.SectionBufferBuilderPack;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import org.jspecify.annotations.Nullable;

import java.util.Map;

public class SectionCompileContext {
    public static final ThreadLocal<Map<ChunkSectionLayer, BufferBuilder>> CURRENT_MAP = new ThreadLocal<>();
    public static final ThreadLocal<SectionBufferBuilderPack> CURRENT_PACK = new ThreadLocal<>();

    public static void clear() {
        CURRENT_MAP.remove();
        CURRENT_PACK.remove();
    }

    public static @Nullable VertexConsumer getOrCreateTranslucentBuffer() {
        Map<ChunkSectionLayer, BufferBuilder> map = CURRENT_MAP.get();
        SectionBufferBuilderPack pack = CURRENT_PACK.get();
        if (map == null || pack == null) return null;

        BufferBuilder existing = map.get(ChunkSectionLayer.TRANSLUCENT);
        if (existing != null) return existing;

        ByteBufferBuilder byteBuilder = pack.buffer(ChunkSectionLayer.TRANSLUCENT);
        BufferBuilder newBuilder = new BufferBuilder(byteBuilder, VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);
        map.put(ChunkSectionLayer.TRANSLUCENT, newBuilder);
        return newBuilder;
    }
}
