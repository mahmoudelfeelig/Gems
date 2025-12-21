package com.feel.gems.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class DataSpecRegressionTest {

    @Test
    void everyGemIdHasDefinitionWithPassivesAndAbilities() {
        for (GemId id : GemId.values()) {
            GemDefinition def = GemRegistry.definition(id);
            assertNotNull(def, "Missing GemDefinition for gem " + id);
            assertFalse(def.passives().isEmpty(), "Passives empty for gem " + id);
            // Some gems legitimately have only a couple abilities; allow empty only if spec later adds.
            assertFalse(def.abilities().isEmpty(), "Abilities empty for gem " + id);
        }
    }
}
