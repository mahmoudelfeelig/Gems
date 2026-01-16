package com.feel.gems.augment;

import java.util.List;
import java.util.Set;

public record AugmentDefinition(
        String id,
        AugmentTarget target,
        String nameKey,
        String descriptionKey,
        Set<String> conflicts,
        List<AugmentModifier> modifiers
) {
}
