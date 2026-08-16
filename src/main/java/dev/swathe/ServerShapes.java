package dev.swathe;

import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Per-player mining shape, as last reported by their client. */
public final class ServerShapes {

	private static final Map<UUID, ShapeSpec> SHAPES = new ConcurrentHashMap<>();

	private ServerShapes() {
	}

	public static void register() {
		ServerPlayNetworking.registerGlobalReceiver(SwathePayload.TYPE, (payload, context) -> {
			UUID id = context.player().getUUID();
			// Clamp on receipt - never trust the client about how big a hole to dig.
			ShapeSpec spec = payload.spec().clamped();

			context.server().execute(() -> SHAPES.put(id, spec));
		});

		ServerPlayConnectionEvents.DISCONNECT.register(
				(handler, server) -> SHAPES.remove(handler.player.getUUID()));
	}

	public static ShapeSpec get(ServerPlayer player) {
		return SHAPES.get(player.getUUID());
	}
}
