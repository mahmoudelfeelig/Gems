package com.blissmc.gems.power;

import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.Identifier;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class ModPassives {
    private static final Map<Identifier, GemPassive> PASSIVES = new HashMap<>();

    static {
        register(new UnimplementedPassive(
                PowerIds.SOUL_CAPTURE,
                "Soul Capture",
                "Stores the most recently killed mob for later release."
        ));
        register(new UnimplementedPassive(
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
        register(new UnimplementedPassive(
                PowerIds.AUTO_SMELT,
                "Auto-smelt",
                "Automatically smelts certain block drops."
        ));
        register(new UnimplementedPassive(
                PowerIds.AUTO_ENCHANT_FIRE_ASPECT,
                "Auto-enchant Fire Aspect",
                "Automatically applies Fire Aspect to held melee weapons."
        ));

        register(new UnimplementedPassive(
                PowerIds.FLUX_CHARGE_STORAGE,
                "Charge Storage",
                "Consume valuables to charge the Flux Beam."
        ));
        register(new UnimplementedPassive(
                PowerIds.FLUX_ALLY_INVERSION,
                "Ally Inversion",
                "Offensive effects on trusted players repair armor instead of damaging."
        ));
        register(new UnimplementedPassive(
                PowerIds.FLUX_OVERCHARGE_RAMP,
                "Overcharge Ramp",
                "At 100% charge, begins charging toward 200% while damaging the holder."
        ));

        register(new UnimplementedPassive(
                PowerIds.AUTO_ENCHANT_UNBREAKING,
                "Auto-enchant Unbreaking",
                "Automatically applies Unbreaking to held gear."
        ));
        register(new UnimplementedPassive(
                PowerIds.DOUBLE_SATURATION,
                "Double Saturation",
                "Food restores twice the normal saturation."
        ));

        register(new UnimplementedPassive(
                PowerIds.FALL_DAMAGE_IMMUNITY,
                "Fall Damage Immunity",
                "Negates fall damage entirely."
        ));
        register(new UnimplementedPassive(
                PowerIds.AUTO_ENCHANT_POWER,
                "Auto-enchant Power",
                "Automatically applies Power to bows."
        ));
        register(new UnimplementedPassive(
                PowerIds.AUTO_ENCHANT_PUNCH,
                "Auto-enchant Punch",
                "Automatically applies Punch to bows."
        ));
        register(new UnimplementedPassive(
                PowerIds.SCULK_SILENCE,
                "Sculk Silence",
                "Immune to triggering sculk shriekers."
        ));
        register(new UnimplementedPassive(
                PowerIds.CROP_TRAMPLE_IMMUNITY,
                "Crop-Trample Immunity",
                "Prevents trampling farmland."
        ));

        register(new AttributeModifierPassive(
                PowerIds.SPEED_I,
                "Speed I",
                "Permanent movement speed bonus.",
                EntityAttributes.GENERIC_MOVEMENT_SPEED,
                EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,
                0.2D
        ));

        register(new AttributeModifierPassive(
                PowerIds.STRENGTH_I,
                "Strength I",
                "Flat attack damage bonus.",
                EntityAttributes.GENERIC_ATTACK_DAMAGE,
                EntityAttributeModifier.Operation.ADD_VALUE,
                3.0D
        ));
        register(new UnimplementedPassive(
                PowerIds.AUTO_ENCHANT_SHARPNESS,
                "Auto-enchant Sharpness",
                "Automatically applies Sharpness to held weapons."
        ));

        register(new UnimplementedPassive(
                PowerIds.AUTO_ENCHANT_MENDING,
                "Auto-enchant Mending",
                "Automatically applies Mending to tools/armor."
        ));
        register(new UnimplementedPassive(
                PowerIds.AUTO_ENCHANT_FORTUNE,
                "Auto-enchant Fortune",
                "Automatically applies Fortune to tools."
        ));
        register(new UnimplementedPassive(
                PowerIds.AUTO_ENCHANT_LOOTING,
                "Auto-enchant Looting",
                "Automatically applies Looting to weapons."
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
        register(new UnimplementedPassive(
                PowerIds.DURABILITY_CHIP,
                "Durability chip",
                "Does extra armor durability damage per strike."
        ));
        register(new UnimplementedPassive(
                PowerIds.ARMOR_MEND_ON_HIT,
                "Armor mend on hit",
                "Slowly repairs the holder's armor when hitting enemies."
        ));
        register(new UnimplementedPassive(
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

