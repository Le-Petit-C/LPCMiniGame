package lpcminigame.games.all;

import com.mojang.brigadier.context.CommandContext;
import lpcminigame.IGameMain;
import net.minecraft.server.command.ServerCommandSource;

import static lpcminigame.Main.*;

public class All implements IGameMain {
    @Override public String getGameId() {return "all";}
    @Override public boolean isGameStarted() {return false;}
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
            if(!game.isGameStarted()) continue;
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
