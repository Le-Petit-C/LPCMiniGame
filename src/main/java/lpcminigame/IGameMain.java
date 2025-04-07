package lpcminigame;

import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
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
    default Text getGameName(){
        return Text.translatable("lpcminigame." + getGameId() + ".gameName");
    }
    default void commandRun(CommandContext<ServerCommandSource> context, RunInfo info) {
        if(isGameStarted())
            info.exception(Text.translatable("lpcminigame.gameAlreadyStarted", getGameName()));
        else{
            startGame(context.getSource().getServer());
            info.success(Text.translatable("lpcminigame.gameStarted", getGameName()));
        }
    }
    default void commandStop(CommandContext<ServerCommandSource> context, RunInfo info) {
        if (!isGameStarted())
            info.exception(Text.translatable("lpcminigame.gameNotRunning", getGameName()));
        else{
            stopGame(context.getSource().getServer());
            info.success(Text.translatable("lpcminigame.gameStopped", getGameName()));
        }
    }
    default void commandClear(CommandContext<ServerCommandSource> context, RunInfo info) {
        clearData(context.getSource().getServer());
        info.success(Text.translatable("lpcminigame.gameCleared", getGameName()));
    }
    default void buildCommand(CommandBuilder command){
        command.then("start", this::commandRun);
        command.then("stop", this::commandStop);
        command.then("clear", this::commandClear);
    }
}
