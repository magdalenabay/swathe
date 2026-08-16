package dev.swathe;

/** The presets the Y key cycles through. Purely a client-side convenience. */
public enum MiningMode {

	SINGLE("1x1"),
	CUBE3("3x3x3"),
	CUBE5("5x5x5"),
	CUSTOM("Custom");

	public final String label;

	MiningMode(String label) {
		this.label = label;
	}

	public MiningMode next() {
		MiningMode[] all = values();
		return all[(ordinal() + 1) % all.length];
	}

	/** Resolves to the shape actually mined. CUSTOM defers to the saved dimensions. */
	public ShapeSpec toSpec(SwatheConfig cfg) {
		return switch (this) {
			case SINGLE -> new ShapeSpec(0, 0, 0, 0, 1, cfg.requireCorrectTool);
			case CUBE3 -> new ShapeSpec(1, 1, 1, 1, 3, cfg.requireCorrectTool);
			case CUBE5 -> new ShapeSpec(2, 2, 2, 2, 5, cfg.requireCorrectTool);
			case CUSTOM -> new ShapeSpec(cfg.left, cfg.right, cfg.up, cfg.down, cfg.depth, cfg.requireCorrectTool)
					.clamped();
		};
	}
}
