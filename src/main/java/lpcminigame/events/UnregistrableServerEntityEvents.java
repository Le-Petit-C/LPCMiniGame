package lpcminigame.events;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;

public interface UnregistrableServerEntityEvents {
    UnregistrableEventEx<ServerEntityEvents.Load> ENTITY_LOAD
            = new UnregistrableEventEx<>(
            ()->ServerEntityEvents.ENTITY_LOAD.register(
                    (entity, world)-> UnregistrableServerEntityEvents.ENTITY_LOAD.invoker().onLoad(entity, world)),
            (entity, world)->{
                for(ServerEntityEvents.Load events : UnregistrableServerEntityEvents.ENTITY_LOAD){
                    events.onLoad(entity, world);
                }
            }
    );
    UnregistrableEventEx<ServerEntityEvents.Unload> ENTITY_UNLOAD
            = new UnregistrableEventEx<>(
            ()->ServerEntityEvents.ENTITY_UNLOAD.register(
                    (entity, world)-> UnregistrableServerEntityEvents.ENTITY_UNLOAD.invoker().onUnload(entity, world)),
            (entity, world)->{
                for(ServerEntityEvents.Unload events : UnregistrableServerEntityEvents.ENTITY_UNLOAD){
                    events.onUnload(entity, world);
                }
            }
    );
}
