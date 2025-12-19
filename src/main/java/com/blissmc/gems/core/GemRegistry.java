package com.blissmc.gems.core;

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
                        "Soul Capture: store the most recently killed mob for later release as a summon/resource.",
                        "Soul Healing: heal the holder on successful soul capture or release (small regen pulse)."
                ),
                List.of(
                        "Dimensional Drift: spawn an invisible fast mount and render the rider invisible briefly.",
                        "Dimensional Void: suppress enemy gem abilities in a radius for a short duration.",
                        "Astral Daggers: fire rapid, accurate daggers that deal ranged damage.",
                        "Unbounded: short-term spectator-like flight/phase through blocks.",
                        "Astral Projection: create a controllable projection while the body is rooted.",
                        "Spook: briefly disorients/knockback nearby enemies.",
                        "Tag: mark a target so they remain tracked/visible through walls temporarily."
                )
        ));

        register(new GemDefinition(
                GemId.FIRE,
                List.of(
                        "Fire Resistance: permanent immunity to fire/lava damage.",
                        "Auto-smelt: ores/blocks broken drop smelted results.",
                        "Auto-enchant Fire Aspect: applies Fire Aspect to held melee weapons."
                ),
                List.of(
                        "Cosy Campfire: place a campfire aura granting allies Regeneration IV in range.",
                        "Crisp: evaporate water in an area and swap nearby blocks to nether variants temporarily.",
                        "Fireball: charge-and-release explosive fireball; charge decays unless standing on obsidian.",
                        "Meteor Shower: rain multiple exploding meteors on a target zone."
                )
        ));

        register(new GemDefinition(
                GemId.FLUX,
                List.of(
                        "Charge Storage: consume valuables (diamond/gold/copper blocks, enchanted diamond gear/tools) to charge the beam up to 100%.",
                        "Ally Inversion: offensive beam effects on trusted players repair armor durability instead of dealing damage.",
                        "Overcharge Ramp: once at 100% charge, after 5s begin charging toward 200% while dealing self-damage each second."
                ),
                List.of(
                        "Flux Beam: long-range beam whose damage/durability shred scales with stored charge (up to 200%).",
                        "Static Burst: burst built from recent damage taken; legacy/optional ability."
                )
        ));

        register(new GemDefinition(
                GemId.LIFE,
                List.of(
                        "Auto-enchant Unbreaking: applies Unbreaking to held gear.",
                        "Double Saturation: food restores twice the normal saturation."
                ),
                List.of(
                        "Vitality Vortex: pulse that buffs/heals allies and debuffs enemies based on surroundings.",
                        "Health Drain: siphon health from a target to heal the user.",
                        "Life Circle: aura that lowers enemy max health while boosting trusted players' max health.",
                        "Heart Lock: temporarily locks an enemy's max health to their current health."
                )
        ));

        register(new GemDefinition(
                GemId.PUFF,
                List.of(
                        "Fall Damage Immunity: negate fall damage.",
                        "Auto-enchant Power: auto-applies Power to bows.",
                        "Auto-enchant Punch: auto-applies Punch to bows.",
                        "Sculk Silence: immune to triggering sculk shriekers.",
                        "Crop-Trample Immunity: cannot trample farmland."
                ),
                List.of(
                        "Double Jump: midair jump reset with short cooldown.",
                        "Dash: rapid dash that damages/knocks back targets passed through.",
                        "Breezy Bash: launch a target upward then spike them downward.",
                        "Group Breezy Bash: radial knock-up/knockback on all untrusted nearby."
                )
        ));

        register(new GemDefinition(
                GemId.SPEED,
                List.of(
                        "Speed I: permanent movement speed bonus (tuneable; previously Speed II)."
                ),
                List.of(
                        "Blur: chain lightning strikes that damage and knock back targets along a path.",
                        "Speed Storm: field that freezes enemies while granting speed/haste to allies.",
                        "Terminal Velocity: short burst of Speed III + Haste II."
                )
        ));

        register(new GemDefinition(
                GemId.STRENGTH,
                List.of(
                        "Strength I (intended II): flat damage buff.",
                        "Auto-enchant Sharpness: Sharpness II at tier 1, V at tier 2."
                ),
                List.of(
                        "Nullify: strip active potion/status effects from enemies.",
                        "Frailer: apply Weakness to enemies.",
                        "Bounty Hunting: track the owner of an input item for a limited time; item is consumed.",
                        "Chad Strength: every fourth hit deals bonus (~3.5 hearts) damage."
                )
        ));

        register(new GemDefinition(
                GemId.WEALTH,
                List.of(
                        "Auto-enchant Mending: applies Mending to tools/armor.",
                        "Auto-enchant Fortune: applies Fortune to tools.",
                        "Auto-enchant Looting: applies Looting to weapons.",
                        "Luck: permanent Luck effect.",
                        "Hero of the Village: permanent hero status.",
                        "Durability chip: extra armor damage dealt to enemies per strike.",
                        "Armor mending on hit: slowly repairs the holder's armor when hitting enemies.",
                        "Double Debris: furnace outputs double netherite scrap."
                ),
                List.of(
                        "Pockets: opens 9-slot extra inventory UI.",
                        "Unfortunate: chance to cancel enemy actions (attacks, block place, eating, etc.).",
                        "Item Lock: temporarily disables a target item for an enemy.",
                        "Amplification: boosts all enchants on tools/armor for ~45 seconds (3-minute cooldown).",
                        "Rich Rush: boosts mob drops and ore yields for ~3 minutes (9-minute cooldown)."
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
