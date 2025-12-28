package com.feel.gems.mixin;

import com.feel.gems.legendary.LegendaryDiscounts;
import com.feel.gems.legendary.LegendaryHeadRules;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.CraftingResultInventory;
import net.minecraft.inventory.RecipeInputInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.CraftingRecipe;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(CraftingScreenHandler.class)
public abstract class CraftingScreenHandlerDiscountMixin {
    @Inject(method = "updateResult", at = @At("TAIL"))
    private static void gems$gateDiscounts(ScreenHandler handler, World world, PlayerEntity player, RecipeInputInventory input, CraftingResultInventory result, RecipeEntry<CraftingRecipe> recipe, CallbackInfo ci) {
        if (!(player instanceof ServerPlayerEntity serverPlayer)) {
            return;
        }
        CraftingRecipeInput recipeInput = input.createRecipeInput();
        RecipeEntry<CraftingRecipe> chosen = null;
        for (RecipeEntry<CraftingRecipe> entry : world.getRecipeManager().getAllMatches(RecipeType.CRAFTING, recipeInput, world)) {
            if (!isAllowed(serverPlayer, input, entry.id())) {
                continue;
            }
            if (LegendaryDiscounts.isDiscountRecipe(entry.id()) && LegendaryDiscounts.canUseDiscount(serverPlayer, entry.id())) {
                chosen = entry;
                break;
            }
            if (chosen == null) {
                chosen = entry;
            }
        }
        if (chosen == null) {
            result.setStack(0, ItemStack.EMPTY);
            return;
        }
        ItemStack output = chosen.value().craft(recipeInput, world.getRegistryManager());
        if (output.isEmpty()) {
            result.setStack(0, ItemStack.EMPTY);
            return;
        }
        result.setStack(0, output);
        result.markDirty();
        result.setLastRecipe(chosen);
        handler.setPreviousTrackedSlot(0, output);
        serverPlayer.networkHandler.sendPacket(new ScreenHandlerSlotUpdateS2CPacket(
                handler.syncId,
                handler.nextRevision(),
                0,
                output
        ));
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
