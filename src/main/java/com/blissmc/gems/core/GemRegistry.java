package com.feel.gems.core;

import com.feel.gems.power.PowerIds;
import net.minecraft.util.Identifier;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Lightweight in-memory registry for gem definitions. Meant to be bridged to the loader's registry system.
 */
public final class GemRegistry {
    private static final Map<GemId, GemDefinition> DEFINITIONS = new EnumMap<>(GemId.class);

    static {
        register(new GemDefinition(
                GemId.ASTRA,
                List.of(
                        PowerIds.SOUL_CAPTURE,
                        PowerIds.SOUL_HEALING
                ),
                List.of(
                        PowerIds.SHADOW_ANCHOR,
                        PowerIds.DIMENSIONAL_VOID,
                        PowerIds.ASTRAL_DAGGERS,
                        PowerIds.UNBOUNDED,
                        PowerIds.ASTRAL_CAMERA,
                        PowerIds.SPOOK,
                        PowerIds.TAG
                )
        ));

        register(new GemDefinition(
                GemId.FIRE,
                List.of(
                        PowerIds.FIRE_RESISTANCE,
                        PowerIds.AUTO_SMELT,
                        PowerIds.AUTO_ENCHANT_FIRE_ASPECT
                ),
                List.of(
                        PowerIds.COSY_CAMPFIRE,
                        PowerIds.HEAT_HAZE_ZONE,
                        PowerIds.FIREBALL,
                        PowerIds.METEOR_SHOWER
                )
        ));

        register(new GemDefinition(
                GemId.FLUX,
                List.of(
                        PowerIds.FLUX_CHARGE_STORAGE,
                        PowerIds.FLUX_ALLY_INVERSION,
                        PowerIds.FLUX_OVERCHARGE_RAMP
                ),
                List.of(
                        PowerIds.FLUX_BEAM,
                        PowerIds.STATIC_BURST
                )
        ));

        register(new GemDefinition(
                GemId.LIFE,
                List.of(
                        PowerIds.AUTO_ENCHANT_UNBREAKING,
                        PowerIds.DOUBLE_SATURATION
                ),
                List.of(
                        PowerIds.VITALITY_VORTEX,
                        PowerIds.HEALTH_DRAIN,
                        PowerIds.LIFE_CIRCLE,
                        PowerIds.HEART_LOCK
                )
        ));

        register(new GemDefinition(
                GemId.PUFF,
                List.of(
                        PowerIds.FALL_DAMAGE_IMMUNITY,
                        PowerIds.AUTO_ENCHANT_POWER,
                        PowerIds.AUTO_ENCHANT_PUNCH,
                        PowerIds.SCULK_SILENCE,
                        PowerIds.CROP_TRAMPLE_IMMUNITY
                ),
                List.of(
                        PowerIds.DOUBLE_JUMP,
                        PowerIds.DASH,
                        PowerIds.BREEZY_BASH,
                        PowerIds.GROUP_BREEZY_BASH
                )
        ));

        register(new GemDefinition(
                GemId.SPEED,
                List.of(
                        PowerIds.SPEED_I,
                        PowerIds.SPEED_HASTE,
                        PowerIds.SPEED_MOMENTUM,
                        PowerIds.SPEED_FRICTIONLESS
                ),
                List.of(
                        PowerIds.ARC_SHOT,
                        PowerIds.SPEED_STORM,
                        PowerIds.TERMINAL_VELOCITY,
                        PowerIds.SPEED_SLIPSTREAM,
                        PowerIds.SPEED_AFTERIMAGE
                )
        ));

        register(new GemDefinition(
                GemId.STRENGTH,
                List.of(
                        PowerIds.STRENGTH_I,
                        PowerIds.AUTO_ENCHANT_SHARPNESS
                ),
                List.of(
                        PowerIds.NULLIFY,
                        PowerIds.FRAILER,
                        PowerIds.BOUNTY_HUNTING,
                        PowerIds.CHAD_STRENGTH
                )
        ));

        register(new GemDefinition(
                GemId.WEALTH,
                List.of(
                        PowerIds.AUTO_ENCHANT_MENDING,
                        PowerIds.AUTO_ENCHANT_FORTUNE,
                        PowerIds.AUTO_ENCHANT_LOOTING,
                        PowerIds.LUCK,
                        PowerIds.HERO_OF_THE_VILLAGE,
                        PowerIds.DURABILITY_CHIP,
                        PowerIds.ARMOR_MEND_ON_HIT,
                        PowerIds.DOUBLE_DEBRIS
                ),
                List.of(
                        PowerIds.POCKETS,
                        PowerIds.FUMBLE,
                        PowerIds.HOTBAR_LOCK,
                        PowerIds.AMPLIFICATION,
                        PowerIds.RICH_RUSH
                )
        ));

        register(new GemDefinition(
                GemId.TERROR,
                List.of(
                        PowerIds.TERROR_DREAD_AURA,
                        PowerIds.TERROR_FEARLESS,
                        PowerIds.TERROR_BLOOD_PRICE
                ),
                List.of(
                        PowerIds.TERROR_TRADE,
                        PowerIds.TERROR_PANIC_RING
                )
        ));

        register(new GemDefinition(
                GemId.SUMMONER,
                List.of(
                        PowerIds.SUMMONER_BOND,
                        PowerIds.SUMMONER_COMMANDERS_MARK,
                        PowerIds.SUMMONER_SOULBOUND,
                        PowerIds.SUMMONER_FAMILIARS_BLESSING
                ),
                List.of(
                        PowerIds.SUMMON_SLOT_1,
                        PowerIds.SUMMON_SLOT_2,
                        PowerIds.SUMMON_SLOT_3,
                        PowerIds.SUMMON_SLOT_4,
                        PowerIds.SUMMON_SLOT_5,
                        PowerIds.SUMMON_RECALL
                )
        ));

        register(new GemDefinition(
                GemId.SPACE,
                List.of(
                        PowerIds.SPACE_LUNAR_SCALING,
                        PowerIds.SPACE_LOW_GRAVITY,
                        PowerIds.SPACE_STARSHIELD
                ),
                List.of(
                        PowerIds.SPACE_ORBITAL_LASER,
                        PowerIds.SPACE_GRAVITY_FIELD,
                        PowerIds.SPACE_BLACK_HOLE,
                        PowerIds.SPACE_WHITE_HOLE
                )
        ));

        register(new GemDefinition(
                GemId.REAPER,
                List.of(
                        PowerIds.REAPER_ROT_EATER,
                        PowerIds.REAPER_UNDEAD_WARD,
                        PowerIds.REAPER_HARVEST
                ),
                List.of(
                        PowerIds.REAPER_GRAVE_STEED,
                        PowerIds.REAPER_WITHERING_STRIKES,
                        PowerIds.REAPER_DEATH_OATH,
                        PowerIds.REAPER_SCYTHE_SWEEP,
                        PowerIds.REAPER_BLOOD_CHARGE,
                        PowerIds.REAPER_SHADE_CLONE
                )
        ));

        register(new GemDefinition(
                GemId.PILLAGER,
                List.of(
                        PowerIds.PILLAGER_RAIDERS_TRAINING,
                        PowerIds.PILLAGER_SHIELDBREAKER,
                        PowerIds.PILLAGER_ILLAGER_DISCIPLINE
                ),
                List.of(
                        PowerIds.PILLAGER_FANGS,
                        PowerIds.PILLAGER_RAVAGE,
                        PowerIds.PILLAGER_VINDICATOR_BREAK,
                        PowerIds.PILLAGER_VOLLEY
                )
        ));

        register(new GemDefinition(
                GemId.SPY_MIMIC,
                List.of(
                        PowerIds.SPY_STILLNESS_CLOAK,
                        PowerIds.SPY_SILENT_STEP,
                        PowerIds.SPY_FALSE_SIGNATURE,
                        PowerIds.SPY_QUICK_HANDS
                ),
                List.of(
                        PowerIds.SPY_MIMIC_FORM,
                        PowerIds.SPY_ECHO,
                        PowerIds.SPY_STEAL,
                        PowerIds.SPY_SMOKE_BOMB,
                        PowerIds.SPY_STOLEN_CAST
                )
        ));

        register(new GemDefinition(
                GemId.BEACON,
                List.of(
                        PowerIds.BEACON_CORE,
                        PowerIds.BEACON_STABILIZE,
                        PowerIds.BEACON_RALLY
                ),
                List.of(
                        PowerIds.BEACON_AURA_SPEED,
                        PowerIds.BEACON_AURA_HASTE,
                        PowerIds.BEACON_AURA_RESISTANCE,
                        PowerIds.BEACON_AURA_JUMP,
                        PowerIds.BEACON_AURA_STRENGTH,
                        PowerIds.BEACON_AURA_REGEN
                )
        ));

        register(new GemDefinition(
                GemId.AIR,
                List.of(
                        PowerIds.AIR_WINDBURST_MACE,
                        PowerIds.AIR_AERIAL_GUARD,
                        PowerIds.AIR_SKYBORN
                ),
                List.of(
                        PowerIds.AIR_WIND_JUMP,
                        PowerIds.AIR_GALE_SLAM,
                        PowerIds.AIR_UPDRAFT_ZONE,
                        PowerIds.AIR_DASH
                )
        ));
    }

    private GemRegistry() {
    }

    private static void register(GemDefinition definition) {
        DEFINITIONS.put(definition.id(), definition);
    }

    public static GemDefinition definition(GemId id) {
        GemDefinition def = DEFINITIONS.get(id);
        if (def == null) {
            throw new IllegalArgumentException("Unknown gem id: " + id);
        }
        return def;
    }
}
