package com.blissmc.gems.core;

import com.blissmc.gems.power.PowerIds;
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
                        PowerIds.SPEED_I
                ),
                List.of(
                        PowerIds.ARC_SHOT,
                        PowerIds.SPEED_STORM,
                        PowerIds.TERMINAL_VELOCITY
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
