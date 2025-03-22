package lpcminigame;

import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.WorldSavePath;

import java.nio.file.Path;

import static lpcminigame.Main.*;

public interface IGameMain extends
        ServerLifecycleEvents.ServerStarted,
        ServerLifecycleEvents.ServerStopped
{
    String getGameId();
    boolean isGameStarted();
    default void onModInitialize(){}
    @Override default void onServerStarted(MinecraftServer world){}
    @Override default void onServerStopped(MinecraftServer world){}
    default void startGame(MinecraftServer server){}
    default void stopGame(MinecraftServer server){}
    default void clearData(MinecraftServer server){}
    default Path getDataDir(MinecraftServer server){
        return server.getSavePath(WorldSavePath.ROOT).resolve(MOD_ID).resolve(getGameId());
    }
    default void commandRun(CommandContext<ServerCommandSource> context, runInfo info) {
        if(isGameStarted()) info.exception("Game \"" + getGameId() + "\" already started");
        else{
            startGame(context.getSource().getServer());
            info.success("Started game \"" + getGameId() + "\"");
        }
    }
    default void commandStop(CommandContext<ServerCommandSource> context, runInfo info) {
        if (!isGameStarted()) info.exception("Game " + getGameId() + " is not running");
        else{
            stopGame(context.getSource().getServer());
            info.success("Stopped game \"" + getGameId() + "\"");
        }
    }
    default void commandClear(CommandContext<ServerCommandSource> context, runInfo info) {
        clearData(context.getSource().getServer());
        info.success("Cleared game \"" + getGameId() + "\" data");
    }
    default void buildCommand(CommandBuilder command){
        command.then("start", this::commandRun);
        command.then("stop", this::commandStop);
        command.then("clear", this::commandClear);
    }
}
