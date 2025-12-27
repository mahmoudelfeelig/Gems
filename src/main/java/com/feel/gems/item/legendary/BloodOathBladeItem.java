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




public final class BloodOathBladeItem extends SwordItem implements LegendaryItem {
    public BloodOathBladeItem(ToolMaterial material, Settings settings) {
        super(material, settings);
    }

    @Override
    public String legendaryId() {
        return Identifier.of(GemsMod.MOD_ID, "blood_oath_blade").toString();
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.translatable("item.gems.blood_oath_blade.desc"));
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return true;
    }
}
