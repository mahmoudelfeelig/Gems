package com.feel.gems.entity;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.World;

/**
 * Shadow clone entity that mimics a player's appearance and randomly moves around.
 * Cannot die, doesn't drop items, but can be hit for visual feedback.
 * Note: NBT persistence not needed as clones are temporary and discarded after duration.
 */
public class ShadowCloneEntity extends PathAwareEntity {
    private UUID ownerUuid;
    private String ownerName;
    private boolean mirageClone;
    
    private int ticksAlive = 0;
    private int nextActionTick = 0;
    private int maxLifetimeTicks = 200; // 10 seconds default, set by ability
    
    public ShadowCloneEntity(EntityType<? extends ShadowCloneEntity> type, World world) {
        super(type, world);
    }
    
    public static DefaultAttributeContainer.Builder createAttributes() {
        return PathAwareEntity.createMobAttributes()
                .add(EntityAttributes.MAX_HEALTH, 20.0D)
                .add(EntityAttributes.MOVEMENT_SPEED, 0.3D)  // Faster movement
                .add(EntityAttributes.FOLLOW_RANGE, 32.0D); // Larger wander range
    }
    
    @Override
    protected void initGoals() {
        // Random wandering behavior with larger range
        this.goalSelector.add(1, new CloneWanderGoal(this, 0.8D, 0.05F));
        this.goalSelector.add(2, new LookAtEntityGoal(this, PlayerEntity.class, 12.0F));
        this.goalSelector.add(3, new LookAroundGoal(this));
    }
    
    public void setOwner(ServerPlayerEntity owner) {
        this.setPersistent();
        
        this.ownerUuid = owner.getUuid();
        this.ownerName = owner.getGameProfile().name();
        this.setCustomName(owner.getDisplayName());
        this.setCustomNameVisible(true); // Always show name tag
        
        // Copy equipment
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.isArmorSlot() || slot == EquipmentSlot.MAINHAND || slot == EquipmentSlot.OFFHAND) {
                ItemStack stack = owner.getEquippedStack(slot);
                this.equipStack(slot, stack.copy());
            }
        }
    }
    
    public UUID getOwnerUuid() {
        return ownerUuid;
    }
    
    public String getOwnerName() {
        return ownerName;
    }
    
    public void setMaxLifetime(int ticks) {
        this.maxLifetimeTicks = ticks;
    }

    public void setMirageClone(boolean mirageClone) {
        this.mirageClone = mirageClone;
    }

    public boolean isMirageClone() {
        return mirageClone;
    }
    
    public GameProfile getOwnerProfile() {
        if (ownerUuid == null || ownerName == null) {
            return null;
        }
        return new GameProfile(ownerUuid, ownerName);
    }
    
    @Override
    public void tick() {
        super.tick();
        ticksAlive++;
        
        World world = this.getEntityWorld();
        if (!world.isClient()) {
            // Auto-despawn after max lifetime
            if (ticksAlive >= maxLifetimeTicks) {
                this.discard();
                return;
            }
            
            // Random item swinging/using animation
            if (!mirageClone && world instanceof ServerWorld && ticksAlive >= nextActionTick) {
                performRandomAction();
                nextActionTick = ticksAlive + 40 + random.nextInt(80); // 2-6 seconds
            }
        }
    }
    
    private void performRandomAction() {
        int action = random.nextInt(5);
        switch (action) {
            case 0 -> swingHand(net.minecraft.util.Hand.MAIN_HAND);
            case 1 -> swingHand(net.minecraft.util.Hand.OFF_HAND);
            case 2 -> setSneaking(!isSneaking());
            case 3 -> jump();
            // case 4: do nothing
        }
    }
    
    @Override
    public boolean isInvulnerable() {
        return false; // Allow damage for visual feedback
    }
    
    @Override
    public boolean damage(ServerWorld world, DamageSource source, float amount) {
        if (mirageClone) {
            com.feel.gems.power.ability.trickster.TricksterMirageRuntime.onMirageCloneHit(this);
            this.discard();
            return true;
        }
        // Take damage but don't die - just play hurt effect
        this.playHurtSound(source);
        // Reset health to prevent actual death
        this.setHealth(this.getMaxHealth());
        return true;
    }
    
    @Override
    protected void dropInventory(ServerWorld world) {
        // Don't drop anything
    }
    
    @Override
    protected void dropEquipment(ServerWorld world, DamageSource source, boolean causedByPlayer) {
        // Don't drop equipment
    }
    
    @Override
    public void onDeath(DamageSource damageSource) {
        // Don't actually die
    }
    
    @Override
    public boolean cannotDespawn() {
        return true;
    }
    
    @Override
    public boolean isPersistent() {
        return true;
    }
    
    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENTITY_PLAYER_HURT;
    }
    
    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_PLAYER_DEATH;
    }
    
    /**
     * Custom wander goal with larger range for more dynamic movement
     */
    private static class CloneWanderGoal extends WanderAroundFarGoal {
        private final ShadowCloneEntity clone;
        
        public CloneWanderGoal(ShadowCloneEntity clone, double speed, float probability) {
            super(clone, speed, probability);
            this.clone = clone;
        }
        
        @Override
        public boolean canStart() {
            // Higher chance to move around actively
            if (clone.random.nextFloat() > 0.15F) {
                return false;
            }
            return super.canStart();
        }
        
        @Override
        protected net.minecraft.util.math.Vec3d getWanderTarget() {
            // Get a target further away for more dynamic movement
            net.minecraft.util.math.Vec3d target = net.minecraft.entity.ai.FuzzyTargeting.find(this.clone, 16, 7);
            return target != null ? target : super.getWanderTarget();
        }
    }
}
