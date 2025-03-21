package lpcminigame.events;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.util.ActionResult;

public interface UnregistrableUseBlockCallback {
    UnregistrableEventEx<UseBlockCallback> EVENT
            = new UnregistrableEventEx<>(
            ()->UseBlockCallback.EVENT.register(
                    (player, world, hand, hit)->
                            UnregistrableUseBlockCallback.EVENT.invoker().interact(player, world, hand, hit)),
            (player, world, hand, hit)->{
                for(UseBlockCallback events : UnregistrableUseBlockCallback.EVENT){
                    ActionResult result = events.interact(player, world, hand, hit);
                    if(result != ActionResult.PASS) return result;
                }
                return ActionResult.PASS;
            }
    );
}
