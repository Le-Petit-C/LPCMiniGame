package lpcminigame.games.dieOnNaturalBlocks;

import lpcminigame.IGameMain;
import lpcminigame.events.UnregistrableEvent;
import lpcminigame.events.UnregistrableServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;

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
        for(ServerWorld world : server.getWorlds()){
            Identifier id = world.getRegistryKey().getValue();
            Path filePath = getDataDir(server).resolve(id.getNamespace()).resolve(id.getPath() + ".dat");
            try (DataInputStream stream = new DataInputStream(new FileInputStream(String.valueOf(filePath)))){
                HashSet<BlockPos> blockPosSet = safeBlocks.get(world);
                if(blockPosSet == null) continue;
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
                    for (BlockPos pos : safeBlocks.get(world)) {
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
    public static boolean ensureFile(Path filePath){
        if(Files.exists(filePath)) return !Files.isDirectory(filePath);
        if(!ensureDir(filePath.getParent())) return false;
        try {
            Files.createFile(filePath);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
    public static boolean ensureDir(Path dirPath){
        try {
            Files.createDirectories(dirPath);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
    @NotNull HashMap<World, HashSet<BlockPos>> safeBlocks = new HashMap<>();

    private UnregistrableEvent<ServerTickEvents.EndTick> endTickEvent;
}
