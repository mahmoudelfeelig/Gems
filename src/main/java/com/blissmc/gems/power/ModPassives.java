package com.feel.gems.power;

import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.Identifier;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class ModPassives {
    private static final Map<Identifier, GemPassive> PASSIVES = new HashMap<>();

    static {
        register(new MarkerPassive(
                PowerIds.SOUL_CAPTURE,
                "Soul Capture",
                "Stores the most recently killed mob for later release."
        ));
        register(new MarkerPassive(
                PowerIds.SOUL_HEALING,
                "Soul Healing",
                "Heals the holder on successful soul capture or release."
        ));

        register(new StatusEffectPassive(
                PowerIds.FIRE_RESISTANCE,
                "Fire Resistance",
                "Permanent immunity to fire/lava damage.",
                StatusEffects.FIRE_RESISTANCE,
                0
        ));
        register(new MarkerPassive(
                PowerIds.AUTO_SMELT,
                "Auto-smelt",
                "Automatically smelts smeltable block drops."
        ));
        register(AutoEnchantPassive.fireAspect(
                PowerIds.AUTO_ENCHANT_FIRE_ASPECT,
                "Auto-enchant Fire Aspect",
                "Automatically applies Fire Aspect to melee weapons.",
                1
        ));

        register(new MarkerPassive(
                PowerIds.FLUX_CHARGE_STORAGE,
                "Charge Storage",
                "Consume valuables to charge the Flux Beam."
        ));
        register(new MarkerPassive(
                PowerIds.FLUX_ALLY_INVERSION,
                "Ally Inversion",
                "Offensive effects on trusted players repair armor instead of damaging."
        ));
        register(new MarkerPassive(
                PowerIds.FLUX_OVERCHARGE_RAMP,
                "Overcharge Ramp",
                "At 100% charge, begins charging toward 200% while damaging the holder."
        ));

        register(AutoEnchantPassive.unbreaking(
                PowerIds.AUTO_ENCHANT_UNBREAKING,
                "Auto-enchant Unbreaking",
                "Automatically applies Unbreaking to tools/armor.",
                3
        ));
        register(new MarkerPassive(
                PowerIds.DOUBLE_SATURATION,
                "Double Saturation",
                "Food restores double saturation."
        ));

        register(new MarkerPassive(
                PowerIds.FALL_DAMAGE_IMMUNITY,
                "Fall Damage Immunity",
                "Negates fall damage entirely."
        ));
        register(AutoEnchantPassive.power(
                PowerIds.AUTO_ENCHANT_POWER,
                "Auto-enchant Power",
                "Automatically applies Power to bows.",
                3
        ));
        register(AutoEnchantPassive.punch(
                PowerIds.AUTO_ENCHANT_PUNCH,
                "Auto-enchant Punch",
                "Automatically applies Punch to bows.",
                1
        ));
        register(new MarkerPassive(
                PowerIds.SCULK_SILENCE,
                "Sculk Silence",
                "Immune to triggering sculk shriekers."
        ));
        register(new MarkerPassive(
                PowerIds.CROP_TRAMPLE_IMMUNITY,
                "Crop-Trample Immunity",
                "Prevents trampling farmland."
        ));

        register(new StatusEffectPassive(
                PowerIds.SPEED_I,
                "Speed I",
                "Permanent Speed I effect.",
                StatusEffects.SPEED,
                0
        ));

        register(new StatusEffectPassive(
                PowerIds.STRENGTH_I,
                "Strength I",
                "Permanent Strength I effect.",
                StatusEffects.STRENGTH,
                0
        ));
        register(AutoEnchantPassive.sharpness(
                PowerIds.AUTO_ENCHANT_SHARPNESS,
                "Auto-enchant Sharpness",
                "Automatically applies Sharpness to melee weapons.",
                3
        ));

        register(AutoEnchantPassive.mending(
                PowerIds.AUTO_ENCHANT_MENDING,
                "Auto-enchant Mending",
                "Automatically applies Mending to tools/armor."
        ));
        register(AutoEnchantPassive.fortune(
                PowerIds.AUTO_ENCHANT_FORTUNE,
                "Auto-enchant Fortune",
                "Automatically applies Fortune to tools.",
                3
        ));
        register(AutoEnchantPassive.looting(
                PowerIds.AUTO_ENCHANT_LOOTING,
                "Auto-enchant Looting",
                "Automatically applies Looting to melee weapons.",
                3
        ));
        register(new StatusEffectPassive(
                PowerIds.LUCK,
                "Luck",
                "Permanent Luck effect.",
                StatusEffects.LUCK,
                0
        ));
        register(new StatusEffectPassive(
                PowerIds.HERO_OF_THE_VILLAGE,
                "Hero of the Village",
                "Permanent Hero of the Village effect.",
                StatusEffects.HERO_OF_THE_VILLAGE,
                0
        ));
        register(new MarkerPassive(
                PowerIds.DURABILITY_CHIP,
                "Durability chip",
                "Does extra armor durability damage per strike."
        ));
        register(new MarkerPassive(
                PowerIds.ARMOR_MEND_ON_HIT,
                "Armor mend on hit",
                "Slowly repairs the holder's armor when hitting enemies."
        ));
        register(new MarkerPassive(
                PowerIds.DOUBLE_DEBRIS,
                "Double Debris",
                "Furnace outputs double netherite scrap."
        ));
    }

    private ModPassives() {
    }

    public static GemPassive get(Identifier id) {
        return PASSIVES.get(id);
    }

    public static Map<Identifier, GemPassive> all() {
        return Collections.unmodifiableMap(PASSIVES);
    }

    private static void register(GemPassive passive) {
        PASSIVES.put(passive.id(), passive);
    }
}
