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
                "Consume valuables (offhand/inventory) to charge Flux (Diamond/Gold/Copper/Emerald/Amethyst Blocks, Netherite Scrap, enchanted diamond gear)."
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
                PowerIds.SPEED_HASTE,
                "Haste I",
                "Permanent Haste I effect.",
                StatusEffects.HASTE,
                0
        ));
        register(new MarkerPassive(
                PowerIds.SPEED_MOMENTUM,
                "Momentum",
                "Abilities scale with your movement speed at cast time."
        ));
        register(new MarkerPassive(
                PowerIds.SPEED_FRICTIONLESS,
                "Frictionless Steps",
                "Reduced slowdown from cobweb, honey, and powder snow."
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

        register(new TerrorDreadAuraPassive());
        register(new TerrorFearlessPassive());
        register(new MarkerPassive(
                PowerIds.TERROR_BLOOD_PRICE,
                "Blood Price",
                "On killing a player, gain a short burst of power."
        ));

        register(new MarkerPassive(
                PowerIds.SUMMONER_BOND,
                "Summoner's Bond",
                "Your summons will never harm you or your trusted players."
        ));
        register(new MarkerPassive(
                PowerIds.SUMMONER_COMMANDERS_MARK,
                "Commander's Mark",
                "Sword hits mark targets; summons prioritize and gain a short damage buff."
        ));
        register(new MarkerPassive(
                PowerIds.SUMMONER_SOULBOUND,
                "Soulbound Minions",
                "Summons despawn when you die or log out."
        ));
        register(new MarkerPassive(
                PowerIds.SUMMONER_FAMILIARS_BLESSING,
                "Familiar's Blessing",
                "Summons spawn with bonus health."
        ));

        // Space
        register(new MarkerPassive(
                PowerIds.SPACE_LUNAR_SCALING,
                "Lunar Scaling",
                "Outgoing damage and self-healing scale with the current moon phase (full moon strongest)."
        ));
        register(new StatusEffectPassive(
                PowerIds.SPACE_LOW_GRAVITY,
                "Low Gravity",
                "Minor Slow Falling and no fall damage while the gem is active.",
                StatusEffects.SLOW_FALLING,
                0
        ));
        register(new MarkerPassive(
                PowerIds.SPACE_STARSHIELD,
                "Starshield",
                "Reduces projectile damage while outdoors at night."
        ));

        // Reaper
        register(new MarkerPassive(
                PowerIds.REAPER_ROT_EATER,
                "Rot Eater",
                "Eating rotten flesh or spider eyes does not apply negative effects."
        ));
        register(new MarkerPassive(
                PowerIds.REAPER_UNDEAD_WARD,
                "Undead Ward",
                "Reduces damage taken from undead mobs."
        ));
        register(new MarkerPassive(
                PowerIds.REAPER_HARVEST,
                "Harvest",
                "Killing mobs grants a brief burst of regeneration."
        ));

        // Pillager
        register(new MarkerPassive(
                PowerIds.PILLAGER_RAIDERS_TRAINING,
                "Raider's Training",
                "Your fired projectiles travel faster."
        ));
        register(new MarkerPassive(
                PowerIds.PILLAGER_SHIELDBREAKER,
                "Shieldbreaker",
                "Your melee hits can disable shields without using an axe."
        ));
        register(new MarkerPassive(
                PowerIds.PILLAGER_ILLAGER_DISCIPLINE,
                "Illager Discipline",
                "When you drop low, gain a brief burst of Resistance (cooldown)."
        ));

        // Spy/Mimic
        register(new MarkerPassive(
                PowerIds.SPY_STILLNESS_CLOAK,
                "Stillness Cloak",
                "Stand still to become invisible with no particles; moving cancels."
        ));
        register(new MarkerPassive(
                PowerIds.SPY_SILENT_STEP,
                "Silent Step",
                "Suppresses sculk sensor/shrieker activation from your actions."
        ));
        register(new MarkerPassive(
                PowerIds.SPY_FALSE_SIGNATURE,
                "False Signature",
                "Reduces how much information enemies can infer from your effects."
        ));
        register(new StatusEffectPassive(
                PowerIds.SPY_QUICK_HANDS,
                "Quick Hands",
                "Permanent minor Haste while the gem is active.",
                StatusEffects.HASTE,
                0
        ));

        // Beacon
        register(new MarkerPassive(
                PowerIds.BEACON_CORE,
                "Beacon Core",
                "Trusted allies near you receive periodic Regeneration pulses."
        ));
        register(new MarkerPassive(
                PowerIds.BEACON_STABILIZE,
                "Stabilize",
                "Reduces harmful effect durations on trusted allies nearby."
        ));
        register(new MarkerPassive(
                PowerIds.BEACON_RALLY,
                "Rally",
                "Casting a beacon aura grants allies brief Absorption."
        ));

        // Air
        register(new AirMacePassive(
                PowerIds.AIR_WINDBURST_MACE,
                "Windburst Mace",
                "Grants a maxed-out mace while the gem is active."
        ));
        register(new MarkerPassive(
                PowerIds.AIR_AERIAL_GUARD,
                "Aerial Guard",
                "Reduced fall damage and knockback while holding the mace."
        ));
        register(new MarkerPassive(
                PowerIds.AIR_SKYBORN,
                "Skyborn",
                "Taking damage midair grants brief Slow Falling (cooldown)."
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
