package com.blissmc.gems.client;

import com.blissmc.gems.core.GemDefinition;
import com.blissmc.gems.core.GemEnergyState;
import com.blissmc.gems.core.GemRegistry;
import com.blissmc.gems.net.ActivateAbilityPayload;
import com.blissmc.gems.power.GemAbility;
import com.blissmc.gems.power.ModAbilities;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.Collections;
import java.util.List;

public final class GemsKeybinds {
    private static final String CATEGORY = "category.gems";

    private static final KeyBinding ACTIVATE_ABILITY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.gems.activate_ability",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            CATEGORY
    ));

    private static final KeyBinding NEXT_ABILITY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.gems.next_ability",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            CATEGORY
    ));

    private static int selectedAbilityIndex = 0;

    private GemsKeybinds() {
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (NEXT_ABILITY.wasPressed()) {
                cycleNext(client);
            }
            while (ACTIVATE_ABILITY.wasPressed()) {
                activateSelected(client);
            }
        });
    }

    private static void cycleNext(MinecraftClient client) {
        List<Identifier> unlocked = unlockedAbilities();
        if (unlocked.isEmpty()) {
            sendActionBar(client, Text.literal("No abilities unlocked."));
            return;
        }
        selectedAbilityIndex = (selectedAbilityIndex + 1) % unlocked.size();
        sendActionBar(client, Text.literal("Selected: " + abilityName(unlocked.get(selectedAbilityIndex))));
    }

    private static void activateSelected(MinecraftClient client) {
        List<Identifier> unlocked = unlockedAbilities();
        if (unlocked.isEmpty()) {
            sendActionBar(client, Text.literal("No abilities unlocked."));
            return;
        }
        if (selectedAbilityIndex < 0 || selectedAbilityIndex >= unlocked.size()) {
            selectedAbilityIndex = 0;
        }

        if (client.getNetworkHandler() == null) {
            sendActionBar(client, Text.literal("Not connected."));
            return;
        }
        ClientPlayNetworking.send(new ActivateAbilityPayload(selectedAbilityIndex));
    }

    private static List<Identifier> unlockedAbilities() {
        if (!ClientGemState.isInitialized()) {
            return List.of();
        }
        GemDefinition def = GemRegistry.definition(ClientGemState.activeGem());
        List<Identifier> all = def.abilities();
        int unlocked = new GemEnergyState(ClientGemState.energy()).unlockedAbilityCount(all.size());
        if (unlocked <= 0) {
            return List.of();
        }
        if (unlocked >= all.size()) {
            return all;
        }
        return Collections.unmodifiableList(all.subList(0, unlocked));
    }

    private static String abilityName(Identifier id) {
        GemAbility ability = ModAbilities.get(id);
        return ability == null ? id.toString() : ability.name();
    }

    private static void sendActionBar(MinecraftClient client, Text text) {
        if (client.player == null) {
            return;
        }
        client.player.sendMessage(text, true);
    }
}
