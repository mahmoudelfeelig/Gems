package com.feel.gems.net;

import com.feel.gems.augment.AugmentDefinition;
import com.feel.gems.augment.AugmentInstance;
import com.feel.gems.augment.AugmentRegistry;
import com.feel.gems.augment.AugmentRuntime;
import com.feel.gems.config.GemsBalance;
import com.feel.gems.core.GemId;
import com.feel.gems.state.GemPlayerState;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Server-side networking for augment management.
 */
public final class ServerAugmentNetworking {
    private ServerAugmentNetworking() {
    }

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(AugmentOpenRequestPayload.ID, (payload, context) ->
                context.server().execute(() -> openScreen(context.player(), payload.gem())));

        ServerPlayNetworking.registerGlobalReceiver(AugmentRemovePayload.ID, (payload, context) ->
                context.server().execute(() -> handleRemove(context.player(), payload)));
    }

    private static void handleRemove(ServerPlayerEntity player, AugmentRemovePayload payload) {
        GemPlayerState.initIfNeeded(player);
        GemId gem = parseGem(payload.gemId());
        if (gem == null) {
            player.sendMessage(Text.translatable("gems.augment.invalid_gem").formatted(Formatting.RED), true);
            return;
        }
        boolean removed = AugmentRuntime.removeGemAugment(player, gem, payload.index());
        if (removed) {
            player.sendMessage(Text.translatable("gems.augment.removed").formatted(Formatting.GREEN), true);
        }
        openScreen(player, gem);
    }

    private static void openScreen(ServerPlayerEntity player, GemId gem) {
        GemPlayerState.initIfNeeded(player);
        List<AugmentInstance> augments = AugmentRuntime.getGemAugments(player, gem);
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
        ServerPlayNetworking.send(player, new AugmentScreenPayload(gem.name(), entries, maxSlots));
    }

    private static GemId parseGem(String gemId) {
        if (gemId == null || gemId.isBlank()) {
            return null;
        }
        try {
            return GemId.valueOf(gemId);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
