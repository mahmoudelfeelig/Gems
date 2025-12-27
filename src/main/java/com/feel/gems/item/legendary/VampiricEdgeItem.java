package com.feel.gems.item.legendary;

import com.feel.gems.GemsMod;
import com.feel.gems.legendary.LegendaryItem;
import java.util.List;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Item.TooltipContext;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;




public final class VampiricEdgeItem extends SwordItem implements LegendaryItem {
    public VampiricEdgeItem(ToolMaterial material, Settings settings) {
        super(material, settings);
    }

    @Override
    public String legendaryId() {
        return Identifier.of(GemsMod.MOD_ID, "vampiric_edge").toString();
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.translatable("item.gems.vampiric_edge.desc"));
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return true;
    }
}
