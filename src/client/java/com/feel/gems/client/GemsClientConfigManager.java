package com.feel.gems.client;

import com.feel.gems.GemsMod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;


public final class GemsClientConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String DIR = "gems";
    private static final String FILE = "client.json";
    private static GemsClientConfig cached;

    private GemsClientConfigManager() {
    }

    public static GemsClientConfig loadOrCreate() {
        if (cached != null) {
            return cached;
        }
        Path path = configPath();
        if (Files.exists(path)) {
            try (var reader = Files.newBufferedReader(path)) {
                GemsClientConfig cfg = GSON.fromJson(reader, GemsClientConfig.class);
                if (cfg != null) {
                    cached = cfg;
                    return cfg;
                }
            } catch (Exception e) {
                GemsMod.LOGGER.warn("Failed to read client config: {}", path, e);
            }
        }
        GemsClientConfig cfg = new GemsClientConfig();
        write(path, cfg);
        cached = cfg;
        return cfg;
    }

    public static GemsClientConfig config() {
        return loadOrCreate();
    }

    public static void save(GemsClientConfig cfg) {
        Path path = configPath();
        cached = cfg;
        write(path, cfg);
    }

    private static Path configPath() {
        return FabricLoader.getInstance().getConfigDir().resolve(DIR).resolve(FILE);
    }

    private static void write(Path path, GemsClientConfig cfg) {
        try {
            Files.createDirectories(path.getParent());
            try (var writer = Files.newBufferedWriter(path)) {
                GSON.toJson(cfg, writer);
            }
        } catch (IOException e) {
            GemsMod.LOGGER.warn("Failed to write client config: {}", path, e);
        }
    }
}
