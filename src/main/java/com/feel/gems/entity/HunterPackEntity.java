package com.feel.gems.entity;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.trust.GemTrust;
import java.util.List;
import java.util.UUID;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.World;

/**
 * Six-Pack Pain entity - a player clone that shares health with other pack members.
 * When hit, gives owner regeneration + random buff, attacker gets debuff.
 * Targets untrusted players and hostile mobs.
 */
public class HunterPackEntity extends PathAwareEntity {
    
    private UUID ownerUuid;
    private String ownerName;
    private UUID packId; // Shared ID for all clones in a pack
    
    public HunterPackEntity(EntityType<? extends HunterPackEntity> type, World world) {
        super(type, world);
    }
    
    private int ticksAlive = 0;
    private int maxLifetimeTicks = 200; // 10 seconds default, set by ability
    
    /**
     * 6 clones with 120 hearts shared = 240 HP total / 6 = 40 HP each
     */
    public static DefaultAttributeContainer.Builder createAttributes() {
        return PathAwareEntity.createMobAttributes()
                .add(EntityAttributes.MAX_HEALTH, 40.0D) // 40 HP each, 240 total shared (120 hearts)
                .add(EntityAttributes.MOVEMENT_SPEED, 0.35D) // Fast like wolves
                .add(EntityAttributes.ATTACK_DAMAGE, 4.0D) // 2 hearts
                .add(EntityAttributes.FOLLOW_RANGE, 35.0D)
                .add(EntityAttributes.ARMOR, 7.5D); // Iron armor equivalent
    }
    
    @Override
    protected void initGoals() {
        var cfg = GemsBalance.v().hunter();
        int closeRange = cfg.sixPackPainCloseTargetRangeBlocks();
        int wideRange = cfg.sixPackPainWideTargetRangeBlocks();
        
        // Attack goals
        this.goalSelector.add(1, new MeleeAttackGoal(this, 1.2D, true));
        this.goalSelector.add(2, new FollowOwnerGoal(this, 1.0D, 10.0F, 2.0F));
        this.goalSelector.add(3, new WanderAroundFarGoal(this, 0.8D, 0.001F));
        this.goalSelector.add(4, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F));
        this.goalSelector.add(5, new LookAroundGoal(this));
        
        // Target goals - priority order as specified
        this.targetSelector.add(1, new RevengeGoal(this));
        this.targetSelector.add(2, new TargetUntrustedPlayerGoal(this, closeRange)); // Close range first
        this.targetSelector.add(3, new TargetUntrustedPlayerGoal(this, wideRange)); // Then wider range
        this.targetSelector.add(4, new ActiveTargetGoal<>(this, HostileEntity.class, true));
    }
    
    public void setOwner(ServerPlayerEntity owner, UUID packId) {
        this.ownerUuid = owner.getUuid();
        this.ownerName = owner.getGameProfile().name();
        this.packId = packId;
        this.setCustomName(owner.getDisplayName());
        this.setCustomNameVisible(true); // Always show name tag
        
        // Copy equipment from owner
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.isArmorSlot() || slot == EquipmentSlot.MAINHAND || slot == EquipmentSlot.OFFHAND) {
                ItemStack stack = owner.getEquippedStack(slot);
                this.equipStack(slot, stack.copy());
            }
        }
    }
    
    public void setMaxLifetime(int ticks) {
        this.maxLifetimeTicks = ticks;
    }
    
    @Override
    public void tick() {
        super.tick();
        ticksAlive++;
        
        // Auto-despawn after max lifetime
        if (!this.getEntityWorld().isClient() && ticksAlive >= maxLifetimeTicks) {
            this.discard();
        }
    }
    
    public UUID getOwnerUuid() {
        return ownerUuid;
    }
    
    public String getOwnerName() {
        return ownerName;
    }
    
    public UUID getPackId() {
        return packId;
    }
    
    @Override
    public boolean damage(ServerWorld world, DamageSource source, float amount) {
        // Apply damage to shared health pool
        boolean result = distributePackDamage(world, source, amount);
        
        // Give owner buffs and attacker debuff
        if (result && source.getAttacker() instanceof LivingEntity attacker) {
            applyHitEffects(world, attacker);
        }
        
        return result;
    }
    
    private boolean distributePackDamage(ServerWorld world, DamageSource source, float amount) {
        if (packId == null) {
            return super.damage(world, source, amount);
        }
        
        // Find all pack members
        List<HunterPackEntity> packMembers = getPackMembers(world);
        if (packMembers.isEmpty()) {
            return super.damage(world, source, amount);
        }
        
        // Calculate total pack health (shared 120 hearts = 240 HP pool)
        float totalHealth = 0;
        for (HunterPackEntity member : packMembers) {
            totalHealth += member.getHealth();
        }
        
        // Apply damage to the shared pool
        float newTotalHealth = totalHealth - amount;
        
        if (newTotalHealth <= 0) {
            // Kill all pack members simultaneously
            for (HunterPackEntity member : packMembers) {
                member.setHealth(0);
                if (member != this) {
                    member.discard();
                }
            }
            // Let this one die properly to trigger death events
            return super.damage(world, source, this.getHealth() + 1);
        }
        
        // Distribute remaining health evenly
        float healthPerMember = newTotalHealth / packMembers.size();
        for (HunterPackEntity member : packMembers) {
            member.setHealth(Math.max(1, healthPerMember));
        }
        
        // Play hurt animation on this one
        this.playHurtSound(source);
        return true;
    }
    
    private List<HunterPackEntity> getPackMembers(ServerWorld world) {
        if (packId == null) return List.of(this);
        
        return world.getEntitiesByClass(
            HunterPackEntity.class,
            this.getBoundingBox().expand(50),
            e -> packId.equals(e.getPackId()) && e.isAlive()
        );
    }
    
    private void applyHitEffects(ServerWorld world, LivingEntity attacker) {
        var cfg = GemsBalance.v().hunter();
        int buffDuration = cfg.sixPackPainBuffDurationTicks();
        int debuffDuration = cfg.sixPackPainDebuffDurationTicks();
        
        // Give owner regeneration + random buff
        ServerPlayerEntity owner = getOwner(world);
        if (owner != null) {
            // Regeneration for buff duration
            owner.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, buffDuration, 1));
            
            // Random positive effect for buff duration
            var positiveEffects = List.of(
                StatusEffects.SPEED,
                StatusEffects.STRENGTH,
                StatusEffects.RESISTANCE,
                StatusEffects.FIRE_RESISTANCE,
                StatusEffects.ABSORPTION,
                StatusEffects.HASTE
            );
            var randomEffect = positiveEffects.get(random.nextInt(positiveEffects.size()));
            owner.addStatusEffect(new StatusEffectInstance(randomEffect, buffDuration, 0));
        }
        
        // Give attacker a debuff
        var debuffs = List.of(
            StatusEffects.SLOWNESS,
            StatusEffects.WEAKNESS,
            StatusEffects.MINING_FATIGUE,
            StatusEffects.BLINDNESS,
            StatusEffects.NAUSEA,
            StatusEffects.GLOWING
        );
        var randomDebuff = debuffs.get(random.nextInt(debuffs.size()));
        attacker.addStatusEffect(new StatusEffectInstance(randomDebuff, debuffDuration, 0));
    }
    
    private ServerPlayerEntity getOwner(ServerWorld world) {
        if (ownerUuid == null) return null;
        MinecraftServer server = world.getServer();
        if (server == null) return null;
        return server.getPlayerManager().getPlayer(ownerUuid);
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
    
    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ENTITY_FOX_AMBIENT;
    }
    
    /**
     * Goal to follow the owner when no target
     */
    private static class FollowOwnerGoal extends net.minecraft.entity.ai.goal.Goal {
        private final HunterPackEntity pack;
        private final double speed;
        private final float maxDistance;
        private final float minDistance;
        private ServerPlayerEntity owner;
        private int updateCountdownTicks;
        
        public FollowOwnerGoal(HunterPackEntity pack, double speed, float maxDistance, float minDistance) {
            this.pack = pack;
            this.speed = speed;
            this.maxDistance = maxDistance;
            this.minDistance = minDistance;
            this.setControls(java.util.EnumSet.of(Control.MOVE, Control.LOOK));
        }
        
        @Override
        public boolean canStart() {
            if (pack.getTarget() != null) return false;
            if (!(pack.getEntityWorld() instanceof ServerWorld world)) return false;
            
            this.owner = pack.getOwner(world);
            if (owner == null) return false;
            if (pack.squaredDistanceTo(owner) < minDistance * minDistance) return false;
            
            return true;
        }
        
        @Override
        public boolean shouldContinue() {
            if (pack.getTarget() != null) return false;
            if (owner == null || !owner.isAlive()) return false;
            return pack.squaredDistanceTo(owner) > minDistance * minDistance;
        }
        
        @Override
        public void start() {
            this.updateCountdownTicks = 0;
        }
        
        @Override
        public void stop() {
            this.owner = null;
            pack.getNavigation().stop();
        }
        
        @Override
        public void tick() {
            pack.getLookControl().lookAt(owner, 10.0F, pack.getMaxLookPitchChange());
            if (--updateCountdownTicks <= 0) {
                updateCountdownTicks = 10;
                if (!pack.isLeashed() && !pack.hasVehicle()) {
                    if (pack.squaredDistanceTo(owner) >= maxDistance * maxDistance) {
                        // Teleport if too far
                        pack.refreshPositionAndAngles(
                            owner.getX() + (pack.random.nextDouble() - 0.5) * 2,
                            owner.getY(),
                            owner.getZ() + (pack.random.nextDouble() - 0.5) * 2,
                            pack.getYaw(),
                            pack.getPitch()
                        );
                    } else {
                        pack.getNavigation().startMovingTo(owner, speed);
                    }
                }
            }
        }
    }
    
    /**
     * Goal to target untrusted players within a specific range
     */
    private static class TargetUntrustedPlayerGoal extends ActiveTargetGoal<PlayerEntity> {
        private final HunterPackEntity pack;
        private final int maxRange;
        
        public TargetUntrustedPlayerGoal(HunterPackEntity pack, int maxRange) {
            super(pack, PlayerEntity.class, true);
            this.pack = pack;
            this.maxRange = maxRange;
        }
        
        @Override
        public boolean canStart() {
            if (pack.ownerUuid == null) return false;
            if (!(pack.getEntityWorld() instanceof ServerWorld world)) return false;
            
            ServerPlayerEntity owner = pack.getOwner(world);
            if (owner == null) return false;
            
            // Find closest untrusted player within range
            PlayerEntity closest = null;
            double closestDistSq = maxRange * maxRange;
            
            for (PlayerEntity player : world.getPlayers()) {
                if (player.getUuid().equals(pack.ownerUuid)) continue;
                if (player instanceof ServerPlayerEntity sp && GemTrust.isTrusted(owner, sp)) continue;
                if (player.isSpectator() || player.isCreative()) continue;
                
                double distSq = pack.squaredDistanceTo(player);
                if (distSq < closestDistSq) {
                    closest = player;
                    closestDistSq = distSq;
                }
            }
            
            if (closest != null) {
                this.targetEntity = closest;
                return true;
            }
            return false;
        }
    }
}
