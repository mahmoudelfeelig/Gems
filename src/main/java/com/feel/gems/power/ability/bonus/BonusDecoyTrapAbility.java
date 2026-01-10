package com.feel.gems.power.ability.bonus;

import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.gem.voidgem.VoidImmunity;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.trust.GemTrust;
import net.minecraft.entity.ItemEntity;
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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Decoy Trap - Place a fake diamond item that explodes when picked up by enemies.
 */
public final class BonusDecoyTrapAbility implements GemAbility {
    // Track which item entities are traps and who placed them
    // Maps itemUuid -> ownerUuid
    private static final Map<UUID, UUID> TRAP_ITEMS = new HashMap<>();
    private static final float EXPLOSION_POWER = 3.0f;
    // Maximum number of traps to prevent memory issues
    private static final int MAX_TRAPS = 100;
    // Counter to periodically cleanup stale entries
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
        return 600; // 30 seconds
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        ServerWorld world = player.getEntityWorld();
        
        // Periodic cleanup to prevent memory leaks
        if (++cleanupCounter >= CLEANUP_INTERVAL) {
            cleanupCounter = 0;
            cleanupStaleTraps(world);
        }
        
        // Enforce max traps limit
        if (TRAP_ITEMS.size() >= MAX_TRAPS) {
            // Remove oldest trap (first entry)
            Iterator<UUID> it = TRAP_ITEMS.keySet().iterator();
            if (it.hasNext()) {
                it.next();
                it.remove();
            }
        }
        
        Vec3d playerPos = new Vec3d(player.getX(), player.getY(), player.getZ());
        Vec3d pos = playerPos.add(player.getRotationVec(1.0f).multiply(2));
        
        // Create a fake diamond item
        ItemStack fakeItem = new ItemStack(Items.DIAMOND);
        fakeItem.set(net.minecraft.component.DataComponentTypes.CUSTOM_NAME,
                net.minecraft.text.Text.literal("§6Shiny Diamond").styled(s -> s.withItalic(false)));
        
        ItemEntity trapItem = new ItemEntity(world, pos.x, pos.y + 0.5, pos.z, fakeItem);
        trapItem.setPickupDelay(20); // 1 second delay before it can be picked up
        trapItem.setNeverDespawn();
        
        world.spawnEntity(trapItem);
        TRAP_ITEMS.put(trapItem.getUuid(), player.getUuid());
        
        // Visual feedback
        world.spawnParticles(ParticleTypes.ENCHANT, pos.x, pos.y + 0.5, pos.z, 20, 0.3, 0.3, 0.3, 0.1);
        player.playSound(SoundEvents.ENTITY_ITEM_PICKUP, 0.5f, 0.5f);
        
        return true;
    }

    /**
     * Check if an item entity is a trap and return the owner's UUID, or null if not a trap.
     */
    public static UUID getTrapOwner(ItemEntity item) {
        return TRAP_ITEMS.get(item.getUuid());
    }

    /**
     * Trigger the trap explosion when picked up.
     * @return true if this was a trap and exploded
     */
    public static boolean triggerTrap(ItemEntity item, ServerPlayerEntity picker) {
        return triggerTrap(item, (net.minecraft.entity.LivingEntity) picker);
    }

    public static boolean triggerTrap(ItemEntity item, net.minecraft.entity.LivingEntity picker) {
        UUID ownerUuid = TRAP_ITEMS.remove(item.getUuid());
        if (ownerUuid == null) {
            return false;
        }
        
        if (picker instanceof ServerPlayerEntity player) {
            // Don't explode for the owner or trusted players
            if (player.getUuid().equals(ownerUuid)) {
                return false;
            }
            if (VoidImmunity.hasImmunity(player)) {
                return false;
            }
            MinecraftServer server = player.getEntityWorld().getServer();
            if (server != null) {
                ServerPlayerEntity owner = server.getPlayerManager().getPlayer(ownerUuid);
                if (owner != null && GemTrust.isTrusted(owner, player)) {
                    return false;
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
        world.createExplosion(null, x, y, z, EXPLOSION_POWER, World.ExplosionSourceType.MOB);
        
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

    /**
     * Cleanup stale trap entries for items that no longer exist.
     * Called periodically to prevent memory leaks.
     */
    private static void cleanupStaleTraps(ServerWorld world) {
        if (world == null || TRAP_ITEMS.isEmpty()) {
            return;
        }
        
        Iterator<UUID> it = TRAP_ITEMS.keySet().iterator();
        while (it.hasNext()) {
            UUID itemUuid = it.next();
            // Try to find the item entity in the world
            // If it doesn't exist, remove the stale entry
            boolean found = false;
            for (var entity : world.iterateEntities()) {
                if (entity.getUuid().equals(itemUuid) && entity instanceof ItemEntity) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                it.remove();
            }
        }
    }

    /**
     * Clear all trap entries. Called on server shutdown.
     */
    public static void clearAllTraps() {
        TRAP_ITEMS.clear();
    }
}
