package lpcminigame.util;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class BlockUtils {
    public static boolean isEmptyCollisionBlock(World world, BlockPos pos){
        return world.getBlockState(pos).getCollisionShape(world, pos).isEmpty();
    }
}
