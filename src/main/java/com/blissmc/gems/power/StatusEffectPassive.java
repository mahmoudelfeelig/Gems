package com.blissmc.gems.power;

import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public final class StatusEffectPassive implements GemPassive {
    private final Identifier id;
    private final String name;
    private final String description;
    private final RegistryEntry<StatusEffect> effect;
    private final int amplifier;

    public StatusEffectPassive(Identifier id, String name, String description, RegistryEntry<StatusEffect> effect, int amplifier) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.effect = effect;
        this.amplifier = amplifier;
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
        StatusEffectInstance current = player.getStatusEffect(effect);
        if (current != null && current.getAmplifier() == amplifier && current.isInfinite()) {
            return;
        }
        player.addStatusEffect(new StatusEffectInstance(effect, StatusEffectInstance.INFINITE, amplifier, true, false, false));
    }

    @Override
    public void remove(ServerPlayerEntity player) {
        player.removeStatusEffect(effect);
    }
}

