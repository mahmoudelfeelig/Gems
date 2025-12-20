package com.feel.gems.client.hud;

import com.feel.gems.core.GemDefinition;
import com.feel.gems.core.GemId;
import com.feel.gems.core.GemRegistry;
import com.feel.gems.item.GemItem;
import com.feel.gems.power.GemAbility;
import com.feel.gems.power.GemPassive;
import com.feel.gems.power.ModAbilities;
import com.feel.gems.power.ModPassives;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.List;

public final class GemsTooltips {
    private static boolean registered = false;

    private GemsTooltips() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;

        ItemTooltipCallback.EVENT.register((stack, ctx, type, lines) -> appendTooltip(stack, lines));
    }

    private static void appendTooltip(ItemStack stack, List<Text> lines) {
        if (!(stack.getItem() instanceof GemItem item)) {
            return;
        }

        GemId gem = item.gemId();
        GemDefinition def = GemRegistry.definition(gem);

        lines.add(Text.literal("Passives").formatted(Formatting.GRAY));
        for (Identifier id : def.passives()) {
            GemPassive passive = ModPassives.get(id);
            String name = passive != null ? passive.name() : id.toString();
            String desc = passive != null ? passive.description() : "";
            lines.add(Text.literal(" - " + name + (desc.isEmpty() ? "" : ": " + desc)).formatted(Formatting.DARK_GRAY));
        }

        lines.add(Text.literal("Abilities").formatted(Formatting.GRAY));
        for (Identifier id : def.abilities()) {
            GemAbility ability = ModAbilities.get(id);
            String name = ability != null ? ability.name() : id.toString();
            String desc = ability != null ? ability.description() : "";
            lines.add(Text.literal(" - " + name + (desc.isEmpty() ? "" : ": " + desc)).formatted(Formatting.DARK_GRAY));
        }

        if (gem == GemId.FLUX) {
            lines.add(Text.literal("Sneak + Right-click to consume charge items.").formatted(Formatting.AQUA));
        }
    }
}
