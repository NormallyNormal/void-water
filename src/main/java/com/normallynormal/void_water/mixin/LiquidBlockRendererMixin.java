package com.normallynormal.void_water.mixin;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.normallynormal.void_water.ClientTrailData;
import com.normallynormal.void_water.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.client.renderer.block.LiquidBlockRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LiquidBlockRenderer.class)
public abstract class LiquidBlockRendererMixin {

//    @ModifyVariable(
//            method = "tesselate",
//            at = @At(value = "STORE"),
//            ordinal = 0,
//            require = 1
//    )
//    private boolean modifyFlag0(
//            boolean originalFlag,
//            BlockAndTintGetter level,
//            BlockPos pos,
//            VertexConsumer buffer,
//            BlockState blockState,
//            FluidState fluidState
//    ) {
//        if (pos.getY() == Util.getMinYForLevel() && ClientTrailData.getTrailLength(pos) > 0) {
//            return false;
//        }
//        return originalFlag;
//    }

    @ModifyVariable(
            method = "tesselate",
            at = @At(value = "STORE"),
            ordinal = 2,
            require = 1
    )
    private boolean modifyFlag1(
            boolean originalFlag,
            BlockAndTintGetter level,
            BlockPos pos,
            VertexConsumer buffer,
            BlockState blockState,
            FluidState fluidState
    ) {
        if (pos.getY() == Util.getMinYForLevel() && ClientTrailData.getTrailLength(pos) > 0) {
            return false;
        }
        return originalFlag;
    }

//    @Inject(
//            method = "isNeighborStateHidingOverlay",
//            at = @At("HEAD"),
//            cancellable = true
//    )
//    private static void alwaysReturnFalseOcclusion(FluidState selfState, BlockState otherState, Direction neighborFace, CallbackInfoReturnable<Boolean> cir) {
//        cir.setReturnValue(false);
//    }
//
//    @Inject(
//            method = "isFaceOccludedBySelf",
//            at = @At("HEAD"),
//            cancellable = true
//    )
//    private static void alwaysReturnFalseOcclusion2(BlockState state, Direction face, CallbackInfoReturnable<Boolean> cir) {
//        cir.setReturnValue(false);
//    }
//
//    @Inject(
//            method = "isFaceOccludedByState",
//            at = @At("HEAD"),
//            cancellable = true
//    )
//    private static void alwaysReturnFalseOcclusion4(Direction face, float height, BlockState state, CallbackInfoReturnable<Boolean> cir) {
//        cir.setReturnValue(false);
//    }
}
