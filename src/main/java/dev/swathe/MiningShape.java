package dev.swathe;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure geometry: turns (target block, face hit, player facing) into the list of
 * extra blocks that should break alongside it.
 *
 * <p>The shape is expressed in face-relative axes so it always reads the same way
 * to the player: "up" is up on screen, "right" is right on screen, and "depth"
 * runs away from the camera into the wall.
 *
 * <p>Shared by both sides so the client preview and the server break agree exactly.
 */
public final class MiningShape {

	private MiningShape() {
	}

	/**
	 * Blocks to break in addition to {@code center}. The centre itself is never
	 * included - whoever called this already broke it.
	 *
	 * @param side         the face the player hit
	 * @param playerFacing the player's horizontal facing, used to orient the plane
	 *                     when mining straight up or straight down
	 */
	public static List<BlockPos> area(BlockPos center, Direction side, Direction playerFacing, ShapeSpec spec) {
		if (spec.isSingle()) {
			return List.of();
		}

		// Into the wall, away from the player.
		Direction depthDir = side.getOpposite();

		Direction upDir;
		Direction rightDir;

		if (side.getAxis().isVertical()) {
			// Mining the floor or ceiling: the plane is horizontal, so "up" on screen
			// is whichever way the player happens to be looking.
			upDir = playerFacing;
			rightDir = playerFacing.getClockWise();
		} else {
			upDir = Direction.UP;
			rightDir = side.getCounterClockWise();
		}

		List<BlockPos> out = new ArrayList<>();

		for (int d = 0; d < spec.depth(); d++) {
			for (int u = -spec.down(); u <= spec.up(); u++) {
				for (int r = -spec.left(); r <= spec.right(); r++) {
					if (d == 0 && u == 0 && r == 0) {
						continue; // the block that was already broken
					}

					out.add(center
							.relative(depthDir, d)
							.relative(upDir, u)
							.relative(rightDir, r));

					if (out.size() >= ShapeSpec.MAX_BLOCKS) {
						return out;
					}
				}
			}
		}

		return out;
	}
}
