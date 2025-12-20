package com.feel.gems.power;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.state.GemsPersistentDataHolder;
import net.minecraft.block.Blocks;
import net.minecraft.entity.projectile.FireballEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.particle.ParticleTypes;

public final class FireballAbility implements GemAbility {
    private static final String KEY_CHARGE_START = "fireballChargeStart";
    private static final String KEY_LAST_FIRE = "fireballLastFire";
    private static final String KEY_LAST_SHOWN_CHARGE = "fireballLastShownCharge";

    @Override
    public Identifier id() {
        return PowerIds.FIREBALL;
    }

    @Override
    public String name() {
        return "Fireball";
    }

    @Override
    public String description() {
        return "Press once to start charging; press again to launch an explosive fireball.";
    }

    @Override
    public int cooldownTicks() {
        return 0; // internal cooldown (charge vs fire)
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        NbtCompound nbt = persistent(player);
        long now = player.getServerWorld().getTime();
        long lastFire = nbt.getLong(KEY_LAST_FIRE);
        if (now - lastFire < GemsBalance.v().fire().fireballInternalCooldownTicks()) {
            player.sendMessage(Text.literal("Fireball is on cooldown."), true);
            return true;
        }

        long start = nbt.getLong(KEY_CHARGE_START);
        if (start <= 0) {
            nbt.putLong(KEY_CHARGE_START, now);
            nbt.putInt(KEY_LAST_SHOWN_CHARGE, -1);
            AbilityFeedback.sound(player, SoundEvents.BLOCK_FIRE_AMBIENT, 0.7F, 1.2F);
            AbilityFeedback.burst(player, ParticleTypes.SMALL_FLAME, 10, 0.2D);
            player.sendMessage(Text.literal("Fireball charge: 0%"), true);
            return true;
        }

        int charge = computeCharge(player.getServerWorld(), player, start, now);
        nbt.remove(KEY_CHARGE_START);
        nbt.remove(KEY_LAST_SHOWN_CHARGE);
        nbt.putLong(KEY_LAST_FIRE, now);
        launch(player, charge);
        AbilityFeedback.burst(player, ParticleTypes.FLAME, 14, 0.25D);
        AbilityFeedback.burst(player, ParticleTypes.SMOKE, 10, 0.25D);
        player.sendMessage(Text.literal("Fireball: " + charge + "%"), true);
        return true;
    }

    public static void tickCharging(ServerPlayerEntity player, long now) {
        NbtCompound nbt = persistent(player);
        long start = nbt.getLong(KEY_CHARGE_START);
        if (start <= 0) {
            return;
        }
        int charge = computeCharge(player.getServerWorld(), player, start, now);
        int last = nbt.contains(KEY_LAST_SHOWN_CHARGE, NbtElement.INT_TYPE) ? nbt.getInt(KEY_LAST_SHOWN_CHARGE) : -1;
        if (charge == last) {
            return;
        }
        nbt.putInt(KEY_LAST_SHOWN_CHARGE, charge);
        player.sendMessage(Text.literal("Fireball charge: " + charge + "%"), true);
    }

    private static int computeCharge(ServerWorld world, ServerPlayerEntity player, long start, long now) {
        int chargeUpTicks = GemsBalance.v().fire().fireballChargeUpTicks();
        int chargeDownTicks = GemsBalance.v().fire().fireballChargeDownTicks();

        long elapsed = now - start;
        if (elapsed <= chargeUpTicks) {
            return (int) Math.round((elapsed * 100.0D) / Math.max(1, chargeUpTicks));
        }

        BlockPos below = player.getBlockPos().down();
        boolean onObsidian = world.getBlockState(below).isOf(Blocks.OBSIDIAN);
        if (onObsidian) {
            return 100;
        }

        long downElapsed = elapsed - chargeUpTicks;
        if (downElapsed >= chargeDownTicks) {
            return 0;
        }
        double remaining = 1.0D - (downElapsed / (double) Math.max(1, chargeDownTicks));
        return (int) Math.round(100.0D * remaining);
    }

    private static void launch(ServerPlayerEntity player, int chargePercent) {
        Vec3d direction = player.getRotationVec(1.0F);
        Vec3d spawnPos = player.getEyePos().add(direction.multiply(1.5D));

        int power = 1 + (chargePercent / 50);
        FireballEntity fireball = new FireballEntity(player.getWorld(), player, direction, power);
        fireball.refreshPositionAndAngles(spawnPos.x, spawnPos.y, spawnPos.z, player.getYaw(), player.getPitch());
        if (fireball instanceof RangeLimitedProjectile limited) {
            limited.gems$setRangeLimit(spawnPos, GemsBalance.v().fire().fireballMaxDistanceBlocks());
        }
        player.getWorld().spawnEntity(fireball);

        AbilityFeedback.sound(player, SoundEvents.ENTITY_GHAST_SHOOT, 1.0F, 1.0F);
    }

    private static NbtCompound persistent(ServerPlayerEntity player) {
        return ((GemsPersistentDataHolder) player).gems$getPersistentData();
    }
}
