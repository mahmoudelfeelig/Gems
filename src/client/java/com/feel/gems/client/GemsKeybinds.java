package com.feel.gems.client;

import com.feel.gems.core.GemDefinition;
import com.feel.gems.core.GemId;
import com.feel.gems.core.GemRegistry;
import com.feel.gems.net.ActivateAbilityPayload;
import com.feel.gems.net.FluxChargePayload;
import com.feel.gems.net.SoulReleasePayload;
import com.feel.gems.net.SummonerLoadoutOpenRequestPayload;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;




public final class GemsKeybinds {
    private static final String CATEGORY = "category.gems";

    private static boolean registered = false;
    private static KeyBinding MODIFIER;

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
    }

    public static boolean isModifierDown() {
        return MODIFIER != null && MODIFIER.isPressed();
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

    public static void activateSlotChord(MinecraftClient client, int slotNumber) {
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

        // Summoner: open loadout editor on the chord after the last ability slot.
        if (ClientGemState.activeGem() == GemId.SUMMONER && slotNumber == abilityCount + 1) {
            ClientPlayNetworking.send(SummonerLoadoutOpenRequestPayload.INSTANCE);
            return;
        }

        // Flux: insert "Charge" as slot 2 (between ability 1 and ability 2).
        if (ClientGemState.activeGem() == GemId.FLUX) {
            if (slotNumber == 2) {
                ClientPlayNetworking.send(FluxChargePayload.INSTANCE);
                return;
            }
            if (slotNumber >= 3) {
                int shiftedIndex = slotNumber - 2;
                if (shiftedIndex < 0 || shiftedIndex >= abilityCount) {
                    return;
                }
                ClientPlayNetworking.send(new ActivateAbilityPayload(shiftedIndex));
                return;
            }
        }

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

    private static String chordKeyLabel(int slotNumber) {
        if (slotNumber < 1 || slotNumber > 9) {
            return "";
        }
        MinecraftClient client = MinecraftClient.getInstance();
        GameOptions options = client.options;
        if (options == null || options.hotbarKeys == null || options.hotbarKeys.length < slotNumber) {
            return "";
        }
        return options.hotbarKeys[slotNumber - 1].getBoundKeyLocalizedText().getString();
    }
}
