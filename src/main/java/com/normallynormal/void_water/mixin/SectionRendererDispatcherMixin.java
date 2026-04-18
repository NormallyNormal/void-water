package com.normallynormal.void_water.mixin;

import com.normallynormal.void_water.Config;
import com.normallynormal.void_water.Util;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.core.SectionPos;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;

@Mixin(SectionRenderDispatcher.RenderSection.class)
public class SectionRendererDispatcherMixin {

    private static Field bbField;
    private static Field originField;

    static {
        try {
            bbField = SectionRenderDispatcher.RenderSection.class.getDeclaredField("bb");
            bbField.setAccessible(true);
        } catch (NoSuchFieldException ignored) {

        }
    }

    static {
        try {
            originField = SectionRenderDispatcher.RenderSection.class.getDeclaredField("origin");
            originField.setAccessible(true);
        } catch (NoSuchFieldException ignored) {

        }
    }

    @Inject(
            method = "setSectionNode(J)V", // Method descriptor
            at = @At("TAIL") // Inject at the end of the method, after bb is assigned
    )
    private void modifyBB(long sectionNode, CallbackInfo ci) throws IllegalAccessException {
        // Modify the bb field
        double i = SectionPos.sectionToBlockCoord(SectionPos.x(sectionNode));
        double j = SectionPos.sectionToBlockCoord(SectionPos.y(sectionNode));
        double k = SectionPos.sectionToBlockCoord(SectionPos.z(sectionNode));
        int minY = Util.getMinYForLevel();

        if (j == minY) {
            double j2 = j - Config.trailLength;
            AABB modifiedBB = new AABB(
                    i, j2, k,
                    i + 16, j + 16, k + 16
            );
            bbField.set(this, modifiedBB);
        }
    }
}
