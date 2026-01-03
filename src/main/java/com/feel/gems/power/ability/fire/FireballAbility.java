package com.feel.gems.power.ability.fire;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import com.feel.gems.power.util.RangeLimitedProjectile;
import com.feel.gems.state.GemsPersistentDataHolder;
import com.feel.gems.util.GemsTime;
import net.minecraft.block.Blocks;
import net.minecraft.entity.projectile.FireballEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;



public final class FireballAbility implements GemAbility {
    private static final String KEY_CHARGE_START = "fireballChargeStart";
    private static final String KEY_LAST_FIRE = "fireballLastFire";
    private static final String KEY_LAST_SHOWN_CHARGE = "fireballLastShownCharge";
    private static final int CHARGE_BAR_SEGMENTS = 20;

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
        long now = GemsTime.now(player);
        long lastFire = nbt.getLong(KEY_LAST_FIRE, 0L);
        if (now - lastFire < GemsBalance.v().fire().fireballInternalCooldownTicks()) {
            player.sendMessage(Text.literal("Fireball is on cooldown."), true);
            return true;
        }

        long start = nbt.getLong(KEY_CHARGE_START, 0L);
        if (start <= 0) {
            nbt.putLong(KEY_CHARGE_START, now);
            nbt.putInt(KEY_LAST_SHOWN_CHARGE, -1);
            AbilityFeedback.sound(player, SoundEvents.BLOCK_FIRE_AMBIENT, 0.7F, 1.2F);
            AbilityFeedback.burst(player, ParticleTypes.SMALL_FLAME, 10, 0.2D);
            player.sendMessage(chargeBarText(0), true);
            return true;
        }

        int charge = computeCharge(player.getEntityWorld(), player, start, now);
        nbt.remove(KEY_CHARGE_START);
        nbt.remove(KEY_LAST_SHOWN_CHARGE);
        nbt.putLong(KEY_LAST_FIRE, now);
        launch(player, charge);
        AbilityFeedback.burst(player, ParticleTypes.FLAME, 14, 0.25D);
        AbilityFeedback.burst(player, ParticleTypes.SMOKE, 10, 0.25D);
        player.sendMessage(Text.literal("Fireball launched!").formatted(Formatting.GOLD), true);
        return true;
    }

    public static void tickCharging(ServerPlayerEntity player, long now) {
        NbtCompound nbt = persistent(player);
        long start = nbt.getLong(KEY_CHARGE_START, 0L);
        if (start <= 0) {
            return;
        }
        int charge = computeCharge(player.getEntityWorld(), player, start, now);
        int bucket = chargeBucket(charge);
        int last = nbt.getInt(KEY_LAST_SHOWN_CHARGE, -1);
        if (bucket == last) {
            return;
        }
        nbt.putInt(KEY_LAST_SHOWN_CHARGE, bucket);
        player.sendMessage(chargeBarText(charge), true);
    }

    private static int chargeBucket(int chargePercent) {
        int clamped = Math.max(0, Math.min(100, chargePercent));
        return (clamped * CHARGE_BAR_SEGMENTS) / 100;
    }

    private static Text chargeBarText(int chargePercent) {
        int clamped = Math.max(0, Math.min(100, chargePercent));
        int filled = chargeBucket(clamped);

        Formatting fillColor;
        if (clamped >= 100) {
            fillColor = Formatting.GOLD;
        } else if (clamped >= 67) {
            fillColor = Formatting.GREEN;
        } else if (clamped >= 34) {
            fillColor = Formatting.YELLOW;
        } else {
            fillColor = Formatting.RED;
        }

        var bar = Text.literal("[");
        for (int i = 0; i < CHARGE_BAR_SEGMENTS; i++) {
            boolean on = i < filled;
            bar.append(Text.literal(on ? "|" : ".").formatted(on ? fillColor : Formatting.DARK_GRAY));
        }
        bar.append(Text.literal("]"));

        var percent = Text.literal(" " + clamped + "%").formatted(Formatting.GRAY);
        var message = Text.literal("Fireball charge ").formatted(Formatting.GOLD)
                .append(bar)
                .append(percent);
        if (clamped >= 100) {
            message.append(Text.literal(" READY").formatted(Formatting.GOLD));
        }
        return message;
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
        ServerWorld world = player.getEntityWorld();
        Vec3d direction = player.getRotationVec(1.0F);
        Vec3d spawnPos = player.getEyePos().add(direction.multiply(1.5D));

        int power = 1 + (chargePercent / 50);
        FireballEntity fireball = new FireballEntity(world, player, direction, power);
        fireball.refreshPositionAndAngles(spawnPos.x, spawnPos.y, spawnPos.z, player.getYaw(), player.getPitch());
        if (fireball instanceof RangeLimitedProjectile limited) {
            limited.gems$setRangeLimit(spawnPos, GemsBalance.v().fire().fireballMaxDistanceBlocks());
        }
        world.spawnEntity(fireball);

        AbilityFeedback.sound(player, SoundEvents.ENTITY_GHAST_SHOOT, 1.0F, 1.0F);
    }

    private static NbtCompound persistent(ServerPlayerEntity player) {
        return ((GemsPersistentDataHolder) player).gems$getPersistentData();
    }
}
