package lpcminigame.util;

import net.minecraft.util.math.Vec3i;

public class MathUtils {
    public static int getChebyshevDistance(Vec3i a, Vec3i b) {
        int dx = Math.abs(a.getX() - b.getX());
        int dy = Math.abs(a.getY() - b.getY());
        int dz = Math.abs(a.getZ() - b.getZ());
        return Math.max(Math.max(dx, dy), dz);
    }
}
