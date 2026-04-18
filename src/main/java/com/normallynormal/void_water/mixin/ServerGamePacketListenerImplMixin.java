package com.normallynormal.void_water.mixin;

import com.normallynormal.void_water.Config;
import com.normallynormal.void_water.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public class ServerGamePacketListenerImplMixin {

    @Shadow
    private boolean clientIsFloating;

    @Shadow
    private int aboveGroundTickCount;

    @Shadow
    private ServerPlayer player;

    @Inject(method = "handleMovePlayer", at = @At("TAIL"))
    private void clientIsFloatingFix(CallbackInfo ci) {
        int minY = Util.getMinYForLevel(player.level());

        int entityMinX = Mth.floor(player.getBoundingBox().minX);
        int entityMaxX = Mth.ceil(player.getBoundingBox().maxX);
        int entityMinZ = Mth.floor(player.getBoundingBox().minZ);
        int entityMaxZ = Mth.ceil(player.getBoundingBox().maxZ);

        double entityTop = player.getBoundingBox().maxY;

        BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();

        if (entityTop < minY && entityTop > minY - Config.trailLength) {
            for (int x = entityMinX; x < entityMaxX; x++) {
                for (int z = entityMinZ; z < entityMaxZ; z++) {
                    mutableBlockPos.set(x, minY, z);
                    boolean bottomFluid = player.level().getFluidState(mutableBlockPos).isEmpty();
                    if (this.clientIsFloating && !bottomFluid) {
                        this.clientIsFloating = false;
                        this.aboveGroundTickCount = 0;
                    }
                }
            }
        }
    }
}
