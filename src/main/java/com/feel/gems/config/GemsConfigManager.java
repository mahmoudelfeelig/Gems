package com.feel.gems.config;

import com.feel.gems.GemsMod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;




public final class GemsConfigManager {
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    private static final String DIR = "gems";
    private static final String FILE = "balance.json";

    private GemsConfigManager() {
    }

    public enum LoadStatus {
        LOADED,
        CREATED,
        ERROR
    }

    public record LoadResult(LoadStatus status, Path path, GemsBalanceConfig config, String error) {
    }

    static LoadResult loadOrCreateWithFallback() {
        return loadInternal(true, true);
    }

    static LoadResult loadOrCreateStrict() {
        return loadInternal(true, false);
    }

    public static LoadResult loadOrCreateForUi() {
        return loadInternal(true, true);
    }

    private static LoadResult loadInternal(boolean createIfMissing, boolean fallbackToDefaults) {
        Path path = balancePath();

        if (!Files.exists(path)) {
            if (!createIfMissing) {
                return new LoadResult(LoadStatus.ERROR, path, null, "Config file does not exist: " + path);
            }
            GemsBalanceConfig cfg = new GemsBalanceConfig();
            write(path, cfg);
            return new LoadResult(LoadStatus.CREATED, path, cfg, null);
        }

        try (Reader reader = Files.newBufferedReader(path)) {
            JsonElement existing = JsonParser.parseReader(reader);
            JsonElement merged = mergeWithDefaults(existing, GSON.toJsonTree(new GemsBalanceConfig()));
            GemsBalanceConfig cfg = GSON.fromJson(merged, GemsBalanceConfig.class);
            if (!merged.equals(existing)) {
                writeJson(path, merged);
            }
            return new LoadResult(LoadStatus.LOADED, path, cfg, null);
        } catch (JsonSyntaxException e) {
            String error = "Failed to parse config " + path + " (keeping file, not overwriting).";
            GemsMod.LOGGER.error(error, e);
            return new LoadResult(LoadStatus.ERROR, path, fallbackToDefaults ? new GemsBalanceConfig() : null, error);
        } catch (IOException e) {
            String error = "Failed to read config " + path + " (keeping file).";
            GemsMod.LOGGER.error(error, e);
            return new LoadResult(LoadStatus.ERROR, path, fallbackToDefaults ? new GemsBalanceConfig() : null, error);
        }
    }

    private static JsonElement mergeWithDefaults(JsonElement existing, JsonElement defaults) {
        if (existing == null || existing.isJsonNull()) {
            return defaults;
        }
        if (defaults == null || defaults.isJsonNull()) {
            return existing;
        }
        if (existing.isJsonObject() && defaults.isJsonObject()) {
            JsonObject merged = new JsonObject();
            JsonObject existingObj = existing.getAsJsonObject();
            JsonObject defaultObj = defaults.getAsJsonObject();
            for (var entry : existingObj.entrySet()) {
                merged.add(entry.getKey(), entry.getValue());
            }
            for (var entry : defaultObj.entrySet()) {
                String key = entry.getKey();
                JsonElement existingValue = merged.get(key);
                if (existingValue == null || existingValue instanceof JsonNull) {
                    merged.add(key, entry.getValue());
                } else {
                    merged.add(key, mergeWithDefaults(existingValue, entry.getValue()));
                }
            }
            return merged;
        }
        return existing;
    }

    private static Path configPath() {
        return balancePath();
    }

    static Path configDir() {
        return FabricLoader.getInstance().getConfigDir().resolve(DIR);
    }

    static Path balancePath() {
        return configDir().resolve(FILE);
    }

    public static Path balancePathForUi() {
        return balancePath();
    }

    static Path resolveInConfigDir(String fileName) {
        return configDir().resolve(fileName);
    }

    static void write(Path path, Object cfg) {
        try {
            Files.createDirectories(path.getParent());
        } catch (IOException e) {
            GemsMod.LOGGER.error("Failed to create config directory {}", path.getParent(), e);
            return;
        }
        try (Writer writer = Files.newBufferedWriter(path)) {
            GSON.toJson(cfg, writer);
        } catch (IOException e) {
            GemsMod.LOGGER.error("Failed to write config {}", path, e);
        }
    }

    private static void writeJson(Path path, JsonElement element) {
        try {
            Files.createDirectories(path.getParent());
        } catch (IOException e) {
            GemsMod.LOGGER.error("Failed to create config directory {}", path.getParent(), e);
            return;
        }
        try (Writer writer = Files.newBufferedWriter(path)) {
            GSON.toJson(element, writer);
        } catch (IOException e) {
            GemsMod.LOGGER.error("Failed to write config {}", path, e);
        }
    }

    public static void writeBalanceForUi(GemsBalanceConfig cfg) {
        write(balancePath(), cfg);
    }
}
