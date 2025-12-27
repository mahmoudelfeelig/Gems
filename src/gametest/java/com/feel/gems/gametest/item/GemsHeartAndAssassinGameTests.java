package com.feel.gems.gametest.item;

import com.feel.gems.assassin.AssassinState;
import com.feel.gems.config.GemsBalance;
import com.feel.gems.core.GemDefinition;
import com.feel.gems.core.GemEnergyState;
import com.feel.gems.core.GemId;
import com.feel.gems.core.GemRegistry;
import com.feel.gems.gametest.util.GemsGameTestUtil;
import com.feel.gems.item.GemItemGlint;
import com.feel.gems.item.GemKeepOnDeath;
import com.feel.gems.item.ModItems;
import com.feel.gems.net.CooldownSnapshotPayload;
import com.feel.gems.power.ability.air.AirDashAbility;
import com.feel.gems.power.ability.air.AirUpdraftZoneAbility;
import com.feel.gems.power.ability.air.AirWindJumpAbility;
import com.feel.gems.power.ability.beacon.BeaconAuraAbility;
import com.feel.gems.power.ability.flux.FluxBeamAbility;
import com.feel.gems.power.ability.pillager.PillagerFangsAbility;
import com.feel.gems.power.ability.pillager.PillagerVolleyAbility;
import com.feel.gems.power.ability.spy.SpyMimicFormAbility;
import com.feel.gems.power.ability.spy.SpyStealAbility;
import com.feel.gems.power.ability.summoner.SummonRecallAbility;
import com.feel.gems.power.ability.summoner.SummonSlotAbility;
import com.feel.gems.power.ability.terror.PanicRingAbility;
import com.feel.gems.power.gem.beacon.BeaconAuraRuntime;
import com.feel.gems.power.gem.flux.FluxCharge;
import com.feel.gems.power.gem.pillager.PillagerDiscipline;
import com.feel.gems.power.gem.pillager.PillagerVolleyRuntime;
import com.feel.gems.power.gem.spy.SpyMimicSystem;
import com.feel.gems.power.gem.summoner.SummonerSummons;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityDisables;
import com.feel.gems.power.runtime.AbilityRuntime;
import com.feel.gems.power.runtime.GemAbilities;
import com.feel.gems.power.runtime.GemAbilityCooldowns;
import com.feel.gems.power.runtime.GemPowers;
import com.feel.gems.state.GemPlayerState;
import com.feel.gems.trade.GemTrading;
import com.feel.gems.trust.GemTrust;
import com.feel.gems.util.GemsTime;
import io.netty.buffer.Unpooled;
import java.util.EnumSet;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.EvokerFangsEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;




public final class GemsHeartAndAssassinGameTests {
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, tickLimit = 200)
    public void assassinConversionAndHeartsApplied(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = context.createMockCreativeServerPlayerInWorld();
        player.changeGameMode(GameMode.SURVIVAL);

        Vec3d startPos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        player.teleport(world, startPos.x, startPos.y, startPos.z, 0.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        AssassinState.initIfNeeded(player);

        // Force conversion using the same persistent path the death mixin uses.
        AssassinState.becomeAssassin(player);
        AssassinState.addAssassinHearts(player, -2); // 8 hearts
        GemPlayerState.applyMaxHearts(player);

        context.runAtTick(2L, () -> {
            if (!AssassinState.isAssassin(player)) {
                context.throwGameTestException("Expected player to be assassin after becomeAssassin");
            }
            if (AssassinState.getAssassinHearts(player) != 8) {
                context.throwGameTestException("Expected assassin hearts to be 8, got " + AssassinState.getAssassinHearts(player));
            }
            double maxHealth = player.getAttributeValue(EntityAttributes.GENERIC_MAX_HEALTH);
            if (Math.abs(maxHealth - 16.0D) > 0.01D) {
                context.throwGameTestException("Expected max health 16.0 for 8 hearts, got " + maxHealth);
            }
            context.complete();
        });
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, tickLimit = 160)
    public void heartDropsAboveFloorAndStopsAtFive(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity killer = context.createMockCreativeServerPlayerInWorld();
        ServerPlayerEntity victim = context.createMockCreativeServerPlayerInWorld();
        killer.changeGameMode(GameMode.SURVIVAL);
        victim.changeGameMode(GameMode.SURVIVAL);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        killer.teleport(world, pos.x, pos.y, pos.z, 0.0F, 0.0F);
        victim.teleport(world, pos.x + 1.0D, pos.y, pos.z, 180.0F, 0.0F);

        GemPlayerState.initIfNeeded(killer);
        GemPlayerState.initIfNeeded(victim);
        GemPlayerState.setActiveGem(killer, GemId.FIRE);
        GemPlayerState.setActiveGem(victim, GemId.LIFE);
        GemsGameTestUtil.resetAssassinState(victim);
        GemPlayerState.setMaxHearts(victim, GemPlayerState.MIN_MAX_HEARTS + 1);
        GemPlayerState.applyMaxHearts(victim);

        context.runAtTick(2L, () -> {
            victim.setHealth(2.0F);
            victim.damage(victim.getDamageSources().playerAttack(killer), 10.0F);
            if (!victim.isDead()) {
                // Guarantee death triggers even if mock combat fails to deliver damage (e.g. PVP quirks).
                victim.kill();
            }
        });

        context.runAtTick(40L, () -> {
            if (!victim.isDead()) {
                context.throwGameTestException("Victim did not die during heart drop scenario");
                return;
            }
            int heartsDropped = world.getEntitiesByClass(ItemEntity.class, new Box(pos, pos.add(1.0D, 1.0D, 1.0D)).expand(3.0D), e -> e.getStack().isOf(ModItems.HEART)).size();
            int invHearts = GemsGameTestUtil.countItem(killer, ModItems.HEART);
            var victimDataAfter = ((com.feel.gems.state.GemsPersistentDataHolder) victim).gems$getPersistentData();
            if (heartsDropped + invHearts != 1) {
                context.throwGameTestException("Expected exactly one heart gained (dropped or picked up), found dropped=" + heartsDropped + " inv=" + invHearts + " assassin=" + victimDataAfter.getBoolean("assassinIsAssassin") + " storedMax=" + victimDataAfter.getInt("maxHearts") + " assassinHearts=" + victimDataAfter.getInt("assassinHearts"));
                return;
            }
            int storedHearts = victimDataAfter.getInt("maxHearts");
            if (storedHearts != GemPlayerState.MIN_MAX_HEARTS) {
                context.throwGameTestException("Victim hearts should clamp to floor after drop: " + storedHearts);
                return;
            }
            boolean becameAssassin = victimDataAfter.getBoolean("assassinIsAssassin");
            if (becameAssassin) {
                context.throwGameTestException("Victim above floor should not convert to assassin");
                return;
            }
            context.complete();
        });
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, tickLimit = 160)
    public void heartAtFloorDoesNotDropAndTurnsAssassin(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity killer = context.createMockCreativeServerPlayerInWorld();
        ServerPlayerEntity victim = context.createMockCreativeServerPlayerInWorld();
        killer.changeGameMode(GameMode.SURVIVAL);
        victim.changeGameMode(GameMode.SURVIVAL);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        killer.teleport(world, pos.x, pos.y, pos.z, 0.0F, 0.0F);
        victim.teleport(world, pos.x + 1.0D, pos.y, pos.z, 180.0F, 0.0F);

        GemPlayerState.initIfNeeded(killer);
        GemPlayerState.initIfNeeded(victim);
        GemPlayerState.setActiveGem(killer, GemId.FIRE);
        GemPlayerState.setActiveGem(victim, GemId.LIFE);
        GemsGameTestUtil.resetAssassinState(victim);
        GemPlayerState.setMaxHearts(victim, GemPlayerState.MIN_MAX_HEARTS);
        GemPlayerState.applyMaxHearts(victim);

        context.runAtTick(2L, () -> {
            victim.setHealth(2.0F);
            victim.damage(victim.getDamageSources().playerAttack(killer), 10.0F);
            if (!victim.isDead()) {
                victim.kill();
            }
        });

        context.runAtTick(40L, () -> {
            if (!victim.isDead()) {
                context.throwGameTestException("Victim did not die during floor clamp scenario");
                return;
            }
            int heartsDropped = world.getEntitiesByClass(ItemEntity.class, new Box(pos, pos.add(1.0D, 1.0D, 1.0D)).expand(3.0D), e -> e.getStack().isOf(ModItems.HEART)).size();
            var victimDataAfter = ((com.feel.gems.state.GemsPersistentDataHolder) victim).gems$getPersistentData();
            if (heartsDropped != 0) {
                context.throwGameTestException("No hearts should drop at the floor (5 hearts)");
                return;
            }
            int invHearts = GemsGameTestUtil.countItem(killer, ModItems.HEART);
            if (invHearts != 0) {
                context.throwGameTestException("Killer should not receive a heart when victim at floor");
                return;
            }
            boolean becameAssassin = victimDataAfter.getBoolean("assassinIsAssassin");
            if (!becameAssassin) {
                context.throwGameTestException("Victim at floor should be converted to assassin on death (storedMax=" + victimDataAfter.getInt("maxHearts") + " assassinHearts=" + victimDataAfter.getInt("assassinHearts") + ")");
                return;
            }
            context.complete();
        });
    }

}

