package com.blissmc.gems.power;

import com.blissmc.gems.state.GemsPersistentDataHolder;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundEvents;

import java.util.UUID;

public final class SoulSystem {
    private static final String KEY_SOUL_TYPE = "soulType";

    private SoulSystem() {
    }

    public static void onKilledMob(ServerPlayerEntity player, LivingEntity killed) {
        if (killed instanceof ServerPlayerEntity) {
            return;
        }
        if (SoulSummons.isSoul(killed)) {
            return;
        }
        if (!GemPowers.isPassiveActive(player, PowerIds.SOUL_CAPTURE)) {
            return;
        }

        Identifier typeId = Registries.ENTITY_TYPE.getId(killed.getType());
        if (typeId == null) {
            return;
        }
        persistent(player).putString(KEY_SOUL_TYPE, typeId.toString());
        com.blissmc.gems.net.GemExtraStateSync.send(player);
        AbilityFeedback.burst(player, ParticleTypes.SCULK_SOUL, 8, 0.25D);

        if (GemPowers.isPassiveActive(player, PowerIds.SOUL_HEALING)) {
            player.heal(2.0F);
        }
    }

    public static boolean hasSoul(ServerPlayerEntity player) {
        return persistent(player).contains(KEY_SOUL_TYPE, NbtElement.STRING_TYPE);
    }

    public static String soulType(ServerPlayerEntity player) {
        return persistent(player).getString(KEY_SOUL_TYPE);
    }

    public static boolean release(ServerPlayerEntity player) {
        if (!GemPowers.isPassiveActive(player, PowerIds.SOUL_CAPTURE)) {
            player.sendMessage(Text.literal("Soul Capture is not active."), true);
            return false;
        }
        NbtCompound nbt = persistent(player);
        if (!nbt.contains(KEY_SOUL_TYPE, NbtElement.STRING_TYPE)) {
            player.sendMessage(Text.literal("No captured soul."), true);
            return false;
        }
        Identifier id = Identifier.tryParse(nbt.getString(KEY_SOUL_TYPE));
        if (id == null) {
            nbt.remove(KEY_SOUL_TYPE);
            player.sendMessage(Text.literal("Captured soul was invalid and was cleared."), true);
            return false;
        }
        EntityType<?> type = Registries.ENTITY_TYPE.get(id);
        ServerWorld world = player.getServerWorld();

        Vec3d pos = player.getPos().add(player.getRotationVec(1.0F).multiply(2.0D));
        Entity entity = type.create(world);
        if (entity == null) {
            player.sendMessage(Text.literal("Cannot summon: " + id), true);
            return false;
        }
        SoulSummons.mark(entity, player.getUuid());
        if (entity instanceof LivingEntity living) {
            living.disableExperienceDropping();
        }
        entity.refreshPositionAndAngles(pos.x, pos.y, pos.z, player.getYaw(), player.getPitch());
        world.spawnEntity(entity);

        if (entity instanceof HostileEntity hostile) {
            addHostileTargeting(hostile, player.getUuid());
        }

        nbt.remove(KEY_SOUL_TYPE);
        com.blissmc.gems.net.GemExtraStateSync.send(player);
        if (GemPowers.isPassiveActive(player, PowerIds.SOUL_HEALING)) {
            player.heal(2.0F);
        }
        AbilityFeedback.sound(player, SoundEvents.ITEM_TOTEM_USE, 0.8F, 1.4F);
        AbilityFeedback.burstAt(world, pos.add(0.0D, 1.0D, 0.0D), ParticleTypes.SCULK_SOUL, 18, 0.35D);
        player.sendMessage(Text.literal("Released soul: " + id), true);
        return true;
    }

    private static void addHostileTargeting(HostileEntity mob, UUID ownerUuid) {
        if (!(mob.getWorld() instanceof ServerWorld world)) {
            return;
        }
        ServerPlayerEntity owner = world.getServer().getPlayerManager().getPlayer(ownerUuid);
        if (owner == null) {
            return;
        }

        // Best-effort: make hostile summons target untrusted players, while excluding the owner and trusted players.
        ((com.blissmc.gems.mixin.accessor.MobEntitySelectorsAccessor) mob).gems$getTargetSelector().add(1,
                new ActiveTargetGoal<>(mob, ServerPlayerEntity.class, true, candidate -> {
                    if (!(candidate instanceof ServerPlayerEntity p)) {
                        return false;
                    }
                    return !com.blissmc.gems.trust.GemTrust.isTrusted(owner, p);
                })
        );
    }

    private static NbtCompound persistent(ServerPlayerEntity player) {
        return ((GemsPersistentDataHolder) player).gems$getPersistentData();
    }
}
