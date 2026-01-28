package com.feel.gems.power.ability.hunter;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.augment.AugmentRuntime;
import com.feel.gems.core.GemId;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.gem.voidgem.VoidImmunity;
import com.feel.gems.power.gem.hunter.HunterPreyMarkRuntime;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import com.feel.gems.trust.GemTrust;
import net.minecraft.entity.Entity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;

public final class HunterPackTacticsAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.HUNTER_PACK_TACTICS;
    }

    @Override
    public String name() {
        return "Pack Tactics";
    }

    @Override
    public String description() {
        return "Nearby trusted allies deal 20% more damage to your marked target for 10s.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().hunter().packTacticsCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        ServerWorld world = player.getEntityWorld();
        int radius = GemsBalance.v().hunter().packTacticsRadiusBlocks();
        int durationTicks = AugmentRuntime.applyDurationMultiplier(player, GemId.HUNTER, GemsBalance.v().hunter().packTacticsDurationTicks());

        // Need a marked target
        var markedTarget = HunterPreyMarkRuntime.getMarkedTarget(player);
        if (markedTarget == null) {
            AbilityFeedback.sound(player, SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), 1.0F, 0.5F);
            return false;
        }

        // Find trusted allies in range
        Box box = player.getBoundingBox().expand(radius);
        int alliesBuffed = 0;

        for (Entity e : world.getOtherEntities(player, box, ent -> ent instanceof ServerPlayerEntity)) {
            ServerPlayerEntity ally = (ServerPlayerEntity) e;
            if (!GemTrust.isTrusted(player, ally)) continue;
            if (VoidImmunity.shouldBlockEffect(player, ally)) continue;

            // Grant pack tactics buff to ally against the marked target
            HunterPackTacticsRuntime.grantBuff(ally, markedTarget.getUuid(), durationTicks);
            AbilityFeedback.burstAt(world, ally.getEntityPos().add(0, 1, 0), ParticleTypes.ENCHANT, 15, 0.5D);
            alliesBuffed++;
        }

        // Also grant to self
        HunterPackTacticsRuntime.grantBuff(player, markedTarget.getUuid(), durationTicks);

        AbilityFeedback.burstAt(world, player.getEntityPos().add(0, 1.5, 0), ParticleTypes.ENCHANT, 25, 0.8D);
        AbilityFeedback.sound(player, SoundEvents.ENTITY_RAVAGER_ROAR, 1.0F, 1.0F);
        return true;
    }
}
