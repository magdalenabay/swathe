package dev.swathe;

import net.fabricmc.api.ModInitializer;

/**
 * Common entrypoint. Runs on a dedicated server, and also on the client - which is what
 * makes singleplayer work, since the integrated server is a real server.
 */
public class Swathe implements ModInitializer {

	public static final String MOD_ID = "swathe";

	@Override
	public void onInitialize() {
		SwathePayload.register();
		ServerShapes.register();
		ServerAreaBreaker.register();
	}
}
