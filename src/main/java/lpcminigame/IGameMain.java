package lpcminigame;

import net.minecraft.server.MinecraftServer;

public interface IGameMain {
    String getGameId();
    default void onServerInitialize(){}
    default String onCommandCalled(MinecraftServer server){return "Null game";}
}
