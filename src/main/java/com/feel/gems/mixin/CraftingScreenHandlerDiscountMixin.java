package com.feel.gems.mixin;

import com.feel.gems.legendary.LegendaryDiscounts;
import com.feel.gems.legendary.LegendaryHeadRules;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.CraftingResultInventory;
import net.minecraft.inventory.RecipeInputInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.CraftingRecipe;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(CraftingScreenHandler.class)
public abstract class CraftingScreenHandlerDiscountMixin {
    @Inject(method = "updateResult", at = @At("TAIL"))
    private static void gems$gateDiscounts(ScreenHandler handler, ServerWorld world, PlayerEntity player, RecipeInputInventory input, CraftingResultInventory result, RecipeEntry<CraftingRecipe> recipe, CallbackInfo ci) {
        if (!(player instanceof ServerPlayerEntity serverPlayer)) {
            return;
        }
        if (recipe == null) {
            return;
        }

        net.minecraft.util.Identifier id = recipe.id().getValue();
        if (!isAllowed(serverPlayer, input, id)) {
            result.setStack(0, ItemStack.EMPTY);
            result.markDirty();
            handler.sendContentUpdates();
            return;
        }
    }

    private static boolean isAllowed(ServerPlayerEntity player, RecipeInputInventory input, net.minecraft.util.Identifier id) {
        if (!LegendaryHeadRules.canCraft(player, input, id)) {
            return false;
        }
        if (LegendaryDiscounts.isDiscountRecipe(id) && !LegendaryDiscounts.canUseDiscount(player, id)) {
            return false;
        }
        return true;
    }
}
