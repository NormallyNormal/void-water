package com.normallynormal.void_water;

import com.mojang.brigadier.Command;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

import java.util.function.Consumer;

/**
 * /voidwater_light <x> <y> <z>
 *
 * Dumps the smooth-corner lighting our renderer would emit at the top edge of the trail at the
 * given block position, replicating what Sodium computes for the bottom edge of a water side
 * face at the same position. Use this to compare two setups that should produce the same
 * lighting (identical relative geometry).
 *
 * For each of the four horizontal faces, prints the two top corners. Each corner reports the
 * four sampled neighbor positions with their lightmaps and shade-brightness, the post-
 * normalization corner lightmap, and the corner AO.
 */
@EventBusSubscriber(modid = VoidWaterMod.MODID, value = Dist.CLIENT)
public class VoidWaterDebugCommand {

    @SubscribeEvent
    public static void onRegister(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(
            Commands.literal("voidwater_light")
                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                    .executes(ctx -> {
                        BlockPos pos = BlockPosArgument.getBlockPos(ctx, "pos");
                        dump(pos, ctx.getSource());
                        return Command.SINGLE_SUCCESS;
                    }))
        );
    }

    private static void dump(BlockPos pos, CommandSourceStack source) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            source.sendFailure(Component.literal("No client level"));
            return;
        }

        Consumer<String> line = s -> source.sendSuccess(() -> Component.literal(s), false);

        line.accept("§e=== Corner lighting at " + pos.toShortString() + " ===");
        int flat = LevelRenderer.getLightColor(level, pos);
        line.accept("flat at pos: " + lightStr(flat));
        line.accept("");

        for (Direction face : Direction.Plane.HORIZONTAL) {
            Direction lateralA, lateralB;
            if (face.getAxis() == Direction.Axis.Z) {
                lateralA = Direction.WEST;
                lateralB = Direction.EAST;
            } else {
                lateralA = Direction.NORTH;
                lateralB = Direction.SOUTH;
            }

            float br = face.getAxis() == Direction.Axis.Z ? 0.8F : 0.6F;
            line.accept("§b-- " + face + " face (directional shade " + br + ") --");
            dumpCorner(level, pos, face, lateralA, br, line);
            dumpCorner(level, pos, face, lateralB, br, line);
        }
    }

    private static void dumpCorner(ClientLevel level, BlockPos pos, Direction face, Direction lateral,
                                   float br, Consumer<String> line) {
        BlockPos adj = pos.relative(face);
        BlockPos adjLat = adj.relative(lateral);
        BlockPos adjDn = adj.below();
        BlockPos adjLatDn = adjLat.below();

        int raw0 = LevelRenderer.getLightColor(level, adj);
        int raw1 = LevelRenderer.getLightColor(level, adjLat);
        int raw2 = LevelRenderer.getLightColor(level, adjDn);
        int raw3 = LevelRenderer.getLightColor(level, adjLatDn);

        int s0 = sampleLightmap(level, adj);
        int s1 = sampleLightmap(level, adjLat);
        int s2 = sampleLightmap(level, adjDn);
        int s3 = sampleLightmap(level, adjLatDn);

        int[] norm = { s0, s1, s2, s3 };
        if (s0 == 0 || s1 == 0 || s2 == 0 || s3 == 0) {
            int min = minNonZero(minNonZero(s0, s1), minNonZero(s2, s3));
            for (int i = 0; i < 4; i++) norm[i] = Math.max(norm[i], min);
        }

        int blockSum = (norm[0] & 0xFF) + (norm[1] & 0xFF) + (norm[2] & 0xFF) + (norm[3] & 0xFF);
        int skySum = ((norm[0] >>> 16) & 0xFF) + ((norm[1] >>> 16) & 0xFF) + ((norm[2] >>> 16) & 0xFF) + ((norm[3] >>> 16) & 0xFF);
        int corner = ((blockSum >> 2) & 0xFF) | (((skySum >> 2) & 0xFF) << 16);

        float ao0 = sampleAO(level, adj);
        float ao1 = sampleAO(level, adjLat);
        float ao2 = sampleAO(level, adjDn);
        float ao3 = sampleAO(level, adjLatDn);
        float ao = (ao0 + ao1 + ao2 + ao3) * 0.25f;

        line.accept("  corner toward " + lateral + ":");
        line.accept("    adj      " + adj.toShortString() + " " + lightStr(raw0) + " ao=" + fmt(ao0) + " " + blockName(level, adj));
        line.accept("    lat      " + adjLat.toShortString() + " " + lightStr(raw1) + " ao=" + fmt(ao1) + " " + blockName(level, adjLat));
        line.accept("    dn       " + adjDn.toShortString() + " " + lightStr(raw2) + " ao=" + fmt(ao2) + " " + blockName(level, adjDn));
        line.accept("    latDn    " + adjLatDn.toShortString() + " " + lightStr(raw3) + " ao=" + fmt(ao3) + " " + blockName(level, adjLatDn));
        line.accept("    §a→ corner " + lightStr(corner) + " ao=" + fmt(ao) + " final-mult=" + fmt(br * ao));
    }

    private static int sampleLightmap(ClientLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isSolidRender() && state.getLightEmission(level, pos) == 0) {
            return 0;
        }
        return LevelRenderer.getLightColor(level, pos);
    }

    private static float sampleAO(ClientLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
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

    private static String lightStr(int lm) {
        int bl = (lm >> 4) & 0xF;
        int sl = (lm >> 20) & 0xF;
        return String.format("bl=%2d sl=%2d (raw=0x%06X)", bl, sl, lm & 0xFFFFFF);
    }

    private static String blockName(ClientLevel level, BlockPos pos) {
        return "§7[" + level.getBlockState(pos).getBlock().getName().getString() + "]§r";
    }

    private static String fmt(float f) {
        return String.format("%.3f", f);
    }
}
