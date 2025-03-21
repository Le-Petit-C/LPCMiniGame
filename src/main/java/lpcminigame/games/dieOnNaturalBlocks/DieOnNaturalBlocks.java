package lpcminigame.games.dieOnNaturalBlocks;

import lpcminigame.IGameMain;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;

import static lpcminigame.util.BlockUtils.*;
import static lpcminigame.util.FileUtils.*;
import static lpcminigame.util.StringUtils.*;

public class DieOnNaturalBlocks implements IGameMain {
    @Override public boolean isGameStarted(){
        return events != null;
    }
    @Override public String getGameId() {
        return "dieOnNaturalBlocks";
    }
    @Override public void startGame(MinecraftServer server){
        if(isGameStarted()) return;
        events = new Events(this);
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
        if(!isGameStarted()) return;
        for (ServerWorld world : server.getWorlds()) {
            Identifier id = world.getRegistryKey().getValue();
            Path filePath = getDataDir(server).resolve(id.getNamespace()).resolve(id.getPath() + ".dat");
            if (ensureFile(filePath)) {
                try (DataOutputStream stream = new DataOutputStream(new FileOutputStream(String.valueOf(filePath)))) {
                    HashSet<BlockPos> set = safePoses.get(world);
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
        events.disable();
        events = null;
    }
    @Override public void clearData(MinecraftServer server) {
        if(isGameStarted()) {
            safePoses.clear();
            addRespawnPoints(server);
        }
        else deletePath(getDataDir(server));
    }

    @NotNull DataMap safePoses = new DataMap();
    @NotNull FallingBlockMap unnaturalFallingBlocks = new FallingBlockMap();
    static class DataMap extends HashMap<String, DataClass>{
        @Nullable DataClass get(World world){
            return super.get(getWorldStringId(world));
        }
        @NotNull DataClass computeIfAbsent(ServerWorld world){
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
        public void addWithoutTest(BlockPos pos) {
            super.add(new BlockPos(pos));
        }
        public void refTest(){
            for (BlockPos pos : posesShouldTest)
                if (isEmptyCollisionBlock(world, pos))
                    super.remove(pos);
            posesShouldTest.clear();
        }
        public void clearTest(){
            posesShouldTest.clear();
        }
    }
    static class FallingBlockMap extends HashMap<String, FallingBlockSet>{
        @Nullable FallingBlockSet get(World world){
            return super.get(getWorldStringId(world));
        }
        @NotNull FallingBlockSet computeIfAbsent(String key){
            return super.computeIfAbsent(key, s -> new FallingBlockSet());
        }
        @NotNull FallingBlockSet computeIfAbsent(World world){
            return computeIfAbsent(getWorldStringId(world));
        }
    }
    static class FallingBlockSet extends HashSet<FallingBlockEntity>{}

    private Events events;
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
