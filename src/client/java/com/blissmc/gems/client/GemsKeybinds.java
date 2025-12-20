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
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.ArrayList;

public final class GemsKeybinds {
    private static final String CATEGORY = "category.gems";

    private static boolean registered = false;
    private static KeyBinding MODIFIER;
    private static final List<KeyBinding> CHORD_SLOTS = new ArrayList<>();

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
                GLFW.GLFW_KEY_LEFT_ALT,
                CATEGORY
        ));

        for (int slot = 1; slot <= 10; slot++) {
            CHORD_SLOTS.add(KeyBindingHelper.registerKeyBinding(new KeyBinding(
                    "key.gems.chord_slot_" + slot,
                    InputUtil.Type.KEYSYM,
                    defaultChordKey(slot),
                    CATEGORY
            )));
        }

        // Prevent "modifier + number" from switching hotbar slots by draining the hotbar presses
        // at the very start of the client tick (before vanilla input handling runs).
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if (client.currentScreen != null) {
                return;
            }
            if (!MODIFIER.isPressed()) {
                return;
            }
            GameOptions options = client.options;
            if (options == null || options.hotbarKeys == null) {
                return;
            }
            for (KeyBinding hotbar : options.hotbarKeys) {
                while (hotbar.wasPressed()) {
                    // drain
                }
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.currentScreen != null) {
                drainQueuedPresses();
                return;
            }

            boolean modifier = MODIFIER.isPressed();
            for (int i = 0; i < CHORD_SLOTS.size(); i++) {
                KeyBinding slot = CHORD_SLOTS.get(i);
                while (slot.wasPressed()) {
                    if (modifier) {
                        activateSlot(client, i + 1);
                    }
                }
            }
        });
    }

    public static String modifierLabel() {
        if (MODIFIER == null) {
            return "";
        }
        return MODIFIER.getBoundKeyLocalizedText().getString();
    }

    public static String chordSlotLabel(int slotNumber) {
        String key = chordKeyLabel(slotNumber);
        if (key.isEmpty()) {
            return "";
        }
        String modifier = modifierLabel();
        if (modifier.isEmpty()) {
            return key;
        }
        return modifier + " + " + key;
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
        ClientAbilitySelection.record(ClientGemState.activeGem(), slotNumber);

        GemDefinition def = GemRegistry.definition(ClientGemState.activeGem());
        int abilityCount = def.abilities().size();

        // Soul release is always the key AFTER the gem's last ability (not "after unlocked abilities").
        if (ClientGemState.activeGem() == GemId.ASTRA && slotNumber == abilityCount + 1) {
            activateSoulRelease(client);
            return;
        }

        int abilityIndex = slotNumber - 1;
        if (abilityIndex < 0 || abilityIndex >= abilityCount) {
            return;
        }

        // Server validates unlocks/cooldowns/suppression.
        ClientPlayNetworking.send(new ActivateAbilityPayload(abilityIndex));
    }

    private static void activateSoulRelease(MinecraftClient client) {
        if (client.getNetworkHandler() == null) {
            sendActionBar(client, Text.literal("Not connected."));
            return;
        }
        if (!ClientGemState.isInitialized()) {
            sendActionBar(client, Text.literal("Gem state not synced yet."));
            return;
        }
        ClientAbilitySelection.record(ClientGemState.activeGem(), GemRegistry.definition(ClientGemState.activeGem()).abilities().size() + 1);
        if (ClientGemState.activeGem() != GemId.ASTRA) {
            return;
        }
        ClientPlayNetworking.send(SoulReleasePayload.INSTANCE);
    }

    private static void sendActionBar(MinecraftClient client, Text text) {
        if (client.player == null) {
            return;
        }
        client.player.sendMessage(text, true);
    }

    private static void drainQueuedPresses() {
        for (KeyBinding slot : CHORD_SLOTS) {
            while (slot.wasPressed()) {
                // drain
            }
        }
    }

    private static String chordKeyLabel(int slotNumber) {
        int idx = slotNumber - 1;
        if (idx < 0 || idx >= CHORD_SLOTS.size()) {
            return "";
        }
        KeyBinding binding = CHORD_SLOTS.get(idx);
        if (binding == null) {
            return "";
        }
        return binding.getBoundKeyLocalizedText().getString();
    }

    private static int defaultChordKey(int slotNumber) {
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
