package com.normallynormal.void_water.mixin.sodium;

import com.normallynormal.void_water.ClientTrailData;
import com.normallynormal.void_water.Util;
import net.caffeinemc.mods.sodium.client.model.color.ColorProvider;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.buffers.ChunkModelBuilder;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.DefaultFluidRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.material.Material;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.TranslucentGeometryCollector;
import net.caffeinemc.mods.sodium.client.world.LevelSlice;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(DefaultFluidRenderer.class)
public abstract class SodiumFluidRendererMixin {

    @ModifyVariable(
            method = "render",
            at = @At(value = "STORE"),
            ordinal = 1,
            require = 1
    )
    private boolean voidWater$suppressDownFace(boolean original,
                                                LevelSlice level,
                                                BlockState blockState,
                                                FluidState fluidState,
                                                BlockPos blockPos,
                                                BlockPos offset,
                                                TranslucentGeometryCollector collector,
                                                ChunkModelBuilder meshBuilder,
                                                Material material,
                                                ColorProvider<FluidState> colorProvider,
                                                TextureAtlasSprite[] sprites) {
        if (blockPos.getY() == Util.getMinYForLevel()
                && ClientTrailData.getTrailLength(blockPos) > 0) {
            return false;
        }
        return original;
    }
}
