package lpcminigame.games.dieOnNaturalBlocks;

import lpcminigame.IGameMain;
import lpcminigame.events.UnregistrableEvent;
import lpcminigame.events.UnregistrableServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import static lpcminigame.util.BlockUtils.*;
import static lpcminigame.util.FileUtils.*;
import static lpcminigame.util.MathUtils.*;
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
                HashSet<BlockPos> blockPosSet =
                        safePoses.computeIfAbsent(world);
                while(stream.available() > 0){
                    blockPosSet.add(new BlockPos(
                            stream.readInt(),
                            stream.readInt(),
                            stream.readInt()
                    ));
                }
            }catch (IOException ignore){}
        }
        safePoses.clearTest();
    }
    @Override public void stopGame(MinecraftServer server){
        for (ServerWorld world : server.getWorlds()) {
            Identifier id = world.getRegistryKey().getValue();
            Path filePath = getDataDir(server).resolve(id.getNamespace()).resolve(id.getPath() + ".dat");
            if (ensureFile(filePath)) {
                try (DataOutputStream stream = new DataOutputStream(new FileOutputStream(String.valueOf(filePath)))) {
                    String worldStringId = getWorldStringId(world);
                    HashSet<BlockPos> set = safePoses.get(worldStringId);
                    if(set == null) continue;
                    for (BlockPos pos : set) {
                        stream.writeInt(pos.getX());
                        stream.writeInt(pos.getY());
                        stream.writeInt(pos.getZ());
                    }
                } catch (IOException ignore) {}
            }
        }
        safePoses.clear();
        endTickEvent.unregister();
        endTickEvent = null;
    }
    @Override public void clearData(MinecraftServer server) {
        if(isGameStarted()) {
            safePoses.clear();
            addRespawnPoints(server);
        }
        else deletePath(getDataDir(server));
    }

    @NotNull DataMap safePoses = new DataMap();
    static class DataMap extends HashMap<String, DataClass>{
        @NotNull HashSet<BlockPos> computeIfAbsent(ServerWorld world){
            return computeIfAbsent(getWorldStringId(world), k -> new DataClass(world));
        }
        public void refTest(){
            for(DataClass data : super.values())
                data.refTest();
        }
        public void clearTest(){
            for(DataClass data : super.values()){
                data.clearTest();
            }
        }
    }
    static class DataClass extends HashSet<BlockPos>{
        public ServerWorld world;
        @NotNull HashSet<BlockPos> posesShouldTest;
        public DataClass(@NotNull ServerWorld world, @NotNull HashSet<BlockPos> poses){
            super(poses);
            this.world = world;
            posesShouldTest = new HashSet<>();
        }
        public DataClass(@NotNull ServerWorld world){this(world, new HashSet<>());}
        @Override public boolean add(BlockPos pos) {
            BlockPos pos1 = new BlockPos(pos);
            posesShouldTest.add(pos1);
            return super.add(pos1);
        }
        public void refTest(){
            for (Iterator<BlockPos> iterator = posesShouldTest.iterator(); iterator.hasNext(); ) {
                BlockPos pos = iterator.next();
                boolean shouldRemove = true;
                for(ServerPlayerEntity player : world.getPlayers()){
                    BlockPos eyePos = BlockPos.ofFloored(player.getEyePos());
                    if(getChebyshevDistance(eyePos, pos) <= 6){
                        shouldRemove = false;
                        break;
                    }
                }
                if(shouldRemove){
                    if(isEmptyCollisionBlock(world, pos))
                        super.remove(pos);
                    iterator.remove();
                }
            }
        }
        public void clearTest(){
            posesShouldTest.clear();
        }
    }

    private UnregistrableEvent<ServerTickEvents.EndTick> endTickEvent;
    private void addRespawnPoints(MinecraftServer server){
        HashSet<BlockPos> set = safePoses.computeIfAbsent(server.getOverworld());
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
