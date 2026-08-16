package dev.swathe;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;

public class SwatheClient implements ClientModInitializer {

	public static final KeyMapping.Category CATEGORY =
			KeyMapping.Category.register(ResourceLocation.fromNamespaceAndPath(Swathe.MOD_ID, "general"));

	private static KeyMapping cycleKey;
	private static KeyMapping settingsKey;

	@Override
	public void onInitializeClient() {
		cycleKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				"key.swathe.cycle", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_Y, CATEGORY));

		settingsKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				"key.swathe.settings", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_U, CATEGORY));

		ClientTickEvents.END_CLIENT_TICK.register(minecraft -> {
			while (cycleKey.consumeClick()) {
				cycleMode(minecraft);
			}

			while (settingsKey.consumeClick()) {
				if (minecraft.screen == null) {
					minecraft.setScreen(new SwatheScreen(null));
				}
			}

			ClientLink.tick();
			MiningQueue.INSTANCE.tick(minecraft);
		});

		ClientLink.register();
		PreviewRenderer.register();
	}

	private static void cycleMode(Minecraft minecraft) {
		SwatheConfig cfg = SwatheConfig.get();
		MiningMode next = cfg.mode().next();

		cfg.setMode(next);
		cfg.save(); // also pushes the new shape to the server

		// A queue built for the old shape would be misleading, so drop it.
		MiningQueue.INSTANCE.clear();

		if (minecraft.player != null) {
			minecraft.player.displayClientMessage(
					Component.translatable("swathe.mode.changed", next.label), true);
		}
	}
}
