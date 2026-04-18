package com.normallynormal.void_water;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;

public class Util {
    public static int getMinYForLevel() {
        int minY = Integer.MIN_VALUE;
        try {
            assert Minecraft.getInstance().level != null;
            minY = Minecraft.getInstance().level.dimensionType().minY();
        }
        catch (NullPointerException ignored) {

        }
        return minY;
    }

    public static int getMinYForLevel(Level level) {
        return level.dimensionType().minY();
    }
}
