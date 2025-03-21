package lpcminigame.games.dieOnNaturalBlocks;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.shape.VoxelShape;

import static lpcminigame.util.StringUtils.*;
import static lpcminigame.games.dieOnNaturalBlocks.DieOnNaturalBlocks.*;

public class OnServerEndTick implements ServerTickEvents.EndTick{
    DieOnNaturalBlocks game;
    OnServerEndTick(DieOnNaturalBlocks game){
        this.game = game;
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
    private boolean testPlayerWithBox(ServerPlayerEntity player, Box playerBox){
        String worldStringId = getWorldStringId(player.getWorld());
        DataClass set = game.safePoses.get(worldStringId);
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
