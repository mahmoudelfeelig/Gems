package com.feel.gems.core;

import com.feel.gems.GemsMod;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.util.Identifier;




/**
 * Lightweight in-memory registry for gem definitions. Meant to be bridged to the loader's registry system.
 */
public final class GemRegistry {
    private static final Map<GemId, GemDefinition> DEFINITIONS = new EnumMap<>(GemId.class);
    private static final Gson GSON = new Gson();
    private static final String DEFINITIONS_PATH = "data/gems/gem_definitions.json";

    static {
        loadDefinitions();
    }

    private GemRegistry() {
    }

    private static void register(GemDefinition definition) {
        DEFINITIONS.put(definition.id(), definition);
    }

    public static GemDefinition definition(GemId id) {
        GemDefinition def = DEFINITIONS.get(id);
        if (def == null) {
            throw new IllegalArgumentException("Unknown gem id: " + id);
        }
        return def;
    }

    private static void loadDefinitions() {
        try (var stream = GemRegistry.class.getClassLoader().getResourceAsStream(DEFINITIONS_PATH)) {
            if (stream == null) {
                throw new IllegalStateException("Missing gem definitions at " + DEFINITIONS_PATH);
            }
            try (var reader = new java.io.InputStreamReader(stream, java.nio.charset.StandardCharsets.UTF_8)) {
                GemDefinitionFile file = GSON.fromJson(reader, GemDefinitionFile.class);
                if (file == null || file.gems == null || file.gems.isEmpty()) {
                    throw new IllegalStateException("No gem definitions found in " + DEFINITIONS_PATH);
                }
                for (GemDefinitionEntry entry : file.gems) {
                    GemDefinition def = entry.toDefinition();
                    register(def);
                }
            }
        } catch (Exception e) {
            if (e instanceof JsonParseException || e instanceof IllegalStateException) {
                GemsMod.LOGGER.error("Failed to load gem definitions", e);
            } else {
                GemsMod.LOGGER.error("Unexpected error loading gem definitions", e);
            }
            throw new IllegalStateException("Failed to load gem definitions from " + DEFINITIONS_PATH, e);
        }
    }

    private static final class GemDefinitionFile {
        private List<GemDefinitionEntry> gems = List.of();
    }

    private static final class GemDefinitionEntry {
        private String id;
        private List<String> passives = List.of();
        private List<String> abilities = List.of();

        private GemDefinition toDefinition() {
            if (id == null || id.isBlank()) {
                throw new IllegalStateException("Gem definition missing id");
            }
            GemId gemId = GemId.valueOf(id.trim().toUpperCase());
            List<Identifier> passiveIds = toIdentifiers(passives, "passives", gemId);
            List<Identifier> abilityIds = toIdentifiers(abilities, "abilities", gemId);
            return new GemDefinition(gemId, passiveIds, abilityIds);
        }
    }

    private static List<Identifier> toIdentifiers(List<String> raw, String label, GemId gemId) {
        if (raw == null) {
            throw new IllegalStateException("Gem " + gemId + " missing " + label + " list");
        }
        List<Identifier> ids = new java.util.ArrayList<>(raw.size());
        for (String value : raw) {
            Identifier id = Identifier.tryParse(value);
            if (id == null) {
                throw new IllegalStateException("Invalid " + label + " id '" + value + "' for gem " + gemId);
            }
            ids.add(id);
        }
        return List.copyOf(ids);
    }
}
