package com.normallynormal.void_water;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.LiquidBlockRenderer;
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

public class VoidTrailRendererSodium {

    public static void render(PoseStack poseStack, MultiBufferSource.BufferSource bufferSource, Vec3 camPos) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) return;

        int minY = Util.getMinYForLevel(level);
        Matrix4f pose = poseStack.last().pose();
        VertexConsumer buffer = bufferSource.getBuffer(RenderType.translucent());

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

        bufferSource.endBatch(RenderType.translucent());
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

        int flatLight = LevelRenderer.getLightColor(level, pos);

        // Smooth corner lighting for the trail's top edge. For each side face direction D,
        // the two top corners must match Sodium's bottom corners of the water block at y=minY,
        // which Sodium computes by sampling the 2x2 block neighborhood at (pos+D, pos+D+lateral,
        // pos+D+DOWN, pos+D+lateral+DOWN). Each (face, lateral) pair samples a different column,
        // so we cannot share corner values between faces (e.g. NORTH face's east-top and EAST
        // face's north-top vertices coincide spatially but sample different blocks).
        CornerLight topNW = cornerLight(level, pos, Direction.NORTH, Direction.WEST);
        CornerLight topNE = cornerLight(level, pos, Direction.NORTH, Direction.EAST);
        CornerLight topSW = cornerLight(level, pos, Direction.SOUTH, Direction.WEST);
        CornerLight topSE = cornerLight(level, pos, Direction.SOUTH, Direction.EAST);
        CornerLight topWS = cornerLight(level, pos, Direction.WEST,  Direction.SOUTH);
        CornerLight topWN = cornerLight(level, pos, Direction.WEST,  Direction.NORTH);
        CornerLight topES = cornerLight(level, pos, Direction.EAST,  Direction.SOUTH);
        CornerLight topEN = cornerLight(level, pos, Direction.EAST,  Direction.NORTH);

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            float x1, x2, z1, z2;
            boolean renderFace;
            int neighborTrail;
            CornerLight cornerLeft, cornerRight;

            switch (direction) {
                case NORTH -> {
                    x1 = wx;           x2 = wx + 1.0F;
                    z1 = wz + 0.001F;  z2 = wz + 0.001F;
                    renderFace = renderNorth; neighborTrail = trailNorth;
                    cornerLeft = topNW; cornerRight = topNE;
                }
                case SOUTH -> {
                    x1 = wx + 1.0F;           x2 = wx;
                    z1 = wz + 1.0F - 0.001F;  z2 = wz + 1.0F - 0.001F;
                    renderFace = renderSouth; neighborTrail = trailSouth;
                    cornerLeft = topSE; cornerRight = topSW;
                }
                case WEST -> {
                    x1 = wx + 0.001F;  x2 = wx + 0.001F;
                    z1 = wz + 1.0F;    z2 = wz;
                    renderFace = renderWest; neighborTrail = trailWest;
                    cornerLeft = topWS; cornerRight = topWN;
                }
                default -> { // EAST
                    x1 = wx + 1.0F - 0.001F;  x2 = wx + 1.0F - 0.001F;
                    z1 = wz;                   z2 = wz + 1.0F;
                    renderFace = renderEast; neighborTrail = trailEast;
                    cornerLeft = topEN; cornerRight = topES;
                }
            }

            float br = direction.getAxis() == Direction.Axis.Z ? 0.8F : 0.6F;

            // For depth > 1 and for the bottom edge of every segment, the samples fall in the void
            // column (no occluder, full sky), so AO collapses to 1.0 — same as the flat lightmap.
            float flatR = br * red;
            float flatG = br * green;
            float flatB = br * blue;

            float topRedL = br * cornerLeft.ao * red;
            float topGreenL = br * cornerLeft.ao * green;
            float topBlueL = br * cornerLeft.ao * blue;
            float topRedR = br * cornerRight.ao * red;
            float topGreenR = br * cornerRight.ao * green;
            float topBlueR = br * cornerRight.ao * blue;

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

                    boolean first = depth == 1;
                    int lt1 = first ? cornerLeft.lightmap : flatLight;
                    int lt2 = first ? cornerRight.lightmap : flatLight;
                    float r1 = first ? topRedL : flatR, g1 = first ? topGreenL : flatG, b1 = first ? topBlueL : flatB;
                    float r2 = first ? topRedR : flatR, g2 = first ? topGreenR : flatG, b2 = first ? topBlueR : flatB;

                    vertex(buffer, pose, x1, yTop,    z1, r1,    g1,    b1,    topAlpha,    u0, v0, lt1,       0, 1, 0);
                    vertex(buffer, pose, x2, yTop,    z2, r2,    g2,    b2,    topAlpha,    u1, v0, lt2,       0, 1, 0);
                    vertex(buffer, pose, x2, yBottom, z2, flatR, flatG, flatB, bottomAlpha, u1, v1, flatLight, 0, 1, 0);
                    vertex(buffer, pose, x1, yBottom, z1, flatR, flatG, flatB, bottomAlpha, u0, v1, flatLight, 0, 1, 0);

                    vertex(buffer, pose, x1, yBottom, z1, flatR, flatG, flatB, bottomAlpha, u0, v1, flatLight, 0, 1, 0);
                    vertex(buffer, pose, x2, yBottom, z2, flatR, flatG, flatB, bottomAlpha, u1, v1, flatLight, 0, 1, 0);
                    vertex(buffer, pose, x2, yTop,    z2, r2,    g2,    b2,    topAlpha,    u1, v0, lt2,       0, 1, 0);
                    vertex(buffer, pose, x1, yTop,    z1, r1,    g1,    b1,    topAlpha,    u0, v0, lt1,       0, 1, 0);
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
        float capRed = red, capGreen = green, capBlue = blue;

        vertex(buffer, pose, wx,     capY, wz + 1, capRed, capGreen, capBlue, capAlpha, cu0, cv1, flatLight, 0, 1, 0);
        vertex(buffer, pose, wx,     capY, wz,     capRed, capGreen, capBlue, capAlpha, cu0, cv0, flatLight, 0, 1, 0);
        vertex(buffer, pose, wx + 1, capY, wz,     capRed, capGreen, capBlue, capAlpha, cu1, cv0, flatLight, 0, 1, 0);
        vertex(buffer, pose, wx + 1, capY, wz + 1, capRed, capGreen, capBlue, capAlpha, cu1, cv1, flatLight, 0, 1, 0);

        vertex(buffer, pose, wx + 1, capY, wz + 1, capRed, capGreen, capBlue, capAlpha, cu1, cv1, flatLight, 0, 1, 0);
        vertex(buffer, pose, wx + 1, capY, wz,     capRed, capGreen, capBlue, capAlpha, cu1, cv0, flatLight, 0, 1, 0);
        vertex(buffer, pose, wx,     capY, wz,     capRed, capGreen, capBlue, capAlpha, cu0, cv0, flatLight, 0, 1, 0);
        vertex(buffer, pose, wx,     capY, wz + 1, capRed, capGreen, capBlue, capAlpha, cu0, cv1, flatLight, 0, 1, 0);
    }

    private record CornerLight(int lightmap, float ao) {}

    /**
     * Smooth corner lightmap + AO at the bottom corner of the side face of the water block
     * at {@code pos}, on the face {@code face}, on the side toward {@code lateral}. This is
     * exactly the corner Sodium produces for the same fluid side face via AoFaceData with
     * {@code offset=true}: sampling lightmaps and shade-brightness at adj, adj+lateral,
     * adj+DOWN, adj+lateral+DOWN (where adj = pos + face), then averaging.
     */
    private static CornerLight cornerLight(BlockAndTintGetter level, BlockPos pos, Direction face, Direction lateral) {
        BlockPos adj = pos.relative(face);
        BlockPos adjLat = adj.relative(lateral);
        BlockPos adjDn = adj.below();
        BlockPos adjLatDn = adjLat.below();

        // Per-block lightmap with the same shortcut Sodium's LightDataAccess.compute uses for
        // full-opaque non-luminous blocks: they contribute 0 so the corner-normalization step
        // below replaces them with the minimum non-zero contributor, matching AoFaceData's
        // calculateCornerBrightness behavior.
        int l0 = sampleLightmap(level, adj);
        int l1 = sampleLightmap(level, adjLat);
        int l2 = sampleLightmap(level, adjDn);
        int l3 = sampleLightmap(level, adjLatDn);

        if (l0 == 0 || l1 == 0 || l2 == 0 || l3 == 0) {
            int min = minNonZero(minNonZero(l0, l1), minNonZero(l2, l3));
            l0 = Math.max(l0, min);
            l1 = Math.max(l1, min);
            l2 = Math.max(l2, min);
            l3 = Math.max(l3, min);
        }

        int blockSum = (l0 & 0xFF) + (l1 & 0xFF) + (l2 & 0xFF) + (l3 & 0xFF);
        int skySum   = ((l0 >>> 16) & 0xFF) + ((l1 >>> 16) & 0xFF) + ((l2 >>> 16) & 0xFF) + ((l3 >>> 16) & 0xFF);
        int lightmap = ((blockSum >> 2) & 0xFF) | (((skySum >> 2) & 0xFF) << 16);

        float ao = (sampleAO(level, adj)
                  + sampleAO(level, adjLat)
                  + sampleAO(level, adjDn)
                  + sampleAO(level, adjLatDn)) * 0.25f;

        return new CornerLight(lightmap, ao);
    }

    private static int sampleLightmap(BlockAndTintGetter level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isSolidRender(level, pos) && state.getLightEmission(level, pos) == 0) {
            return 0;
        }
        return LevelRenderer.getLightColor(level, state, pos);
    }

    private static float sampleAO(BlockAndTintGetter level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        // Match Sodium's LightDataAccess.compute: emissive blocks contribute AO=1.0 instead of
        // their underlying shade brightness (e.g. glowstone is solid so getShadeBrightness=0.2,
        // but it shouldn't darken the corner — Sodium skips the shade lookup for lu>0).
        if (state.getLightEmission(level, pos) != 0) {
            return 1.0f;
        }
        return state.getShadeBrightness(level, pos);
    }

    private static int minNonZero(int a, int b) {
        if (a == 0) return b;
        if (b == 0) return a;
        return Math.min(a, b);
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
