package com.ciilusfletchingtable;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import net.fabricmc.loader.api.FabricLoader;

public final class FletchingTableConfig {
	public static final int DEFAULT_BASE_OUTPUT = 8;
	public static final int MIN_BASE_OUTPUT = 1;
	public static final int MAX_BASE_OUTPUT = 64;

	private static final String BASE_OUTPUT_KEY = "base_output";
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static int baseOutput = DEFAULT_BASE_OUTPUT;

	private FletchingTableConfig() {
	}

	public static void load() {
		Path path = getPath();
		if (!Files.exists(path)) {
			save();
			return;
		}

		try (Reader reader = Files.newBufferedReader(path)) {
			JsonObject root = GSON.fromJson(reader, JsonObject.class);
			if (root != null && root.has(BASE_OUTPUT_KEY)) {
				baseOutput = clamp(root.get(BASE_OUTPUT_KEY).getAsInt());
			}
		} catch (Exception exception) {
			CiiluSFletchingTable.LOGGER.warn("Could not load fletching table config", exception);
			baseOutput = DEFAULT_BASE_OUTPUT;
		}
	}

	public static void save() {
		Path path = getPath();
		try {
			Files.createDirectories(path.getParent());
			JsonObject root = new JsonObject();
			root.addProperty(BASE_OUTPUT_KEY, baseOutput);
			try (Writer writer = Files.newBufferedWriter(
				path,
				StandardOpenOption.CREATE,
				StandardOpenOption.TRUNCATE_EXISTING,
				StandardOpenOption.WRITE
			)) {
				GSON.toJson(root, writer);
			}
		} catch (Exception exception) {
			CiiluSFletchingTable.LOGGER.warn("Could not save fletching table config", exception);
		}
	}

	public static int getBaseOutput() {
		return baseOutput;
	}

	public static void setBaseOutput(int value) {
		baseOutput = clamp(value);
	}

	private static int clamp(int value) {
		return Math.max(MIN_BASE_OUTPUT, Math.min(MAX_BASE_OUTPUT, value));
	}

	private static Path getPath() {
		return FabricLoader.getInstance().getConfigDir().resolve(CiiluSFletchingTable.MOD_ID + ".json");
	}
}
