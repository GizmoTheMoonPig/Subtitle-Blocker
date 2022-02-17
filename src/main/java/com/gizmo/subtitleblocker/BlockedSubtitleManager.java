package com.gizmo.subtitleblocker;

import com.gizmo.subtitleblocker.SubtitleBlocker;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import net.minecraft.resources.ResourceLocation;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class BlockedSubtitleManager {

	public static List<ResourceLocation> blockedSubtitles = new ArrayList<>();
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	public static void load() {
		//clear list, then reload it
		blockedSubtitles.clear();
		blockedSubtitles.addAll(loadBlockedSubtitles());
	}

	public static void saveBlockedSubtitles() {
		try (Writer writer = new OutputStreamWriter(new FileOutputStream("blocked_subtitles.dat"), StandardCharsets.UTF_8)) {
			writer.write(GSON.toJson(blockedSubtitles));
		} catch (IOException exception) {
			SubtitleBlocker.LOGGER.fatal("Couldn't save blocked subtitle list! Please report this! \nError: " + exception);
		}
	}

	private static List<ResourceLocation> loadBlockedSubtitles() {
		try (InputStreamReader reader = new InputStreamReader(new FileInputStream("blocked_subtitles.dat"), StandardCharsets.UTF_8)) {
			return GSON.fromJson(new JsonReader(reader), new TypeToken<List<ResourceLocation>>() {
			}.getType());
		} catch (JsonSyntaxException | IOException e) {
			SubtitleBlocker.LOGGER.fatal("Couldn't load blocked subtitle list! Please report this! \nError: " + e);
			return new ArrayList<>();
		}
	}
}
