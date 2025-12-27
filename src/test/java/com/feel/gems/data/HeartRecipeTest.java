package com.feel.gems.data;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;


public class HeartRecipeTest {

    @Test
    void heartRecipeUsesExpensiveComponents() throws Exception {
        Path recipePath = Path.of("src", "main", "resources", "data", "gems", "recipe", "heart.json");
        assertTrue(Files.exists(recipePath), "Heart recipe file is missing");

        JsonObject root = JsonParser.parseString(Files.readString(recipePath)).getAsJsonObject();
        assertEquals("minecraft:crafting_shaped", root.get("type").getAsString());

        JsonObject key = root.getAsJsonObject("key");
        assertEquals("minecraft:netherite_scrap", key.getAsJsonObject("N").get("item").getAsString());
        assertEquals("minecraft:iron_block", key.getAsJsonObject("I").get("item").getAsString());
        assertEquals("minecraft:gold_block", key.getAsJsonObject("G").get("item").getAsString());

        var pattern = root.getAsJsonArray("pattern");
        assertEquals(3, pattern.size());
        assertEquals("NIN", pattern.get(0).getAsString());
        assertEquals("IGI", pattern.get(1).getAsString());
        assertEquals("NIN", pattern.get(2).getAsString());

        JsonObject result = root.getAsJsonObject("result");
        assertEquals("gems:heart", result.get("id").getAsString());
        assertEquals(1, result.get("count").getAsInt());
    }
}
