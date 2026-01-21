package com.feel.gems.util;

import java.util.function.Consumer;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class GemsTooltipFormat {
    private GemsTooltipFormat() {
    }

    public static void appendDescription(Consumer<Text> tooltip, Text... lines) {
        if (tooltip == null || lines == null || lines.length == 0) {
            return;
        }
        tooltip.accept(Text.translatable("gems.tooltip.description").formatted(Formatting.LIGHT_PURPLE));
        for (Text line : lines) {
            if (line == null) {
                continue;
            }
            tooltip.accept(Text.literal(" - ").append(line).formatted(Formatting.GRAY));
        }
    }
}
