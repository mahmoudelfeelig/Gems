package com.feel.gems.item.legendary;

import com.feel.gems.GemsMod;
import com.feel.gems.legendary.LegendaryItem;
import java.util.function.Consumer;
import net.minecraft.item.Item;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.ItemStack;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;




public final class VampiricEdgeItem extends Item implements LegendaryItem {
    public VampiricEdgeItem(ToolMaterial material, Settings settings) {
        super(settings.sword(material, 3.0F, -2.4F).enchantable(15));
    }

    @Override
    public String legendaryId() {
        return Identifier.of(GemsMod.MOD_ID, "vampiric_edge").toString();
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, TooltipDisplayComponent displayComponent, Consumer<Text> tooltip, TooltipType type) {
        tooltip.accept(Text.translatable("item.gems.vampiric_edge.desc"));
    }
}
