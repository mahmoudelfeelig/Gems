package com.feel.gems.item;

import com.feel.gems.augment.AugmentDefinition;
import com.feel.gems.augment.AugmentInstance;
import com.feel.gems.augment.AugmentRegistry;
import com.feel.gems.augment.AugmentRuntime;
import com.feel.gems.augment.AugmentTarget;
import com.feel.gems.legendary.LegendaryItem;
import java.util.function.Consumer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

public final class AugmentItem extends Item {
    private final String augmentId;

    public AugmentItem(String augmentId, Settings settings) {
        super(settings.maxCount(16));
        this.augmentId = augmentId;
    }

    public String augmentId() {
        return augmentId;
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        if (world.isClient()) {
            return ActionResult.SUCCESS;
        }
        if (!(user instanceof ServerPlayerEntity player)) {
            return ActionResult.PASS;
        }
        ItemStack stack = user.getStackInHand(hand);
        AugmentInstance instance = AugmentRuntime.getInstance(stack);
        if (instance == null) {
            instance = AugmentRuntime.rollInstance(augmentId);
            if (instance != null) {
                AugmentRuntime.assignInstance(stack, instance);
            }
        }
        if (instance == null) {
            return ActionResult.PASS;
        }

        AugmentDefinition def = AugmentRegistry.get(instance.augmentId());
        if (def == null) {
            return ActionResult.PASS;
        }

        if (def.target() == AugmentTarget.GEM) {
            ItemStack target = hand == Hand.MAIN_HAND ? user.getOffHandStack() : user.getMainHandStack();
            if (!(target.getItem() instanceof GemItem)) {
                player.sendMessage(Text.translatable("gems.augment.need_gem").formatted(Formatting.RED), true);
                return ActionResult.FAIL;
            }
            if (AugmentRuntime.applyGemAugment(player, target, instance)) {
                consume(player, stack);
                return ActionResult.SUCCESS;
            }
            return ActionResult.FAIL;
        }

        ItemStack target = hand == Hand.MAIN_HAND ? user.getOffHandStack() : user.getMainHandStack();
        if (!(target.getItem() instanceof LegendaryItem)) {
            player.sendMessage(Text.translatable("gems.augment.need_legendary").formatted(Formatting.RED), true);
            return ActionResult.FAIL;
        }
        if (AugmentRuntime.applyLegendaryAugment(player, target, instance)) {
            consume(player, stack);
            return ActionResult.SUCCESS;
        }
        return ActionResult.FAIL;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, TooltipDisplayComponent displayComponent, Consumer<Text> tooltip, TooltipType type) {
        AugmentInstance instance = AugmentRuntime.getInstance(stack);
        AugmentDefinition def = AugmentRegistry.get(augmentId);
        if (def != null) {
            tooltip.accept(Text.translatable(def.nameKey()).formatted(Formatting.GOLD));
            tooltip.accept(Text.translatable(def.descriptionKey()).formatted(Formatting.GRAY));
        }
        if (instance != null) {
            tooltip.accept(Text.translatable("gems.augment.rarity", instance.rarity().name()).formatted(Formatting.DARK_GRAY));
        }
    }

    private static void consume(ServerPlayerEntity player, ItemStack stack) {
        if (player.getAbilities().creativeMode) {
            return;
        }
        stack.decrement(1);
    }
}
