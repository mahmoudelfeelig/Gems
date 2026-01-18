package com.feel.gems.power.ability.bonus;

import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.gem.voidgem.VoidImmunity;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.trust.GemTrust;
import com.feel.gems.config.GemsBalance;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.LinkedHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Decoy Trap - Place a fake diamond item that explodes when picked up by enemies.
 */
public final class BonusDecoyTrapAbility implements GemAbility {
    // Track which item entities are traps and who placed them.
    // Maps itemUuid -> trap data, insertion order preserved for eviction.
    private static final Map<UUID, TrapEntry> TRAP_ITEMS = new LinkedHashMap<>();
    // Counter to periodically cleanup stale entries.
    private static int cleanupCounter = 0;
    private static final int CLEANUP_INTERVAL = 20; // Every 20 activations

    @Override
    public Identifier id() {
        return PowerIds.BONUS_DECOY_TRAP;
    }

    @Override
    public String name() {
        return "Decoy Trap";
    }

    @Override
    public String description() {
        return "Place a fake valuable item that explodes when picked up by enemies.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().bonusPool().decoyTrapCooldownSeconds * 20;
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        ServerWorld world = player.getEntityWorld();
        long now = world.getTime();
        var cfg = GemsBalance.v().bonusPool();
        
        // Periodic cleanup to prevent memory leaks
        if (++cleanupCounter >= CLEANUP_INTERVAL) {
            cleanupCounter = 0;
            cleanupStaleTraps(world);
            pruneExpiredTraps(world, now);
        }
        
        // Enforce max traps limit
        int maxTraps = Math.max(0, cfg.decoyTrapMaxActive);
        if (maxTraps > 0 && TRAP_ITEMS.size() >= maxTraps) {
            // Remove oldest trap (first entry)
            Iterator<Map.Entry<UUID, TrapEntry>> it = TRAP_ITEMS.entrySet().iterator();
            if (it.hasNext()) {
                UUID toRemove = it.next().getKey();
                it.remove();
                discardTrapEntity(world.getServer(), toRemove);
            }
        }
        
        Vec3d playerPos = new Vec3d(player.getX(), player.getY(), player.getZ());
        Vec3d pos = playerPos.add(player.getRotationVec(1.0f).multiply(2));
        
        // Create a fake diamond item
        ItemStack fakeItem = new ItemStack(Items.DIAMOND);
        fakeItem.set(net.minecraft.component.DataComponentTypes.CUSTOM_NAME,
                net.minecraft.text.Text.literal("§6Shiny Diamond").styled(s -> s.withItalic(false)));
        
        ItemEntity trapItem = new ItemEntity(world, pos.x, pos.y + 0.5, pos.z, fakeItem);
        int armTicks = Math.max(0, cfg.decoyTrapArmTimeSeconds) * 20;
        trapItem.setPickupDelay(armTicks);
        
        world.spawnEntity(trapItem);
        long despawnTicks = Math.max(0, cfg.decoyTrapDespawnSeconds) * 20L;
        long expiresAt = despawnTicks > 0 ? now + despawnTicks : Long.MAX_VALUE;
        TRAP_ITEMS.put(trapItem.getUuid(), new TrapEntry(player.getUuid(), world.getRegistryKey(), expiresAt));
        
        // Visual feedback
        world.spawnParticles(ParticleTypes.ENCHANT, pos.x, pos.y + 0.5, pos.z, 20, 0.3, 0.3, 0.3, 0.1);
        player.playSound(SoundEvents.ENTITY_ITEM_PICKUP, 0.5f, 0.5f);
        
        return true;
    }

    /**
     * Check if an item entity is a trap and return the owner's UUID, or null if not a trap.
     */
    public static UUID getTrapOwner(ItemEntity item) {
        TrapEntry entry = TRAP_ITEMS.get(item.getUuid());
        return entry == null ? null : entry.ownerUuid;
    }

    /**
     * Trigger the trap explosion when picked up.
     * @return true if this was a trap and exploded
     */
    public static boolean triggerTrap(ItemEntity item, ServerPlayerEntity picker) {
        return triggerTrap(item, (net.minecraft.entity.LivingEntity) picker);
    }

    public static boolean triggerTrap(ItemEntity item, net.minecraft.entity.LivingEntity picker) {
        TrapEntry entry = TRAP_ITEMS.remove(item.getUuid());
        if (entry == null) {
            return false;
        }
        UUID ownerUuid = entry.ownerUuid;
        
        if (picker instanceof ServerPlayerEntity player) {
            // Don't explode for the owner or trusted players
            if (player.getUuid().equals(ownerUuid)) {
                item.discard();
                return true;
            }
            if (VoidImmunity.hasImmunity(player)) {
                item.discard();
                return true;
            }
            MinecraftServer server = player.getEntityWorld().getServer();
            if (server != null) {
                ServerPlayerEntity owner = server.getPlayerManager().getPlayer(ownerUuid);
                if (owner != null && GemTrust.isTrusted(owner, player)) {
                    item.discard();
                    return true;
                }
            }
        }
        
        // Explode!
        if (!(picker.getEntityWorld() instanceof ServerWorld world)) {
            return false;
        }
        double x = item.getX();
        double y = item.getY();
        double z = item.getZ();
        float power = Math.max(0.0F, GemsBalance.v().bonusPool().decoyTrapExplosionDamage);
        world.createExplosion(null, x, y, z, power, World.ExplosionSourceType.MOB);
        
        // Remove the item
        item.discard();
        return true;
    }

    /**
     * Clean up a trap item (e.g., when it despawns or is removed).
     */
    public static void removeTrap(UUID itemUuid) {
        TRAP_ITEMS.remove(itemUuid);
    }

    public static void tickTrapItem(ItemEntity item) {
        TrapEntry entry = TRAP_ITEMS.get(item.getUuid());
        if (entry == null) {
            return;
        }
        if (!(item.getEntityWorld() instanceof ServerWorld world)) {
            TRAP_ITEMS.remove(item.getUuid());
            return;
        }
        long now = world.getTime();
        if (now >= entry.expiresAt) {
            TRAP_ITEMS.remove(item.getUuid());
            item.discard();
        }
    }

    /**
     * Cleanup stale trap entries for items that no longer exist.
     * Called periodically to prevent memory leaks.
     */
    private static void cleanupStaleTraps(ServerWorld world) {
        if (world == null || TRAP_ITEMS.isEmpty()) {
            return;
        }
        
        Iterator<Map.Entry<UUID, TrapEntry>> it = TRAP_ITEMS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, TrapEntry> entry = it.next();
            UUID itemUuid = entry.getKey();
            if (!isTrapPresent(world.getServer(), entry.getValue(), itemUuid)) {
                it.remove();
            }
        }
    }

    private static void pruneExpiredTraps(ServerWorld world, long now) {
        if (TRAP_ITEMS.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<UUID, TrapEntry>> it = TRAP_ITEMS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, TrapEntry> entry = it.next();
            TrapEntry trap = entry.getValue();
            if (now < trap.expiresAt) {
                continue;
            }
            it.remove();
            discardTrapEntity(world.getServer(), entry.getKey());
        }
    }

    private static boolean isTrapPresent(MinecraftServer server, TrapEntry entry, UUID itemUuid) {
        if (server == null || entry == null) {
            return false;
        }
        ServerWorld world = server.getWorld(entry.dimension);
        if (world == null) {
            return false;
        }
        Entity entity = world.getEntity(itemUuid);
        return entity instanceof ItemEntity;
    }

    private static void discardTrapEntity(MinecraftServer server, UUID itemUuid) {
        if (server == null) {
            return;
        }
        for (ServerWorld world : server.getWorlds()) {
            Entity entity = world.getEntity(itemUuid);
            if (entity instanceof ItemEntity item) {
                item.discard();
                return;
            }
        }
    }

    /**
     * Clear all trap entries. Called on server shutdown.
     */
    public static void clearAllTraps() {
        TRAP_ITEMS.clear();
    }

    private static final class TrapEntry {
        final UUID ownerUuid;
        final net.minecraft.registry.RegistryKey<World> dimension;
        final long expiresAt;
        private TrapEntry(UUID ownerUuid, net.minecraft.registry.RegistryKey<World> dimension, long expiresAt) {
            this.ownerUuid = ownerUuid;
            this.dimension = dimension;
            this.expiresAt = expiresAt;
        }
    }
}
