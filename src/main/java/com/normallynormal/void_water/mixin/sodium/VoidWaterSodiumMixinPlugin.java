package com.normallynormal.void_water.mixin.sodium;

import net.neoforged.fml.loading.LoadingModList;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class VoidWaterSodiumMixinPlugin implements IMixinConfigPlugin {
    private boolean sodiumPresent;

    @Override
    public void onLoad(String mixinPackage) {
        try {
            sodiumPresent = LoadingModList.get().getModFileById("sodium") != null;
        } catch (Throwable t) {
            sodiumPresent = false;
        }
        System.out.println("[VoidWater] Sodium mixin config loaded. sodiumPresent=" + sodiumPresent);
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return sodiumPresent;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass,
                         String mixinClassName, IMixinInfo mixinInfo) {}

    @Override
    public void postApply(String targetClassName, ClassNode targetClass,
                          String mixinClassName, IMixinInfo mixinInfo) {}
}
