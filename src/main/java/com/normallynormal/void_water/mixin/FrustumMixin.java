package com.normallynormal.void_water.mixin;

import com.normallynormal.void_water.Config;
import com.normallynormal.void_water.Util;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.client.renderer.culling.Frustum;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Frustum.class)
public class FrustumMixin {
    @Inject(
            method = "cubeInFrustum(Lnet/minecraft/world/level/levelgen/structure/BoundingBox;)I",
            at = @At("TAIL"),
            cancellable = true
    )
    private void modifyCubeInFrustum(BoundingBox boundingBox, CallbackInfoReturnable<Integer> cir) {
        // Original return value
        int originalReturnValue = cir.getReturnValue();

        // Implement the custom logic for trueMinY
        int trueMinY = Util.getMinYForLevel(); // Replace with your actual implementation
        int minY = boundingBox.minY();

        if (minY == trueMinY) {
            minY -= Config.trailLength;
        }

        // Recalculate with the adjusted minY and set the return value
        int modifiedReturnValue = newCubeInFrustum(
                (double) boundingBox.minX(),
                (double) minY,
                (double) boundingBox.minZ(),
                (double) (boundingBox.maxX() + 1),
                (double) (boundingBox.maxY() + 1),
                (double) (boundingBox.maxZ() + 1)
        );

        cir.setReturnValue(modifiedReturnValue);
    }

    private int newCubeInFrustum(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        try {
            java.lang.reflect.Method method = Frustum.class.getDeclaredMethod("cubeInFrustum", double.class, double.class, double.class, double.class, double.class, double.class);
            method.setAccessible(true);
            return (int) method.invoke(this, minX, minY, minZ, maxX, maxY, maxZ);
        } catch (Exception e) {
            throw new RuntimeException("Failed to call cubeInFrustum", e);
        }
    }
}