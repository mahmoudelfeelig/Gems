package com.feel.gems.power;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TimeSourceConsistencyTest {
    @Test
    void powerAndNetCodeDoesNotUsePerWorldTime() throws IOException {
        List<Path> roots = List.of(
                Path.of("src/main/java/com/blissmc/gems/power"),
                Path.of("src/main/java/com/blissmc/gems/net")
        );

        List<String> offenders = new ArrayList<>();
        for (Path root : roots) {
            if (!Files.exists(root)) {
                continue;
            }
            try (var stream = Files.walk(root)) {
                stream.filter(p -> p.toString().endsWith(".java")).forEach(p -> {
                    try {
                        String src = Files.readString(p, StandardCharsets.UTF_8);
                        if (src.contains("getServerWorld().getTime()") || src.contains(".getTime()")) {
                            offenders.add(p.toString());
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        }

        assertTrue(offenders.isEmpty(), "Found per-world time usage in: " + offenders);
    }
}

