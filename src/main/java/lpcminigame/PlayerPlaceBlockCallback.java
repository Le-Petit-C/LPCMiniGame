package lpcminigame;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.LinkedList;
import java.util.Queue;

public class PlayerPlaceBlockCallback implements UseBlockCallback, ServerTickEvents.EndTick {
    @Override
    public ActionResult interact(PlayerEntity player, World world, Hand hand, BlockHitResult hit) {
        BlockPos pos = hit.getBlockPos();
        Block block = world.getBlockState(pos).getBlock();
        queue.add(new PossiblyChangedBlockData(world, pos, player, block));
        pos = pos.offset(hit.getSide());
        block = world.getBlockState(pos).getBlock();
        queue.add(new PossiblyChangedBlockData(world, pos, player, block));
        return ActionResult.PASS;
    }
    @Override
    public void onEndTick(MinecraftServer minecraftServer) {
        while(!queue.isEmpty()){
            PossiblyChangedBlockData data = queue.poll();
            Block block = data.world.getBlockState(data.pos).getBlock();
            if(block != data.oldBlock) blockPlacedByPlayer(data.world, data.pos, data.player);
        }
    }

    private final Queue<PossiblyChangedBlockData> queue = new LinkedList<>();
    private record PossiblyChangedBlockData(World world, BlockPos pos, PlayerEntity player, Block oldBlock){}
    private void blockPlacedByPlayer(World world, BlockPos pos, PlayerEntity player){
        player.sendMessage(Text.of("Place block detected!"), true);
    }
}
