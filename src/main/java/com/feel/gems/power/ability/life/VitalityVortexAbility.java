package com.feel.gems.power.ability.life;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.gem.voidgem.VoidImmunity;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import com.feel.gems.trust.GemTrust;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;



public final class VitalityVortexAbility implements GemAbility {
    private enum VortexMode {
        DEFAULT("Default"),
        AQUATIC("Aquatic"),
        INFERNAL("Infernal"),
        SCULK("Sculk"),
        VERDANT("Verdant"),
        END("End");

        private final String label;

        VortexMode(String label) {
            this.label = label;
        }
    }

    @Override
    public Identifier id() {
        return PowerIds.VITALITY_VORTEX;
    }

    @Override
    public String name() {
        return "Vitality Vortex";
    }

    @Override
    public String description() {
        return "Area pulse that buffs trusted players and weakens enemies based on your surroundings.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().life().vitalityVortexCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        ServerWorld world = player.getEntityWorld();
        VortexMode mode = detectMode(world, player);
        int duration = GemsBalance.v().life().vitalityVortexDurationTicks();
        int radius = GemsBalance.v().life().vitalityVortexRadiusBlocks();
        Box box = new Box(player.getBlockPos()).expand(radius);
        for (LivingEntity other : world.getEntitiesByClass(LivingEntity.class, box, e -> e.isAlive() && e != player)) {
            boolean ally = other instanceof ServerPlayerEntity otherPlayer && GemTrust.isTrusted(player, otherPlayer);
            if (ally) {
                applyAlly(mode, other, duration);
            } else {
                if (other instanceof ServerPlayerEntity otherPlayer && !VoidImmunity.canBeTargeted(player, otherPlayer)) {
                    continue;
                }
                applyEnemy(mode, other, duration);
            }
        }

        AbilityFeedback.sound(player, SoundEvents.BLOCK_BEACON_POWER_SELECT, 0.8F, 1.1F);
        AbilityFeedback.burst(player, particleFor(mode), 20, 0.35D);
        player.sendMessage(Text.translatable("gems.ability.life.vitality_vortex.mode", mode.label), true);
        return true;
    }

    private static net.minecraft.particle.ParticleEffect particleFor(VortexMode mode) {
        return switch (mode) {
            case AQUATIC -> ParticleTypes.BUBBLE;
            case INFERNAL -> ParticleTypes.FLAME;
            case SCULK -> ParticleTypes.SCULK_SOUL;
            case VERDANT -> ParticleTypes.HAPPY_VILLAGER;
            case END -> ParticleTypes.PORTAL;
            default -> ParticleTypes.HEART;
        };
    }

    private static void applyAlly(VortexMode mode, LivingEntity target, int duration) {
        float heal = GemsBalance.v().life().vitalityVortexAllyHeal();
        switch (mode) {
            case AQUATIC -> {
                target.heal(heal);
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, duration, 0, true, false, false));
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.WATER_BREATHING, duration, 0, true, false, false));
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.DOLPHINS_GRACE, duration, 0, true, false, false));
            }
            case INFERNAL -> {
                target.heal(heal);
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, duration, 0, true, false, false));
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, duration, 0, true, false, false));
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, duration, 0, true, false, false));
            }
            case SCULK -> {
                target.heal(heal);
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, duration, 0, true, false, false));
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, duration, 0, true, false, false));
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, duration, 0, true, false, false));
            }
            case VERDANT -> {
                target.heal(heal);
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, duration, 1, true, false, false));
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, duration, 0, true, false, false));
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.SATURATION, 40, 0, true, false, false));
            }
            case END -> {
                target.heal(heal);
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, duration, 0, true, false, false));
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, duration, 0, true, false, false));
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, duration, 0, true, false, false));
            }
            default -> {
                target.heal(heal);
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, duration, 1, true, false, false));
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, duration, 0, true, false, false));
            }
        }
    }

    private static void applyEnemy(VortexMode mode, LivingEntity target, int duration) {
        switch (mode) {
            case AQUATIC -> {
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, duration, 1, true, false, false));
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, duration, 0, true, false, false));
            }
            case INFERNAL -> {
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, duration, 0, true, false, false));
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, duration, 0, true, false, false));
            }
            case SCULK -> {
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, duration, 0, true, false, false));
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, duration, 1, true, false, false));
            }
            case VERDANT -> {
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, duration, 1, true, false, false));
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, duration, 0, true, false, false));
            }
            case END -> {
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, duration, 1, true, false, false));
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, duration, 0, true, false, false));
            }
            default -> {
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, duration, 0, true, false, false));
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, duration, 0, true, false, false));
            }
        }
    }

    private static VortexMode detectMode(ServerWorld world, ServerPlayerEntity player) {
        if (world.getRegistryKey() == World.NETHER) {
            return VortexMode.INFERNAL;
        }
        if (world.getRegistryKey() == World.END) {
            return VortexMode.END;
        }

        BlockPos center = player.getBlockPos();
        BlockPos.Mutable pos = new BlockPos.Mutable();

        int water = 0;
        int lava = 0;
        int sculk = 0;
        int verdant = 0;

        int r = GemsBalance.v().life().vitalityVortexScanRadiusBlocks();
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    pos.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    BlockState state = world.getBlockState(pos);
                    var fluid = state.getFluidState();
                    if (fluid.isIn(FluidTags.LAVA)) {
                        lava++;
                    } else if (fluid.isIn(FluidTags.WATER)) {
                        water++;
                    }

                    if (isSculk(state)) {
                        sculk++;
                    } else if (isVerdant(state)) {
                        verdant++;
                    }
                }
            }
        }

        if (lava > 0) {
            return VortexMode.INFERNAL;
        }
        if (water > 0 || player.isSubmergedInWater()) {
            return VortexMode.AQUATIC;
        }
        if (sculk > 0) {
            return VortexMode.SCULK;
        }
        if (verdant >= GemsBalance.v().life().vitalityVortexVerdantThreshold()) {
            return VortexMode.VERDANT;
        }
        return VortexMode.DEFAULT;
    }

    private static boolean isSculk(BlockState state) {
        return state.isOf(Blocks.SCULK)
                || state.isOf(Blocks.SCULK_VEIN)
                || state.isOf(Blocks.SCULK_SENSOR)
                || state.isOf(Blocks.CALIBRATED_SCULK_SENSOR)
                || state.isOf(Blocks.SCULK_SHRIEKER)
                || state.isOf(Blocks.SCULK_CATALYST);
    }

    private static boolean isVerdant(BlockState state) {
        return state.isIn(BlockTags.LEAVES)
                || state.isIn(BlockTags.FLOWERS)
                || state.isOf(Blocks.GRASS_BLOCK)
                || state.isOf(Blocks.TALL_GRASS)
                || state.isOf(Blocks.FERN)
                || state.isOf(Blocks.LARGE_FERN)
                || state.isOf(Blocks.VINE);
    }
}
