package lpcminigame.events;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public interface PlayerPlaceBlockCallback{
    UnregistrableEventEx<PlayerPlaceBlockCallback> EVENT = new UnregistrableEventEx<>(
            null,
            (world, pos, player)->{
                for(PlayerPlaceBlockCallback event : PlayerPlaceBlockCallback.EVENT){
                    ActionResult result = event.interact(world, pos, player);
                    if(result != ActionResult.PASS) return result;
                }
                return ActionResult.PASS;
            }
            );
    ActionResult interact(World world, BlockPos pos, PlayerEntity player);
}
