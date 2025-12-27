package com.feel.gems.gametest.terror;

import com.feel.gems.core.GemId;
import com.feel.gems.power.ability.terror.TerrorTradeAbility;
import com.feel.gems.state.GemPlayerState;
import com.feel.gems.state.PlayerNbt;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;




public final class GemsTerrorTradeGameTests {
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, tickLimit = 200)
    public void terrorTradeNormalKillsBothAndTargetLosesTwoHearts(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity caster = context.createMockCreativeServerPlayerInWorld();
        ServerPlayerEntity victim = context.createMockCreativeServerPlayerInWorld();
        caster.changeGameMode(GameMode.SURVIVAL);
        victim.changeGameMode(GameMode.SURVIVAL);
        caster.setInvulnerable(false);
        victim.setInvulnerable(false);

        Vec3d casterPos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        // Keep the victim well within the EMPTY_STRUCTURE bounds to avoid barrier occlusion during raycasts.
        Vec3d victimPos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 2.5D));
        caster.teleport(world, casterPos.x, casterPos.y, casterPos.z, 0.0F, 0.0F);
        victim.teleport(world, victimPos.x, victimPos.y, victimPos.z, 180.0F, 0.0F);

        GemPlayerState.initIfNeeded(caster);
        GemPlayerState.setActiveGem(caster, GemId.TERROR);
        GemPlayerState.setMaxHearts(caster, 10);
        GemPlayerState.applyMaxHearts(caster);

        GemPlayerState.initIfNeeded(victim);
        GemPlayerState.setMaxHearts(victim, 10);
        GemPlayerState.applyMaxHearts(victim);

        // Totems should save the target in all modes in real gameplay.
        // Note: GameTest mock players only reliably take certain damage sources, so we don't assert totem consumption here.
        victim.setStackInHand(net.minecraft.util.Hand.OFF_HAND, new ItemStack(Items.TOTEM_OF_UNDYING));

        context.runAtTick(20L, () -> {
            // Re-aim right before activation; GameTest player rotations can be flaky if set too early.
            Vec3d aimAt = new Vec3d(victim.getX(), victim.getEyeY(), victim.getZ());
            double dx = aimAt.x - caster.getX();
            double dz = aimAt.z - caster.getZ();
            double dy = aimAt.y - caster.getEyeY();
            float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0D);
            float pitch = (float) (-Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz))));
            caster.teleport(world, caster.getX(), caster.getY(), caster.getZ(), yaw, pitch);
        });

        context.runAtTick(22L, () -> {
            boolean ok = new TerrorTradeAbility().activate(caster);
            if (!ok) {
                context.throwGameTestException("Terror Trade did not activate (raycast target not acquired)");
            }
        });

        context.runAtTick(60L, () -> {
            // Target pays 2 hearts instead of the normal 1-heart drop on death.
            if (GemPlayerState.getMaxHearts(victim) != 8) {
                context.throwGameTestException("Victim max hearts should be 8 after Terror Trade penalty");
                return;
            }

            // Caster has no extra penalties (only the normal death heart loss).
            if (GemPlayerState.getMaxHearts(caster) != 9) {
                context.throwGameTestException("Caster max hearts should be 9 (no extra penalties beyond normal death loss)");
                return;
            }
            if (GemPlayerState.getEnergyCapPenalty(caster) != 0) {
                context.throwGameTestException("Caster energy cap penalty should remain 0 in normal-player Terror Trade");
                return;
            }

            // Normal mode does not consume "uses".
            if (PlayerNbt.getInt(caster, "terrorTradeUses", 0) != 0) {
                context.throwGameTestException("Normal-player Terror Trade should not consume uses");
                return;
            }

            // Victim should not be dead after the trade attempt.
            if (victim.isDead()) {
                context.throwGameTestException("Victim died during Terror Trade");
                return;
            }

            context.complete();
        });
    }
}
