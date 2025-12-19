package com.blissmc.gems.power;

import com.blissmc.gems.GemsMod;
import net.minecraft.util.Identifier;

public final class PowerIds {
    // Passives
    public static final Identifier SOUL_CAPTURE = id("soul_capture");
    public static final Identifier SOUL_HEALING = id("soul_healing");

    public static final Identifier FIRE_RESISTANCE = id("fire_resistance");
    public static final Identifier AUTO_SMELT = id("auto_smelt");
    public static final Identifier AUTO_ENCHANT_FIRE_ASPECT = id("auto_enchant_fire_aspect");

    public static final Identifier FLUX_CHARGE_STORAGE = id("flux_charge_storage");
    public static final Identifier FLUX_ALLY_INVERSION = id("flux_ally_inversion");
    public static final Identifier FLUX_OVERCHARGE_RAMP = id("flux_overcharge_ramp");

    public static final Identifier AUTO_ENCHANT_UNBREAKING = id("auto_enchant_unbreaking");
    public static final Identifier DOUBLE_SATURATION = id("double_saturation");

    public static final Identifier FALL_DAMAGE_IMMUNITY = id("fall_damage_immunity");
    public static final Identifier AUTO_ENCHANT_POWER = id("auto_enchant_power");
    public static final Identifier AUTO_ENCHANT_PUNCH = id("auto_enchant_punch");
    public static final Identifier SCULK_SILENCE = id("sculk_silence");
    public static final Identifier CROP_TRAMPLE_IMMUNITY = id("crop_trample_immunity");

    public static final Identifier SPEED_I = id("speed_i");

    public static final Identifier STRENGTH_I = id("strength_i");
    public static final Identifier AUTO_ENCHANT_SHARPNESS = id("auto_enchant_sharpness");

    public static final Identifier AUTO_ENCHANT_MENDING = id("auto_enchant_mending");
    public static final Identifier AUTO_ENCHANT_FORTUNE = id("auto_enchant_fortune");
    public static final Identifier AUTO_ENCHANT_LOOTING = id("auto_enchant_looting");
    public static final Identifier LUCK = id("luck");
    public static final Identifier HERO_OF_THE_VILLAGE = id("hero_of_the_village");
    public static final Identifier DURABILITY_CHIP = id("durability_chip");
    public static final Identifier ARMOR_MEND_ON_HIT = id("armor_mend_on_hit");
    public static final Identifier DOUBLE_DEBRIS = id("double_debris");

    // Abilities
    public static final Identifier DIMENSIONAL_DRIFT = id("dimensional_drift");
    public static final Identifier DIMENSIONAL_VOID = id("dimensional_void");
    public static final Identifier ASTRAL_DAGGERS = id("astral_daggers");
    public static final Identifier UNBOUNDED = id("unbounded");
    public static final Identifier ASTRAL_PROJECTION = id("astral_projection");
    public static final Identifier SPOOK = id("spook");
    public static final Identifier TAG = id("tag");

    public static final Identifier COSY_CAMPFIRE = id("cosy_campfire");
    public static final Identifier CRISP = id("crisp");
    public static final Identifier FIREBALL = id("fireball");
    public static final Identifier METEOR_SHOWER = id("meteor_shower");

    public static final Identifier FLUX_BEAM = id("flux_beam");
    public static final Identifier STATIC_BURST = id("static_burst");

    public static final Identifier VITALITY_VORTEX = id("vitality_vortex");
    public static final Identifier HEALTH_DRAIN = id("health_drain");
    public static final Identifier LIFE_CIRCLE = id("life_circle");
    public static final Identifier HEART_LOCK = id("heart_lock");

    public static final Identifier DOUBLE_JUMP = id("double_jump");
    public static final Identifier DASH = id("dash");
    public static final Identifier BREEZY_BASH = id("breezy_bash");
    public static final Identifier GROUP_BREEZY_BASH = id("group_breezy_bash");

    public static final Identifier BLUR = id("blur");
    public static final Identifier SPEED_STORM = id("speed_storm");
    public static final Identifier TERMINAL_VELOCITY = id("terminal_velocity");

    public static final Identifier NULLIFY = id("nullify");
    public static final Identifier FRAILER = id("frailer");
    public static final Identifier BOUNTY_HUNTING = id("bounty_hunting");
    public static final Identifier CHAD_STRENGTH = id("chad_strength");

    public static final Identifier POCKETS = id("pockets");
    public static final Identifier UNFORTUNATE = id("unfortunate");
    public static final Identifier ITEM_LOCK = id("item_lock");
    public static final Identifier AMPLIFICATION = id("amplification");
    public static final Identifier RICH_RUSH = id("rich_rush");

    private PowerIds() {
    }

    private static Identifier id(String path) {
        return Identifier.of(GemsMod.MOD_ID, path);
    }
}

