package lpcminigame.games.dieOnNaturalBlocks;

import lpcminigame.IGameMain;
import lpcminigame.events.PlayerPlaceBlockCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.HashSet;

public class DieOnNaturalBlocks implements IGameMain {
    @Override public String getGameId() {
        return "dieOnNaturalBlocks";
    }
    @Override public String onCommandCalled(MinecraftServer server){
        if(gameStarted) return "Game " + getGameId() + " already started";
        PlayerPlaceBlockCallback.EVENT.register(new OnPlayerPlaceBlock(this));
        ServerTickEvents.END_SERVER_TICK.register(new OnServerEndTick(this));
        for(ServerWorld world : server.getWorlds())
            safeBlocks.put(world, new HashSet<>());
        int respawnRadius = server.getSpawnRadius(server.getOverworld());
        BlockPos worldSpawnPos = server.getOverworld().getSpawnPos();
        HashSet<BlockPos> set = safeBlocks.get(server.getOverworld());
        int minY = server.getOverworld().getBottomY();
        int maxY = minY + server.getOverworld().getHeight() - 1;
        for(int x = -respawnRadius; x <= respawnRadius ; ++x){
            for(int y = minY; y <= maxY; ++y){
                for(int z = -respawnRadius; z <= respawnRadius; ++z){
                    set.add(new BlockPos(
                            worldSpawnPos.getX() + x,
                            y,
                            worldSpawnPos.getZ() + z)
                    );
                }
            }
        }
        gameStarted = true;
        return "Game " + getGameId() + " starts";
    }
    boolean gameStarted = false;
    @NotNull HashMap<World, HashSet<BlockPos>> safeBlocks = new HashMap<>();
}
