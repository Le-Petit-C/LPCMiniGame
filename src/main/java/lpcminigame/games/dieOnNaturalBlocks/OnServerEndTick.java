package lpcminigame.games.dieOnNaturalBlocks;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;

import java.util.HashSet;

public class OnServerEndTick implements ServerTickEvents.EndTick{
    DieOnNaturalBlocks game;
    OnServerEndTick(DieOnNaturalBlocks game){
        this.game = game;
    }
    @Override public void onEndTick(MinecraftServer server) {
        for(ServerPlayerEntity player : server.getPlayerManager().getPlayerList()){
            Vec3d eyePos = player.getEyePos();
            HashSet<BlockPos> set = game.safeBlocks.get(player.getWorld());
            for (BlockPos pos : BlockPos.iterate(
                    BlockPos.ofFloored(eyePos.getX() - 6, eyePos.getY() - 6, eyePos.getZ() - 6),
                    BlockPos.ofFloored(eyePos.getX() + 6, eyePos.getY() + 6, eyePos.getZ() + 6))){
                BlockState state = player.getWorld().getBlockState(pos);
                VoxelShape shape = state.getCollisionShape(player.getWorld(), pos);
                if(shape.isEmpty()) set.add(pos);
            }
            if(player.isCreative()) continue;
            if(player.isSpawnForced()) continue;
            if(!player.isOnGround()) continue;
            Box playerBox = player.getBoundingBox();
            double expandValue = 0.0001;
            if(testPlayerWithBox(player, playerBox.expand(expandValue, 0, 0))) continue;
            if(testPlayerWithBox(player, playerBox.expand(0, expandValue, 0))) continue;
            testPlayerWithBox(player, playerBox.expand(0, 0, expandValue));
        }
    }
    private boolean testPlayerWithBox(ServerPlayerEntity player, Box playerBox){
        HashSet<BlockPos> set = game.safeBlocks.get(player.getWorld());
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
