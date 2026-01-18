package com.feel.gems.net;

import com.feel.gems.augment.AugmentDefinition;
import com.feel.gems.augment.AugmentInstance;
import com.feel.gems.augment.AugmentRegistry;
import com.feel.gems.augment.AugmentRuntime;
import com.feel.gems.config.GemsBalance;
import com.feel.gems.state.GemPlayerState;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import com.feel.gems.item.GemItem;

/**
 * Server-side networking for augment management.
 */
public final class ServerAugmentNetworking {
    private ServerAugmentNetworking() {
    }

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(AugmentOpenRequestPayload.ID, (payload, context) ->
                context.server().execute(() -> openScreen(context.player(), payload.mainHand())));

        ServerPlayNetworking.registerGlobalReceiver(AugmentRemovePayload.ID, (payload, context) ->
                context.server().execute(() -> handleRemove(context.player(), payload)));
    }

    private static void handleRemove(ServerPlayerEntity player, AugmentRemovePayload payload) {
        GemPlayerState.initIfNeeded(player);
        ItemStack stack = payload.mainHand() ? player.getMainHandStack() : player.getOffHandStack();
        if (!(stack.getItem() instanceof GemItem)) {
            player.sendMessage(Text.translatable("gems.augment.need_gem").formatted(Formatting.RED), true);
            return;
        }
        GemItem gemItem = (GemItem) stack.getItem();
        boolean removed = AugmentRuntime.removeGemAugment(stack, payload.index());
        if (removed) {
            player.sendMessage(Text.translatable("gems.augment.removed").formatted(Formatting.GREEN), true);
            if (GemPlayerState.getActiveGem(player) == gemItem.gemId()) {
                AugmentRuntime.captureActiveGemAugments(player, gemItem.gemId(), stack);
                GemCooldownSync.send(player);
            }
        }
        openScreen(player, payload.mainHand());
    }

    private static void openScreen(ServerPlayerEntity player, boolean mainHand) {
        GemPlayerState.initIfNeeded(player);
        ItemStack stack = mainHand ? player.getMainHandStack() : player.getOffHandStack();
        if (!(stack.getItem() instanceof GemItem gemItem)) {
            player.sendMessage(Text.translatable("gems.augment.need_gem").formatted(Formatting.RED), true);
            return;
        }
        List<AugmentInstance> augments = AugmentRuntime.getGemAugments(stack);
        List<AugmentScreenPayload.AugmentEntry> entries = new ArrayList<>(augments.size());
        for (AugmentInstance instance : augments) {
            AugmentDefinition def = AugmentRegistry.get(instance.augmentId());
            String name = def != null ? Text.translatable(def.nameKey()).getString() : instance.augmentId();
            String desc = def != null ? Text.translatable(def.descriptionKey()).getString() : "";
            String rarity = instance.rarity().name();
            entries.add(new AugmentScreenPayload.AugmentEntry(
                    instance.augmentId(),
                    name,
                    desc,
                    rarity,
                    instance.magnitude()
            ));
        }
        int maxSlots = GemsBalance.v().augments().gemMaxSlots();
        ServerPlayNetworking.send(player, new AugmentScreenPayload(gemItem.gemId().name(), mainHand, entries, maxSlots));
    }
}
