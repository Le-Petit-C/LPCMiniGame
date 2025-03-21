package lpcminigame.games.dieOnNaturalBlocks;

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

import static lpcminigame.util.BlockUtils.*;
import static lpcminigame.games.dieOnNaturalBlocks.DieOnNaturalBlocks.*;

public class Events implements
        ServerTickEvents.EndTick,
        UseBlockCallback,
        ServerEntityEvents.Load,
        ServerEntityEvents.Unload,
        PlayerBlockBreakEvents.After
{
    Events(DieOnNaturalBlocks game){
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
            DieOnNaturalBlocks.DataClass set = game.safePoses.get(world);
            if(set == null || !set.contains(pos)) return;
            set.remove(pos);
            game.unnaturalFallingBlocks.computeIfAbsent(world).add(block);
        }
    }
    @Override public void onUnload(Entity entity, ServerWorld world) {
        if(entity instanceof FallingBlockEntity block){
            DieOnNaturalBlocks.FallingBlockSet blockSet = game.unnaturalFallingBlocks.get(world);
            if(blockSet == null || !blockSet.contains(block)) return;
            blockSet.remove(block);
            DieOnNaturalBlocks.DataClass data = game.safePoses.computeIfAbsent(world);
            if(entity.isRemoved()) data.add(block.getBlockPos());
            else data.addWithoutTest(block.getBlockPos());
        }
    }
    @Override public void onEndTick(MinecraftServer server) {
        for(ServerPlayerEntity player : server.getPlayerManager().getPlayerList()){
            if(player.isDead()) continue;
            if(player.isCreative()) continue;
            if(player.isSpectator()) continue;
            Box playerBox = player.getBoundingBox();
            double expandValue = 0.0001;
            if(testPlayerWithBox(player, playerBox.expand(expandValue, 0, 0))) continue;
            if(testPlayerWithBox(player, playerBox.expand(0, expandValue, 0))) continue;
            testPlayerWithBox(player, playerBox.expand(0, 0, expandValue));
        }
        game.safePoses.refTest();
    }
    @Override public ActionResult interact(PlayerEntity player, World world, Hand hand, BlockHitResult hit) {
        BlockPos pos = hit.getBlockPos();
        if(world instanceof ServerWorld serverWorld){
            DieOnNaturalBlocks.DataClass set = game.safePoses.computeIfAbsent(serverWorld);
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
    private final DieOnNaturalBlocks game;
    private final @NotNull UnregistrableEvent<ServerTickEvents.EndTick> endTick;
    private final @NotNull UnregistrableEvent<UseBlockCallback> useBlockCallback;
    private final @NotNull UnregistrableEvent<ServerEntityEvents.Load> loadEntity;
    private final @NotNull UnregistrableEvent<ServerEntityEvents.Unload> unloadEntity;
    private final @NotNull UnregistrableEvent<PlayerBlockBreakEvents.After> afterBlockBroken;
    private boolean testPlayerWithBox(ServerPlayerEntity player, Box playerBox){
        DieOnNaturalBlocks.DataClass set = game.safePoses.get(player.getWorld());
        if(set == null) return false;
        for (BlockPos pos : BlockPos.iterate(
                BlockPos.ofFloored(playerBox.minX, playerBox.minY, playerBox.minZ),
                BlockPos.ofFloored(playerBox.maxX, playerBox.maxY, playerBox.maxZ))) {
            if(set.contains(pos)) continue;
            BlockState state = player.getWorld().getBlockState(pos);
            if(state.getBlock().equals(Blocks.OBSIDIAN)) continue;
            VoxelShape shape = state.getCollisionShape(player.getWorld(), pos);
            if(shape.isEmpty()) continue;
            if (playerBox.intersects(shape.getBoundingBox().offset(pos))){
                player.kill(player.getServerWorld());
                return true;
            }
        }
        return false;
    }

}
