package com.feel.gems.client;

import com.feel.gems.core.GemDefinition;
import com.feel.gems.core.GemId;
import com.feel.gems.core.GemRegistry;
import com.feel.gems.net.ActivateAbilityPayload;
import com.feel.gems.net.ActivateBonusAbilityPayload;
import com.feel.gems.net.BonusSelectionOpenRequestPayload;
import com.feel.gems.net.FluxChargePayload;
import com.feel.gems.net.SoulReleasePayload;
import com.feel.gems.net.SummonerLoadoutOpenRequestPayload;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;




public final class GemsKeybinds {
    private static final KeyBinding.Category CATEGORY = KeyBinding.Category.create(Identifier.of("gems", "controls"));

    private static boolean registered = false;
    private static KeyBinding MODIFIER;
    private static KeyBinding[] CUSTOM_KEYS;
    private static KeyBinding[] BONUS_ABILITY_KEYS;
    private static KeyBinding BONUS_SCREEN_KEY;

    private GemsKeybinds() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;

        MODIFIER = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.gems.modifier", GLFW.GLFW_KEY_LEFT_ALT, CATEGORY));

        // Always register the custom keybinds so they appear in the Controls menu even if the user
        // starts in chord mode and switches later via config.
        CUSTOM_KEYS = new KeyBinding[9];
        for (int i = 0; i < CUSTOM_KEYS.length; i++) {
            CUSTOM_KEYS[i] = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.gems.ability_" + (i + 1), GLFW.GLFW_KEY_UNKNOWN, CATEGORY));
        }
        
        // Bonus ability keybinds (unbound by default, customizable via Controls)
        BONUS_ABILITY_KEYS = new KeyBinding[2];
        BONUS_ABILITY_KEYS[0] = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.gems.bonus_ability_1", GLFW.GLFW_KEY_UNKNOWN, CATEGORY));
        BONUS_ABILITY_KEYS[1] = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.gems.bonus_ability_2", GLFW.GLFW_KEY_UNKNOWN, CATEGORY));
        
        // Bonus selection screen keybind (B by default)
        BONUS_SCREEN_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.gems.bonus_screen", GLFW.GLFW_KEY_B, CATEGORY));
    }

    public static boolean isModifierDown() {
        return useChordControls() && MODIFIER != null && MODIFIER.isPressed();
    }

    public static String modifierLabel() {
        if (!useChordControls() || MODIFIER == null) {
            return "";
        }
        return MODIFIER.getBoundKeyLocalizedText().getString();
    }

    public static String chordSlotLabel(int slotNumber) {
        if (!useChordControls()) {
            return customSlotLabel(slotNumber);
        }
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

    public static void tick(MinecraftClient client) {
        // Check bonus screen key (works regardless of control mode)
        if (BONUS_SCREEN_KEY != null && client.currentScreen == null) {
            while (BONUS_SCREEN_KEY.wasPressed()) {
                openBonusScreen(client);
            }
        }
        
        // Check bonus ability keybinds (work regardless of control mode)
        if (BONUS_ABILITY_KEYS != null && client.currentScreen == null) {
            for (int i = 0; i < BONUS_ABILITY_KEYS.length; i++) {
                while (BONUS_ABILITY_KEYS[i].wasPressed()) {
                    activateBonusAbility(client, i);
                }
            }
        }
        
        if (!useCustomControls() || CUSTOM_KEYS == null || client.currentScreen != null) {
            return;
        }
        for (int i = 0; i < CUSTOM_KEYS.length; i++) {
            while (CUSTOM_KEYS[i].wasPressed()) {
                activateSlotChord(client, i + 1);
            }
        }
    }
    
    private static void activateBonusAbility(MinecraftClient client, int slotIndex) {
        if (client.getNetworkHandler() == null) {
            sendActionBar(client, Text.literal("Not connected."));
            return;
        }
        if (!ClientGemState.isInitialized()) {
            sendActionBar(client, Text.literal("Gem state not synced yet."));
            return;
        }
        if (ClientGemState.energy() < 10) {
            sendActionBar(client, Text.literal("You need energy 10/10 to use bonus abilities."));
            return;
        }
        ClientPlayNetworking.send(new ActivateBonusAbilityPayload(slotIndex));
    }
    
    private static void openBonusScreen(MinecraftClient client) {
        if (client.getNetworkHandler() == null) {
            sendActionBar(client, Text.literal("Not connected."));
            return;
        }
        if (!ClientGemState.isInitialized()) {
            sendActionBar(client, Text.literal("Gem state not synced yet."));
            return;
        }
        if (ClientGemState.energy() < 10) {
            sendActionBar(client, Text.literal("You need energy 10/10 to access bonus powers."));
            return;
        }
        ClientPlayNetworking.send(BonusSelectionOpenRequestPayload.INSTANCE);
    }

    public static boolean useChordControls() {
        return GemsClientConfigManager.config().controlMode == GemsClientConfig.ControlMode.CHORD;
    }

    private static boolean useCustomControls() {
        return GemsClientConfigManager.config().controlMode == GemsClientConfig.ControlMode.CUSTOM;
    }

    private static String customSlotLabel(int slotNumber) {
        if (CUSTOM_KEYS == null || slotNumber < 1 || slotNumber > CUSTOM_KEYS.length) {
            return "";
        }
        KeyBinding key = CUSTOM_KEYS[slotNumber - 1];
        if (key == null) {
            return "";
        }
        return key.getBoundKeyLocalizedText().getString();
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

        // Bonus abilities: slots 5-6 (if player has energy 10)
        // These are mapped to bonus ability index 0-1 (claimed abilities)
        if (slotNumber == 5 || slotNumber == 6) {
            if (ClientGemState.energy() >= 10) {
                int bonusSlot = slotNumber - 5; // 0 or 1
                ClientPlayNetworking.send(new ActivateBonusAbilityPayload(bonusSlot));
                return;
            }
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
