package lpcminigame.games.dieOnNaturalBlocks;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

import static lpcminigame.util.StringUtils.*;
import static lpcminigame.util.BlockUtils.*;
import static lpcminigame.games.dieOnNaturalBlocks.DieOnNaturalBlocks.*;

public class OnBlockPlaced implements UseBlockCallback {
    @NotNull DieOnNaturalBlocks game;
    OnBlockPlaced(@NotNull DieOnNaturalBlocks game){
        this.game = game;
    }
    @Override public ActionResult interact(PlayerEntity player, World world, Hand hand, BlockHitResult hit) {
        BlockPos pos = hit.getBlockPos();
        DataClass set = game.safePoses.get(getWorldStringId(world));
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
        return ActionResult.PASS;
    }
}
