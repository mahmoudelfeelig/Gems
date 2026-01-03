package com.feel.gems.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;


public class DataSpecRegressionTest {

    @Test
    void everyGemIdHasDefinitionWithPassivesAndAbilities() {
        GemRegistry.init();
        for (GemId id : GemId.values()) {
            GemDefinition def = GemRegistry.definition(id);
            assertNotNull(def, "Missing GemDefinition for gem " + id);
            assertFalse(def.passives().isEmpty(), "Passives empty for gem " + id);
            // Some gems legitimately have only a couple abilities; allow empty only if spec later adds.
            assertFalse(def.abilities().isEmpty(), "Abilities empty for gem " + id);
        }
    }

    @Test
    void gemDefinitionsFileCoversAllGemIds() throws Exception {
        GemRegistry.init();
        java.nio.file.Path path = java.nio.file.Path.of("src", "main", "resources", "data", "gems", "gem_definitions.json");
        assertTrue(java.nio.file.Files.exists(path), "Gem definitions JSON is missing");

        var root = com.google.gson.JsonParser.parseString(java.nio.file.Files.readString(path)).getAsJsonObject();
        var gems = root.getAsJsonArray("gems");
        assertNotNull(gems, "Gem definitions JSON missing 'gems' array");

        java.util.Set<String> ids = new java.util.HashSet<>();
        for (var entry : gems) {
            ids.add(entry.getAsJsonObject().get("id").getAsString().toUpperCase());
        }
        for (GemId id : GemId.values()) {
            assertTrue(ids.contains(id.name()), "Gem definitions missing " + id);
        }
    }
}
