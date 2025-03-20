package lpcminigame.games.dieOnNaturalBlocks;

import lpcminigame.IGameMain;
import lpcminigame.events.UnregistrableEvent;
import lpcminigame.events.UnregistrableServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;

import static lpcminigame.util.FileUtils.*;
import static lpcminigame.util.StringUtils.*;

public class DieOnNaturalBlocks implements IGameMain {
    @Override public boolean isGameStarted(){
        return endTickEvent != null;
    }
    @Override public String getGameId() {
        return "dieOnNaturalBlocks";
    }
    @Override public void startGame(MinecraftServer server){
        if(isGameStarted()) return;
        endTickEvent = UnregistrableServerTickEvents.END_SERVER_TICK.register(new OnServerEndTick(this));
        addRespawnPoints(server);
        for(ServerWorld world : server.getWorlds()){
            Identifier id = world.getRegistryKey().getValue();
            Path filePath = getDataDir(server).resolve(id.getNamespace()).resolve(id.getPath() + ".dat");
            try (DataInputStream stream = new DataInputStream(new FileInputStream(String.valueOf(filePath)))){
                String worldStringId = getWorldStringId(world);
                HashSet<BlockPos> blockPosSet = safeBlocks.computeIfAbsent(worldStringId, k -> new HashSet<>());
                while(stream.available() > 0){
                    blockPosSet.add(new BlockPos(
                            stream.readInt(),
                            stream.readInt(),
                            stream.readInt()
                    ));
                }
            }catch (IOException ignore){}
        }
    }
    @Override public void stopGame(MinecraftServer server){
        for (ServerWorld world : server.getWorlds()) {
            Identifier id = world.getRegistryKey().getValue();
            Path filePath = getDataDir(server).resolve(id.getNamespace()).resolve(id.getPath() + ".dat");
            if (ensureFile(filePath)) {
                try (DataOutputStream stream = new DataOutputStream(new FileOutputStream(String.valueOf(filePath)))) {
                    String worldStringId = getWorldStringId(world);
                    HashSet<BlockPos> set = safeBlocks.get(worldStringId);
                    if(set == null) continue;
                    for (BlockPos pos : set) {
                        stream.writeInt(pos.getX());
                        stream.writeInt(pos.getY());
                        stream.writeInt(pos.getZ());
                    }
                } catch (IOException ignore) {}
            }
        }
        safeBlocks.clear();
        endTickEvent.unregister();
        endTickEvent = null;
    }
    @Override public void clearData(MinecraftServer server) {
        if(isGameStarted()) {
            safeBlocks.clear();
            addRespawnPoints(server);
        }
        else deletePath(getDataDir(server));
    }
    @NotNull HashMap<String, HashSet<BlockPos>> safeBlocks = new HashMap<>();

    private UnregistrableEvent<ServerTickEvents.EndTick> endTickEvent;
    private void addRespawnPoints(MinecraftServer server){
        String overworldStringId = getWorldStringId(server.getOverworld());
        HashSet<BlockPos> set = safeBlocks.computeIfAbsent(overworldStringId, k -> new HashSet<>());
        int respawnRadius = server.getSpawnRadius(server.getOverworld());
        BlockPos worldSpawnPos = server.getOverworld().getSpawnPos();
        int minY = server.getOverworld().getBottomY();
        int maxY = minY + server.getOverworld().getHeight() - 1;
        for(BlockPos pos : BlockPos.iterate(
                worldSpawnPos.getX() - respawnRadius, minY, worldSpawnPos.getZ() - respawnRadius,
                worldSpawnPos.getX() + respawnRadius, maxY, worldSpawnPos.getZ() + respawnRadius
        )){
            if(!set.contains(pos))
                set.add(new BlockPos(pos));
        }
    }
}
