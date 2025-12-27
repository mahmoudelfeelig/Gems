package com.feel.gems.power.passive;

import com.feel.gems.power.api.GemPassive;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;




public final class AttributeModifierPassive implements GemPassive {
    private final Identifier id;
    private final String name;
    private final String description;
    private final RegistryEntry<EntityAttribute> attribute;
    private final EntityAttributeModifier.Operation operation;
    private final double value;

    public AttributeModifierPassive(
            Identifier id,
            String name,
            String description,
            RegistryEntry<EntityAttribute> attribute,
            EntityAttributeModifier.Operation operation,
            double value
    ) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.attribute = attribute;
        this.operation = operation;
        this.value = value;
    }

    @Override
    public Identifier id() {
        return id;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String description() {
        return description;
    }

    @Override
    public void apply(ServerPlayerEntity player) {
        EntityAttributeInstance instance = player.getAttributeInstance(attribute);
        if (instance == null) {
            return;
        }
        instance.removeModifier(id);
        instance.addPersistentModifier(new EntityAttributeModifier(id, value, operation));
    }

    @Override
    public void remove(ServerPlayerEntity player) {
        EntityAttributeInstance instance = player.getAttributeInstance(attribute);
        if (instance == null) {
            return;
        }
        instance.removeModifier(id);
    }
}
