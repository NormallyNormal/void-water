package com.normallynormal.void_water.mixin;

import com.normallynormal.void_water.ClientTrailData;
import com.normallynormal.void_water.Util;
import com.normallynormal.void_water.VoidTrailData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Level.class)
public class LevelMixin {
    @ModifyVariable(method = "getFluidState", at = @At("HEAD"))
    private BlockPos injected(BlockPos pos) {
        Level level = (Level)(Object) this;
        if (level.isClientSide() && !ClientTrailData.serverHasMod) return pos;
        int minY = Util.getMinYForLevel(level);
        if (pos.getY() < minY) {
            int trailLength;
            if (level instanceof ServerLevel serverLevel) {
                trailLength = VoidTrailData.getOrCreate(serverLevel)
                    .getTrailLengths()
                    .getOrDefault(VoidTrailData.pack(pos.getX(), pos.getZ()), 0);
            } else {
                trailLength = ClientTrailData.getTrailLength(pos);
            }
            if (trailLength > 0 && pos.getY() >= minY - trailLength) {
                return pos.atY(minY);
            }
        }
        return pos;
    }
}
