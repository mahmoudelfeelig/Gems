package com.feel.gems.power.ability.summoner;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.core.GemId;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import com.feel.gems.power.runtime.GemPowers;
import com.feel.gems.power.gem.summoner.SummonerBudget;
import com.feel.gems.power.gem.summoner.SummonerCommanderMark;
import com.feel.gems.power.gem.summoner.SummonerLoadouts;
import com.feel.gems.power.gem.summoner.SummonerSummons;
import com.feel.gems.state.GemPlayerState;
import com.feel.gems.util.MobBlacklist;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;


public final class SummonSlotAbility implements GemAbility {
    private final int slot;

    public SummonSlotAbility(int slot) {
        this.slot = slot;
    }

    @Override
    public Identifier id() {
        return switch (slot) {
            case 1 -> PowerIds.SUMMON_SLOT_1;
            case 2 -> PowerIds.SUMMON_SLOT_2;
            case 3 -> PowerIds.SUMMON_SLOT_3;
            case 4 -> PowerIds.SUMMON_SLOT_4;
            case 5 -> PowerIds.SUMMON_SLOT_5;
            default -> PowerIds.SUMMON_SLOT_1;
        };
    }

    @Override
    public String name() {
        return "Summon " + slot;
    }

    @Override
    public String description() {
        return "Summons the configured minions for slot " + slot + " (budgeted by Summoner points).";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().summoner().summonSlotCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        GemPlayerState.initIfNeeded(player);
        if (GemPlayerState.getActiveGem(player) != GemId.SUMMONER) {
            return false;
        }

        var cfg = GemsBalance.v().summoner();
        SummonerLoadouts.Loadout loadout = SummonerLoadouts.resolve(player, cfg);
        List<SummonerLoadouts.Entry> specs = switch (slot) {
            case 1 -> loadout.slot1();
            case 2 -> loadout.slot2();
            case 3 -> loadout.slot3();
            case 4 -> loadout.slot4();
            case 5 -> loadout.slot5();
            default -> loadout.slot1();
        };
        if (specs.isEmpty()) {
            player.sendMessage(Text.literal("No summons configured for slot " + slot + "."), true);
            return false;
        }

        int totalBudget = cfg.maxPoints();
        int totalCost = SummonerBudget.totalLoadoutCost(cfg.costs(), loadout);
        if (totalCost > totalBudget) {
            player.sendMessage(Text.literal("Summoner loadout exceeds budget (" + totalCost + " > " + totalBudget + ")."), true);
            return false;
        }

        int active = SummonerSummons.pruneAndCount(player);
        int remainingSlots = Math.max(0, cfg.maxActiveSummons() - active);
        if (remainingSlots <= 0) {
            player.sendMessage(Text.literal("Too many active summons."), true);
            return false;
        }

        long until = SummonerSummons.computeUntilTick(player, cfg.summonLifetimeTicks());
        boolean bonusHealth = GemPowers.isPassiveActive(player, PowerIds.SUMMONER_FAMILIARS_BLESSING);
        float bonusHealthAmount = cfg.summonBonusHealth();

        UUID markTarget = SummonerCommanderMark.activeTargetUuid(player);

        int spawned = 0;
        for (var spec : specs) {
            Identifier typeId = Identifier.tryParse(spec.entityId());
            if (typeId == null) {
                continue;
            }
            EntityType<?> type = Registries.ENTITY_TYPE.get(typeId);
            if (MobBlacklist.isBlacklisted(type)) {
                continue;
            }
            for (int i = 0; i < spec.count() && spawned < remainingSlots; i++) {
                Entity e = type.create(player.getServerWorld());
                if (!(e instanceof MobEntity mob)) {
                    continue;
                }
                Vec3d spawnPos = spawnPos(player, spawned);
                mob.refreshPositionAndAngles(spawnPos.x, spawnPos.y, spawnPos.z, player.getYaw(), player.getPitch());
                mob.initialize(player.getServerWorld(), player.getServerWorld().getLocalDifficulty(BlockPos.ofFloored(spawnPos)), SpawnReason.MOB_SUMMONED, null);
                SummonerSummons.mark(mob, player.getUuid(), until);
                mob.disableExperienceDropping();
                if (bonusHealth) {
                    SummonerSummons.applyBonusHealth(mob, bonusHealthAmount);
                }
                mob.setHealth(mob.getMaxHealth());
                player.getServerWorld().spawnEntity(mob);
                SummonerSummons.trackSpawn(player, mob);

                if (markTarget != null) {
                    Entity target = SummonerSummons.findEntity(player.getServer(), markTarget);
                    if (target instanceof LivingEntity living && living.getWorld() == mob.getWorld()) {
                        mob.setTarget(living);
                    }
                }

                spawned++;
            }
        }

        if (spawned <= 0) {
            player.sendMessage(Text.literal("No valid summons spawned (check costs + entity ids)."), true);
            return false;
        }

        AbilityFeedback.burst(player, net.minecraft.particle.ParticleTypes.PORTAL, 18, 0.35D);
        AbilityFeedback.sound(player, net.minecraft.sound.SoundEvents.ENTITY_EVOKER_PREPARE_SUMMON, 0.8F, 1.2F);
        player.sendMessage(Text.literal("Summoned " + spawned + " minion(s)."), true);
        return true;
    }

    private static Vec3d spawnPos(ServerPlayerEntity player, int index) {
        Vec3d forward = player.getRotationVec(1.0F).normalize();
        Vec3d base = player.getPos().add(forward.multiply(2.0D));
        double radius = 0.4D + (index % 3) * 0.25D;
        double angle = (Math.PI * 2.0D) * ((index % 8) / 8.0D);
        return base.add(Math.cos(angle) * radius, 0.1D, Math.sin(angle) * radius);
    }
}
