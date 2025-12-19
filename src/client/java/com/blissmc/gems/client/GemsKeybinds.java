package com.blissmc.gems.client;

import com.blissmc.gems.core.GemDefinition;
import com.blissmc.gems.core.GemId;
import com.blissmc.gems.core.GemRegistry;
import com.blissmc.gems.net.ActivateAbilityPayload;
import com.blissmc.gems.net.SoulReleasePayload;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public final class GemsKeybinds {
    private static final String CATEGORY = "category.gems";

    private static boolean registered = false;
    private static KeyBinding MODIFIER;
    private static final List<KeyBinding> SLOTS = new ArrayList<>();

    private GemsKeybinds() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;

        MODIFIER = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.gems.modifier",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_LEFT_CONTROL,
                CATEGORY
        ));

        int slotCount = Math.min(10, maxAbilityCount() + 1); // +1 => soul release (after the last ability)
        for (int i = 1; i <= slotCount; i++) {
            SLOTS.add(KeyBindingHelper.registerKeyBinding(new KeyBinding(
                    "key.gems.ability_slot_" + i,
                    InputUtil.Type.KEYSYM,
                    defaultDigitKey(i),
                    CATEGORY
            )));
        }

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.currentScreen != null) {
                drainQueuedPresses();
                return;
            }

            boolean modifierDown = MODIFIER.isPressed();
            for (int i = 0; i < SLOTS.size(); i++) {
                KeyBinding slot = SLOTS.get(i);
                while (slot.wasPressed()) {
                    if (modifierDown) {
                        activateSlot(client, i + 1);
                    }
                }
            }
        });
    }

    private static void activateSlot(MinecraftClient client, int slotNumber) {
        if (client.getNetworkHandler() == null) {
            sendActionBar(client, Text.literal("Not connected."));
            return;
        }
        if (!ClientGemState.isInitialized()) {
            sendActionBar(client, Text.literal("Gem state not synced yet."));
            return;
        }

        GemDefinition def = GemRegistry.definition(ClientGemState.activeGem());
        int abilityCount = def.abilities().size();

        // Soul release is always the key AFTER the gem's last ability (not "after unlocked abilities").
        if (slotNumber == abilityCount + 1) {
            ClientPlayNetworking.send(SoulReleasePayload.INSTANCE);
            return;
        }

        int abilityIndex = slotNumber - 1;
        if (abilityIndex < 0 || abilityIndex >= abilityCount) {
            return;
        }

        // Server validates unlocks/cooldowns/suppression.
        ClientPlayNetworking.send(new ActivateAbilityPayload(abilityIndex));
    }

    private static void sendActionBar(MinecraftClient client, Text text) {
        if (client.player == null) {
            return;
        }
        client.player.sendMessage(text, true);
    }

    private static void drainQueuedPresses() {
        for (KeyBinding slot : SLOTS) {
            while (slot.wasPressed()) {
                // drain
            }
        }
    }

    private static int maxAbilityCount() {
        int max = 0;
        for (GemId id : GemId.values()) {
            max = Math.max(max, GemRegistry.definition(id).abilities().size());
        }
        return max;
    }

    private static int defaultDigitKey(int slotNumber) {
        return switch (slotNumber) {
            case 1 -> GLFW.GLFW_KEY_1;
            case 2 -> GLFW.GLFW_KEY_2;
            case 3 -> GLFW.GLFW_KEY_3;
            case 4 -> GLFW.GLFW_KEY_4;
            case 5 -> GLFW.GLFW_KEY_5;
            case 6 -> GLFW.GLFW_KEY_6;
            case 7 -> GLFW.GLFW_KEY_7;
            case 8 -> GLFW.GLFW_KEY_8;
            case 9 -> GLFW.GLFW_KEY_9;
            case 10 -> GLFW.GLFW_KEY_0;
            default -> GLFW.GLFW_KEY_UNKNOWN;
        };
    }
}
