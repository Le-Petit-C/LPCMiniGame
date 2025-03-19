package lpcminigame.games.dieOnNaturalBlocks;

import lpcminigame.events.PlayerPlaceBlockCallback;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.HashSet;

public class OnPlayerPlaceBlock implements PlayerPlaceBlockCallback {
    DieOnNaturalBlocks game;
    OnPlayerPlaceBlock(DieOnNaturalBlocks game){
        this.game = game;
    }
    @Override public ActionResult interact(World world, BlockPos pos, PlayerEntity player) {
        HashSet<BlockPos> set = game.safeBlocks.get(world);
        if(set == null) return ActionResult.PASS;
        set.add(pos);
        return ActionResult.PASS;
    }
}
