package com.normallynormal.void_water;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.HashMap;
import java.util.Map;

public record VoidTrailSyncPacket(Map<Long, Integer> trailLengths) implements CustomPacketPayload {
    public static final Type<VoidTrailSyncPacket> TYPE = new Type<>(
        Identifier.fromNamespaceAndPath(VoidWaterMod.MODID, "trail_sync")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, VoidTrailSyncPacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.map(HashMap::new, ByteBufCodecs.VAR_LONG, ByteBufCodecs.VAR_INT),
        VoidTrailSyncPacket::trailLengths,
        VoidTrailSyncPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(VoidTrailSyncPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> ClientTrailData.applyUpdate(packet.trailLengths()));
    }
}
