package com.blissmc.gems.power;

import net.minecraft.util.Identifier;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class ModAbilities {
    private static final Map<Identifier, GemAbility> ABILITIES = new HashMap<>();

    static {
        // Astra
        register(new UnimplementedAbility(PowerIds.DIMENSIONAL_DRIFT, "Dimensional Drift", "Invisible mount + invisibility burst.", 20 * 20));
        register(new UnimplementedAbility(PowerIds.DIMENSIONAL_VOID, "Dimensional Void", "Suppress enemy gem abilities in a radius.", 60 * 20));
        register(new UnimplementedAbility(PowerIds.ASTRAL_DAGGERS, "Astral Daggers", "Fire rapid ranged daggers.", 8 * 20));
        register(new UnimplementedAbility(PowerIds.UNBOUNDED, "Unbounded", "Short-term spectator-like flight/phase.", 60 * 20));
        register(new UnimplementedAbility(PowerIds.ASTRAL_PROJECTION, "Astral Projection", "Control a projection while body is rooted.", 60 * 20));
        register(new UnimplementedAbility(PowerIds.SPOOK, "Spook", "Brief disorient/fear effect to nearby enemies.", 30 * 20));
        register(new UnimplementedAbility(PowerIds.TAG, "Tag", "Mark a target for tracking/visibility.", 20 * 20));

        // Fire
        register(new UnimplementedAbility(PowerIds.COSY_CAMPFIRE, "Cosy Campfire", "Ally Regeneration IV aura.", 45 * 20));
        register(new UnimplementedAbility(PowerIds.CRISP, "Crisp", "Evaporate water + netherify area temporarily.", 90 * 20));
        register(new FireballAbility());
        register(new UnimplementedAbility(PowerIds.METEOR_SHOWER, "Meteor Shower", "Call multiple exploding meteors.", 120 * 20));

        // Flux
        register(new UnimplementedAbility(PowerIds.FLUX_BEAM, "Flux Beam", "Scaling beam damage/durability shred based on charge.", 4 * 20));
        register(new UnimplementedAbility(PowerIds.STATIC_BURST, "Static Burst", "Release stored recent damage in a burst.", 30 * 20));

        // Life
        register(new UnimplementedAbility(PowerIds.VITALITY_VORTEX, "Vitality Vortex", "Area pulse with contextual effects.", 30 * 20));
        register(new UnimplementedAbility(PowerIds.HEALTH_DRAIN, "Health Drain", "Siphon health from a target to heal the user.", 12 * 20));
        register(new UnimplementedAbility(PowerIds.LIFE_CIRCLE, "Life Circle", "Aura that shifts max health between allies and enemies.", 60 * 20));
        register(new UnimplementedAbility(PowerIds.HEART_LOCK, "Heart Lock", "Temporarily locks an enemy max health to current.", 45 * 20));

        // Puff
        register(new UnimplementedAbility(PowerIds.DOUBLE_JUMP, "Double Jump", "Midair jump reset.", 2 * 20));
        register(new UnimplementedAbility(PowerIds.DASH, "Dash", "Fast dash; damages/knocks back targets.", 6 * 20));
        register(new UnimplementedAbility(PowerIds.BREEZY_BASH, "Breezy Bash", "Launch target upward then spike down.", 20 * 20));
        register(new UnimplementedAbility(PowerIds.GROUP_BREEZY_BASH, "Group Breezy Bash", "Radial knock-up/knockback of untrusted players.", 45 * 20));

        // Speed
        register(new UnimplementedAbility(PowerIds.BLUR, "Blur", "Successive lightning strikes for damage/knockback.", 20 * 20));
        register(new UnimplementedAbility(PowerIds.SPEED_STORM, "Speed Storm", "Freeze enemies; speed/haste allies.", 60 * 20));
        register(new UnimplementedAbility(PowerIds.TERMINAL_VELOCITY, "Terminal Velocity", "Short burst of Speed III + Haste II.", 30 * 20));

        // Strength
        register(new UnimplementedAbility(PowerIds.NULLIFY, "Nullify", "Removes potion/status effects from enemies.", 20 * 20));
        register(new FrailerAbility());
        register(new UnimplementedAbility(PowerIds.BOUNTY_HUNTING, "Bounty Hunting", "Track an item owner for a duration; consumes item.", 60 * 20));
        register(new UnimplementedAbility(PowerIds.CHAD_STRENGTH, "Chad Strength", "Every 4th hit deals bonus damage.", 0));

        // Wealth
        register(new UnimplementedAbility(PowerIds.POCKETS, "Pockets", "Access 9 extra inventory slots.", 0));
        register(new UnimplementedAbility(PowerIds.UNFORTUNATE, "Unfortunate", "Chance to cancel enemy actions.", 20 * 20));
        register(new UnimplementedAbility(PowerIds.ITEM_LOCK, "Item Lock", "Temporarily disables a target item for an enemy.", 30 * 20));
        register(new UnimplementedAbility(PowerIds.AMPLIFICATION, "Amplification", "Boosts enchants on gear for ~45s.", 180 * 20));
        register(new UnimplementedAbility(PowerIds.RICH_RUSH, "Rich Rush", "Boost mob/ore drops for ~3m.", 540 * 20));
    }

    private ModAbilities() {
    }

    public static GemAbility get(Identifier id) {
        return ABILITIES.get(id);
    }

    public static Map<Identifier, GemAbility> all() {
        return Collections.unmodifiableMap(ABILITIES);
    }

    public static void override(GemAbility ability) {
        ABILITIES.put(ability.id(), ability);
    }

    private static void register(GemAbility ability) {
        ABILITIES.put(ability.id(), ability);
    }
}
