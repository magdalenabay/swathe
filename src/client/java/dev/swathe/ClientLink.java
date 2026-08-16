package dev.swathe;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/**
 * Decides which of the two mining strategies is in play.
 *
 * <p>If the server also has Swathe it will do the breaking properly - whole shape at
 * once, no reach limit - and the client just tells it what shape to cut. If it does not,
 * the client falls back to driving vanilla's mining loop itself.
 *
 * <p>In singleplayer the integrated server is a real server running the same mod, so the
 * server-side path is what you get.
 */
public final class ClientLink {

	/** Last shape the server was told about, so we only send on an actual change. */
	private static ShapeSpec lastSent = null;

	private ClientLink() {
	}

	public static void register() {
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> invalidate());
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> invalidate());
	}

	/** True when the server has the mod and is handling area breaks itself. */
	public static boolean serverHandlesBreaking() {
		return ClientPlayNetworking.canSend(SwathePayload.TYPE);
	}

	/**
	 * Forget what the server was told, so the next tick re-sends.
	 *
	 * <p>Called on join, on disconnect, and whenever settings change. Doing the actual
	 * send from the tick rather than here matters: the channel is not necessarily
	 * negotiated yet at join time, and a send that quietly no-ops would leave the server
	 * with no shape on record - which would mean nothing breaks at all, since the client
	 * has already stood down.
	 */
	public static void invalidate() {
		lastSent = null;
	}

	/** Runs each client tick. Cheap record comparison; sends only when it differs. */
	public static void tick() {
		if (!serverHandlesBreaking()) {
			return;
		}

		ShapeSpec spec = SwatheConfig.get().toSpec();

		if (!spec.equals(lastSent)) {
			ClientPlayNetworking.send(new SwathePayload(spec));
			lastSent = spec;
		}
	}
}
