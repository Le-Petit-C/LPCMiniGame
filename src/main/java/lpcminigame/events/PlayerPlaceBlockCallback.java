package lpcminigame.events;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public interface PlayerPlaceBlockCallback{
    Event<PlayerPlaceBlockCallback> EVENT = EventFactory.createArrayBacked(PlayerPlaceBlockCallback.class,
        (listeners) -> (world, pos, player) -> {
            for (PlayerPlaceBlockCallback listener : listeners) {
                ActionResult result = listener.interact(world, pos, player);
                if(result != ActionResult.PASS) {
                    return result;
                }
            }
            return ActionResult.PASS;
        });

    ActionResult interact(World world, BlockPos pos, PlayerEntity player);

    /*private final Queue<PossiblyChangedBlockData> queue = new LinkedList<>();
    private record PossiblyChangedBlockData(World world, BlockPos pos, PlayerEntity player, Block oldBlock){}
    public static void blockPlacedByPlayer(World world, BlockPos pos, PlayerEntity player){
        player.sendMessage(Text.of("Place block detected!"), true);
    }*/
}
