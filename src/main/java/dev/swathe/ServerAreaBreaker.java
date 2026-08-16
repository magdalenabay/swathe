package dev.swathe;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Server-side area breaking: the whole shape goes at once, with no reach limit and no
 * sequential mining, because here we actually own the world.
 *
 * <p>Each extra block is broken through the same sequence vanilla's own
 * {@code ServerPlayerGameMode.destroyBlock} uses, so drops, XP, durability, block
 * entities and advancement triggers all behave exactly as if the block had been mined
 * by hand. {@code PlayerBlockBreakEvents.BEFORE} is re-fired per block so claim and
 * protection mods still get their veto.
 */
public final class ServerAreaBreaker {

	/** Guards against our own breaks recursing back into this handler. */
	private static final ThreadLocal<Boolean> BUSY = ThreadLocal.withInitial(() -> false);

	private ServerAreaBreaker() {
	}

	public static void register() {
		PlayerBlockBreakEvents.AFTER.register(ServerAreaBreaker::onBlockBroken);
	}

	private static void onBlockBroken(Level level, Player player, BlockPos pos, BlockState state, BlockEntity be) {
		if (level.isClientSide() || BUSY.get()) {
			return;
		}

		if (!(player instanceof ServerPlayer serverPlayer) || !(level instanceof ServerLevel serverLevel)) {
			return;
		}

		ShapeSpec spec = ServerShapes.get(serverPlayer);

		if (spec == null || spec.isSingle()) {
			return;
		}

		ItemStack tool = player.getMainHandItem();

		// Wrong tool on the centre block means no area break at all.
		if (spec.requireCorrectTool() && tool.getDestroySpeed(state) <= 1.0F) {
			return;
		}

		// The server never saw which face was hit, so derive it from where the player is
		// looking - which is also how the original Zone Miner orients its shape.
		Direction side = Direction.getApproximateNearest(player.getLookAngle()).getOpposite();
		List<BlockPos> area = MiningShape.area(pos, side, player.getDirection(), spec);

		BUSY.set(true);

		try {
			for (BlockPos target : area) {
				breakOne(serverLevel, serverPlayer, target, spec);
			}
		} finally {
			BUSY.set(false);
		}
	}

	private static void breakOne(ServerLevel level, ServerPlayer player, BlockPos pos, ShapeSpec spec) {
		BlockState state = level.getBlockState(pos);

		if (state.isAir() || state.getBlock() instanceof LiquidBlock) {
			return;
		}

		// Bedrock, barriers, portal frames: negative hardness means unbreakable.
		if (state.getDestroySpeed(level, pos) < 0.0F) {
			return;
		}

		if (!player.hasCorrectToolForDrops(state)) {
			return;
		}

		ItemStack tool = player.getMainHandItem();

		if (spec.requireCorrectTool() && tool.getDestroySpeed(state) <= 1.0F) {
			return;
		}

		// The tool broke partway through the area - stop rather than punch out the rest.
		if (tool.isEmpty() && !player.isCreative()) {
			return;
		}

		BlockEntity blockEntity = level.getBlockEntity(pos);

		// Let claim / protection mods veto each block individually.
		if (!PlayerBlockBreakEvents.BEFORE.invoker().beforeBlockBreak(level, player, pos, state, blockEntity)) {
			return;
		}

		Block block = state.getBlock();
		ItemStack toolBefore = tool.copy();

		block.playerWillDestroy(level, pos, state, player);
		boolean removed = level.removeBlock(pos, false);

		if (removed) {
			block.destroy(level, pos, state);
		}

		if (!player.isCreative()) {
			// Durability, stats and "item used" bookkeeping.
			tool.mineBlock(level, state, pos, player);

			if (removed) {
				// Drops and XP, with the tool as it was before this block wore it down.
				block.playerDestroy(level, player, pos, state, blockEntity, toolBefore);
			}
		}

		PlayerBlockBreakEvents.AFTER.invoker().afterBlockBreak(level, player, pos, state, blockEntity);
	}
}
