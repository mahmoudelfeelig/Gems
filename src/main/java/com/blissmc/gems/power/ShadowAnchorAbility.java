package com.blissmc.gems.power;

import com.blissmc.gems.state.GemsPersistentDataHolder;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.server.network.ServerPlayerEntity;
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
        long now = player.getServerWorld().getTime();

        if (nbt.contains(KEY_ANCHOR_UNTIL, NbtElement.LONG_TYPE) && now <= nbt.getLong(KEY_ANCHOR_UNTIL)) {
            String dim = nbt.getString(KEY_ANCHOR_DIM);
            if (dim.equals(player.getWorld().getRegistryKey().getValue().toString())
                    && nbt.contains(KEY_ANCHOR_POS, NbtElement.COMPOUND_TYPE)) {
                BlockPos pos = net.minecraft.nbt.NbtHelper.toBlockPos(nbt, KEY_ANCHOR_POS).orElse(null);
                if (pos == null) {
                    nbt.remove(KEY_ANCHOR_UNTIL);
                    nbt.remove(KEY_ANCHOR_DIM);
                    nbt.remove(KEY_ANCHOR_POS);
                    player.sendMessage(Text.literal("Anchor was invalid."), true);
                    return true;
                }
                player.teleport(player.getServerWorld(), pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, player.getYaw(), player.getPitch());
                nbt.remove(KEY_ANCHOR_UNTIL);
                nbt.remove(KEY_ANCHOR_DIM);
                nbt.remove(KEY_ANCHOR_POS);
                player.sendMessage(Text.literal("Returned to anchor."), true);
                return true;
            }
        }

        Vec3d current = player.getPos();
        BlockPos anchor = BlockPos.ofFloored(current);
        nbt.putLong(KEY_ANCHOR_UNTIL, now + 10 * 20L);
        nbt.putString(KEY_ANCHOR_DIM, player.getWorld().getRegistryKey().getValue().toString());
        nbt.put(KEY_ANCHOR_POS, net.minecraft.nbt.NbtHelper.fromBlockPos(anchor));
        player.sendMessage(Text.literal("Anchor set (10s)."), true);
        return true;
    }

    private static NbtCompound persistent(ServerPlayerEntity player) {
        return ((GemsPersistentDataHolder) player).gems$getPersistentData();
    }
}
