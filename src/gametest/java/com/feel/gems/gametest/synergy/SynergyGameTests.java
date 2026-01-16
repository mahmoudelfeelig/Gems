package com.feel.gems.gametest.synergy;

import com.feel.gems.core.GemId;
import com.feel.gems.core.GemRegistry;
import com.feel.gems.gametest.util.GemsGameTestUtil;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.synergy.SynergyDefinition;
import com.feel.gems.synergy.SynergyRegistry;
import com.feel.gems.synergy.SynergyRuntime;
import com.feel.gems.trust.GemTrust;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.TestContext;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.network.packet.s2c.play.PositionFlag;

public final class SynergyGameTests {
    private static void teleport(ServerPlayerEntity player, ServerWorld world, double x, double y, double z) {
        player.teleport(world, x, y, z, EnumSet.noneOf(PositionFlag.class), 0.0F, 0.0F, false);
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void synergyTriggersWithMutualTrust(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity p1 = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        ServerPlayerEntity p2 = GemsGameTestUtil.createMockCreativeServerPlayer(context);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(p1, world, pos.x, pos.y, pos.z);
        teleport(p2, world, pos.x + 1.0D, pos.y, pos.z + 1.0D);

        context.runAtTick(20L, () -> {
            GemTrust.trust(p1, p2.getUuid());
            GemTrust.trust(p2, p1.getUuid());
            SynergyRuntime.onAbilityCast(p1, GemId.SUMMONER, PowerIds.SUMMON_SLOT_1);
            SynergyRuntime.onAbilityCast(p2, GemId.HUNTER, PowerIds.HUNTER_POUNCE);
        });

        context.runAtTick(40L, () -> {
            if (!p1.hasStatusEffect(StatusEffects.STRENGTH) || !p2.hasStatusEffect(StatusEffects.STRENGTH)) {
                context.throwGameTestException("Expected Pack Alpha synergy to apply Strength to both participants");
                return;
            }
            GemsGameTestUtil.complete(context);
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void synergyBlockedWithoutMutualTrust(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity p1 = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        ServerPlayerEntity p2 = GemsGameTestUtil.createMockCreativeServerPlayer(context);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(p1, world, pos.x, pos.y, pos.z);
        teleport(p2, world, pos.x + 1.0D, pos.y, pos.z + 1.0D);

        context.runAtTick(20L, () -> {
            SynergyRuntime.onAbilityCast(p1, GemId.SUMMONER, PowerIds.SUMMON_SLOT_1);
            SynergyRuntime.onAbilityCast(p2, GemId.HUNTER, PowerIds.HUNTER_POUNCE);
        });

        context.runAtTick(40L, () -> {
            if (p1.hasStatusEffect(StatusEffects.STRENGTH) || p2.hasStatusEffect(StatusEffects.STRENGTH)) {
                context.throwGameTestException("Synergy should not trigger without mutual trust");
                return;
            }
            GemsGameTestUtil.complete(context);
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void allSynergyEffectsExecute(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity p1 = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        ServerPlayerEntity p2 = GemsGameTestUtil.createMockCreativeServerPlayer(context);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(p1, world, pos.x, pos.y, pos.z);
        teleport(p2, world, pos.x + 1.0D, pos.y, pos.z + 1.0D);

        context.runAtTick(20L, () -> {
            long now = world.getTime();
            for (SynergyDefinition synergy : SynergyRegistry.getAll()) {
                List<SynergyDefinition.SynergyParticipant> participants = new ArrayList<>();
                int i = 0;
                for (GemId gem : synergy.requiredGems()) {
                    ServerPlayerEntity player = (i++ % 2 == 0) ? p1 : p2;
                    Identifier abilityId = pickAbilityId(synergy, gem);
                    participants.add(new SynergyDefinition.SynergyParticipant(player, gem, abilityId, now));
                }
                synergy.effect().apply(participants);
            }
            GemsGameTestUtil.complete(context);
        });
    }

    private static Identifier pickAbilityId(SynergyDefinition synergy, GemId gem) {
        if (synergy.isAbilitySpecific()) {
            Set<Identifier> required = synergy.requiredAbilities().orElse(Set.of());
            if (!required.isEmpty()) {
                return required.iterator().next();
            }
        }
        List<Identifier> abilities = GemRegistry.definition(gem).abilities();
        if (!abilities.isEmpty()) {
            return abilities.get(0);
        }
        return PowerIds.FIREBALL;
    }
}
