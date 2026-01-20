package com.feel.gems.item.legendary;

import com.feel.gems.GemsMod;
import com.feel.gems.config.GemsBalance;
import com.feel.gems.legendary.LegendaryItem;
import com.feel.gems.state.GemsPersistentDataHolder;
import com.feel.gems.util.GemsTime;
import java.util.function.Consumer;
import net.minecraft.item.Item;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.ItemStack;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;




public final class ThirdStrikeBladeItem extends Item implements LegendaryItem {
    private static final String KEY_THIRD_STRIKE_COUNT = "legendaryThirdStrikeCount";
    private static final String KEY_THIRD_STRIKE_LAST = "legendaryThirdStrikeLast";
    private static final String KEY_THIRD_STRIKE_BONUS = "legendaryThirdStrikeBonus";
    private static final String KEY_THIRD_STRIKE_BONUS_UNTIL = "legendaryThirdStrikeBonusUntil";

    public ThirdStrikeBladeItem(ToolMaterial material, Settings settings) {
        super(settings.sword(material, 3.0F, -2.4F).enchantable(15));
    }

    @Override
    public String legendaryId() {
        return Identifier.of(GemsMod.MOD_ID, "third_strike_blade").toString();
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, TooltipDisplayComponent displayComponent, Consumer<Text> tooltip, TooltipType type) {
        tooltip.accept(Text.translatable("item.gems.third_strike_blade.desc"));
    }

    public static void recordCriticalHit(ServerPlayerEntity attacker) {
        if (attacker == null) {
            return;
        }
        long now = GemsTime.now(attacker);
        var data = ((GemsPersistentDataHolder) attacker).gems$getPersistentData();
        long last = data.getLong(KEY_THIRD_STRIKE_LAST, 0L);
        int window = GemsBalance.v().legendary().thirdStrikeWindowTicks();
        int count = data.getInt(KEY_THIRD_STRIKE_COUNT, 0);
        if (window > 0 && now - last > window) {
            count = 0;
        }
        count += 1;
        data.putInt(KEY_THIRD_STRIKE_COUNT, count);
        data.putLong(KEY_THIRD_STRIKE_LAST, now);
        if (count % 3 == 0) {
            float bonus = GemsBalance.v().legendary().thirdStrikeBonusDamage();
            if (bonus > 0.0F) {
                data.putFloat(KEY_THIRD_STRIKE_BONUS, bonus);
                data.putLong(KEY_THIRD_STRIKE_BONUS_UNTIL, now + 5L);
            }
        }
    }

    public static float consumeQueuedBonus(ServerPlayerEntity attacker) {
        if (attacker == null) {
            return 0.0F;
        }
        var data = ((GemsPersistentDataHolder) attacker).gems$getPersistentData();
        long until = data.getLong(KEY_THIRD_STRIKE_BONUS_UNTIL, 0L);
        if (until <= 0L || GemsTime.now(attacker) > until) {
            data.remove(KEY_THIRD_STRIKE_BONUS);
            data.remove(KEY_THIRD_STRIKE_BONUS_UNTIL);
            return 0.0F;
        }
        float bonus = data.getFloat(KEY_THIRD_STRIKE_BONUS, 0.0F);
        data.remove(KEY_THIRD_STRIKE_BONUS);
        data.remove(KEY_THIRD_STRIKE_BONUS_UNTIL);
        return Math.max(0.0F, bonus);
    }
}
