package com.feel.gems.client.hud;

import com.feel.gems.client.ClientAbilitySelection;
import com.feel.gems.client.ClientCooldowns;
import com.feel.gems.client.ClientExtraState;
import com.feel.gems.client.ClientGemState;
import com.feel.gems.client.GemsKeybinds;
import com.feel.gems.core.GemDefinition;
import com.feel.gems.core.GemEnergyState;
import com.feel.gems.core.GemEnergyTier;
import com.feel.gems.core.GemId;
import com.feel.gems.core.GemRegistry;
import com.feel.gems.power.GemAbility;
import com.feel.gems.power.ModAbilities;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

import java.util.List;

public final class GemsHud {
    private static boolean registered = false;

    private GemsHud() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;

        HudRenderCallback.EVENT.register((ctx, tickCounter) -> render(ctx));
    }

    private static void render(DrawContext ctx) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) {
            return;
        }
        if (client.options.hudHidden) {
            return;
        }

        TextRenderer tr = client.textRenderer;
        int x = 8;
        int y = 8;
        int lineHeight = tr.fontHeight + 2;

        if (!ClientGemState.isInitialized()) {
            ctx.drawTextWithShadow(tr, "Gem: syncing…", x, y, 0xAAAAAA);
            return;
        }

        GemId gem = ClientGemState.activeGem();
        int energy = ClientGemState.energy();
        int maxHearts = ClientGemState.maxHearts();
        GemEnergyTier tier = new GemEnergyState(energy).tier();

        ctx.drawTextWithShadow(tr, "Gem: " + title(gem.name()), x, y, 0xFFFFFF);
        y += lineHeight;
        ctx.drawTextWithShadow(tr, "Energy: " + tierLabel(tier) + " (" + energy + "/10)", x, y, tierColor(tier));
        y += lineHeight;
        ctx.drawTextWithShadow(tr, "Max hearts: " + (maxHearts), x, y, 0xFFFFFF);
        y += lineHeight;

        if (gem == GemId.FLUX) {
            int charge = ClientExtraState.fluxChargePercent();
            int color = charge >= 200 ? 0xFF4444 : (charge >= 100 ? 0x55FFFF : 0xFFFFFF);
            ctx.drawTextWithShadow(tr, "Flux charge: " + charge + "%", x, y, color);
            y += lineHeight;
        }
        if (gem == GemId.ASTRA) {
            if (ClientExtraState.hasSoul()) {
                String type = prettyEntityId(ClientExtraState.soulTypeId());
                ctx.drawTextWithShadow(tr, "Soul: captured (" + type + ")", x, y, 0x55FFFF);
            } else {
                ctx.drawTextWithShadow(tr, "Soul: none", x, y, 0xAAAAAA);
            }
            y += lineHeight;
        }

        String modifier = GemsKeybinds.modifierLabel();
        if (!modifier.isEmpty()) {
            ctx.drawTextWithShadow(tr, "Abilities: hold " + modifier + " + [1..]", x, y, 0xAAAAAA);
            y += lineHeight;
        }

        GemDefinition def = GemRegistry.definition(gem);
        List<Identifier> abilities = def.abilities();
        int unlocked = new GemEnergyState(energy).unlockedAbilityCount(abilities.size());
        int selectedSlot = ClientAbilitySelection.slotNumber(gem);

        for (int i = 0; i < abilities.size(); i++) {
            Identifier id = abilities.get(i);
            GemAbility ability = ModAbilities.get(id);
            String name = ability != null ? ability.name() : id.toString();

            boolean isUnlocked = i < unlocked;
            int remaining = ClientCooldowns.remainingTicks(gem, id);

            String key = GemsKeybinds.chordSlotLabel(i + 1);
            String suffix;
            int color;
            if (!isUnlocked) {
                suffix = " (locked)";
                color = 0x777777;
            } else if (remaining > 0) {
                suffix = " (" + seconds(remaining) + "s)";
                color = 0xFFCC33;
            } else {
                suffix = "";
                color = 0xFFFFFF;
            }

            boolean selected = selectedSlot == (i + 1);
            String prefix = selected ? "» " : "";
            ctx.drawTextWithShadow(tr, prefix + key + " " + name + suffix, x, y, selected ? 0x55FF55 : color);
            y += lineHeight;
            if (y > client.getWindow().getScaledHeight() - lineHeight) {
                break;
            }
        }

        if (gem == GemId.FLUX) {
            int slot = abilities.size() + 1;
            if (slot <= 9) {
                boolean selected = selectedSlot == slot;
                ctx.drawTextWithShadow(
                        tr,
                        (selected ? "» " : "") + GemsKeybinds.chordSlotLabel(slot) + " Flux Charge",
                        x,
                        y,
                        selected ? 0x55FF55 : 0x55FFFF
                );
                y += lineHeight;
            }
        }

        if (gem == GemId.ASTRA) {
            int soulSlot = abilities.size() + 1;
            if (soulSlot <= 10) {
                boolean selected = selectedSlot == soulSlot;
                ctx.drawTextWithShadow(
                        tr,
                        (selected ? "» " : "") + GemsKeybinds.chordSlotLabel(soulSlot) + " Soul Release",
                        x,
                        y,
                        selected ? 0x55FF55 : 0x55FFFF
                );
            }
        }
    }

    private static int seconds(int remainingTicks) {
        return Math.max(1, (remainingTicks + 19) / 20);
    }

    private static int tierColor(GemEnergyTier tier) {
        return switch (tier) {
            case BROKEN -> 0xFF4444;
            case COMMON -> 0xAAAAAA;
            case RARE -> 0x55AAFF;
            case ELITE -> 0x55FF55;
            case MYTHICAL -> 0xCC66FF;
            case LEGENDARY, LEGENDARY_PLUS_1, LEGENDARY_PLUS_2, LEGENDARY_PLUS_3, LEGENDARY_PLUS_4, LEGENDARY_PLUS_5 -> 0xFFD700;
        };
    }

    private static String tierLabel(GemEnergyTier tier) {
        return switch (tier) {
            case BROKEN -> "Broken";
            case COMMON -> "Common";
            case RARE -> "Rare";
            case ELITE -> "Elite";
            case MYTHICAL -> "Mythical";
            case LEGENDARY -> "Legendary";
            case LEGENDARY_PLUS_1 -> "Legendary +1";
            case LEGENDARY_PLUS_2 -> "Legendary +2";
            case LEGENDARY_PLUS_3 -> "Legendary +3";
            case LEGENDARY_PLUS_4 -> "Legendary +4";
            case LEGENDARY_PLUS_5 -> "Legendary +5";
        };
    }

    private static String title(String raw) {
        if (raw.isEmpty()) {
            return raw;
        }
        String lower = raw.toLowerCase();
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    private static String prettyEntityId(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "?";
        }
        int idx = raw.indexOf(':');
        String path = idx >= 0 ? raw.substring(idx + 1) : raw;
        return path.replace('_', ' ');
    }
}
