package com.feel.gems.gametest.ability;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.core.GemId;
import com.feel.gems.gametest.util.GemsGameTestUtil;
import com.feel.gems.power.runtime.AbilityRuntime;
import com.feel.gems.power.ability.strength.*;
import com.feel.gems.power.runtime.GemPowers;
import com.feel.gems.state.GemPlayerState;
import com.feel.gems.state.GemsPersistentDataHolder;
import com.feel.gems.util.GemsNbt;
import java.util.EnumSet;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.Vec3d;

public final class GemsStrengthGameTests {

    private static void teleport(ServerPlayerEntity player, ServerWorld world, double x, double y, double z, float yaw, float pitch) {
        player.teleport(world, x, y, z, EnumSet.noneOf(PositionFlag.class), yaw, pitch, false);
    }

    private static void aimAt(ServerPlayerEntity player, ServerWorld world, Vec3d target) {
        Vec3d pos = player.getEntityPos();
        double dx = target.x - pos.x;
        double dz = target.z - pos.z;
        double dy = target.y - player.getEyeY();
        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0D);
        float pitch = (float) (-Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz))));
        teleport(player, world, pos.x, pos.y, pos.z, yaw, pitch);
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void strengthChadStrengthActivates(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.STRENGTH);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);

        context.runAtTick(5L, () -> {
            boolean ok = new ChadStrengthAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Chad Strength did not activate");
            }
            if (!AbilityRuntime.isChadStrengthActive(player)) {
                context.throwGameTestException("Chad Strength should be active after activation");
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void strengthFrailerAppliesWeakness(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);

        ZombieEntity target = EntityType.ZOMBIE.create(world, net.minecraft.entity.SpawnReason.MOB_SUMMONED);
        if (target == null) {
            context.throwGameTestException("Failed to create zombie");
            return;
        }
        target.refreshPositionAndAngles(pos.x, pos.y, pos.z + 3.0D, 180.0F, 0.0F);
        world.spawnEntity(target);
        aimAt(player, world, target.getEntityPos().add(0.0D, 1.2D, 0.0D));

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.STRENGTH);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);

        context.runAtTick(5L, () -> {
            boolean ok = new FrailerAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Frailer did not activate");
            }
            if (!target.hasStatusEffect(StatusEffects.WEAKNESS)) {
                context.throwGameTestException("Frailer should apply Weakness");
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void strengthNullifyClearsEnemyEffects(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        ServerPlayerEntity ally = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);
        GemsGameTestUtil.forceSurvival(ally);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);

        ZombieEntity target = EntityType.ZOMBIE.create(world, net.minecraft.entity.SpawnReason.MOB_SUMMONED);
        if (target == null) {
            context.throwGameTestException("Failed to create zombie");
            return;
        }
        target.refreshPositionAndAngles(pos.x + 2.0D, pos.y, pos.z, 180.0F, 0.0F);
        world.spawnEntity(target);

        teleport(ally, world, pos.x + 1.0D, pos.y, pos.z + 1.0D, 0.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.STRENGTH);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);

        com.feel.gems.trust.GemTrust.trust(player, ally.getUuid());
        target.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(StatusEffects.WEAKNESS, 200, 0));
        ally.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(StatusEffects.WEAKNESS, 200, 0));

        context.runAtTick(5L, () -> {
            // Nullify activates the buff mode - effect applies on hit
            boolean ok = new NullifyAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Nullify did not activate");
            }
            if (target.hasStatusEffect(StatusEffects.WEAKNESS)) {
                context.throwGameTestException("Nullify should clear enemy effects");
            }
            if (!ally.hasStatusEffect(StatusEffects.WEAKNESS)) {
                context.throwGameTestException("Nullify should not clear trusted ally effects");
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void strengthBountyHuntingTracksOwner(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        ServerPlayerEntity owner = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);
        GemsGameTestUtil.forceSurvival(owner);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);
        teleport(owner, world, pos.x + 4.0D, pos.y, pos.z, 0.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.STRENGTH);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);

        ItemStack token = new ItemStack(Items.DIAMOND);
        AbilityRuntime.setOwnerWithName(token, owner.getUuid(), owner.getName().getString());
        player.setStackInHand(net.minecraft.util.Hand.MAIN_HAND, token);

        context.runAtTick(5L, () -> {
            boolean ok = new BountyHuntingAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Bounty Hunting did not activate");
            }
            if (!player.getMainHandStack().isEmpty()) {
                context.throwGameTestException("Bounty Hunting should consume the held item");
            }
            NbtCompound data = ((GemsPersistentDataHolder) player).gems$getPersistentData();
            if (data.getLong("bountyUntil", 0L) <= 0L) {
                context.throwGameTestException("Bounty Hunting should set a tracking duration");
            }
            java.util.UUID tracked = GemsNbt.getUuid(data, "bountyTarget");
            if (tracked == null || !tracked.equals(owner.getUuid())) {
                context.throwGameTestException("Bounty Hunting should track the owner");
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void strengthPassivesApply(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.STRENGTH);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);

        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        player.setStackInHand(net.minecraft.util.Hand.MAIN_HAND, sword);
        player.setHealth(4.0F);

        context.runAtTick(5L, () -> {
            GemPowers.maintain(player);
            if (!player.hasStatusEffect(StatusEffects.STRENGTH)) {
                context.throwGameTestException("Strength passive should apply Strength");
            }
            int sharpness = net.minecraft.enchantment.EnchantmentHelper.getLevel(
                    world.getRegistryManager().getOptionalEntry(net.minecraft.enchantment.Enchantments.SHARPNESS).orElseThrow(),
                    sword
            );
            if (sharpness < 3) {
                context.throwGameTestException("Auto-enchant Sharpness should apply");
            }
            if (!player.hasStatusEffect(StatusEffects.RESISTANCE)) {
                context.throwGameTestException("Adrenaline should grant Resistance at low health");
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void strengthConfigValuesValid(TestContext context) {
        var cfg = GemsBalance.v().strength();
        
        if (cfg.chadCooldownTicks() < 0) {
            context.throwGameTestException("Chad Strength cooldown cannot be negative");
        }
        if (cfg.frailerCooldownTicks() < 0) {
            context.throwGameTestException("Frailer cooldown cannot be negative");
        }
        if (cfg.nullifyCooldownTicks() < 0) {
            context.throwGameTestException("Nullify cooldown cannot be negative");
        }
        
        context.complete();
    }
}
