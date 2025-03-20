package lpcminigame.util;

import net.minecraft.world.World;

public class StringUtils {
    public static String getWorldStringId(World world){
        return world.getRegistryKey().getValue().toString();
    }
}
