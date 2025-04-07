package lpcminigame.games.dieOnTouchingGround;

import lpcminigame.events.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.UUID;

import static lpcminigame.util.BlockUtils.*;
import static lpcminigame.games.dieOnTouchingGround.DieOnTouchingGround.*;

public class Events implements
        ServerTickEvents.EndTick,
        UseBlockCallback,
        ServerEntityEvents.Load,
        ServerEntityEvents.Unload,
        PlayerBlockBreakEvents.After
{
    private static final double expandValue = 0.001;
    private final HashMap<UUID, Double> playerYMaxRecord = new HashMap<>();
    Events(DieOnTouchingGround game){
        this.game = game;
        endTick = UnregistrableServerTickEvents.END_SERVER_TICK.register(this);
        useBlockCallback = UnregistrableUseBlockCallback.EVENT.register(this);
        loadEntity = UnregistrableServerEntityEvents.ENTITY_LOAD.register(this);
        unloadEntity = UnregistrableServerEntityEvents.ENTITY_UNLOAD.register(this);
        afterBlockBroken = UnregistrablePlayerBlockBreakEvents.AFTER.register(this);
    }
    public void disable(){
        endTick.unregister();
        useBlockCallback.unregister();
        loadEntity.unregister();
        unloadEntity.unregister();
        afterBlockBroken.unregister();
    }
    @Override public void onLoad(Entity entity, ServerWorld world) {
        if(entity instanceof FallingBlockEntity block){
            BlockPos pos = block.getBlockPos();
            DieOnTouchingGround.DataClass set = game.safePoses.get(world);
            if(set == null || !set.contains(pos)) return;
            set.remove(pos);
            game.unnaturalFallingBlocks.computeIfAbsent(world).add(block);
        }
    }
    @Override public void onUnload(Entity entity, ServerWorld world) {
        if(entity instanceof FallingBlockEntity block){
            DieOnTouchingGround.FallingBlockSet blockSet = game.unnaturalFallingBlocks.get(world);
            if(blockSet == null || !blockSet.contains(block)) return;
            blockSet.remove(block);
            DieOnTouchingGround.DataClass data = game.safePoses.computeIfAbsent(world);
            if(entity.isRemoved()) data.add(block.getBlockPos());
            else data.addWithoutTest(block.getBlockPos());
        }
    }
    @Override public void onEndTick(MinecraftServer server) {
        for(ServerPlayerEntity player : server.getPlayerManager().getPlayerList()){
            if(player.isDead()) continue;
            if(player.isCreative()) continue;
            if(player.isSpectator()) continue;
            if(player.getVehicle() != null){
                if(game.banVehicle) player.kill(player.getServerWorld());
                continue;
            }
            Box playerBox = player.getBoundingBox();
            boolean shouldResetBox;
            if(player.isOnGround()) shouldResetBox = false;
            else if(player.getMovement().getY() <= 0 && playerYMaxRecord.containsKey(player.getUuid()))
                shouldResetBox = playerYMaxRecord.get(player.getUuid()) < playerBox.maxY;
            else shouldResetBox = false;
            Box box;
            if(!shouldResetBox){
                playerYMaxRecord.put(player.getUuid(), playerBox.maxY);
                box = playerBox;
            }
            else box = new Box(
                        playerBox.minX,
                        playerBox.minY,
                        playerBox.minZ,
                        playerBox.maxX,
                        playerYMaxRecord.get(player.getUuid()),
                        playerBox.maxZ
                );
            if(game.difficulty.testAllDirections){
                if(testPlayerWithBox(player, box.expand(expandValue, 0, 0))) continue;
                if(testPlayerWithBox(player, box.expand(0, 0, expandValue))) continue;
                testPlayerWithBox(player, box.expand(0, expandValue, 0));
            }
            else {
                Box box1 = new Box(box.getMinPos().add(0, -expandValue, 0), box.getMaxPos());
                testPlayerWithBox(player, box1);
            }
        }
        game.safePoses.refTest();
    }
    @Override public ActionResult interact(PlayerEntity player, World world, Hand hand, BlockHitResult hit) {
        BlockPos pos = hit.getBlockPos();
        if(world instanceof ServerWorld serverWorld){
            DieOnTouchingGround.DataClass set = game.safePoses.computeIfAbsent(serverWorld);
            for(Direction direction : Direction.values()){
                BlockPos pos1 = pos.offset(direction);
                if(isEmptyCollisionBlock(world, pos1))
                    set.add(pos1);
            }
            pos = pos.offset(hit.getSide());
            for(Direction direction : Direction.values()){
                BlockPos pos1 = pos.offset(direction);
                if(isEmptyCollisionBlock(world, pos1))
                    set.add(pos1);
            }
        }
        return ActionResult.PASS;
    }
    @Override public void afterBlockBreak(World world, PlayerEntity player, BlockPos pos, BlockState state, @Nullable BlockEntity block) {
        DataClass data = game.safePoses.get(world);
        if(data != null) data.remove(pos);
    }
    private final DieOnTouchingGround game;
    private final @NotNull UnregistrableEvent<ServerTickEvents.EndTick> endTick;
    private final @NotNull UnregistrableEvent<UseBlockCallback> useBlockCallback;
    private final @NotNull UnregistrableEvent<ServerEntityEvents.Load> loadEntity;
    private final @NotNull UnregistrableEvent<ServerEntityEvents.Unload> unloadEntity;
    private final @NotNull UnregistrableEvent<PlayerBlockBreakEvents.After> afterBlockBroken;
    private boolean testPlayerWithBox(ServerPlayerEntity player, Box playerBox){
        double nearestDistanceSquare = Double.MAX_VALUE;
        BlockPos directlyDownBlockPos = BlockPos.ofFloored(player.getPos().add(0, -expandValue, 0));
        boolean touchingSafe = false, touchingDangerous = false;
        DieOnTouchingGround.DataClass set = game.safePoses.get(player.getWorld());
        if(set == null) return false;
        for (BlockPos pos : BlockPos.iterate(
                BlockPos.ofFloored(playerBox.minX, playerBox.minY, playerBox.minZ),
                BlockPos.ofFloored(playerBox.maxX, playerBox.maxY, playerBox.maxZ))) {
            boolean isSafe;
            BlockState state = player.getWorld().getBlockState(pos);
            VoxelShape shape = state.getCollisionShape(player.getWorld(), pos);
            if (!shape.isEmpty() && playerBox.intersects(shape.getBoundingBox().offset(pos))){
                isSafe = set.contains(pos) || state.getBlock().equals(Blocks.OBSIDIAN);
            }
            else if(game.difficulty.directlyBelowEmptyAsTouchingSafe && pos.equals(directlyDownBlockPos)){
                isSafe = true;
            }
            else continue;
            if (game.difficulty.testOnlyNearestTouch) {
                double distanceSquare = pos.getSquaredDistance(player.getPos());
                if (distanceSquare >= nearestDistanceSquare) continue;
                nearestDistanceSquare = distanceSquare;
                touchingSafe = isSafe;
                touchingDangerous = !isSafe;
            }
            else {
                if (isSafe) touchingSafe = true;
                else touchingDangerous = true;
            }
        }
        if(touchingSafe && game.difficulty.escapeWhenTouchingSafe) return false;
        else if(touchingDangerous) player.kill(player.getServerWorld());
        return touchingDangerous;
    }

}
