package lpcminigame.games.dieOnNaturalBlocks;

import com.google.gson.Gson;
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

import static lpcminigame.Main.*;
import static lpcminigame.util.BlockUtils.*;
import static lpcminigame.util.FileUtils.*;
import static lpcminigame.util.StringUtils.*;

public class DieOnNaturalBlocks implements IGameMain {
    @Override public void onServerStarted(MinecraftServer server){
        ConfigDataWrapper.readAndLoad(this, server);
    }
    @Override public void onServerStopped(MinecraftServer server){
        if(isGameStarted()) stopGame(server);
        ConfigDataWrapper.wrapAndSave(this, server);
    }
    @Override public boolean isGameStarted(){return events != null;}
    @Override public String getGameId() {return "dieOnNaturalBlocks";}
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
        ConfigDataWrapper.wrapAndSave(this, server);
    }
    @Override public void clearData(MinecraftServer server) {
        if(isGameStarted()) {
            safePoses.clear();
            addRespawnPoints(server);
        }
        else deletePath(getDataDir(server));
    }
    @Override public void buildCommand(CommandBuilder command){
        IGameMain.super.buildCommand(command);
        CommandBuilder difficultyCommand = new CommandBuilder("difficulty");
        difficultyCommand.executes((context, info) -> info.success("Current Difficulty: " + difficulty.id));
        for(Difficulty difficulty : Difficulty.values())
            difficultyCommand.then(difficulty.id, (context, info) -> commandDifficulty(difficulty, info));
        command.then(difficultyCommand);
        CommandBuilder banVehicleCommand = new CommandBuilder("banVehicle");
        banVehicleCommand.executes((context, info) -> info.success("Current banVehicle: " + banVehicle));
        banVehicleCommand.then("true", (context, info) -> commandBanVehicle(true, info));
        banVehicleCommand.then("false", (context, info) -> commandBanVehicle(false, info));
        command.then(banVehicleCommand);
    }
    enum Difficulty{
        GOD("god", true, false, false, false),
        STRICT("strict", false, false, false, false),
        NORMAL("normal", false, false, true, false),
        LOOSE1("Dream-loose", false, true, true, false),
        LOOSE2("Another-loose", false, false, false, true),
        LOOSEST("loosest", false, true, false, true);
        public final String id;
        public final boolean testAllDirections;
        public final boolean directlyBelowEmptyAsTouchingSafe;
        public final boolean testOnlyNearestTouch;
        public final boolean escapeWhenTouchingSafe;
        Difficulty(String id, boolean testAllDirections, boolean directlyBelowEmptyAsTouchingSafe, boolean testOnlyNearestTouch, boolean escapeWhenTouchingSafe){
            this.id = id;
            this.testAllDirections = testAllDirections;
            this.directlyBelowEmptyAsTouchingSafe = directlyBelowEmptyAsTouchingSafe;
            this.testOnlyNearestTouch = testOnlyNearestTouch;
            this.escapeWhenTouchingSafe = escapeWhenTouchingSafe;
        }
        @NotNull public static Difficulty fromString(String id){
            for(Difficulty difficulty : Difficulty.values()){
                if(difficulty.id.equals(id))
                    return difficulty;
            }
            return NORMAL;
        }
    }
    Difficulty difficulty = Difficulty.NORMAL;
    boolean banVehicle = false;
    void commandDifficulty(Difficulty difficulty, runInfo info) {
        if(this.difficulty.equals(difficulty))
            info.exception("Difficulty is already set to " + difficulty.id);
        else{
            this.difficulty = difficulty;
            info.success("Difficulty now set to " + difficulty.id);
        }
    }
    void commandBanVehicle(boolean banVehicle, runInfo info) {
        if(this.banVehicle == banVehicle)
            info.exception("banVehicle is already set to " + banVehicle);
        else{
            this.banVehicle = banVehicle;
            info.success("banVehicle now set to " + banVehicle);
        }
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
    private static class ConfigDataWrapper {
        private String difficulty;
        private boolean banVehicle;
        private static String settingFilePath(DieOnNaturalBlocks game, MinecraftServer server){
            return String.valueOf(game.getDataDir(server).resolve("config.json"));
        }
        public static void readAndLoad(DieOnNaturalBlocks game, MinecraftServer server){
            try {
                ConfigDataWrapper config = (new Gson())
                        .fromJson(new FileReader(settingFilePath(game, server)), ConfigDataWrapper.class);
                game.difficulty = Difficulty.fromString(config.difficulty);
                game.banVehicle = config.banVehicle;
            } catch (FileNotFoundException ignore) {}
        }
        public static void wrapAndSave(DieOnNaturalBlocks game, MinecraftServer server){Gson gson = new Gson();
            ConfigDataWrapper wrapper = new ConfigDataWrapper();
            wrapper.difficulty = game.difficulty.id;
            wrapper.banVehicle = game.banVehicle;
            try (FileWriter writer = new FileWriter(settingFilePath(game, server))) {
                gson.toJson(wrapper, writer);
            } catch (IOException ignore) {}
        }
    }
}
