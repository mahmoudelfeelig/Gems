package com.feel.gems.power.ability.bonus;

import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;

public final class BonusFrostbiteAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.BONUS_FROSTBITE;
    }

    @Override
    public String name() {
        return "Frostbite";
    }

    @Override
    public String description() {
        return "Freeze nearby enemies with slowness and mining fatigue.";
    }

    @Override
    public int cooldownTicks() {
        return 600; // 30 seconds
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        Box area = player.getBoundingBox().expand(8);
        player.getEntityWorld().getOtherEntities(player, area, e -> e instanceof net.minecraft.entity.LivingEntity)
                .forEach(e -> {
                    if (e instanceof net.minecraft.entity.LivingEntity living) {
                        living.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 200, 2));
                        living.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, 200, 1));
                    }
                });
        return true;
    }
}
