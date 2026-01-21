package com.feel.gems.power.ability.pillager;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.augment.AugmentRuntime;
import com.feel.gems.core.GemId;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import com.feel.gems.state.GemsPersistentDataHolder;
import com.feel.gems.util.GemsTime;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;



public final class PillagerVindicatorBreakAbility implements GemAbility {
    static final String KEY_UNTIL = "pillagerVindicatorBreakUntil";

    @Override
    public Identifier id() {
        return PowerIds.PILLAGER_VINDICATOR_BREAK;
    }

    @Override
    public String name() {
        return "Vindicator Break";
    }

    @Override
    public String description() {
        return "Temporarily empowers melee hits and disables shields on contact.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().pillager().vindicatorBreakCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        int duration = AugmentRuntime.applyDurationMultiplier(player, GemId.PILLAGER, GemsBalance.v().pillager().vindicatorBreakDurationTicks());
        if (duration <= 0) {
            return false;
        }
        NbtCompound nbt = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        nbt.putLong(KEY_UNTIL, GemsTime.now(player) + duration);

        int amp = GemsBalance.v().pillager().vindicatorBreakStrengthAmplifier();
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, duration, amp, true, false, false));
        AbilityFeedback.sound(player, SoundEvents.ENTITY_VINDICATOR_CELEBRATE, 0.6F, 1.0F);
        return true;
    }

    public static boolean isActive(ServerPlayerEntity player, long now) {
        NbtCompound nbt = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        long until = nbt.getLong(KEY_UNTIL, 0L);
        return until > 0 && now < until;
    }

    public static void clear(ServerPlayerEntity player) {
        ((GemsPersistentDataHolder) player).gems$getPersistentData().remove(KEY_UNTIL);
    }
}

