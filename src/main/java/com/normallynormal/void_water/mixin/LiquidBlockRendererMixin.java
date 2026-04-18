package com.normallynormal.void_water.mixin;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.normallynormal.void_water.ClientTrailData;
import com.normallynormal.void_water.Config;
import com.normallynormal.void_water.SectionCompileContext;
import com.normallynormal.void_water.Util;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.block.LiquidBlockRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.client.textures.FluidSpriteCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static net.minecraft.client.renderer.block.LiquidBlockRenderer.shouldRenderFace;

@Mixin(LiquidBlockRenderer.class)
public abstract class LiquidBlockRendererMixin {

//    @Inject(method = "shouldRenderFace", at = @At("HEAD"), cancellable = true)
//    private static void onShouldRenderFace(FluidState fluidState, BlockState blockState, Direction side, FluidState neighborFluid, CallbackInfoReturnable<Boolean> cir) {
//        fluidState.
//        int minY = Util.getMinYForLevel();
//        if (pos.getY() <= minY && side == Direction.DOWN) {
//            cir.setReturnValue(false);
//        }
//    }
    @ModifyVariable(
            method = "tesselate",
            at = @At(value = "STORE"),
            ordinal = 1,
            require = 1
    )
    private boolean modifyFlag2(
            boolean originalFlag2,
            BlockAndTintGetter level,
            BlockPos pos,
            VertexConsumer buffer,
            BlockState blockState,
            FluidState fluidState
    ) {
        if (pos.getY() == Util.getMinYForLevel() && ClientTrailData.getTrailLength(pos) > 0) {
            return false;
        }
        return originalFlag2;
    }

    @Inject(method = "tesselate", at = @At("HEAD"))
    private void onTesselate(BlockAndTintGetter level, BlockPos pos, VertexConsumer buffer, BlockState blockState, FluidState fluidState, CallbackInfo ci) {
        int minY = Util.getMinYForLevel();
        int trailLength = ClientTrailData.getTrailLength(pos);
        if (pos.getY() == minY && trailLength > 0) {
            VertexConsumer trailBuffer = SectionCompileContext.getOrCreateTranslucentBuffer();
            if (trailBuffer == null) trailBuffer = buffer;

            TextureAtlasSprite[] sprites = FluidSpriteCache.getFluidSprites(level, pos, fluidState);
            TextureAtlasSprite sideSprite = sprites[1];
            int tintColor = net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions.of(fluidState).getTintColor(fluidState, level, pos);
            float alpha = (float)(tintColor >> 24 & 255) / 255.0F;
            float red   = (float)(tintColor >> 16 & 0xFF) / 255.0F;
            float green = (float)(tintColor >> 8  & 0xFF) / 255.0F;
            float blue  = (float)(tintColor        & 0xFF) / 255.0F;
            float upShade    = level.getShade(Direction.UP,    true);
            float downShade  = level.getShade(Direction.DOWN,  true);
            float northShade = level.getShade(Direction.NORTH, true);
            float westShade  = level.getShade(Direction.WEST,  true);
            FluidState northFluid = level.getBlockState(pos.relative(Direction.NORTH)).getFluidState();
            FluidState southFluid = level.getBlockState(pos.relative(Direction.SOUTH)).getFluidState();
            FluidState westFluid  = level.getBlockState(pos.relative(Direction.WEST)).getFluidState();
            FluidState eastFluid  = level.getBlockState(pos.relative(Direction.EAST)).getFluidState();
            boolean renderNorth = LiquidBlockRenderer.shouldRenderFace(fluidState, blockState, Direction.NORTH, northFluid);
            boolean renderSouth = LiquidBlockRenderer.shouldRenderFace(fluidState, blockState, Direction.SOUTH, southFluid);
            boolean renderWest  = LiquidBlockRenderer.shouldRenderFace(fluidState, blockState, Direction.WEST,  westFluid);
            boolean renderEast  = LiquidBlockRenderer.shouldRenderFace(fluidState, blockState, Direction.EAST,  eastFluid);
            float localX = (float)(pos.getX() & 15);
            float localY = (float)(pos.getY() & 15);
            float localZ = (float)(pos.getZ() & 15);
            int light = this.getLightColor(level, pos);

            int trailNorth = ClientTrailData.getTrailLength(pos.relative(Direction.NORTH));
            int trailSouth = ClientTrailData.getTrailLength(pos.relative(Direction.SOUTH));
            int trailWest  = ClientTrailData.getTrailLength(pos.relative(Direction.WEST));
            int trailEast  = ClientTrailData.getTrailLength(pos.relative(Direction.EAST));

            for (Direction direction : Direction.Plane.HORIZONTAL) {
                float x1, x2, z1, z2;
                float nx, ny, nz;
                boolean renderAtMinY;
                int neighborTrail;
                switch (direction) {
                    case NORTH:
                        x1 = localX;       x2 = localX + 1.0F;
                        z1 = localZ + 0.001F; z2 = localZ + 0.001F;
                        nx = 0; ny = 0; nz = -1;
                        renderAtMinY = renderNorth; neighborTrail = trailNorth;
                        break;
                    case SOUTH:
                        x1 = localX + 1.0F; x2 = localX;
                        z1 = localZ + 1.0F - 0.001F; z2 = localZ + 1.0F - 0.001F;
                        nx = 0; ny = 0; nz = 1;
                        renderAtMinY = renderSouth; neighborTrail = trailSouth;
                        break;
                    case WEST:
                        x1 = localX + 0.001F; x2 = localX + 0.001F;
                        z1 = localZ + 1.0F;   z2 = localZ;
                        nx = -1; ny = 0; nz = 0;
                        renderAtMinY = renderWest; neighborTrail = trailWest;
                        break;
                    default:
                        x1 = localX + 1.0F - 0.001F; x2 = localX + 1.0F - 0.001F;
                        z1 = localZ;       z2 = localZ + 1.0F;
                        nx = 1; ny = 0; nz = 0;
                        renderAtMinY = renderEast; neighborTrail = trailEast;
                }
                if (renderAtMinY) {
                    BlockPos neighborPos = pos.relative(direction);
                    if (sprites[2] != null) {
                        if (level.getBlockState(neighborPos).shouldDisplayFluidOverlay(level, neighborPos, fluidState)) {
                            sideSprite = sprites[2];
                        }
                    }
                }
                float topAlpha = alpha;
                for (int depth = 1; depth <= trailLength; depth++) {
                    float bottomAlpha = alpha * (1 - ((float)(depth) / -(-Config.trailLength)));
                    bottomAlpha = (float) Math.pow(bottomAlpha, Config.trailDecay);

                    if (depth == Config.trailLength + 1) {
                        bottomAlpha = 1;
                    }

                    if (renderAtMinY || neighborTrail < depth) {
                        float u0 = sideSprite.getU(0.0F);
                        float u1 = sideSprite.getU(0.5F);
                        float v0 = sideSprite.getV(0.0F);
                        float v1 = sideSprite.getV(0.5F);

                        float faceShade  = direction.getAxis() == Direction.Axis.Z ? northShade : westShade;
                        float shadedRed   = upShade * faceShade * red;
                        float shadedGreen = upShade * faceShade * green;
                        float shadedBlue  = upShade * faceShade * blue;

                        float yTop    = localY + 1 - depth + 0.001f;
                        float yBottom = localY     - depth + 0.001f;

                        this.vertex(trailBuffer, x1, yTop,    z1, shadedRed, shadedGreen, shadedBlue, topAlpha,    u0, v0, light, nx,  ny,  nz);
                        this.vertex(trailBuffer, x2, yTop,    z2, shadedRed, shadedGreen, shadedBlue, topAlpha,    u1, v0, light, nx,  ny,  nz);
                        this.vertex(trailBuffer, x2, yBottom, z2, shadedRed, shadedGreen, shadedBlue, bottomAlpha, u1, v1, light, nx,  ny,  nz);
                        this.vertex(trailBuffer, x1, yBottom, z1, shadedRed, shadedGreen, shadedBlue, bottomAlpha, u0, v1, light, nx,  ny,  nz);

                        this.vertex(trailBuffer, x1, yBottom, z1, shadedRed, shadedGreen, shadedBlue, bottomAlpha, u0, v1, light, -nx, -ny, -nz);
                        this.vertex(trailBuffer, x2, yBottom, z2, shadedRed, shadedGreen, shadedBlue, bottomAlpha, u1, v1, light, -nx, -ny, -nz);
                        this.vertex(trailBuffer, x2, yTop,    z2, shadedRed, shadedGreen, shadedBlue, topAlpha,    u1, v0, light, -nx, -ny, -nz);
                        this.vertex(trailBuffer, x1, yTop,    z1, shadedRed, shadedGreen, shadedBlue, topAlpha,    u0, v0, light, -nx, -ny, -nz);
                    }

                    topAlpha = bottomAlpha;
                }
            }

            // Bottom cap at the tip of the visible trail
            float capAlpha = (float) Math.pow(
                alpha * (1 - ((float)(trailLength) / -(-Config.trailLength))),
                Config.trailDecay
            );
            TextureAtlasSprite capSprite = sprites[0];
            float cu0 = capSprite.getU(0.0F), cu1 = capSprite.getU(1.0F);
            float cv0 = capSprite.getV(0.0F), cv1 = capSprite.getV(1.0F);
            float capY = localY - trailLength + 0.001f;
            float capRed = downShade * red, capGreen = downShade * green, capBlue = downShade * blue;

            this.vertex(trailBuffer, localX,       capY, localZ + 1, capRed, capGreen, capBlue, capAlpha, cu0, cv1, light, 0, -1, 0);
            this.vertex(trailBuffer, localX,       capY, localZ,     capRed, capGreen, capBlue, capAlpha, cu0, cv0, light, 0, -1, 0);
            this.vertex(trailBuffer, localX + 1,   capY, localZ,     capRed, capGreen, capBlue, capAlpha, cu1, cv0, light, 0, -1, 0);
            this.vertex(trailBuffer, localX + 1,   capY, localZ + 1, capRed, capGreen, capBlue, capAlpha, cu1, cv1, light, 0, -1, 0);

            this.vertex(trailBuffer, localX + 1,   capY, localZ + 1, capRed, capGreen, capBlue, capAlpha, cu1, cv1, light, 0, 1, 0);
            this.vertex(trailBuffer, localX + 1,   capY, localZ,     capRed, capGreen, capBlue, capAlpha, cu1, cv0, light, 0, 1, 0);
            this.vertex(trailBuffer, localX,       capY, localZ,     capRed, capGreen, capBlue, capAlpha, cu0, cv0, light, 0, 1, 0);
            this.vertex(trailBuffer, localX,       capY, localZ + 1, capRed, capGreen, capBlue, capAlpha, cu0, cv1, light, 0, 1, 0);
        }
    }

    private void vertex(
            VertexConsumer vertexConsumer,
            float x, float y, float z,
            float red, float green, float blue, float alpha,
            float u, float v,
            int light,
            float nx, float ny, float nz
    ) {
        vertexConsumer.addVertex(x, y, z)
                .setColor(red, green, blue, alpha)
                .setUv(u, v)
                .setLight(light)
                .setNormal(nx, ny, nz);
    }

    private static boolean isNeighborSameFluid(FluidState firstState, FluidState secondState) {
        return secondState.getType().isSame(firstState.getType());
    }

    private static boolean isFaceOccludedByState(BlockGetter level, Direction face, float height, BlockPos pos, BlockState state) {
        if (state.canOcclude()) {
            VoxelShape voxelshape = Shapes.box(0.0, 0.0, 0.0, 1.0, (double)height, 1.0);
            VoxelShape voxelshape1 = state.getOcclusionShape();
            return Shapes.blockOccludes(voxelshape, voxelshape1, face);
        } else {
            return false;
        }
    }

    private static boolean isFaceOccludedByNeighbor(BlockGetter level, BlockPos pos, Direction side, float height, BlockState blockState) {
        return isFaceOccludedByState(level, side, height, pos.relative(side), blockState);
    }

    private static boolean isFaceOccludedBySelf(BlockGetter level, BlockPos pos, BlockState state, Direction face) {
        return isFaceOccludedByState(level, face.getOpposite(), 1.0F, pos, state);
    }

    private int getLightColor(BlockAndTintGetter level, BlockPos pos) {
        int i = LevelRenderer.getLightColor(level, pos);
        int j = LevelRenderer.getLightColor(level, pos.above());
        int k = i & 0xFF;
        int l = j & 0xFF;
        int i1 = i >> 16 & 0xFF;
        int j1 = j >> 16 & 0xFF;
        return (k > l ? k : l) | (i1 > j1 ? i1 : j1) << 16;
    }

    private float getHeight(BlockAndTintGetter level, Fluid fluid, BlockPos pos) {
        BlockState blockstate = level.getBlockState(pos);
        return this.getHeight(level, fluid, pos, blockstate, blockstate.getFluidState());
    }

    private float getHeight(BlockAndTintGetter level, Fluid fluid, BlockPos pos, BlockState blockState, FluidState fluidState) {
        if (fluid.isSame(fluidState.getType())) {
            BlockState blockstate = level.getBlockState(pos.above());
            return fluid.isSame(blockstate.getFluidState().getType()) ? 1.0F : fluidState.getOwnHeight();
        } else {
            return !blockState.isSolid() ? 0.0F : -1.0F;
        }
    }

    private float calculateAverageHeight(BlockAndTintGetter level, Fluid fluid, float currentHeight, float height1, float height2, BlockPos pos) {
        if (!(height2 >= 1.0F) && !(height1 >= 1.0F)) {
            float[] afloat = new float[2];
            if (height2 > 0.0F || height1 > 0.0F) {
                float f = this.getHeight(level, fluid, pos);
                if (f >= 1.0F) {
                    return 1.0F;
                }

                this.addWeightedHeight(afloat, f);
            }

            this.addWeightedHeight(afloat, currentHeight);
            this.addWeightedHeight(afloat, height2);
            this.addWeightedHeight(afloat, height1);
            return afloat[0] / afloat[1];
        } else {
            return 1.0F;
        }
    }

    private void addWeightedHeight(float[] output, float height) {
        if (height >= 0.8F) {
            output[0] += height * 10.0F;
            output[1] += 10.0F;
        } else if (height >= 0.0F) {
            output[0] += height;
            output[1]++;
        }
    }
}