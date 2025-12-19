package com.blissmc.gems.power;

import com.blissmc.gems.state.GemsPersistentDataHolder;
import net.minecraft.block.Blocks;
import net.minecraft.entity.projectile.FireballEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public final class FireballAbility implements GemAbility {
    private static final String KEY_CHARGE_START = "fireballChargeStart";
    private static final String KEY_LAST_FIRE = "fireballLastFire";

    private static final int CHARGE_UP_TICKS = 3 * 20;
    private static final int CHARGE_DOWN_TICKS = 3 * 20;
    private static final int FIRE_COOLDOWN_TICKS = 4 * 20;

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
        if (now - lastFire < FIRE_COOLDOWN_TICKS) {
            player.sendMessage(Text.literal("Fireball is on cooldown."), true);
            return true;
        }

        long start = nbt.getLong(KEY_CHARGE_START);
        if (start <= 0) {
            nbt.putLong(KEY_CHARGE_START, now);
            player.sendMessage(Text.literal("Charging Fireball..."), true);
            return true;
        }

        int charge = computeCharge(player.getServerWorld(), player, start, now);
        nbt.remove(KEY_CHARGE_START);
        nbt.putLong(KEY_LAST_FIRE, now);
        launch(player, charge);
        player.sendMessage(Text.literal("Fireball: " + charge + "%"), true);
        return true;
    }

    private static int computeCharge(ServerWorld world, ServerPlayerEntity player, long start, long now) {
        long elapsed = now - start;
        if (elapsed <= CHARGE_UP_TICKS) {
            return (int) Math.round((elapsed * 100.0D) / CHARGE_UP_TICKS);
        }

        BlockPos below = player.getBlockPos().down();
        boolean onObsidian = world.getBlockState(below).isOf(Blocks.OBSIDIAN);
        if (onObsidian) {
            return 100;
        }

        long downElapsed = elapsed - CHARGE_UP_TICKS;
        if (downElapsed >= CHARGE_DOWN_TICKS) {
            return 0;
        }
        double remaining = 1.0D - (downElapsed / (double) CHARGE_DOWN_TICKS);
        return (int) Math.round(100.0D * remaining);
    }

    private static void launch(ServerPlayerEntity player, int chargePercent) {
        Vec3d direction = player.getRotationVec(1.0F);
        Vec3d spawnPos = player.getEyePos().add(direction.multiply(1.5D));

        int power = 1 + (chargePercent / 50);
        FireballEntity fireball = new FireballEntity(player.getWorld(), player, direction, power);
        fireball.refreshPositionAndAngles(spawnPos.x, spawnPos.y, spawnPos.z, player.getYaw(), player.getPitch());
        player.getWorld().spawnEntity(fireball);

        player.getWorld().playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.ENTITY_GHAST_SHOOT,
                SoundCategory.PLAYERS,
                1.0F,
                1.0F
        );
    }

    private static NbtCompound persistent(ServerPlayerEntity player) {
        return ((GemsPersistentDataHolder) player).gems$getPersistentData();
    }
}
