package com.feel.gems.legendary;

import com.feel.gems.GemsMod;
import com.mojang.authlib.GameProfile;
import java.util.Set;
import java.util.UUID;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.inventory.RecipeInputInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public final class LegendaryHeadRules {
    private static final Set<Identifier> PLAYER_HEAD_RECIPES = Set.of(
            id("blood_oath_blade"),
            id("blood_oath_blade_discount"),
            id("tracker_compass"),
            id("tracker_compass_discount")
    );

    private LegendaryHeadRules() {
    }

    public static boolean canCraft(ServerPlayerEntity player, RecipeInputInventory input, Identifier recipeId) {
        if (!PLAYER_HEAD_RECIPES.contains(recipeId)) {
            return true;
        }
        UUID playerId = player.getUuid();
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getStack(slot);
            if (stack.isEmpty() || !stack.isOf(Items.PLAYER_HEAD)) {
                continue;
            }
            ProfileComponent profile = stack.get(DataComponentTypes.PROFILE);
            if (profile == null) {
                return false;
            }
            GameProfile gameProfile = profile.gameProfile();
            if (gameProfile == null || gameProfile.getId() == null) {
                return false;
            }
            if (playerId.equals(gameProfile.getId())) {
                return false;
            }
        }
        return true;
    }

    private static Identifier id(String path) {
        return Identifier.of(GemsMod.MOD_ID, path);
    }
}
