package dev.swathe.mixin;

import dev.swathe.MiningQueue;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Stops vanilla mining the crosshair block while the queue is running.
 *
 * <p>Without this, vanilla and the queue both call continueDestroyBlock in the same
 * tick on different positions. Each call resets the other's progress and nothing ever
 * finishes breaking.
 */
@Mixin(Minecraft.class)
public class MinecraftMixin {

	@Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
	private void swathe$yieldToQueue(boolean down, CallbackInfo ci) {
		if (MiningQueue.INSTANCE.isActive()) {
			ci.cancel();
		}
	}
}
