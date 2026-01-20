package com.feel.gems.item.legendary;

import com.feel.gems.GemsMod;
import com.feel.gems.legendary.LegendaryItem;
import com.feel.gems.util.GemsTooltipFormat;
import java.util.function.Consumer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * Chrono Charm - reduces ability cooldowns while carried.
 */
public final class ChronoCharmItem extends Item implements LegendaryItem {
    public ChronoCharmItem(Settings settings) {
        super(settings);
    }

    @Override
    public String legendaryId() {
        return Identifier.of(GemsMod.MOD_ID, "chrono_charm").toString();
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, TooltipDisplayComponent displayComponent, Consumer<Text> tooltip, TooltipType type) {
        GemsTooltipFormat.appendDescription(
                tooltip,
                Text.translatable("gems.item.chrono_charm.tooltip.1"),
                Text.translatable("gems.item.chrono_charm.tooltip.2")
        );
    }
}
