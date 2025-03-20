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
			RequiredArgumentBuilder<ServerCommandSource, String> argument2 = CommandManager.argument("option", arg);
			argument2.suggests(new StringCollectionCommandSuggestionProvider(List.of("start", "stop", "clear")));
			argument2.executes(this);
			RequiredArgumentBuilder<ServerCommandSource, String> argument1 = CommandManager.argument("gameId", arg);
			ArrayList<String> suggestList = new ArrayList<>(gameIds);
			suggestList.add("all");
			argument1.suggests(new StringCollectionCommandSuggestionProvider(suggestList));
			argument1.then(argument2);
			LiteralArgumentBuilder<ServerCommandSource> builder = CommandManager.literal("lpcminigame");
			builder.requires(source -> source.hasPermissionLevel(2));
			builder.then(argument1);
			dispatcher.register(builder);
		}

		@Override
		public int run(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
			String gameId = StringArgumentType.getString(context, "gameId");
			boolean isAll = gameId.equals("all");
			IGameMain game = gameIdToGame.get(gameId);
			if(game == null && !isAll) throw new CommandSyntaxException(null, ()->"No matching game");
			String option = StringArgumentType.getString(context, "option");
			MinecraftServer server = context.getSource().getServer();
			String feedbackString;
			boolean exception = false;
            switch (option) {
				case "start":
					if(isAll){
						exception = true;
						for(IGameMain gameMain : games){
							if(!gameMain.isGameStarted()){
								gameMain.startGame(server);
								exception = false;
							}
						}
						if(exception) feedbackString = "All games already started";
						else feedbackString = "Started all games";
					}
					else{
						exception = game.isGameStarted();
						if(exception) feedbackString = "Game \"" + game.getGameId() + "\" already started";
						else{
							game.startGame(server);
							feedbackString = "Started game \"" + game.getGameId() + "\"";
						}
					}
					break;
				case "stop":
					if(isAll){
						exception = true;
						for(IGameMain gameMain : games){
							if(gameMain.isGameStarted()){
								gameMain.stopGame(server);
								exception = false;
							}
						}
						if(exception) feedbackString = "No game is running";
						else feedbackString = "Stopped all games";
					}
					else{
						exception = !game.isGameStarted();
						if (exception) feedbackString = "Game " + game.getGameId() + " is not running";
						else{
							game.stopGame(server);
							feedbackString = "Stopped game \"" + game.getGameId() + "\"";
						}
					}
					break;
				case "clear":
					if(isAll){
						for(IGameMain gameMain : games)
							if(gameMain.isGameStarted())
								gameMain.clearData(server);
						feedbackString = "Cleared all game data";
					}
					else{
						game.clearData(server);
						feedbackString = "Cleared game \"" + game.getGameId() + "\" data";
					}
                    break;
				default:
					exception = true;
					feedbackString = "Argument 2 should be \"start\", \"stop\" or \"clear\" but not \"" + option +"\"";
            }
			if(exception) throw new CommandSyntaxException(null, () -> feedbackString);
			context.getSource().sendFeedback(() -> Text.literal(feedbackString), true);
			return 1;
		}
	}
}