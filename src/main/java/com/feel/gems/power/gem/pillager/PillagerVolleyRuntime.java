package com.feel.gems.power.gem.pillager;

import com.feel.gems.bonus.PrismSelectionsState;
import com.feel.gems.config.GemsBalance;
import com.feel.gems.core.GemId;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import com.feel.gems.state.GemPlayerState;
import com.feel.gems.state.GemsPersistentDataHolder;
import com.feel.gems.util.GemsTime;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;


public final class PillagerVolleyRuntime {
    private static final String KEY_UNTIL = "pillagerVolleyUntil";
    private static final String KEY_NEXT_SHOT = "pillagerVolleyNextShot";
    private static final Set<UUID> ACTIVE = new HashSet<>();

    private PillagerVolleyRuntime() {
    }

    public static void start(ServerPlayerEntity player, int durationTicks) {
        long now = GemsTime.now(player);
        NbtCompound nbt = persistent(player);
        nbt.putLong(KEY_UNTIL, now + durationTicks);
        nbt.putLong(KEY_NEXT_SHOT, now);
        ACTIVE.add(player.getUuid());
        AbilityFeedback.sound(player, SoundEvents.ITEM_CROSSBOW_QUICK_CHARGE_3, 0.9F, 1.2F);
    }

    public static void stop(ServerPlayerEntity player) {
        stop(player, true);
    }

    private static void stop(ServerPlayerEntity player, boolean removeFromActive) {
        NbtCompound nbt = persistent(player);
        nbt.remove(KEY_UNTIL);
        nbt.remove(KEY_NEXT_SHOT);
        if (removeFromActive) {
            ACTIVE.remove(player.getUuid());
        }
    }

    public static void tick(MinecraftServer server) {
        if (ACTIVE.isEmpty()) {
            return;
        }
        Iterator<UUID> it = ACTIVE.iterator();
        while (it.hasNext()) {
            UUID uuid = it.next();
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(uuid);
            if (player == null) {
                it.remove();
                continue;
            }
            GemPlayerState.initIfNeeded(player);
            if (GemPlayerState.getEnergy(player) <= 1) {
                stop(player, false);
                it.remove();
                continue;
            }
            GemId activeGem = GemPlayerState.getActiveGem(player);
            if (activeGem != GemId.PILLAGER) {
                if (activeGem != GemId.PRISM || !PrismSelectionsState.hasAbility(player, PowerIds.PILLAGER_VOLLEY)) {
                    stop(player, false);
                    it.remove();
                    continue;
                }
            }
            NbtCompound nbt = persistent(player);
            long now = GemsTime.now(player);
            long until = nbt.getLong(KEY_UNTIL, 0L);
            if (until <= 0 || now >= until) {
                stop(player, false);
                it.remove();
                continue;
            }
            long next = nbt.getLong(KEY_NEXT_SHOT, 0L);
            if (now < next) {
                continue;
            }

            int period = Math.max(1, GemsBalance.v().pillager().volleyPeriodTicks());
            nbt.putLong(KEY_NEXT_SHOT, now + period);
            fireShot(player);
        }
    }

    private static void fireShot(ServerPlayerEntity player) {
        if (!(player.getEntityWorld() instanceof ServerWorld world)) {
            return;
        }
        int arrows = GemsBalance.v().pillager().volleyArrowsPerShot();
        if (arrows <= 0) {
            return;
        }
        float velocity = GemsBalance.v().pillager().volleyArrowVelocity();
        float inaccuracy = GemsBalance.v().pillager().volleyArrowInaccuracy();
        float damage = GemsBalance.v().pillager().volleyArrowDamage();

        Vec3d dir = player.getRotationVec(1.0F);
        Vec3d spawn = player.getEyePos().add(dir.multiply(1.2D));

        for (int i = 0; i < arrows; i++) {
            ArrowEntity arrow = new ArrowEntity(world, player, new ItemStack(Items.ARROW), new ItemStack(Items.BOW));
            arrow.setPosition(spawn.x, spawn.y, spawn.z);
            arrow.setVelocity(dir.x, dir.y, dir.z, velocity, inaccuracy);
            arrow.setDamage(damage);
            arrow.pickupType = ArrowEntity.PickupPermission.DISALLOWED;
            world.spawnEntity(arrow);
        }

        AbilityFeedback.burstAt(world, spawn, ParticleTypes.CRIT, 3, 0.05D);
        AbilityFeedback.sound(player, SoundEvents.ENTITY_SKELETON_SHOOT, 0.7F, 1.3F);
    }

    private static NbtCompound persistent(ServerPlayerEntity player) {
        return ((GemsPersistentDataHolder) player).gems$getPersistentData();
    }
}
