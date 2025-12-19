package com.blissmc.gems.power;

import com.blissmc.gems.trust.GemTrust;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

public final class VitalityVortexAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.VITALITY_VORTEX;
    }

    @Override
    public String name() {
        return "Vitality Vortex";
    }

    @Override
    public String description() {
        return "Area pulse that buffs trusted players and weakens enemies (contextual).";
    }

    @Override
    public int cooldownTicks() {
        return 30 * 20;
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        ServerWorld world = player.getServerWorld();
        boolean nether = world.getRegistryKey() == World.NETHER;
        int duration = 8 * 20;

        for (ServerPlayerEntity other : world.getPlayers(p -> p.squaredDistanceTo(player) <= 8.0D * 8.0D)) {
            if (GemTrust.isTrusted(player, other)) {
                other.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, duration, 1, true, false, false));
                other.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, duration, 0, true, false, false));
                if (nether) {
                    other.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, duration, 0, true, false, false));
                }
            } else {
                other.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, duration, 0, true, false, false));
                other.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, duration, 0, true, false, false));
            }
        }

        player.sendMessage(Text.literal("Vitality Vortex pulsed."), true);
        return true;
    }
}
