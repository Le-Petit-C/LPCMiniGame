package lpcminigame;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import lpcminigame.games.dieOnNaturalBlocks.DieOnNaturalBlocks;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.server.command.CommandManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;

public class Main implements ModInitializer, ServerLifecycleEvents.ServerStopping {
	public static final String MOD_ID = "lpcminigame";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static final Collection<IGameMain> games = List.of(
			new DieOnNaturalBlocks()
	);
	public static final ArrayList<String> gameIds = buildGameIds();
	public static final LinkedHashMap<String, IGameMain> gameIdToGame = buildGameIdToGame();

	@Override public void onInitialize() {
		LOGGER.info("LPCMiniGame Initializing");
		for(IGameMain game : games) game.onServerInitialize();
		CommandRegistrationCallback.EVENT.register(new LPCMiniGameCommand());
		ServerLifecycleEvents.SERVER_STOPPING.register(this);
		LOGGER.info("LPCMiniGame Initialized");
	}
	@Override public void onServerStopping(MinecraftServer server) {
		for(IGameMain game : games){
			if(game.isGameStarted())
				game.stopGame(server);
		}
	}

	private static ArrayList<String> buildGameIds(){
		ArrayList<String> list = new ArrayList<>();
		for(IGameMain game : games) list.add(game.getGameId());
		return list;
	}
	private static LinkedHashMap<String, IGameMain> buildGameIdToGame(){
		LinkedHashMap<String, IGameMain> map = new LinkedHashMap<>();
		for(IGameMain game : games) map.put(game.getGameId(), game);
		return map;
	}

	private static class LPCMiniGameCommand implements CommandRegistrationCallback, Command<ServerCommandSource> {
		@Override
		public void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
			StringArgumentType arg = StringArgumentType.word();
			RequiredArgumentBuilder<ServerCommandSource, String> argument2 = CommandManager.argument("start/stop", arg);
			argument2.suggests(new StringCollectionCommandSuggestionProvider(List.of("start", "stop")));
			argument2.executes(this);
			RequiredArgumentBuilder<ServerCommandSource, String> argument1 = CommandManager.argument("gameId", arg);
			argument1.suggests(new StringCollectionCommandSuggestionProvider(gameIds));
			argument1.then(argument2);
			LiteralArgumentBuilder<ServerCommandSource> builder = CommandManager.literal("lpcminigame");
			builder.requires(source -> source.hasPermissionLevel(2));
			builder.then(argument1);
			dispatcher.register(builder);
		}

		@Override
		public int run(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
			String gameId = StringArgumentType.getString(context, "gameId");
			IGameMain game = gameIdToGame.get(gameId);
			if(game == null) throw new CommandSyntaxException(null, ()->"No matching game");
			String startOrStop = StringArgumentType.getString(context, "start/stop");
			if(startOrStop.equals("start")){
				if(game.isGameStarted()) throw new CommandSyntaxException(null, ()-> "Game " + game.getGameId() + " already started");
				game.startGame(context.getSource().getServer());
				context.getSource().sendFeedback(() -> Text.literal("Game " + game.getGameId() + " starts"), true);
				return 1;
			}
			if(startOrStop.equals("stop")){
				if(!game.isGameStarted()) throw new CommandSyntaxException(null, ()-> "Game " + game.getGameId() + " is not started");
				game.stopGame(context.getSource().getServer());
				context.getSource().sendFeedback(() -> Text.literal("Game " + game.getGameId() + " stops"), true);
				return 1;
			}
			throw new CommandSyntaxException(null, ()-> "Argument 2 should be \"start\" or \"stop\" but not \"" + startOrStop +"\"");
		}
	}
}