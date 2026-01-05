package com.feel.gems.power.ability.trickster;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

/**
 * Mind Games - reverse an enemy's movement controls for 5s.
 * (left becomes right, forward becomes back)
 */
public final class TricksterMindGamesAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.TRICKSTER_MIND_GAMES;
    }

    @Override
    public String name() {
        return "Mind Games";
    }

    @Override
    public String description() {
        return "Reverse an enemy's movement controls for 5s.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().trickster().mindGamesCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        ServerWorld world = player.getEntityWorld();
        int range = GemsBalance.v().trickster().mindGamesRangeBlocks();
        int durationTicks = GemsBalance.v().trickster().mindGamesDurationTicks();

        // Raycast to find target
        HitResult hit = player.raycast(range, 0.0F, false);
        if (!(hit instanceof EntityHitResult entityHit) || !(entityHit.getEntity() instanceof ServerPlayerEntity target)) {
            AbilityFeedback.sound(player, SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), 1.0F, 0.5F);
            return false;
        }

        // Apply mind games effect
        TricksterMindGamesRuntime.applyMindGames(target, durationTicks);

        AbilityFeedback.burstAt(world, target.getEntityPos().add(0, 2, 0), ParticleTypes.WITCH, 30, 0.8D);
        AbilityFeedback.sound(player, SoundEvents.ENTITY_ILLUSIONER_PREPARE_BLINDNESS, 1.0F, 1.2F);
        AbilityFeedback.sound(target, SoundEvents.ENTITY_ILLUSIONER_PREPARE_BLINDNESS, 1.0F, 1.2F);
        return true;
    }
}
