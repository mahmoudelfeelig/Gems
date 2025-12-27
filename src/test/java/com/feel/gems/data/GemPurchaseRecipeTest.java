package com.feel.gems.data;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class GemPurchaseRecipeTest {

    @Test
    void gemPurchaseRecipeUsesExpensiveComponents() throws Exception {
        Path recipePath = Path.of("src", "main", "resources", "data", "gems", "recipe", "gem_purchase.json");
        assertTrue(Files.exists(recipePath), "Gem purchase recipe file is missing");

        JsonObject root = JsonParser.parseString(Files.readString(recipePath)).getAsJsonObject();
        assertEquals("minecraft:crafting_shaped", root.get("type").getAsString());

        JsonObject key = root.getAsJsonObject("key");
        assertEquals("minecraft:netherite_block", key.getAsJsonObject("N").get("item").getAsString());
        assertEquals("minecraft:beacon", key.getAsJsonObject("B").get("item").getAsString());
        assertEquals("minecraft:diamond_block", key.getAsJsonObject("D").get("item").getAsString());
        assertEquals("minecraft:end_crystal", key.getAsJsonObject("E").get("item").getAsString());

        var pattern = root.getAsJsonArray("pattern");
        assertEquals(3, pattern.size());
        assertEquals("EBE", pattern.get(0).getAsString());
        assertEquals("DND", pattern.get(1).getAsString());
        assertEquals("EBE", pattern.get(2).getAsString());

        JsonObject result = root.getAsJsonObject("result");
        assertEquals("gems:gem_purchase", result.get("id").getAsString());
        assertEquals(1, result.get("count").getAsInt());
    }
}
