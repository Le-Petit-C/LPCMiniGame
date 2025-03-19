package lpcminigame;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import lpcminigame.games.dieOnNaturalBlocks.DieOnNaturalBlocks;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.server.command.CommandManager;

import java.util.Collection;
import java.util.LinkedHashMap;
//import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class Main implements ModInitializer {
	public static final String MOD_ID = "lpcminigame";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static final Collection<IGameMain> games = List.of(
			new DieOnNaturalBlocks()
	);
	//public static final LinkedHashSet<String> gameIds = buildGameIds();
	public static final LinkedHashMap<String, IGameMain> gameIdToGame = buildGameIdToGame();

	@Override
	public void onInitialize() {
		LOGGER.info("LPCMiniGame Initializing");
		for(IGameMain game : games) game.onServerInitialize();
		CommandRegistrationCallback.EVENT.register(new LPCMiniGameCommand());
		LOGGER.info("LPCMiniGame Initialized");
	}

	/*private static LinkedHashSet<String> buildGameIds(){
		LinkedHashSet<String> set = new LinkedHashSet<>();
		for(IGameMain game : games) set.add(game.getGameId());
		return set;
	}*/
	private static LinkedHashMap<String, IGameMain> buildGameIdToGame(){
		LinkedHashMap<String, IGameMain> map = new LinkedHashMap<>();
		for(IGameMain game : games) map.put(game.getGameId(), game);
		return map;
	}
	private static class LPCMiniGameCommand implements CommandRegistrationCallback, Command<ServerCommandSource>, SuggestionProvider<ServerCommandSource> {
		@Override
		public void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
			LiteralArgumentBuilder<ServerCommandSource> builder = CommandManager.literal("lpcminigame");
			builder.requires(source -> source.hasPermissionLevel(2));
			StringArgumentType arg = StringArgumentType.word();
			RequiredArgumentBuilder<ServerCommandSource, String> argument1 = CommandManager.argument("gameId", arg);
			argument1.suggests(this);
			argument1.executes(this);
			builder.then(argument1);
			dispatcher.register(builder);
		}

		@Override
		public int run(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
			String gameId = StringArgumentType.getString(context, "gameId");
			IGameMain game = gameIdToGame.get(gameId);
			if(game == null) throw new CommandSyntaxException(null, ()->"No matching game");
			String str = game.onCommandCalled(context.getSource().getServer());
			context.getSource().sendFeedback(() -> Text.literal(str), true);
			return 1;
		}

		@Override
		public CompletableFuture<Suggestions> getSuggestions(
				CommandContext<ServerCommandSource> context,
				SuggestionsBuilder builder) throws CommandSyntaxException {
			boolean hasMatches = false;
			for(IGameMain game : games){
				String str = game.getGameId();
				if(!str.contains(builder.getRemaining())) continue;
				hasMatches = true;
				builder.suggest(str);
			}
			if(!hasMatches) throw new CommandSyntaxException(null,()->"No matching string.");
			return builder.buildFuture();
		}
	}
}