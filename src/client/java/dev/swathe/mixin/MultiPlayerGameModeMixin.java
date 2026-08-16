package dev.swathe.mixin;

import dev.swathe.MiningQueue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Detects the player finishing a block so the surrounding area can be queued.
 *
 * <p>State is captured at HEAD on purpose: by the time the method returns the block is
 * already air client-side, and the tool check would have nothing left to test against.
 */
@Mixin(MultiPlayerGameMode.class)
public class MultiPlayerGameModeMixin {

	@Unique
	private BlockPos swathe$pos;

	@Unique
	private BlockState swathe$state;

	@Unique
	private Direction swathe$side;

	@Inject(method = "destroyBlock", at = @At("HEAD"))
	private void swathe$captureBeforeBreak(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
		Minecraft minecraft = Minecraft.getInstance();

		swathe$pos = pos;
		swathe$state = minecraft.level != null ? minecraft.level.getBlockState(pos) : null;
		swathe$side = swathe$resolveSide(minecraft, pos);
	}

	@Inject(method = "destroyBlock", at = @At("RETURN"))
	private void swathe$queueAreaAfterBreak(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
		if (cir.getReturnValueZ() && swathe$state != null && pos.equals(swathe$pos)) {
			MiningQueue.onPlayerBrokeBlock(pos, swathe$state, swathe$side);
		}

		swathe$state = null;
	}

	/** Prefer the real face under the crosshair; fall back to the look vector. */
	@Unique
	private Direction swathe$resolveSide(Minecraft minecraft, BlockPos pos) {
		if (minecraft.hitResult instanceof BlockHitResult hit && hit.getBlockPos().equals(pos)) {
			return hit.getDirection();
		}

		if (minecraft.player != null) {
			return Direction.getApproximateNearest(minecraft.player.getViewVector(1.0F)).getOpposite();
		}

		return Direction.UP;
	}
}
