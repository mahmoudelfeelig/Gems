package com.feel.gems.power.ability.duelist;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import com.feel.gems.state.PlayerStateManager;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;

public final class DuelistParryAbility implements GemAbility {
    private static final String PARRY_KEY = "duelist_parry_window";

    @Override
    public Identifier id() {
        return PowerIds.DUELIST_PARRY;
    }

    @Override
    public String name() {
        return "Parry";
    }

    @Override
    public String description() {
        return "Brief window to deflect incoming melee attacks; successful parry stuns the attacker.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().duelist().parryCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        int windowTicks = GemsBalance.v().duelist().parryWindowTicks();
        PlayerStateManager.setTemporary(player, PARRY_KEY, windowTicks);

        AbilityFeedback.burstAt(player.getEntityWorld(), player.getEntityPos().add(0.0D, 1.0D, 0.0D),
                ParticleTypes.ENCHANT, 20, 0.5D);
        AbilityFeedback.sound(player, SoundEvents.ITEM_SHIELD_BLOCK, 1.0F, 1.5F);
        return true;
    }

    public static boolean isParrying(ServerPlayerEntity player) {
        return PlayerStateManager.getTemporary(player, PARRY_KEY) > 0;
    }

    public static void consumeParry(ServerPlayerEntity player) {
        PlayerStateManager.clearTemporary(player, PARRY_KEY);
    }
}
