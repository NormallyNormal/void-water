package com.normallynormal.void_water.mixin;

import com.normallynormal.void_water.Util;
import net.minecraft.client.renderer.SectionOcclusionGraph;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(SectionOcclusionGraph.class)
public class SectionOcclusionGraphMixin {
    @Redirect(
            method = "lambda$runPartialUpdate$4(Lnet/minecraft/client/renderer/culling/Frustum;Lnet/minecraft/client/renderer/chunk/SectionRenderDispatcher$RenderSection;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/culling/Frustum;isVisible(Lnet/minecraft/world/phys/AABB;)Z"
            )
    )
    private boolean ignoreFrustum(Frustum frustum, AABB bb) {
        return bb.minY <= Util.getMinYForLevel() || frustum.isVisible(bb);
    }
}
