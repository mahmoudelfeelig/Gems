package com.feel.gems.gametest.core;

import com.feel.gems.core.GemDefinition;
import com.feel.gems.core.GemEnergyState;
import com.feel.gems.core.GemId;
import com.feel.gems.core.GemRegistry;
import com.feel.gems.gametest.util.GemsGameTestUtil;
import com.feel.gems.item.GemKeepOnDeath;
import com.feel.gems.item.GemOwnership;
import com.feel.gems.item.ModItems;
import com.feel.gems.net.CooldownSnapshotPayload;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityDisables;
import com.feel.gems.power.runtime.GemAbilities;
import com.feel.gems.power.runtime.GemAbilityCooldowns;
import com.feel.gems.power.runtime.GemPowers;
import com.feel.gems.state.GemPlayerState;
import com.feel.gems.util.GemsTime;
import io.netty.buffer.Unpooled;
import java.util.EnumSet;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import com.feel.gems.event.GemsPlayerDeath;




public final class GemsCoreGameTests {
    private static void teleport(ServerPlayerEntity player, ServerWorld world, double x, double y, double z, float yaw, float pitch) {
        player.teleport(world, x, y, z, EnumSet.noneOf(PositionFlag.class), yaw, pitch, false);
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void deathKeepsActiveGemOnly(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        player.changeGameMode(GameMode.SURVIVAL);

        Vec3d startPos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, startPos.x, startPos.y, startPos.z, 0.0F, 0.0F);

        // GameTest creates "embedded" players and runs normal join callbacks; to avoid racey interactions with
        // JOIN/COPY_FROM logic (e.g. ensureActiveGemItem), perform test setup after the player is fully connected.
        context.runAtTick(20L, () -> {
            player.getInventory().clear();
            GemPlayerState.initIfNeeded(player);
            GemPlayerState.setActiveGem(player, GemId.ASTRA);
            player.giveItemStack(new ItemStack(ModItems.ASTRA_GEM));
            player.giveItemStack(new ItemStack(ModItems.FIRE_GEM));
        });

        // GameTest servers often run with gamerules that can affect drops (e.g. keepInventory).
        // Validate the core stash/restore behavior directly:
        // - stash removes exactly one active gem item (kept across death)
        // - stash does not remove non-active gems
        // - restore returns the active gem item
        context.runAtTick(40L, () -> {
            if (!GemsGameTestUtil.hasItem(player, ModItems.ASTRA_GEM)) {
                context.throwGameTestException("Setup error: missing active Astra gem item");
            }
            if (!GemsGameTestUtil.hasItem(player, ModItems.FIRE_GEM)) {
                context.throwGameTestException("Setup error: missing non-active Fire gem item");
            }

            int astraBefore = GemsGameTestUtil.countItem(player, ModItems.ASTRA_GEM);
            int fireBefore = GemsGameTestUtil.countItem(player, ModItems.FIRE_GEM);

            GemKeepOnDeath.stash(player);

            int astraAfterStash = GemsGameTestUtil.countItem(player, ModItems.ASTRA_GEM);
            int fireAfterStash = GemsGameTestUtil.countItem(player, ModItems.FIRE_GEM);

            if (astraAfterStash != Math.max(0, astraBefore - 1)) {
                context.throwGameTestException("Active gem item should decrement by 1 after stash, before=" + astraBefore + " after=" + astraAfterStash);
            }
            if (fireAfterStash != fireBefore) {
                context.throwGameTestException("Non-active gem item should not change after stash, before=" + fireBefore + " after=" + fireAfterStash);
            }

            GemKeepOnDeath.restore(player);
            int astraAfterRestore = GemsGameTestUtil.countItem(player, ModItems.ASTRA_GEM);
            if (astraAfterRestore != astraBefore) {
                context.throwGameTestException("Active gem item should be restored after stash+restore, before=" + astraBefore + " after=" + astraAfterRestore);
            }

            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 120)
    public void killingPlayerConsumesTheirActiveGemItem(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity killer = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        ServerPlayerEntity victim = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        killer.changeGameMode(GameMode.SURVIVAL);
        victim.changeGameMode(GameMode.SURVIVAL);

        Vec3d base = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(killer, world, base.x, base.y, base.z, 0.0F, 0.0F);
        teleport(victim, world, base.x + 2.0D, base.y, base.z, 0.0F, 0.0F);

        context.runAtTick(20L, () -> {
            killer.getInventory().clear();
            victim.getInventory().clear();
            GemPlayerState.initIfNeeded(killer);
            GemPlayerState.initIfNeeded(victim);
            GemPlayerState.setActiveGem(victim, GemId.FIRE);

            ItemStack stolen = new ItemStack(ModItems.FIRE_GEM);
            GemOwnership.tagOwned(stolen, victim.getUuid(), GemPlayerState.getGemEpoch(victim));
            killer.giveItemStack(stolen);

            if (!GemsGameTestUtil.hasItem(killer, ModItems.FIRE_GEM)) {
                context.throwGameTestException("Setup error: missing victim active gem item on killer");
            }

            GemsPlayerDeath.onDeathTail(victim, victim.getDamageSources().playerAttack(killer));

            if (GemsGameTestUtil.hasItem(killer, ModItems.FIRE_GEM)) {
                context.throwGameTestException("Victim active gem item should be removed after kill");
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void abilityDisablesClearAndCooldownSnapshotPersists(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        player.changeGameMode(GameMode.SURVIVAL);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.AIR);
        GemPlayerState.setEnergy(player, 8);

        var abilities = GemRegistry.definition(GemId.AIR).abilities();
        int dashIndex = abilities.indexOf(PowerIds.AIR_DASH);
        if (dashIndex < 0) {
            context.throwGameTestException("AIR gem missing dash ability registration");
            return;
        }

        AbilityDisables.disable(player, PowerIds.AIR_DASH);
        GemPlayerState.setActiveGem(player, GemId.FIRE);
        GemPlayerState.setActiveGem(player, GemId.AIR);
        if (!AbilityDisables.isDisabled(player, PowerIds.AIR_DASH)) {
            context.throwGameTestException("Ability disables should persist on gem switch (stolen abilities stay disabled until recovered)");
            return;
        }

        // Simulate the victim recovering the stolen ability (e.g. thief switches gems / victim kills thief).
        AbilityDisables.enable(player, PowerIds.AIR_DASH);
        if (AbilityDisables.isDisabled(player, PowerIds.AIR_DASH)) {
            context.throwGameTestException("Ability should be re-enabled after recovery");
            return;
        }

        context.runAtTick(2L, () -> {
            long now = GemsTime.now(player);
            GemAbilities.activateByIndex(player, dashIndex);

            long nextAllowed = GemAbilityCooldowns.nextAllowedTick(player, PowerIds.AIR_DASH);
            if (nextAllowed <= now) {
                context.throwGameTestException("Dash ability did not start cooldown after activation");
                return;
            }

            int remaining = GemAbilityCooldowns.remainingTicks(player, PowerIds.AIR_DASH, now);
            RegistryByteBuf buf = new RegistryByteBuf(Unpooled.buffer(), world.getRegistryManager());
            CooldownSnapshotPayload payload = new CooldownSnapshotPayload(GemId.AIR.ordinal(), java.util.List.of(remaining), java.util.List.of(remaining));
            CooldownSnapshotPayload.CODEC.encode(buf, payload);
            buf.readerIndex(0);
            CooldownSnapshotPayload decoded = CooldownSnapshotPayload.CODEC.decode(buf);

            if (decoded.remainingAbilityCooldownTicks().isEmpty()) {
                context.throwGameTestException("Cooldown snapshot payload lost cooldown entries");
                return;
            }
            if (!decoded.remainingAbilityCooldownTicks().get(0).equals(remaining)) {
                context.throwGameTestException("Cooldown snapshot payload did not preserve remaining ticks");
                return;
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 120)
    public void unlockOrderFollowsSpec(TestContext context) {
        // Use Fire as a representative 4-ability gem and also validate all gems respect the energy spec.
        GemDefinition fire = GemRegistry.definition(GemId.FIRE);
        int fireAbilities = fire.abilities().size();
        if (fireAbilities < 3) {
            context.throwGameTestException("Fire gem should have at least 3 abilities for unlock progression test");
            return;
        }

        int at2 = fire.availableAbilities(new GemEnergyState(2)).size();
        int at3 = fire.availableAbilities(new GemEnergyState(3)).size();
        int at4 = fire.availableAbilities(new GemEnergyState(4)).size();
        int at5 = fire.availableAbilities(new GemEnergyState(5)).size();

        if (at2 != 1 || at3 != Math.min(2, fireAbilities) || at4 != Math.min(3, fireAbilities) || at5 != fireAbilities) {
            context.throwGameTestException("Fire unlock counts did not follow spec: " + at2 + "," + at3 + "," + at4 + "," + at5);
            return;
        }

        // All gems should follow the same energy unlock curve.
        for (GemId id : GemId.values()) {
            GemDefinition def = GemRegistry.definition(id);
            if (def == null) {
                continue;
            }
            int total = def.abilities().size();
            int e2 = def.availableAbilities(new GemEnergyState(2)).size();
            int e3 = def.availableAbilities(new GemEnergyState(3)).size();
            int e4 = def.availableAbilities(new GemEnergyState(4)).size();
            int e5 = def.availableAbilities(new GemEnergyState(5)).size();

            if (e2 != Math.min(1, total) || e3 != Math.min(2, total) || e4 != Math.min(3, total) || e5 != total) {
                context.throwGameTestException("Unlock curve mismatch for gem " + id + " counts=" + e2 + "," + e3 + "," + e4 + "," + e5 + " total=" + total);
                return;
            }
        }

        context.complete();
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 120)
    public void energyLadderGatesAbilitiesAndLosesOnDeath(TestContext context) {
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        player.changeGameMode(GameMode.SURVIVAL);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.FIRE);
        GemDefinition def = GemRegistry.definition(GemId.FIRE);
        int total = def.abilities().size();

        int e0 = def.availableAbilities(new GemEnergyState(0)).size();
        int e1 = def.availableAbilities(new GemEnergyState(1)).size();
        int e2 = def.availableAbilities(new GemEnergyState(2)).size();
        int e4 = def.availableAbilities(new GemEnergyState(4)).size();
        int e5 = def.availableAbilities(new GemEnergyState(5)).size();

        if (e0 != 0 || e1 != 0 || e2 != Math.min(1, total) || e4 != Math.min(3, total) || e5 != total) {
            context.throwGameTestException("Ability gating mismatch for Fire gem");
            return;
        }

        if (GemPlayerState.getEnergy(player) != GemPlayerState.DEFAULT_ENERGY) {
            context.throwGameTestException("Default energy should start at 3");
            return;
        }

        GemPlayerState.addEnergy(player, 1);
        if (GemPlayerState.getEnergy(player) != 4) {
            context.throwGameTestException("Kill-style energy gain should increase energy by 1 to 4");
            return;
        }

        GemPlayerState.addEnergy(player, -1); // simulate death loss
        if (GemPlayerState.getEnergy(player) != 3) {
            context.throwGameTestException("Death should reduce energy by 1 to 3");
            return;
        }

        context.complete();
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 160)
    public void passivesApplyAndStopWhenDisabled(TestContext context) {
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        player.changeGameMode(GameMode.SURVIVAL);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.SPEED);
        GemPlayerState.setEnergy(player, 4);

        GemPowers.sync(player);
        if (!player.hasStatusEffect(StatusEffects.SPEED)) {
            context.throwGameTestException("Speed passive should apply when energy is above zero");
            return;
        }

        GemPlayerState.setEnergy(player, 0);
        GemPowers.sync(player);
        if (player.hasStatusEffect(StatusEffects.SPEED)) {
            context.throwGameTestException("Speed passive should clear when energy hits zero");
            return;
        }

        GemPlayerState.setActiveGem(player, GemId.AIR);
        GemPlayerState.setEnergy(player, 4);
        GemPowers.maintain(player);
        if (!GemsGameTestUtil.containsAirMace(player)) {
            context.throwGameTestException("Maintained passive should grant air mace when enabled");
            return;
        }

        player.getInventory().clear();
        GemPlayerState.setEnergy(player, 0);
        GemPowers.maintain(player);
        if (GemsGameTestUtil.containsAirMace(player)) {
            context.throwGameTestException("Maintained passives should stop ticking when disabled");
            return;
        }

        context.complete();
    }

}

