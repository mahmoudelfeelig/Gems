package com.blissmc.gems.power;

import com.blissmc.gems.state.GemsPersistentDataHolder;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

public final class SoulSystem {
    private static final String KEY_SOUL_TYPE = "soulType";

    private SoulSystem() {
    }

    public static void onKilledMob(ServerPlayerEntity player, LivingEntity killed) {
        if (killed instanceof ServerPlayerEntity) {
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
        entity.refreshPositionAndAngles(pos.x, pos.y, pos.z, player.getYaw(), player.getPitch());
        world.spawnEntity(entity);

        nbt.remove(KEY_SOUL_TYPE);
        if (GemPowers.isPassiveActive(player, PowerIds.SOUL_HEALING)) {
            player.heal(2.0F);
        }
        player.sendMessage(Text.literal("Released soul: " + id), true);
        return true;
    }

    private static NbtCompound persistent(ServerPlayerEntity player) {
        return ((GemsPersistentDataHolder) player).gems$getPersistentData();
    }
}
