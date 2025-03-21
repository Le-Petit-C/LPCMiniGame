package lpcminigame.events;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;

public interface UnregistrablePlayerBlockBreakEvents {
    UnregistrableEventEx<PlayerBlockBreakEvents.After> AFTER
            = new UnregistrableEventEx<>(
            ()->PlayerBlockBreakEvents.AFTER.register(
                    (world, player, pos, state, block)->
                            UnregistrablePlayerBlockBreakEvents.AFTER.invoker()
                                    .afterBlockBreak(world, player, pos, state, block)),
            (world, player, pos, state, block)->{
                for(PlayerBlockBreakEvents.After events : UnregistrablePlayerBlockBreakEvents.AFTER){
                    events.afterBlockBreak(world, player, pos, state, block);
                }
            }
    );
}
