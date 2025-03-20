package lpcminigame.events;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

public interface UnregistrableServerTickEvents {
    UnregistrableEventEx<ServerTickEvents.EndTick> END_SERVER_TICK
            = new UnregistrableEventEx<>(
            ()->ServerTickEvents.END_SERVER_TICK.register(
                    (server)-> UnregistrableServerTickEvents.END_SERVER_TICK.invoker().onEndTick(server)),
            (server)->{
                for(ServerTickEvents.EndTick events : UnregistrableServerTickEvents.END_SERVER_TICK){
                    events.onEndTick(server);
                }
            }
    );
}
