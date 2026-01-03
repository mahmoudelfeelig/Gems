package com.feel.gems.item.legendary;

import com.feel.gems.GemsMod;
import com.feel.gems.legendary.LegendaryItem;
import java.util.function.Consumer;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.item.BowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;




public final class HunterSightBowItem extends BowItem implements LegendaryItem {
    public HunterSightBowItem(Settings settings) {
        super(settings.enchantable(1));
    }

    @Override
    public String legendaryId() {
        return Identifier.of(GemsMod.MOD_ID, "hunters_sight_bow").toString();
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, TooltipDisplayComponent displayComponent, Consumer<Text> tooltip, TooltipType type) {
        tooltip.accept(Text.translatable("item.gems.hunters_sight_bow.desc"));
    }
}
