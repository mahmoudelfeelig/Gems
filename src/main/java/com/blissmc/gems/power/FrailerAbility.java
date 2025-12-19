package com.blissmc.gems.power;

import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import com.blissmc.gems.trust.GemTrust;

public final class FrailerAbility implements GemAbility {
    private static final int WEAKNESS_DURATION_TICKS = 8 * 20;

    @Override
    public Identifier id() {
        return PowerIds.FRAILER;
    }

    @Override
    public String name() {
        return "Frailer";
    }

    @Override
    public String description() {
        return "Applies Weakness to a targeted enemy.";
    }

    @Override
    public int cooldownTicks() {
        return 20 * 20;
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        var target = Targeting.raycastLiving(player, 20.0D);
        if (target == null) {
            player.sendMessage(Text.literal("No target."), true);
            return true;
        }
        if (target instanceof ServerPlayerEntity other && GemTrust.isTrusted(player, other)) {
            player.sendMessage(Text.literal("Target is trusted."), true);
            return true;
        }

        target.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, WEAKNESS_DURATION_TICKS, 0));
        player.sendMessage(Text.literal("Frailer: weakened " + target.getName().getString()), true);
        return true;
    }
}
