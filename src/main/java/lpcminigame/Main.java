package lpcminigame;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import lpcminigame.games.all.All;
import lpcminigame.games.dieOnTouchingGround.DieOnTouchingGround;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.server.command.CommandManager;

import java.util.List;

public class Main implements ModInitializer, CommandRegistrationCallback {
	public static final String MOD_ID = "lpcminigame";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static final All all = new All();
	public static final List<IGameMain> games = List.of(
			new DieOnTouchingGround(),
			all
	);

	@Override public void onInitialize() {
		LOGGER.info("LPCMiniGame Initializing");
		for(IGameMain game : games) game.onModInitialize();
		CommandRegistrationCallback.EVENT.register(this);
		ServerLifecycleEvents.SERVER_STARTED.register(all);
		ServerLifecycleEvents.SERVER_STOPPED.register(all);
		LOGGER.info("LPCMiniGame Initialized");
	}
	@Override
	public void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
		CommandBuilder builder = new CommandBuilder("lpcminigame");
		builder.requires(source -> source.hasPermissionLevel(2));
		for (IGameMain game : games) {
			CommandBuilder subcommand = new CommandBuilder(game.getGameId());
			game.buildCommand(subcommand);
			builder.then(subcommand);
		}
		dispatcher.register(builder);
	}
	public interface Runner { void run(CommandContext<ServerCommandSource> context, RunInfo info);}
	public static class RunInfo {
		public Text message = Text.empty();
		public boolean exception = false;
		public int ret = 0;
		public void exception(Text message){
			this.message = message;
			exception = true;
			ret = 0;
		}
		public void success(Text message, int ret){
			this.message = message;
			this.ret = ret;
			exception = false;
		}
		public void success(Text message){
			success(message, 1);
		}
	}
	@SuppressWarnings({"UnusedReturnValue", "unused"})
	public static class CommandBuilder extends LiteralArgumentBuilder<ServerCommandSource>{
		public CommandBuilder(String literal) {super(literal);}
		public CommandBuilder executes(Runner runner){
			super.executes((context) ->{
				RunInfo info = new RunInfo();
				runner.run(context, info);
				if(info.exception) throw new CommandSyntaxException(null, () -> info.message.getString());
				else context.getSource().sendFeedback(() -> info.message, true);
				return info.ret;
			});
			return this;
		}
		public CommandBuilder then(String literal, Command<ServerCommandSource> command){
			then(CommandManager.literal(literal).executes(command));
			return this;
		}
		public CommandBuilder then(String literal, Runner runner){
			CommandBuilder subcommand = new CommandBuilder(literal);
			subcommand.executes(runner);
			then(subcommand);
			return this;
		}
	}
}