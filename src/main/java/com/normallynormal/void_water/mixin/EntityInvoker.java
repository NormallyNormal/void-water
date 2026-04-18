package com.normallynormal.void_water.mixin;

import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.fluids.FluidType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Entity.class)
public interface EntityInvoker {
    @Invoker("setFluidTypeHeight")
    void invokeSetFluidTypeHeight(FluidType fluidType, double height);
}