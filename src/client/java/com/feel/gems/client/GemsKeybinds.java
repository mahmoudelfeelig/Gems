package com.feel.gems.client;

import com.feel.gems.core.GemDefinition;
import com.feel.gems.core.GemId;
import com.feel.gems.core.GemRegistry;
import com.feel.gems.net.ActivateAbilityPayload;
import com.feel.gems.net.ActivateBonusAbilityPayload;
import com.feel.gems.net.AugmentOpenRequestPayload;
import com.feel.gems.net.BonusSelectionOpenRequestPayload;
import com.feel.gems.net.FluxChargePayload;
import com.feel.gems.net.LoadoutOpenRequestPayload;
import com.feel.gems.net.PrismSelectionOpenRequestPayload;
import com.feel.gems.net.SpyObservedOpenRequestPayload;
import com.feel.gems.net.SoulReleasePayload;
import com.feel.gems.net.SummonerLoadoutOpenRequestPayload;
import com.feel.gems.net.TrophyNecklaceOpenRequestPayload;
import com.feel.gems.client.screen.GuidebookScreen;
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
    private static KeyBinding SPY_OBSERVED_SCREEN_KEY;
    private static KeyBinding TROPHY_NECKLACE_SCREEN_KEY;
    private static KeyBinding LOADOUT_PRESETS_KEY;
    private static KeyBinding AUGMENT_SCREEN_KEY;
    private static KeyBinding GUIDEBOOK_KEY;
    private static KeyBinding TOGGLE_CONTROL_MODE_KEY;

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
        
        // Bonus ability keybinds (C and V by default, customizable via Controls)
        BONUS_ABILITY_KEYS = new KeyBinding[2];
        BONUS_ABILITY_KEYS[0] = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.gems.bonus_ability_1", GLFW.GLFW_KEY_C, CATEGORY));
        BONUS_ABILITY_KEYS[1] = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.gems.bonus_ability_2", GLFW.GLFW_KEY_V, CATEGORY));
        
        // Bonus selection screen keybind (B by default)
        BONUS_SCREEN_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.gems.bonus_screen", GLFW.GLFW_KEY_B, CATEGORY));

        // Spy observed abilities screen keybind (O by default)
        SPY_OBSERVED_SCREEN_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.gems.spy_observed_screen", GLFW.GLFW_KEY_O, CATEGORY));

        // Trophy Necklace screen keybind (P by default)
        TROPHY_NECKLACE_SCREEN_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.gems.trophy_necklace_screen", GLFW.GLFW_KEY_P, CATEGORY));

        // Loadout presets screen keybind (L by default)
        LOADOUT_PRESETS_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.gems.loadout_presets", GLFW.GLFW_KEY_L, CATEGORY));

        // Augment management screen keybind (U by default)
        AUGMENT_SCREEN_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.gems.augment_screen", GLFW.GLFW_KEY_U, CATEGORY));

        // Guidebook keybind (tilde by default)
        GUIDEBOOK_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.gems.guidebook", GLFW.GLFW_KEY_GRAVE_ACCENT, CATEGORY));

        // Toggle control mode keybind (unbound by default - user can assign)
        TOGGLE_CONTROL_MODE_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.gems.toggle_control_mode", GLFW.GLFW_KEY_UNKNOWN, CATEGORY));
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

    public static String bonusScreenLabel() {
        if (BONUS_SCREEN_KEY == null) {
            return "";
        }
        return BONUS_SCREEN_KEY.getBoundKeyLocalizedText().getString();
    }

    public static String bonusAbilityLabel(int bonusSlotIndex) {
        if (BONUS_ABILITY_KEYS == null || bonusSlotIndex < 0 || bonusSlotIndex >= BONUS_ABILITY_KEYS.length) {
            return "";
        }
        KeyBinding key = BONUS_ABILITY_KEYS[bonusSlotIndex];
        if (key == null) {
            return "";
        }
        return key.getBoundKeyLocalizedText().getString();
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

        // Spy observed abilities screen key
        if (SPY_OBSERVED_SCREEN_KEY != null && client.currentScreen == null) {
            while (SPY_OBSERVED_SCREEN_KEY.wasPressed()) {
                openSpyObservedScreen(client);
            }
        }

        // Trophy Necklace screen key
        if (TROPHY_NECKLACE_SCREEN_KEY != null && client.currentScreen == null) {
            while (TROPHY_NECKLACE_SCREEN_KEY.wasPressed()) {
                openTrophyNecklaceScreen(client);
            }
        }

        // Loadout presets screen key
        if (LOADOUT_PRESETS_KEY != null && client.currentScreen == null) {
            while (LOADOUT_PRESETS_KEY.wasPressed()) {
                openLoadoutPresetsScreen(client);
            }
        }

        // Augment screen key
        if (AUGMENT_SCREEN_KEY != null && client.currentScreen == null) {
            while (AUGMENT_SCREEN_KEY.wasPressed()) {
                openAugmentScreen(client);
            }
        }

        // Guidebook key
        if (GUIDEBOOK_KEY != null && client.currentScreen == null) {
            while (GUIDEBOOK_KEY.wasPressed()) {
                GuidebookScreen.open(client);
            }
        }

        // Toggle control mode key
        if (TOGGLE_CONTROL_MODE_KEY != null) {
            while (TOGGLE_CONTROL_MODE_KEY.wasPressed()) {
                toggleControlMode(client);
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
            sendActionBar(client, Text.translatable("gems.client.not_connected"));
            return;
        }
        if (!ClientGemState.isInitialized()) {
            sendActionBar(client, Text.translatable("gems.client.gem_state_not_synced"));
            return;
        }
        if (ClientGemState.energy() < 10) {
            sendActionBar(client, Text.translatable("gems.bonus.need_energy_use"));
            return;
        }
        ClientPlayNetworking.send(new ActivateBonusAbilityPayload(slotIndex));
    }
    
    private static void openBonusScreen(MinecraftClient client) {
        if (client.getNetworkHandler() == null) {
            sendActionBar(client, Text.translatable("gems.client.not_connected"));
            return;
        }
        if (!ClientGemState.isInitialized()) {
            sendActionBar(client, Text.translatable("gems.client.gem_state_not_synced"));
            return;
        }
        
        // Prism gem uses B key to open Prism selection screen (requires energy 10)
        if (ClientGemState.activeGem() == GemId.PRISM) {
            if (ClientGemState.energy() < 10) {
                sendActionBar(client, Text.translatable("gems.prism.need_energy_access"));
                return;
            }
            ClientPlayNetworking.send(PrismSelectionOpenRequestPayload.INSTANCE);
            return;
        }
        
        // All other gems use B key for bonus selection (requires energy 10+)
        if (ClientGemState.energy() < 10) {
            sendActionBar(client, Text.translatable("gems.bonus.need_energy_access"));
            return;
        }
        ClientPlayNetworking.send(BonusSelectionOpenRequestPayload.INSTANCE);
    }

    private static void openTrophyNecklaceScreen(MinecraftClient client) {
        if (client.getNetworkHandler() == null) {
            sendActionBar(client, Text.translatable("gems.client.not_connected"));
            return;
        }
        ClientPlayNetworking.send(TrophyNecklaceOpenRequestPayload.INSTANCE);
    }

    private static void openSpyObservedScreen(MinecraftClient client) {
        if (client.getNetworkHandler() == null) {
            sendActionBar(client, Text.translatable("gems.client.not_connected"));
            return;
        }
        if (!ClientGemState.isInitialized()) {
            sendActionBar(client, Text.translatable("gems.client.gem_state_not_synced"));
            return;
        }
        GemId gem = ClientGemState.activeGem();
        if (gem != GemId.SPY && gem != GemId.PRISM) {
            sendActionBar(client, Text.translatable("gems.spy.observed.not_spy"));
            return;
        }
        ClientPlayNetworking.send(SpyObservedOpenRequestPayload.INSTANCE);
    }

    private static void openLoadoutPresetsScreen(MinecraftClient client) {
        if (client.getNetworkHandler() == null) {
            sendActionBar(client, Text.translatable("gems.client.not_connected"));
            return;
        }
        if (!ClientGemState.isInitialized()) {
            sendActionBar(client, Text.translatable("gems.client.gem_state_not_synced"));
            return;
        }
        ClientPlayNetworking.send(new LoadoutOpenRequestPayload(ClientGemState.activeGem()));
    }

    private static void openAugmentScreen(MinecraftClient client) {
        if (client.getNetworkHandler() == null) {
            sendActionBar(client, Text.translatable("gems.client.not_connected"));
            return;
        }
        if (!ClientGemState.isInitialized()) {
            sendActionBar(client, Text.translatable("gems.client.gem_state_not_synced"));
            return;
        }
        ClientPlayNetworking.send(new AugmentOpenRequestPayload(ClientGemState.activeGem()));
    }

    private static void toggleControlMode(MinecraftClient client) {
        GemsClientConfig cfg = GemsClientConfigManager.config();
        if (cfg.controlMode == GemsClientConfig.ControlMode.CHORD) {
            cfg.controlMode = GemsClientConfig.ControlMode.CUSTOM;
            sendActionBar(client, Text.translatable("gems.controls.mode.custom"));
        } else {
            cfg.controlMode = GemsClientConfig.ControlMode.CHORD;
            sendActionBar(client, Text.translatable("gems.controls.mode.chord"));
        }
        GemsClientConfigManager.save(cfg);
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
            sendActionBar(client, Text.translatable("gems.client.not_connected"));
            return;
        }
        if (!ClientGemState.isInitialized()) {
            sendActionBar(client, Text.translatable("gems.client.gem_state_not_synced"));
            return;
        }
        ClientAbilitySelection.record(ClientGemState.activeGem(), slotNumber);

        GemDefinition def = GemRegistry.definition(ClientGemState.activeGem());
        int abilityCount = def.abilities().size();
        
        // Prism: uses selected abilities from ClientPrismState instead of gem definition
        if (ClientGemState.activeGem() == GemId.PRISM) {
            int prismAbilityCount = ClientPrismState.getAbilities().size();
            int abilityIndex = slotNumber - 1;
            if (abilityIndex < 0 || abilityIndex >= prismAbilityCount) {
                return;
            }
            // Server validates unlocks/cooldowns/suppression.
            ClientPlayNetworking.send(new ActivateAbilityPayload(abilityIndex));
            return;
        }

        // Chaos: has independent slots, each can be rolled or used
        if (ClientGemState.activeGem() == GemId.CHAOS) {
            int slotIndex = slotNumber - 1;
            if (slotIndex < 0 || slotIndex >= ClientChaosState.slotCount()) {
                return;
            }
            // Server handles both rolling new abilities and using existing ones
            ClientPlayNetworking.send(new ActivateAbilityPayload(slotIndex));
            return;
        }

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
            sendActionBar(client, Text.translatable("gems.client.not_connected"));
            return;
        }
        if (!ClientGemState.isInitialized()) {
            sendActionBar(client, Text.translatable("gems.client.gem_state_not_synced"));
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
