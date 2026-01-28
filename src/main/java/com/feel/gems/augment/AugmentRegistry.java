package com.feel.gems.augment;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class AugmentRegistry {
    private static final Map<String, AugmentDefinition> DEFINITIONS = new LinkedHashMap<>();

    static {
        registerDefaults();
    }

    private AugmentRegistry() {
    }

    public static AugmentDefinition get(String id) {
        return DEFINITIONS.get(id);
    }

    public static Collection<AugmentDefinition> all() {
        return Collections.unmodifiableCollection(DEFINITIONS.values());
    }

    private static void register(AugmentDefinition def) {
        DEFINITIONS.put(def.id(), def);
    }

    private static void registerDefaults() {
        // Gem augments
        register(new AugmentDefinition(
                "focus",
                AugmentTarget.GEM,
                "gems.augment.focus",
                "gems.augment.focus.desc",
                Set.of("focus"),
                List.of(new AugmentModifier(AugmentModifierType.COOLDOWN_MULTIPLIER, 0.08f))
        ));

        register(new AugmentDefinition(
                "resonance",
                AugmentTarget.GEM,
                "gems.augment.resonance",
                "gems.augment.resonance.desc",
                Set.of("resonance"),
                List.of(new AugmentModifier(AugmentModifierType.PASSIVE_AMP_BONUS, 1.0f))
        ));

        register(new AugmentDefinition(
                "persistence",
                AugmentTarget.GEM,
                "gems.augment.persistence",
                "gems.augment.persistence.desc",
                Set.of("persistence"),
                List.of(new AugmentModifier(AugmentModifierType.DURATION_MULTIPLIER, 0.05f))
        ));

        // Legendary inscriptions
        register(new AugmentDefinition(
                "edge",
                AugmentTarget.LEGENDARY,
                "gems.inscription.edge",
                "gems.inscription.edge.desc",
                Set.of("edge"),
                List.of(new AugmentModifier(AugmentModifierType.LEGENDARY_ATTACK_DAMAGE, 1.5f))
        ));

        register(new AugmentDefinition(
                "swift",
                AugmentTarget.LEGENDARY,
                "gems.inscription.swift",
                "gems.inscription.swift.desc",
                Set.of("swift"),
                List.of(
                        new AugmentModifier(AugmentModifierType.LEGENDARY_ATTACK_SPEED, 0.08f),
                        new AugmentModifier(AugmentModifierType.LEGENDARY_MOVE_SPEED, 0.02f)
                )
        ));

        register(new AugmentDefinition(
                "ward",
                AugmentTarget.LEGENDARY,
                "gems.inscription.ward",
                "gems.inscription.ward.desc",
                Set.of("ward"),
                List.of(new AugmentModifier(AugmentModifierType.LEGENDARY_ARMOR, 2.0f))
        ));
    }
}
