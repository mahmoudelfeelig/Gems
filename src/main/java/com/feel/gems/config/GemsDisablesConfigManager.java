package com.feel.gems.config;

import com.feel.gems.GemsMod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;

public final class GemsDisablesConfigManager {
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    private static final String FILE = "disables.json";

    private GemsDisablesConfigManager() {
    }

    public enum LoadStatus {
        LOADED,
        CREATED,
        ERROR
    }

    public record LoadResult(LoadStatus status, Path path, GemsDisablesConfig config, String error) {
    }

    static LoadResult loadOrCreateWithFallback() {
        return loadInternal(true, true);
    }

    public static LoadResult loadOrCreateStrict() {
        return loadInternal(true, false);
    }

    public static LoadResult loadOrCreateForUi() {
        return loadInternal(true, true);
    }

    private static LoadResult loadInternal(boolean createIfMissing, boolean fallbackToDefaults) {
        Path path = disablesPath();

        if (!Files.exists(path)) {
            if (!createIfMissing) {
                return new LoadResult(LoadStatus.ERROR, path, null, "Config file does not exist: " + path);
            }
            GemsDisablesConfig cfg = new GemsDisablesConfig();
            GemsConfigManager.write(path, cfg);
            return new LoadResult(LoadStatus.CREATED, path, cfg, null);
        }

        try (Reader reader = Files.newBufferedReader(path)) {
            GemsDisablesConfig cfg = GSON.fromJson(reader, GemsDisablesConfig.class);
            return new LoadResult(LoadStatus.LOADED, path, cfg == null ? new GemsDisablesConfig() : cfg, null);
        } catch (JsonSyntaxException e) {
            String error = "Failed to parse config " + path + " (keeping file, not overwriting).";
            GemsMod.LOGGER.error(error, e);
            return new LoadResult(LoadStatus.ERROR, path, fallbackToDefaults ? new GemsDisablesConfig() : null, error);
        } catch (IOException e) {
            String error = "Failed to read config " + path + " (keeping file).";
            GemsMod.LOGGER.error(error, e);
            return new LoadResult(LoadStatus.ERROR, path, fallbackToDefaults ? new GemsDisablesConfig() : null, error);
        }
    }

    static Path disablesPath() {
        return GemsConfigManager.configDir().resolve(FILE);
    }

    public static Path disablesPathForUi() {
        return disablesPath();
    }

    public static void writeDisablesForUi(GemsDisablesConfig cfg) {
        GemsConfigManager.write(disablesPath(), cfg);
    }
}

