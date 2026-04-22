package com.normallynormal.void_water;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.LiquidBlockRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.textures.FluidSpriteCache;
import org.joml.Matrix4f;

import java.util.Map;

public class VoidTrailRenderer {

    public static void render(PoseStack poseStack, MultiBufferSource.BufferSource bufferSource, Vec3 camPos) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) return;

        int minY = Util.getMinYForLevel(level);
        Matrix4f pose = poseStack.last().pose();
        VertexConsumer buffer = bufferSource.getBuffer(RenderType.translucentMovingBlock());

        for (Map.Entry<Long, Integer> entry : ClientTrailData.getAll().entrySet()) {
            int trailLength = entry.getValue();
            if (trailLength == 0) continue;

            long packed = entry.getKey();
            int bx = VoidTrailData.unpackX(packed);
            int bz = VoidTrailData.unpackZ(packed);
            BlockPos pos = new BlockPos(bx, minY, bz);
            FluidState fs = level.getFluidState(pos);
            if (fs.isEmpty()) continue;

            renderTrail(buffer, pose, level, pos, fs, trailLength, camPos);
        }

        bufferSource.endBatch(RenderType.translucentMovingBlock());
    }

    private static void renderTrail(VertexConsumer buffer, Matrix4f pose, BlockAndTintGetter level,
                                    BlockPos pos, FluidState fs, int trailLength, Vec3 camPos) {
        TextureAtlasSprite[] sprites = FluidSpriteCache.getFluidSprites(level, pos, fs);
        TextureAtlasSprite sideSprite = sprites[1];

        int tintColor = IClientFluidTypeExtensions.of(fs).getTintColor(fs, level, pos);
        float alpha = (float)(tintColor >> 24 & 255) / 255.0F;
        float red   = (float)(tintColor >> 16 & 0xFF) / 255.0F;
        float green = (float)(tintColor >>  8 & 0xFF) / 255.0F;
        float blue  = (float)(tintColor       & 0xFF) / 255.0F;

        float upShade    = level.getShade(Direction.UP,    true);
        float downShade  = level.getShade(Direction.DOWN,  true);
        float northShade = level.getShade(Direction.NORTH, true);
        float westShade  = level.getShade(Direction.WEST,  true);

        int trailNorth = ClientTrailData.getTrailLength(pos.relative(Direction.NORTH));
        int trailSouth = ClientTrailData.getTrailLength(pos.relative(Direction.SOUTH));
        int trailWest  = ClientTrailData.getTrailLength(pos.relative(Direction.WEST));
        int trailEast  = ClientTrailData.getTrailLength(pos.relative(Direction.EAST));

        BlockState blockState = level.getBlockState(pos);
        boolean renderNorth = LiquidBlockRenderer.shouldRenderFace(level, pos, fs, blockState, Direction.NORTH, level.getBlockState(pos.relative(Direction.NORTH)).getFluidState());
        boolean renderSouth = LiquidBlockRenderer.shouldRenderFace(level, pos, fs, blockState, Direction.SOUTH, level.getBlockState(pos.relative(Direction.SOUTH)).getFluidState());
        boolean renderWest  = LiquidBlockRenderer.shouldRenderFace(level, pos, fs, blockState, Direction.WEST,  level.getBlockState(pos.relative(Direction.WEST)).getFluidState());
        boolean renderEast  = LiquidBlockRenderer.shouldRenderFace(level, pos, fs, blockState, Direction.EAST,  level.getBlockState(pos.relative(Direction.EAST)).getFluidState());

        float wx = (float)(pos.getX() - camPos.x);
        float wy = (float)(pos.getY() - camPos.y);
        float wz = (float)(pos.getZ() - camPos.z);

        int light = getLightColor(level, pos);

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            float x1, x2, z1, z2, nx, nz;
            boolean renderFace;
            int neighborTrail;

            switch (direction) {
                case NORTH -> {
                    x1 = wx;           x2 = wx + 1.0F;
                    z1 = wz + 0.001F;  z2 = wz + 0.001F;
                    nx = 0; nz = -1;
                    renderFace = renderNorth; neighborTrail = trailNorth;
                }
                case SOUTH -> {
                    x1 = wx + 1.0F;           x2 = wx;
                    z1 = wz + 1.0F - 0.001F;  z2 = wz + 1.0F - 0.001F;
                    nx = 0; nz = 1;
                    renderFace = renderSouth; neighborTrail = trailSouth;
                }
                case WEST -> {
                    x1 = wx + 0.001F;  x2 = wx + 0.001F;
                    z1 = wz + 1.0F;    z2 = wz;
                    nx = -1; nz = 0;
                    renderFace = renderWest; neighborTrail = trailWest;
                }
                default -> { // EAST
                    x1 = wx + 1.0F - 0.001F;  x2 = wx + 1.0F - 0.001F;
                    z1 = wz;                   z2 = wz + 1.0F;
                    nx = 1; nz = 0;
                    renderFace = renderEast; neighborTrail = trailEast;
                }
            }

            if (renderFace && sprites[2] != null) {
                BlockPos neighborPos = pos.relative(direction);
                if (level.getBlockState(neighborPos).shouldDisplayFluidOverlay(level, neighborPos, fs)) {
                    sideSprite = sprites[2];
                }
            }

            float faceShade  = direction.getAxis() == Direction.Axis.Z ? northShade : westShade;
            float shadedRed   = upShade * faceShade * red;
            float shadedGreen = upShade * faceShade * green;
            float shadedBlue  = upShade * faceShade * blue;

            float topAlpha = alpha;
            for (int depth = 1; depth <= trailLength; depth++) {
                float bottomAlpha = alpha * (1 - ((float) depth / -(-Config.trailLength)));
                bottomAlpha = (float) Math.pow(bottomAlpha, Config.trailDecay);

                if (renderFace || neighborTrail < depth) {
                    float u0 = sideSprite.getU(0.0F);
                    float u1 = sideSprite.getU(0.5F);
                    float v0 = sideSprite.getV(0.0F);
                    float v1 = sideSprite.getV(0.5F);

                    float yTop    = wy + 1 - depth + 0.001f;
                    float yBottom = wy     - depth + 0.001f;

                    vertex(buffer, pose, x1, yTop,    z1, shadedRed, shadedGreen, shadedBlue, topAlpha,    u0, v0, light,  nx, 0,  nz);
                    vertex(buffer, pose, x2, yTop,    z2, shadedRed, shadedGreen, shadedBlue, topAlpha,    u1, v0, light,  nx, 0,  nz);
                    vertex(buffer, pose, x2, yBottom, z2, shadedRed, shadedGreen, shadedBlue, bottomAlpha, u1, v1, light,  nx, 0,  nz);
                    vertex(buffer, pose, x1, yBottom, z1, shadedRed, shadedGreen, shadedBlue, bottomAlpha, u0, v1, light,  nx, 0,  nz);

                    vertex(buffer, pose, x1, yBottom, z1, shadedRed, shadedGreen, shadedBlue, bottomAlpha, u0, v1, light, -nx, 0, -nz);
                    vertex(buffer, pose, x2, yBottom, z2, shadedRed, shadedGreen, shadedBlue, bottomAlpha, u1, v1, light, -nx, 0, -nz);
                    vertex(buffer, pose, x2, yTop,    z2, shadedRed, shadedGreen, shadedBlue, topAlpha,    u1, v0, light, -nx, 0, -nz);
                    vertex(buffer, pose, x1, yTop,    z1, shadedRed, shadedGreen, shadedBlue, topAlpha,    u0, v0, light, -nx, 0, -nz);
                }

                topAlpha = bottomAlpha;
            }
        }

        // Bottom cap
        float capAlpha = (float) Math.pow(
            alpha * (1 - ((float) trailLength / -(-Config.trailLength))),
            Config.trailDecay
        );
        TextureAtlasSprite capSprite = sprites[0];
        float cu0 = capSprite.getU(0.0F), cu1 = capSprite.getU(1.0F);
        float cv0 = capSprite.getV(0.0F), cv1 = capSprite.getV(1.0F);
        float capY = wy - trailLength + 0.001f;
        float capRed = downShade * red, capGreen = downShade * green, capBlue = downShade * blue;

        vertex(buffer, pose, wx,     capY, wz + 1, capRed, capGreen, capBlue, capAlpha, cu0, cv1, light, 0, -1, 0);
        vertex(buffer, pose, wx,     capY, wz,     capRed, capGreen, capBlue, capAlpha, cu0, cv0, light, 0, -1, 0);
        vertex(buffer, pose, wx + 1, capY, wz,     capRed, capGreen, capBlue, capAlpha, cu1, cv0, light, 0, -1, 0);
        vertex(buffer, pose, wx + 1, capY, wz + 1, capRed, capGreen, capBlue, capAlpha, cu1, cv1, light, 0, -1, 0);

        vertex(buffer, pose, wx + 1, capY, wz + 1, capRed, capGreen, capBlue, capAlpha, cu1, cv1, light, 0, 1, 0);
        vertex(buffer, pose, wx + 1, capY, wz,     capRed, capGreen, capBlue, capAlpha, cu1, cv0, light, 0, 1, 0);
        vertex(buffer, pose, wx,     capY, wz,     capRed, capGreen, capBlue, capAlpha, cu0, cv0, light, 0, 1, 0);
        vertex(buffer, pose, wx,     capY, wz + 1, capRed, capGreen, capBlue, capAlpha, cu0, cv1, light, 0, 1, 0);
    }

    private static int getLightColor(BlockAndTintGetter level, BlockPos pos) {
        int i = LevelRenderer.getLightColor(level, pos);
        int j = LevelRenderer.getLightColor(level, pos.above());
        int k = i & 0xFF, l = j & 0xFF;
        int i1 = i >> 16 & 0xFF, j1 = j >> 16 & 0xFF;
        return (k > l ? k : l) | (i1 > j1 ? i1 : j1) << 16;
    }

    private static void vertex(VertexConsumer buf, Matrix4f pose,
                                float x, float y, float z,
                                float r, float g, float b, float a,
                                float u, float v, int light,
                                float nx, float ny, float nz) {
        buf.addVertex(pose, x, y, z)
           .setColor(r, g, b, a)
           .setUv(u, v)
           .setLight(light)
           .setNormal(nx, ny, nz);
    }
}
