package com.feel.gems.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.world.World;

/**
 * Stationary test dummy that mimics a player-sized target for ability/item testing.
 */
public final class TestDummyEntity extends PathAwareEntity {
    private static final Text DEFAULT_NAME = Text.literal("Test Dummy");

    public TestDummyEntity(EntityType<? extends TestDummyEntity> type, World world) {
        super(type, world);
        this.setPersistent();
        this.setCustomName(DEFAULT_NAME);
        this.setCustomNameVisible(true);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return PathAwareEntity.createMobAttributes()
                .add(EntityAttributes.MAX_HEALTH, 200.0D)
                .add(EntityAttributes.MOVEMENT_SPEED, 0.0D)
                .add(EntityAttributes.ARMOR, 10.0D);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(1, new LookAtEntityGoal(this, PlayerEntity.class, 10.0F));
        this.goalSelector.add(2, new LookAroundGoal(this));
    }

    @Override
    public boolean isPersistent() {
        return true;
    }

    @Override
    public boolean cannotDespawn() {
        return true;
    }

    @Override
    protected void dropInventory(ServerWorld world) {
        // No drops.
    }

    @Override
    protected void dropEquipment(ServerWorld world, DamageSource source, boolean causedByPlayer) {
        // No drops.
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENTITY_ARMOR_STAND_HIT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_ARMOR_STAND_BREAK;
    }
}
