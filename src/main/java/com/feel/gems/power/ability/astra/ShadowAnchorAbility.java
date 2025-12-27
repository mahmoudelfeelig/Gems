package com.feel.gems.power.ability.astra;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.core.GemDefinition;
import com.feel.gems.core.GemId;
import com.feel.gems.core.GemRegistry;
import com.feel.gems.net.AbilityCooldownPayload;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import com.feel.gems.power.runtime.GemAbilityCooldowns;
import com.feel.gems.state.GemsPersistentDataHolder;
import com.feel.gems.util.GemsTime;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;



public final class ShadowAnchorAbility implements GemAbility {
    private static final String KEY_ANCHOR_UNTIL = "shadowAnchorUntil";
    private static final String KEY_ANCHOR_DIM = "shadowAnchorDim";
    private static final String KEY_ANCHOR_POS = "shadowAnchorPos";

    @Override
    public Identifier id() {
        return PowerIds.SHADOW_ANCHOR;
    }

    @Override
    public String name() {
        return "Shadow Anchor";
    }

    @Override
    public String description() {
        return "Shadow Anchor: press once to set an anchor; press again quickly to return.";
    }

    @Override
    public int cooldownTicks() {
        return 0; // 2-stage: set anchor then return
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        NbtCompound nbt = persistent(player);
        long now = GemsTime.now(player);

        if (nbt.contains(KEY_ANCHOR_UNTIL, NbtElement.LONG_TYPE) && now <= nbt.getLong(KEY_ANCHOR_UNTIL)) {
            if (player.isSneaking()) {
                nbt.remove(KEY_ANCHOR_UNTIL);
                nbt.remove(KEY_ANCHOR_DIM);
                nbt.remove(KEY_ANCHOR_POS);
                player.sendMessage(Text.literal("Anchor cleared."), true);
                startPostCooldown(player, now);
                return true;
            }

            String dim = nbt.getString(KEY_ANCHOR_DIM);
            if (dim.equals(player.getWorld().getRegistryKey().getValue().toString())
                    && nbt.contains(KEY_ANCHOR_POS)) {
                BlockPos pos = net.minecraft.nbt.NbtHelper.toBlockPos(nbt, KEY_ANCHOR_POS).orElse(null);
                if (pos == null) {
                    nbt.remove(KEY_ANCHOR_UNTIL);
                    nbt.remove(KEY_ANCHOR_DIM);
                    nbt.remove(KEY_ANCHOR_POS);
                    player.sendMessage(Text.literal("Anchor was invalid."), true);
                    startPostCooldown(player, now);
                    return true;
                }
                Vec3d from = player.getPos();
                player.teleport(player.getServerWorld(), pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, player.getYaw(), player.getPitch());
                AbilityFeedback.sound(player, SoundEvents.ENTITY_ENDERMAN_TELEPORT, 0.9F, 1.1F);
                AbilityFeedback.burstAt(player.getServerWorld(), from.add(0.0D, 1.0D, 0.0D), ParticleTypes.PORTAL, 20, 0.4D);
                AbilityFeedback.burst(player, ParticleTypes.PORTAL, 20, 0.4D);
                nbt.remove(KEY_ANCHOR_UNTIL);
                nbt.remove(KEY_ANCHOR_DIM);
                nbt.remove(KEY_ANCHOR_POS);
                player.sendMessage(Text.literal("Returned to anchor."), true);
                startPostCooldown(player, now);
                return true;
            }
        }

        // If an old anchor expired, clear it and apply post-cooldown.
        if (nbt.contains(KEY_ANCHOR_UNTIL, NbtElement.LONG_TYPE)) {
            long until = nbt.getLong(KEY_ANCHOR_UNTIL);
            if (until > 0 && now > until) {
                clearAnchor(nbt);
                player.sendMessage(Text.literal("Anchor expired."), true);
                startPostCooldown(player, now);
                return false;
            }
        }

        Vec3d current = player.getPos();
        BlockPos anchor = BlockPos.ofFloored(current);
        nbt.putLong(KEY_ANCHOR_UNTIL, now + GemsBalance.v().astra().shadowAnchorWindowTicks());
        nbt.putString(KEY_ANCHOR_DIM, player.getWorld().getRegistryKey().getValue().toString());
        nbt.put(KEY_ANCHOR_POS, net.minecraft.nbt.NbtHelper.fromBlockPos(anchor));
        AbilityFeedback.sound(player, SoundEvents.BLOCK_END_PORTAL_FRAME_FILL, 0.7F, 1.3F);
        AbilityFeedback.burst(player, ParticleTypes.PORTAL, 12, 0.2D);
        player.sendMessage(Text.literal("Anchor set."), true);
        return true;
    }

    public static void tick(ServerPlayerEntity player, long now) {
        NbtCompound nbt = persistent(player);
        if (!nbt.contains(KEY_ANCHOR_UNTIL, NbtElement.LONG_TYPE)) {
            return;
        }
        long until = nbt.getLong(KEY_ANCHOR_UNTIL);
        if (until <= 0 || now <= until) {
            return;
        }
        clearAnchor(nbt);
        startPostCooldown(player, now);
    }

    private static void startPostCooldown(ServerPlayerEntity player, long now) {
        int cooldown = GemsBalance.v().astra().shadowAnchorPostCooldownTicks();
        if (cooldown <= 0) {
            return;
        }
        GemAbilityCooldowns.setNextAllowedTick(player, PowerIds.SHADOW_ANCHOR, now + cooldown);

        // Best-effort client HUD sync.
        GemDefinition def = GemRegistry.definition(GemId.ASTRA);
        int index = def.abilities().indexOf(PowerIds.SHADOW_ANCHOR);
        if (index >= 0) {
            ServerPlayNetworking.send(player, new AbilityCooldownPayload(GemId.ASTRA.ordinal(), index, cooldown));
        }
    }

    private static void clearAnchor(NbtCompound nbt) {
        nbt.remove(KEY_ANCHOR_UNTIL);
        nbt.remove(KEY_ANCHOR_DIM);
        nbt.remove(KEY_ANCHOR_POS);
    }

    private static NbtCompound persistent(ServerPlayerEntity player) {
        return ((GemsPersistentDataHolder) player).gems$getPersistentData();
    }
}
