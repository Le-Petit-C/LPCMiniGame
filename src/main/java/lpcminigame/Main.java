package lpcminigame;

import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main implements ModInitializer {
	public static final String MOD_ID = "lpcminigame";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("LPCMiniGame Initializing");
		PlayerPlaceBlockCallback playerUseBlockCallback = new PlayerPlaceBlockCallback();
		UseBlockCallback.EVENT.register(playerUseBlockCallback);
		ServerTickEvents.END_SERVER_TICK.register(playerUseBlockCallback);
		LOGGER.info("LPCMiniGame Initialized");
	}
}