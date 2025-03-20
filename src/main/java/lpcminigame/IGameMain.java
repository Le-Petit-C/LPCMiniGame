package lpcminigame;

import net.minecraft.server.MinecraftServer;
import net.minecraft.util.WorldSavePath;

import java.nio.file.Path;

public interface IGameMain {
    String getGameId();
    boolean isGameStarted();
    default void onServerInitialize(){}
    default void startGame(MinecraftServer server){}
    default void stopGame(MinecraftServer server){}
    default Path getDataDir(MinecraftServer server){
        return server.getSavePath(WorldSavePath.ROOT).resolve(Main.MOD_ID).resolve(getGameId());
    }
}
