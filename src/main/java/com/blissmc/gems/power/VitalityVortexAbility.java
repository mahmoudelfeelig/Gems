package com.blissmc.gems.power;

import com.blissmc.gems.config.GemsBalance;
import com.blissmc.gems.trust.GemTrust;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.FluidTags;

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
        ServerWorld world = player.getServerWorld();
        VortexMode mode = detectMode(world, player);
        int duration = GemsBalance.v().life().vitalityVortexDurationTicks();
        int radius = GemsBalance.v().life().vitalityVortexRadiusBlocks();

        for (ServerPlayerEntity other : world.getPlayers(p -> p.squaredDistanceTo(player) <= radius * (double) radius)) {
            if (GemTrust.isTrusted(player, other)) {
                applyAlly(mode, other, duration);
            } else {
                applyEnemy(mode, other, duration);
            }
        }

        player.sendMessage(Text.literal("Vitality Vortex: " + mode.label), true);
        return true;
    }

    private static void applyAlly(VortexMode mode, ServerPlayerEntity player, int duration) {
        float heal = GemsBalance.v().life().vitalityVortexAllyHeal();
        switch (mode) {
            case AQUATIC -> {
                player.heal(heal);
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, duration, 0, true, false, false));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.WATER_BREATHING, duration, 0, true, false, false));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.DOLPHINS_GRACE, duration, 0, true, false, false));
            }
            case INFERNAL -> {
                player.heal(heal);
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, duration, 0, true, false, false));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, duration, 0, true, false, false));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, duration, 0, true, false, false));
            }
            case SCULK -> {
                player.heal(heal);
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, duration, 0, true, false, false));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, duration, 0, true, false, false));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, duration, 0, true, false, false));
            }
            case VERDANT -> {
                player.heal(heal);
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, duration, 1, true, false, false));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, duration, 0, true, false, false));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.SATURATION, 40, 0, true, false, false));
            }
            case END -> {
                player.heal(heal);
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, duration, 0, true, false, false));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, duration, 0, true, false, false));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, duration, 0, true, false, false));
            }
            default -> {
                player.heal(heal);
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, duration, 1, true, false, false));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, duration, 0, true, false, false));
            }
        }
    }

    private static void applyEnemy(VortexMode mode, ServerPlayerEntity player, int duration) {
        switch (mode) {
            case AQUATIC -> {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, duration, 1, true, false, false));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, duration, 0, true, false, false));
            }
            case INFERNAL -> {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, duration, 0, true, false, false));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, duration, 0, true, false, false));
            }
            case SCULK -> {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, duration, 0, true, false, false));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, duration, 1, true, false, false));
            }
            case VERDANT -> {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, duration, 1, true, false, false));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, duration, 0, true, false, false));
            }
            case END -> {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, duration, 1, true, false, false));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, duration, 0, true, false, false));
            }
            default -> {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, duration, 0, true, false, false));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, duration, 0, true, false, false));
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
