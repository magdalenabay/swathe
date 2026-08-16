package dev.swathe;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/**
 * The U-key settings screen. Deliberately hand-rolled rather than built on Cloth or
 * YACL so the mod's only dependency stays Fabric API.
 */
public class SwatheScreen extends Screen {

	private final Screen parent;
	private final SwatheConfig cfg = SwatheConfig.get();

	/** Label updaters, run after any change so every widget reflects the new state. */
	private final List<Runnable> refreshers = new ArrayList<>();

	public SwatheScreen(Screen parent) {
		super(Component.translatable("swathe.screen.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		refreshers.clear();

		int cx = this.width / 2;
		int y = 24;

		StringWidget heading = new StringWidget(this.title, this.font);
		heading.setPosition(cx - heading.getWidth() / 2, y);
		addRenderableWidget(heading);
		y += 22;

		Button mode = Button.builder(Component.empty(), button -> {
			cfg.setMode(cfg.mode().next());
			MiningQueue.INSTANCE.clear();
			refresh();
		}).bounds(cx - 100, y, 200, 20).build();

		addRenderableWidget(mode);
		refreshers.add(() -> mode.setMessage(
				Component.translatable("swathe.screen.mode", cfg.mode().label)));
		y += 26;

		y = addAdjustRow(y, "swathe.screen.left", () -> cfg.left, v -> cfg.left = v, 0, ShapeSpec.MAX_RADIUS);
		y = addAdjustRow(y, "swathe.screen.right", () -> cfg.right, v -> cfg.right = v, 0, ShapeSpec.MAX_RADIUS);
		y = addAdjustRow(y, "swathe.screen.up", () -> cfg.up, v -> cfg.up = v, 0, ShapeSpec.MAX_RADIUS);
		y = addAdjustRow(y, "swathe.screen.down", () -> cfg.down, v -> cfg.down = v, 0, ShapeSpec.MAX_RADIUS);
		y = addAdjustRow(y, "swathe.screen.depth", () -> cfg.depth, v -> cfg.depth = v, 1, ShapeSpec.MAX_DEPTH);

		y += 4;

		Button preview = Button.builder(Component.empty(), button -> {
			cfg.preview = !cfg.preview;
			refresh();
		}).bounds(cx - 100, y, 98, 20).build();

		addRenderableWidget(preview);
		refreshers.add(() -> preview.setMessage(onOff("swathe.screen.preview", cfg.preview)));

		Button toolCheck = Button.builder(Component.empty(), button -> {
			cfg.requireCorrectTool = !cfg.requireCorrectTool;
			refresh();
		}).bounds(cx + 2, y, 98, 20).build();

		addRenderableWidget(toolCheck);
		refreshers.add(() -> toolCheck.setMessage(onOff("swathe.screen.tool", cfg.requireCorrectTool)));
		y += 26;

		final int summaryY = y + 4;

		StringWidget summary = new StringWidget(Component.empty(), this.font);
		summary.setPosition(cx - 100, summaryY);
		addRenderableWidget(summary);
		refreshers.add(() -> {
			Component text = Component.translatable("swathe.screen.size", cfg.toSpec().size());
			summary.setMessage(text);
			// Built empty, so it has no width of its own to centre within.
			summary.setWidth(this.font.width(text));
			summary.setPosition(cx - this.font.width(text) / 2, summaryY);
		});
		y += 26;

		addRenderableWidget(Button.builder(
				Component.translatable("swathe.screen.done"),
				button -> this.onClose()).bounds(cx - 100, y, 200, 20).build());

		refresh();
	}

	/** A "Label  [-] n [+]" row. Returns the y for the next row. */
	private int addAdjustRow(int y, String key, IntSupplier get, IntConsumer set, int min, int max) {
		int cx = this.width / 2;

		StringWidget label = new StringWidget(Component.translatable(key), this.font);
		label.setPosition(cx - 100, y + 6);
		addRenderableWidget(label);

		addRenderableWidget(Button.builder(Component.literal("-"), button -> {
			set.accept(Math.max(min, get.getAsInt() - 1));
			refresh();
		}).bounds(cx + 20, y, 20, 20).build());

		StringWidget value = new StringWidget(Component.empty(), this.font);
		addRenderableWidget(value);
		refreshers.add(() -> {
			Component text = Component.literal(String.valueOf(get.getAsInt()));
			value.setMessage(text);
			value.setWidth(this.font.width(text));
			value.setPosition(cx + 55 - this.font.width(text) / 2, y + 6);
		});

		addRenderableWidget(Button.builder(Component.literal("+"), button -> {
			set.accept(Math.min(max, get.getAsInt() + 1));
			refresh();
		}).bounds(cx + 80, y, 20, 20).build());

		return y + 24;
	}

	private static Component onOff(String key, boolean on) {
		return Component.translatable(key).append(": ").append(on ? "ON" : "OFF");
	}

	private void refresh() {
		refreshers.forEach(Runnable::run);
	}

	@Override
	public void onClose() {
		cfg.save(); // also pushes the new shape to the server

		if (this.minecraft != null) {
			this.minecraft.setScreen(this.parent);
		}
	}
}
