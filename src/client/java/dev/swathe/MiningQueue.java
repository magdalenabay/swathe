package dev.swathe;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * The fallback used when the server does not have Swathe installed.
 *
 * <p>A client alone cannot delete blocks - the server owns the world. So instead of
 * breaking the area itself, this queues the extra blocks and drives Minecraft's own
 * mining loop over them one at a time, as if the player had aimed at each block and held
 * attack. The server sees ordinary, correctly-timed mining, so drops, durability,
 * enchantments and protection plugins all behave normally on a vanilla server.
 *
 * <p>The price is that blocks break sequentially and blocks past the player's reach wait
 * until they are in range. When the server does have the mod, {@link ServerAreaBreaker}
 * takes over and neither limitation applies.
 */
public final class MiningQueue {

	public static final MiningQueue INSTANCE = new MiningQueue();

	private final List<BlockPos> queue = new ArrayList<>();

	/** Face originally hit, reused for every queued block so particles face the player. */
	private Direction side = Direction.UP;

	/** Aborts the queue if the player swaps tools mid-break. */
	private Item tool = null;

	private MiningQueue() {
	}

	/** True while we are driving the mining loop ourselves. */
	public boolean isActive() {
		return !queue.isEmpty();
	}

	/** Blocks still waiting to break - the preview renderer draws these. */
	public List<BlockPos> pending() {
		return queue;
	}

	public void clear() {
		queue.clear();
		tool = null;
	}

	/**
	 * Called from the break mixin the moment the player finishes a block themselves.
	 * Ignored while the queue is already running - otherwise every block we break would
	 * spawn another area and the mod would eat the world.
	 */
	public static void onPlayerBrokeBlock(BlockPos center, BlockState centerState, Direction hitSide) {
		// The server is doing this properly; stay out of the way.
		if (ClientLink.serverHandlesBreaking()) {
			return;
		}

		Minecraft minecraft = Minecraft.getInstance();
		SwatheConfig cfg = SwatheConfig.get();
		ShapeSpec spec = cfg.toSpec();

		if (spec.isSingle() || INSTANCE.isActive()) {
			return;
		}

		LocalPlayer player = minecraft.player;
		ClientLevel level = minecraft.level;

		if (player == null || level == null) {
			return;
		}

		ItemStack held = player.getMainHandItem();

		// Bare hands or the wrong tool on the centre block means no area break.
		if (spec.requireCorrectTool() && !isProperTool(held, centerState)) {
			return;
		}

		List<BlockPos> area = MiningShape.area(center, hitSide, player.getDirection(), spec);

		if (area.isEmpty()) {
			return;
		}

		// Nearest first, so the area peels away from the player instead of jumping about.
		Vec3 eye = player.getEyePosition();
		area.sort((a, b) -> Double.compare(
				eye.distanceToSqr(Vec3.atCenterOf(a)),
				eye.distanceToSqr(Vec3.atCenterOf(b))));

		INSTANCE.queue.clear();
		INSTANCE.queue.addAll(area);
		INSTANCE.side = hitSide;
		INSTANCE.tool = held.getItem();
	}

	/** Runs once per client tick. Advances one block at a time, exactly like vanilla. */
	public void tick(Minecraft minecraft) {
		if (queue.isEmpty()) {
			return;
		}

		LocalPlayer player = minecraft.player;
		ClientLevel level = minecraft.level;

		if (player == null || level == null || minecraft.gameMode == null) {
			clear();
			return;
		}

		// Letting go of attack cancels the rest of the area. Keeps the mod predictable:
		// nothing keeps mining after you stop asking it to.
		if (!minecraft.options.keyAttack.isDown() || player.getMainHandItem().getItem() != tool) {
			minecraft.gameMode.stopDestroyBlock();
			clear();
			return;
		}

		ShapeSpec spec = SwatheConfig.get().toSpec();
		double reach = player.blockInteractionRange();
		double reachSq = reach * reach;
		Vec3 eye = player.getEyePosition();

		BlockPos target = null;
		Iterator<BlockPos> it = queue.iterator();

		while (it.hasNext()) {
			BlockPos pos = it.next();

			if (!canBreak(level.getBlockState(pos), level, pos, player, spec)) {
				it.remove();
				continue;
			}

			if (eye.distanceToSqr(Vec3.atCenterOf(pos)) > reachSq) {
				// Out of reach for now. Keep it - walking forward brings it into range.
				continue;
			}

			target = pos;
			break;
		}

		if (target == null) {
			// Either nothing is left, or everything left is out of reach and we wait.
			return;
		}

		if (minecraft.gameMode.continueDestroyBlock(target, side)) {
			level.addBreakingBlockEffect(target, side);
			player.swing(InteractionHand.MAIN_HAND);
		}
	}

	/** Shared by the queue and the preview so what you see is what actually breaks. */
	public static boolean canBreak(BlockState state, ClientLevel level, BlockPos pos,
			LocalPlayer player, ShapeSpec spec) {
		if (state.isAir() || state.getBlock() instanceof LiquidBlock) {
			return false;
		}

		// Bedrock, barriers, portal frames: negative hardness means unbreakable.
		if (state.getDestroySpeed(level, pos) < 0.0F) {
			return false;
		}

		if (!player.hasCorrectToolForDrops(state)) {
			return false;
		}

		return !spec.requireCorrectTool() || isProperTool(player.getMainHandItem(), state);
	}

	/**
	 * "Appropriate tool" test: the held item mines this block faster than a bare hand
	 * would. Stops a pickaxe dragging dirt and leaves along with the stone.
	 */
	public static boolean isProperTool(ItemStack stack, BlockState state) {
		return stack.getDestroySpeed(state) > 1.0F;
	}
}
