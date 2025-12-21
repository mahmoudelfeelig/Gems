package com.feel.gems.data;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class HeartRecipeTest {

    @Test
    void heartRecipeUsesExpensiveComponents() throws Exception {
        Path recipePath = Path.of("src", "main", "resources", "data", "gems", "recipe", "heart.json");
        assertTrue(Files.exists(recipePath), "Heart recipe file is missing");

        JsonObject root = JsonParser.parseString(Files.readString(recipePath)).getAsJsonObject();
        assertEquals("minecraft:crafting_shaped", root.get("type").getAsString());

        JsonObject key = root.getAsJsonObject("key");
        assertEquals("minecraft:netherite_scrap", key.getAsJsonObject("S").get("item").getAsString());
        assertEquals("minecraft:gold_block", key.getAsJsonObject("G").get("item").getAsString());

        JsonObject result = root.getAsJsonObject("result");
        assertEquals("gems:heart", result.get("id").getAsString());
        assertEquals(1, result.get("count").getAsInt());
    }
}
