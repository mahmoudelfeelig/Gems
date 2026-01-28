package com.feel.gems.client.hud;

import com.feel.gems.client.ClientChaosState;
import com.feel.gems.client.ClientPrismState;
import com.feel.gems.client.ClientStolenState;
import com.feel.gems.config.GemsBalance;
import com.feel.gems.core.GemDefinition;
import com.feel.gems.core.GemId;
import com.feel.gems.core.GemRegistry;
import com.feel.gems.item.GemItem;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.api.GemPassive;
import com.feel.gems.power.registry.ModAbilities;
import com.feel.gems.power.registry.ModPassives;
import com.feel.gems.util.GemsNbt;
import java.util.List;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;




public final class GemsTooltips {
    private static boolean registered = false;

    private GemsTooltips() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;

        ItemTooltipCallback.EVENT.register((stack, ctx, type, lines) -> {
            appendOwnerTooltip(stack, lines);
            appendTooltip(stack, lines);
        });
    }

    private static final String OWNER_NAME_KEY = "gemsOwnerName";
    private static final String OWNER_UUID_KEY = "gemsOwner";
    private static final String PREV_OWNER_NAME_KEY = "gemsPrevOwnerName";

    private static void appendOwnerTooltip(ItemStack stack, List<Text> lines) {
        NbtComponent custom = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (custom == null) {
            return;
        }
        NbtCompound nbt = custom.copyNbt();
        if (!nbt.contains(OWNER_NAME_KEY)) {
            return;
        }
        String ownerName = nbt.getString(OWNER_NAME_KEY).orElse("");
        if (!ownerName.isEmpty()) {
            String displayName = ownerName;
            String prevOwnerName = nbt.getString(PREV_OWNER_NAME_KEY).orElse("");
            if (!prevOwnerName.isEmpty()) {
                MinecraftClient client = MinecraftClient.getInstance();
                if (client.player != null) {
                    var ownerUuid = GemsNbt.getUuid(nbt, OWNER_UUID_KEY);
                    if (ownerUuid != null && ownerUuid.equals(client.player.getUuid())) {
                        displayName = prevOwnerName;
                    } else if (ownerUuid == null && ownerName.equalsIgnoreCase(client.player.getName().getString())) {
                        displayName = prevOwnerName;
                    }
                }
            }
            lines.add(Text.translatable("gems.item.last_owner", displayName).formatted(Formatting.GRAY));
        }
    }

    private static void appendTooltip(ItemStack stack, List<Text> lines) {
        if (!(stack.getItem() instanceof GemItem item)) {
            return;
        }

        GemId gem = item.gemId();
        GemDefinition def = GemRegistry.definition(gem);

        if (gem == GemId.PRISM || gem == GemId.CHAOS) {
            appendDynamicSections(gem, def, lines);
        } else {
            Formatting headerColor = Formatting.LIGHT_PURPLE;
            Formatting abilityColor = Formatting.AQUA;
            Formatting passiveColor = Formatting.GREEN;

            lines.add(Text.translatable("gems.tooltip.passives").formatted(headerColor));
            for (Identifier id : def.passives()) {
                GemPassive passive = ModPassives.get(id);
                appendEntry(lines, passiveColor, passive != null ? passive.name() : id.toString(), passive != null ? passive.description() : "");
            }

            lines.add(Text.translatable("gems.tooltip.abilities").formatted(headerColor));
            for (Identifier id : def.abilities()) {
                GemAbility ability = ModAbilities.get(id);
                appendEntry(lines, abilityColor, ability != null ? ability.name() : id.toString(), ability != null ? ability.description() : "");
            }
        }

        if (gem == GemId.FLUX) {
            String chord = com.feel.gems.client.GemsKeybinds.chordSlotLabel(2);
            if (!chord.isEmpty()) {
                lines.add(Text.translatable("gems.tooltip.flux.charge_key", chord).formatted(Formatting.AQUA));
            }
            lines.add(Text.translatable("gems.tooltip.flux.charge_items").formatted(Formatting.AQUA));
            var flux = GemsBalance.v().flux();
            lines.add(Text.literal(" - Diamond Block: +" + flux.chargeDiamondBlock() + "%").formatted(Formatting.DARK_AQUA));
            lines.add(Text.literal(" - Gold Block: +" + flux.chargeGoldBlock() + "%").formatted(Formatting.DARK_AQUA));
            lines.add(Text.literal(" - Copper Block: +" + flux.chargeCopperBlock() + "%").formatted(Formatting.DARK_AQUA));
            lines.add(Text.literal(" - Emerald Block: +" + flux.chargeEmeraldBlock() + "%").formatted(Formatting.DARK_AQUA));
            lines.add(Text.literal(" - Amethyst Block: +" + flux.chargeAmethystBlock() + "%").formatted(Formatting.DARK_AQUA));
            lines.add(Text.literal(" - Netherite Scrap: +" + flux.chargeNetheriteScrap() + "%").formatted(Formatting.DARK_AQUA));
            lines.add(Text.literal(" - Enchanted diamond tool/armor: +" + flux.chargeEnchantedDiamondItem() + "%").formatted(Formatting.DARK_AQUA));
        }

        appendStolenSections(lines);
    }

    private static void appendDynamicSections(GemId gem, GemDefinition def, List<Text> lines) {
        Formatting headerColor = Formatting.LIGHT_PURPLE;
        Formatting abilityColor = Formatting.AQUA;
        Formatting passiveColor = Formatting.GREEN;

        lines.add(Text.translatable("gems.tooltip.passives").formatted(headerColor));
        boolean anyPassives = false;

        for (Identifier id : def.passives()) {
            GemPassive passive = ModPassives.get(id);
            appendEntry(lines, passiveColor, passive != null ? passive.name() : id.toString(), passive != null ? passive.description() : "");
            anyPassives = true;
        }

        if (gem == GemId.PRISM) {
            for (ClientPrismState.PrismPassiveEntry entry : ClientPrismState.getPassives()) {
                GemPassive passive = ModPassives.get(entry.id());
                appendEntry(lines, passiveColor, entry.name(), passive != null ? passive.description() : "");
                anyPassives = true;
            }
        } else if (gem == GemId.CHAOS) {
            for (int i = 0; i < ClientChaosState.slotCount(); i++) {
                ClientChaosState.SlotState slot = ClientChaosState.getSlot(i);
                if (!slot.isActive()) {
                    continue;
                }
                String name = slot.passiveName();
                GemPassive passive = slot.passiveId() != null ? ModPassives.get(slot.passiveId()) : null;
                appendEntry(lines, passiveColor, name, passive != null ? passive.description() : "");
                anyPassives = true;
            }
        }

        if (!anyPassives) {
            lines.add(Text.translatable("gems.item.prism.no_selection").formatted(Formatting.GRAY));
        }

        lines.add(Text.translatable("gems.tooltip.abilities").formatted(headerColor));
        boolean anyAbilities = false;

        for (Identifier id : def.abilities()) {
            GemAbility ability = ModAbilities.get(id);
            appendEntry(lines, abilityColor, ability != null ? ability.name() : id.toString(), ability != null ? ability.description() : "");
            anyAbilities = true;
        }

        if (gem == GemId.PRISM) {
            for (ClientPrismState.PrismAbilityEntry entry : ClientPrismState.getAbilities()) {
                GemAbility ability = ModAbilities.get(entry.id());
                appendEntry(lines, abilityColor, entry.name(), ability != null ? ability.description() : "");
                anyAbilities = true;
            }
        } else if (gem == GemId.CHAOS) {
            for (int i = 0; i < ClientChaosState.slotCount(); i++) {
                ClientChaosState.SlotState slot = ClientChaosState.getSlot(i);
                if (!slot.isActive()) {
                    continue;
                }
                String name = slot.abilityName();
                GemAbility ability = slot.abilityId() != null ? ModAbilities.get(slot.abilityId()) : null;
                appendEntry(lines, abilityColor, name, ability != null ? ability.description() : "");
                anyAbilities = true;
            }
        }

        if (!anyAbilities) {
            lines.add(Text.translatable("gems.item.prism.no_selection").formatted(Formatting.GRAY));
        }
    }

    private static void appendStolenSections(List<Text> lines) {
        if (ClientStolenState.hasStolenPassives()) {
            lines.add(Text.translatable("gems.tooltip.stolen_passives").formatted(Formatting.GOLD));
            for (Identifier id : ClientStolenState.stolenPassives()) {
                GemPassive passive = ModPassives.get(id);
                appendEntry(lines, Formatting.YELLOW, passive != null ? passive.name() : id.toString(), passive != null ? passive.description() : "");
            }
        }
        if (ClientStolenState.hasStolenAbilities()) {
            lines.add(Text.translatable("gems.tooltip.stolen_abilities").formatted(Formatting.GOLD));
            for (Identifier id : ClientStolenState.stolenAbilities()) {
                GemAbility ability = ModAbilities.get(id);
                appendEntry(lines, Formatting.YELLOW, ability != null ? ability.name() : id.toString(), ability != null ? ability.description() : "");
            }
        }
    }

    private static void appendEntry(List<Text> lines, Formatting color, String name, String desc) {
        if (name == null || name.isBlank()) {
            return;
        }
        String suffix = desc != null && !desc.isBlank() ? ": " + desc : "";
        lines.add(Text.literal(" - " + name + suffix).formatted(color == null ? Formatting.DARK_GRAY : color));
    }
}
