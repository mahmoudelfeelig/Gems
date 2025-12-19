package com.blissmc.gems.power;

import com.blissmc.gems.trust.GemTrust;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class TagAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.TAG;
    }

    @Override
    public String name() {
        return "Tag";
    }

    @Override
    public String description() {
        return "Marks a target so they glow through walls for a short time.";
    }

    @Override
    public int cooldownTicks() {
        return 20 * 20;
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        LivingEntity target = Targeting.raycastLiving(player, 30.0D);
        if (target == null) {
            player.sendMessage(Text.literal("No target."), true);
            return true;
        }
        if (target instanceof ServerPlayerEntity other && GemTrust.isTrusted(player, other)) {
            player.sendMessage(Text.literal("Target is trusted."), true);
            return true;
        }
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 12 * 20, 0, true, false, false));
        player.sendMessage(Text.literal("Tagged " + target.getName().getString()), true);
        return true;
    }
}

