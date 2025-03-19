package lpcminigame.mixin;

import lpcminigame.events.PlayerPlaceBlockCallback;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerInteractionManager.class)
public abstract class ServerPlayerInteractionManagerMixin {
	@Unique private BlockPos pos1, pos2;
	@Unique private Block block1, block2;
	@Inject(at = @At("HEAD"), method = "interactBlock")
	private void interactBlockHead(
			ServerPlayerEntity player, World world, ItemStack stack,
			Hand hand, BlockHitResult hitResult, CallbackInfoReturnable<ActionResult> cir) {
		pos1 = hitResult.getBlockPos();
		pos2 = pos1.offset(hitResult.getSide());
		block1 = world.getBlockState(pos1).getBlock();
		block2 = world.getBlockState(pos2).getBlock();
	}
	@Inject(at = @At("RETURN"), method = "interactBlock")
	private void interactBlockReturn(
			ServerPlayerEntity player, World world, ItemStack stack,
			Hand hand, BlockHitResult hitResult, CallbackInfoReturnable<ActionResult> cir){
		if(!world.getBlockState(pos1).getBlock().equals(block1))
			PlayerPlaceBlockCallback.EVENT.invoker().interact(world, pos1, player);
		if(!world.getBlockState(pos2).getBlock().equals(block2))
			PlayerPlaceBlockCallback.EVENT.invoker().interact(world, pos2, player);
	}
}