package com.blissmc.gems.config;

import com.blissmc.gems.GemsMod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class GemsConfigManager {
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    private static final String DIR = "gems";
    private static final String FILE = "balance.json";

    private GemsConfigManager() {
    }

    static GemsBalanceConfig loadOrCreate() {
        Path path = configPath();
        GemsBalanceConfig cfg = read(path);
        if (cfg == null) {
            cfg = new GemsBalanceConfig();
            write(path, cfg);
        }
        return cfg;
    }

    private static Path configPath() {
        return FabricLoader.getInstance().getConfigDir().resolve(DIR).resolve(FILE);
    }

    private static GemsBalanceConfig read(Path path) {
        if (!Files.exists(path)) {
            return null;
        }
        try (Reader reader = Files.newBufferedReader(path)) {
            return GSON.fromJson(reader, GemsBalanceConfig.class);
        } catch (JsonSyntaxException e) {
            GemsMod.LOGGER.error("Failed to parse config {}, using defaults.", path, e);
            return null;
        } catch (IOException e) {
            GemsMod.LOGGER.error("Failed to read config {}, using defaults.", path, e);
            return null;
        }
    }

    private static void write(Path path, GemsBalanceConfig cfg) {
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
}

