package lpcminigame.games.all;

import com.mojang.brigadier.context.CommandContext;
import lpcminigame.IGameMain;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;

import static lpcminigame.Main.*;

public class All implements IGameMain {
    @Override public void onServerStarted(MinecraftServer server){
        for(IGameMain game : games){
            if(game instanceof All) continue;
            game.onServerStarted(server);
        }
    }
    @Override public void onServerStopped(MinecraftServer server){
        for(IGameMain game : games){
            if(game instanceof All) continue;
            game.onServerStopped(server);
        }
    }
    @Override public String getGameId() {return "all";}
    @Override public boolean isGameStarted() {return false;}
    @Override public void stopGame(MinecraftServer server){
        for(IGameMain game : games){
            if(game instanceof All) continue;
            game.stopGame(server);
        }
    }
    @Override public void startGame(MinecraftServer server){
        for(IGameMain game : games){
            if(game instanceof All) continue;
            game.startGame(server);
        }
    }
    @Override public void commandRun(CommandContext<ServerCommandSource> context, runInfo info) {
        info.exception("All games already started");
        for(IGameMain game : games){
            if(game.isGameStarted()) continue;
            game.startGame(context.getSource().getServer());
            info.success("Started all games", info.ret + 1);
        }
    }
    @Override public void commandStop(CommandContext<ServerCommandSource> context, runInfo info) {
        info.exception("No game is running");
        for(IGameMain game : games){
            if(!game.isGameStarted() || game instanceof All) continue;
            game.stopGame(context.getSource().getServer());
            info.success("Stopped all games", info.ret + 1);
        }
    }
    @Override public void commandClear(CommandContext<ServerCommandSource> context, runInfo info) {
        for(IGameMain game : games)
            game.clearData(context.getSource().getServer());
        info.success("Cleared all game data");
    }
}
