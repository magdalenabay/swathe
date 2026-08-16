package dev.swathe;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.List;

/** Draws the white outline over every block that is actually going to break. */
public final class PreviewRenderer {

	/** White at roughly 40% alpha - visible against stone without hiding it. */
	private static final int OUTLINE_COLOR = 0x66FFFFFF;

	private PreviewRenderer() {
	}

	public static void register() {
		WorldRenderEvents.AFTER_ENTITIES.register(context -> {
			SwatheConfig cfg = SwatheConfig.get();
			ShapeSpec spec = cfg.toSpec();

			if (!cfg.preview || spec.isSingle()) {
				return;
			}

			Minecraft minecraft = Minecraft.getInstance();
			ClientLevel level = minecraft.level;
			LocalPlayer player = minecraft.player;

			if (player == null || level == null) {
				return;
			}

			PoseStack poseStack = context.matrices();
			MultiBufferSource consumers = context.consumers();

			if (poseStack == null || consumers == null) {
				return;
			}

			List<BlockPos> targets = targets(minecraft, level, player, spec);

			if (targets.isEmpty()) {
				return;
			}

			Vec3 camera = minecraft.gameRenderer.getMainCamera().getPosition();
			VertexConsumer lines = consumers.getBuffer(RenderType.lines());

			for (BlockPos pos : targets) {
				VoxelShape shape = level.getBlockState(pos).getShape(level, pos);

				if (shape.isEmpty()) {
					continue;
				}

				ShapeRenderer.renderShape(
						poseStack,
						lines,
						shape,
						pos.getX() - camera.x,
						pos.getY() - camera.y,
						pos.getZ() - camera.z,
						OUTLINE_COLOR);
			}
		});
	}

	/**
	 * While a client-side break is running, show what is still queued. Otherwise show
	 * what would break if the player started now - filtered by the same rules the miner
	 * uses, so the preview never promises a block that will not go.
	 */
	private static List<BlockPos> targets(Minecraft minecraft, ClientLevel level, LocalPlayer player, ShapeSpec spec) {
		if (MiningQueue.INSTANCE.isActive()) {
			return MiningQueue.INSTANCE.pending();
		}

		if (minecraft.hitResult == null
				|| minecraft.hitResult.getType() != HitResult.Type.BLOCK
				|| !(minecraft.hitResult instanceof BlockHitResult hit)) {
			return List.of();
		}

		BlockPos center = hit.getBlockPos();
		BlockState centerState = level.getBlockState(center);

		if (centerState.isAir()) {
			return List.of();
		}

		if (spec.requireCorrectTool() && !MiningQueue.isProperTool(player.getMainHandItem(), centerState)) {
			return List.of();
		}

		// The server-side breaker has no reach limit, so only filter by reach when the
		// client is the one doing the mining.
		boolean limitToReach = !ClientLink.serverHandlesBreaking();
		double reachSq = player.blockInteractionRange() * player.blockInteractionRange();
		Vec3 eye = player.getEyePosition();

		List<BlockPos> out = new ArrayList<>();

		for (BlockPos pos : MiningShape.area(center, hit.getDirection(), player.getDirection(), spec)) {
			if (!MiningQueue.canBreak(level.getBlockState(pos), level, pos, player, spec)) {
				continue;
			}

			if (limitToReach && eye.distanceToSqr(Vec3.atCenterOf(pos)) > reachSq) {
				continue;
			}

			out.add(pos);
		}

		return out;
	}
}
