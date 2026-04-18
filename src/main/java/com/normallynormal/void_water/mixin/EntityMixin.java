package com.normallynormal.void_water.mixin;

import com.normallynormal.void_water.ClientTrailData;
import com.normallynormal.void_water.Util;
import com.normallynormal.void_water.VoidTrailData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.extensions.IEntityExtension;
import net.neoforged.neoforge.fluids.FluidType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Mixin(Entity.class)
public class EntityMixin {
    @Shadow
    private BlockPos blockPosition;

    @Shadow
    public Level level() {
        return null;
    }

    @Shadow
    public Vec3 getDeltaMovement() {
        return null;
    }

    @Shadow
    public void setDeltaMovement(Vec3 deltaMovement) {

    }

    @Shadow
    private AABB bb;

    @Inject(
            method = "updateFluidHeightAndDoFluidPushing(Z)V",
            at = @At("RETURN")
    )
    private void modifyFluidPushing(boolean doFluidPushing, CallbackInfo ci) {
        Level level = this.level();
        if (level.isClientSide() && !ClientTrailData.serverHasMod) return;
        int levelFloor = Util.getMinYForLevel(level);
        double entityTop = bb.maxY;
        int entityMinX = Mth.floor(bb.minX);
        int entityMaxX = Mth.ceil(bb.maxX);
        int entityMinZ = Mth.floor(bb.minZ);
        int entityMaxZ = Mth.ceil(bb.maxZ);

        BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();

        Vec3 modifiedFlowVector = Vec3.ZERO;

        Set<FluidType> fluidsIn = new HashSet<>();

        if (entityTop < levelFloor) {
            for (int x = entityMinX; x < entityMaxX; x++) {
                for (int z = entityMinZ; z < entityMaxZ; z++) {
                    int colTrailLength = getVoidTrailLength(x, z);
                    if (colTrailLength == 0 || entityTop <= levelFloor - colTrailLength) continue;
                    mutableBlockPos.set(x, levelFloor, z);
                    FluidState fluidstate = level.getFluidState(mutableBlockPos);
                    FluidType fluidType = fluidstate.getFluidType();
                    if (!fluidType.isAir() && fluidType.canPushEntity((Entity)(Object)this) && !fluidsIn.contains(fluidType)) {
                        double scale = ((IEntityExtension) this).getFluidMotionScale(fluidType);
                        modifiedFlowVector = modifiedFlowVector.add(0, -scale, 0);
                        fluidsIn.add(fluidType);
                    }
                }
            }
            Vec3 deltaMovement = this.getDeltaMovement();
            this.setDeltaMovement(deltaMovement.add(modifiedFlowVector));
        }
    }

    @Inject(method = "checkInsideBlocks", at = @At("RETURN"))
    private void onCheckInsideBlocks(List<?> blocks, InsideBlockEffectApplier.StepBasedCollector applier, CallbackInfo ci) {
        Level level = this.level();
        if (level.isClientSide() && !ClientTrailData.serverHasMod) return;
        int minY = Util.getMinYForLevel(level);
        if (bb.maxY >= minY) return;

        int entityMinX = Mth.floor(bb.minX);
        int entityMaxX = Mth.ceil(bb.maxX);
        int entityMinZ = Mth.floor(bb.minZ);
        int entityMaxZ = Mth.ceil(bb.maxZ);
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        Set<FluidType> processed = new HashSet<>();

        for (int x = entityMinX; x < entityMaxX; x++) {
            for (int z = entityMinZ; z < entityMaxZ; z++) {
                int trailLength = getVoidTrailLength(x, z);
                if (trailLength == 0 || bb.maxY <= minY - trailLength) continue;
                mutablePos.set(x, minY, z);
                FluidState fs = level.getFluidState(mutablePos);
                if (!fs.isEmpty() && processed.add(fs.getFluidType())) {
                    fs.entityInside(level, mutablePos.immutable(), (Entity)(Object)this, applier);
                }
            }
        }
    }

    @ModifyVariable(method = "updateFluidOnEyes()V", at = @At("STORE"), ordinal = 0)
    private double modifyD0(double d0) {
        int minY = Util.getMinYForLevel(this.level());
        if (d0 < minY) {
            int trailLength = getVoidTrailLength(blockPosition.getX(), blockPosition.getZ());
            if (trailLength > 0 && d0 > minY - trailLength) {
                return minY + 0.01;
            }
        }
        return d0;
    }

    private int getVoidTrailLength(int x, int z) {
        Level level = this.level();
        if (level instanceof ServerLevel serverLevel) {
            return VoidTrailData.getOrCreate(serverLevel)
                .getTrailLengths()
                .getOrDefault(VoidTrailData.pack(x, z), 0);
        } else {
            return ClientTrailData.getTrailLength(new BlockPos(x, 0, z));
        }
    }
}
