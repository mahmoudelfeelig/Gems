package com.feel.gems.net;

import com.feel.gems.augment.AugmentDefinition;
import com.feel.gems.augment.AugmentInstance;
import com.feel.gems.augment.AugmentRegistry;
import com.feel.gems.augment.AugmentRuntime;
import com.feel.gems.config.GemsBalance;
import com.feel.gems.legendary.LegendaryItem;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Server-side networking for legendary inscription management.
 */
public final class ServerInscriptionNetworking {
    private ServerInscriptionNetworking() {
    }

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(InscriptionOpenRequestPayload.ID, (payload, context) ->
                context.server().execute(() -> openScreen(context.player(), payload.mainHand())));

        ServerPlayNetworking.registerGlobalReceiver(InscriptionRemovePayload.ID, (payload, context) ->
                context.server().execute(() -> handleRemove(context.player(), payload)));
    }

    private static void handleRemove(ServerPlayerEntity player, InscriptionRemovePayload payload) {
        ItemStack stack = payload.mainHand() ? player.getMainHandStack() : player.getOffHandStack();
        if (!(stack.getItem() instanceof LegendaryItem)) {
            player.sendMessage(Text.translatable("gems.augment.need_legendary").formatted(Formatting.RED), true);
            return;
        }
        boolean removed = AugmentRuntime.removeLegendaryAugment(stack, payload.index());
        if (removed) {
            player.sendMessage(Text.translatable("gems.augment.removed").formatted(Formatting.GREEN), true);
            AugmentRuntime.applyLegendaryModifiers(player);
        }
        openScreen(player, payload.mainHand());
    }

    private static void openScreen(ServerPlayerEntity player, boolean mainHand) {
        ItemStack stack = mainHand ? player.getMainHandStack() : player.getOffHandStack();
        if (!(stack.getItem() instanceof LegendaryItem)) {
            player.sendMessage(Text.translatable("gems.augment.need_legendary").formatted(Formatting.RED), true);
            return;
        }
        List<AugmentInstance> inscriptions = AugmentRuntime.getLegendaryAugments(stack);
        List<InscriptionScreenPayload.Entry> entries = new ArrayList<>(inscriptions.size());
        for (AugmentInstance instance : inscriptions) {
            AugmentDefinition def = AugmentRegistry.get(instance.augmentId());
            String name = def != null ? Text.translatable(def.nameKey()).getString() : instance.augmentId();
            String desc = def != null ? Text.translatable(def.descriptionKey()).getString() : "";
            String rarity = instance.rarity().name();
            entries.add(new InscriptionScreenPayload.Entry(
                    instance.augmentId(),
                    name,
                    desc,
                    rarity,
                    instance.magnitude()
            ));
        }
        int maxSlots = GemsBalance.v().augments().legendaryMaxSlots();
        String itemKey = stack.getItem().getTranslationKey();
        ServerPlayNetworking.send(player, new InscriptionScreenPayload(itemKey, mainHand, entries, maxSlots));
    }
}
