package dev.swathe;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Persisted settings. Plain JSON in {@code config/swathe.json}, no config-lib dependency. */
public class SwatheConfig {

	private static final Logger LOGGER = LoggerFactory.getLogger(Swathe.MOD_ID);
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("swathe.json");

	private static SwatheConfig instance;

	/** Stored as a name so an unknown/renamed mode degrades to SINGLE instead of crashing. */
	public String mode = MiningMode.SINGLE.name();

	public int left = 1;
	public int right = 1;
	public int up = 1;
	public int down = 1;
	public int depth = 1;

	/** Draw the white outline preview before breaking. */
	public boolean preview = true;

	/** Only extend the break when the held tool actually speeds up the extra block. */
	public boolean requireCorrectTool = true;

	public static SwatheConfig get() {
		if (instance == null) {
			instance = load();
		}

		return instance;
	}

	public MiningMode mode() {
		try {
			return MiningMode.valueOf(mode);
		} catch (IllegalArgumentException e) {
			return MiningMode.SINGLE;
		}
	}

	public void setMode(MiningMode m) {
		this.mode = m.name();
	}

	public ShapeSpec toSpec() {
		return mode().toSpec(this);
	}

	private static SwatheConfig load() {
		if (Files.exists(PATH)) {
			try {
				SwatheConfig cfg = GSON.fromJson(Files.readString(PATH), SwatheConfig.class);

				if (cfg != null) {
					cfg.left = ShapeSpec.clampRadius(cfg.left);
					cfg.right = ShapeSpec.clampRadius(cfg.right);
					cfg.up = ShapeSpec.clampRadius(cfg.up);
					cfg.down = ShapeSpec.clampRadius(cfg.down);
					cfg.depth = ShapeSpec.clampDepth(cfg.depth);
					return cfg;
				}
			} catch (Exception e) {
				LOGGER.warn("[swathe] could not read config, using defaults", e);
			}
		}

		return new SwatheConfig();
	}

	public void save() {
		try {
			Files.createDirectories(PATH.getParent());
			Files.writeString(PATH, GSON.toJson(this));
		} catch (IOException e) {
			LOGGER.warn("[swathe] could not write config", e);
		}

		// Whatever changed, the server needs to hear about it on the next tick.
		ClientLink.invalidate();
	}
}
