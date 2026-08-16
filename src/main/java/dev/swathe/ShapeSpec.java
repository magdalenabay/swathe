package dev.swathe;

/**
 * A resolved mining shape, in face-relative units. This is the only thing the client
 * and server actually need to agree on, so it is what goes over the wire.
 */
public record ShapeSpec(int left, int right, int up, int down, int depth, boolean requireCorrectTool) {

	/** Hard ceiling so a fat custom shape can never stall a server tick. */
	public static final int MAX_BLOCKS = 512;

	public static final int MAX_RADIUS = 8;
	public static final int MAX_DEPTH = 8;

	public static final ShapeSpec SINGLE = new ShapeSpec(0, 0, 0, 0, 1, true);

	/** Clamps every field into range. Applied on receipt so a bad client cannot ask for a 64x64 hole. */
	public ShapeSpec clamped() {
		return new ShapeSpec(
				clampRadius(left),
				clampRadius(right),
				clampRadius(up),
				clampRadius(down),
				clampDepth(depth),
				requireCorrectTool);
	}

	/** True when this shape is just the block you aimed at, so there is nothing extra to do. */
	public boolean isSingle() {
		return left == 0 && right == 0 && up == 0 && down == 0 && depth <= 1;
	}

	/** Total blocks broken per swing, centre block included. */
	public int size() {
		return Math.min(MAX_BLOCKS, (left + right + 1) * (up + down + 1) * Math.max(1, depth));
	}

	public static int clampRadius(int v) {
		return Math.max(0, Math.min(MAX_RADIUS, v));
	}

	public static int clampDepth(int v) {
		return Math.max(1, Math.min(MAX_DEPTH, v));
	}
}
