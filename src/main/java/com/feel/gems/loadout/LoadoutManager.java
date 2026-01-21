package com.feel.gems.loadout;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.core.GemDefinition;
import com.feel.gems.core.GemId;
import com.feel.gems.core.GemRegistry;
import com.feel.gems.state.GemPlayerState;
import com.feel.gems.state.GemsPersistentDataHolder;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

/**
 * Manages loadout presets for players.
 * Loadouts unlock at energy 6+ (configurable).
 * Each gem can have up to 5 presets saved.
 */
public final class LoadoutManager {
    private static final String KEY_LOADOUTS = "gemLoadouts";
    private static final String KEY_ACTIVE_PRESET = "activePreset";

    private LoadoutManager() {
    }

    // ========== Energy Requirement ==========

    /**
     * Check if the player meets the energy requirement for loadout presets.
     */
    public static boolean canUseLoadouts(ServerPlayerEntity player) {
        if (!GemsBalance.v().loadouts().enabled()) {
            return false;
        }
        int energy = GemPlayerState.getEnergy(player);
        return energy >= GemsBalance.v().loadouts().unlockEnergy();
    }

    // ========== Preset Management ==========

    /**
     * Get all saved presets for a gem.
     */
    public static List<GemLoadout> getPresets(ServerPlayerEntity player, GemId gem) {
        NbtCompound root = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        NbtCompound loadouts = root.getCompound(KEY_LOADOUTS).orElse(null);
        if (loadouts == null) {
            return List.of();
        }

        NbtList gemPresets = loadouts.getList(gem.name()).orElse(null);
        if (gemPresets == null || gemPresets.isEmpty()) {
            return List.of();
        }

        List<GemLoadout> result = new ArrayList<>();
        for (int i = 0; i < gemPresets.size(); i++) {
            NbtCompound preset = gemPresets.getCompound(i).orElse(null);
            if (preset != null) {
                GemLoadout loadout = fromNbt(preset, gem);
                if (loadout != null) {
                    result.add(loadout);
                }
            }
        }
        return result;
    }

    /**
     * Save a preset for a gem. Returns the index of the saved preset.
     */
    public static int savePreset(ServerPlayerEntity player, GemLoadout loadout) {
        if (!canUseLoadouts(player)) {
            player.sendMessage(
                    Text.translatable("gems.loadout.locked", GemsBalance.v().loadouts().unlockEnergy())
                            .formatted(Formatting.RED),
                    false
            );
            return -1;
        }

        GemId gem = loadout.gem();
        List<GemLoadout> existing = new ArrayList<>(getPresets(player, gem));

        // Check max presets
        if (existing.size() >= GemLoadout.MAX_PRESETS_PER_GEM) {
            player.sendMessage(
                    Text.translatable("gems.loadout.max_presets", GemLoadout.MAX_PRESETS_PER_GEM)
                            .formatted(Formatting.RED),
                    false
            );
            return -1;
        }

        existing.add(loadout);
        saveAllPresets(player, gem, existing);

        player.sendMessage(
                Text.translatable("gems.loadout.saved", loadout.name()).formatted(Formatting.GREEN),
                false
        );
        return existing.size() - 1;
    }

    /**
     * Delete a preset by index.
     */
    public static boolean deletePreset(ServerPlayerEntity player, GemId gem, int index) {
        List<GemLoadout> existing = new ArrayList<>(getPresets(player, gem));
        if (index < 0 || index >= existing.size()) {
            return false;
        }

        String name = existing.get(index).name();
        existing.remove(index);
        saveAllPresets(player, gem, existing);

        player.sendMessage(
                Text.translatable("gems.loadout.deleted", name).formatted(Formatting.YELLOW),
                false
        );
        return true;
    }

    /**
     * Load (activate) a preset by index.
     */
    public static boolean loadPreset(ServerPlayerEntity player, GemId gem, int index) {
        if (!canUseLoadouts(player)) {
            player.sendMessage(
                    Text.translatable("gems.loadout.locked", GemsBalance.v().loadouts().unlockEnergy())
                            .formatted(Formatting.RED),
                    false
            );
            return false;
        }

        List<GemLoadout> presets = getPresets(player, gem);
        if (index < 0 || index >= presets.size()) {
            player.sendMessage(
                    Text.translatable("gems.loadout.not_found").formatted(Formatting.RED),
                    false
            );
            return false;
        }

        GemLoadout loadout = presets.get(index);

        // Apply the loadout
        applyLoadout(player, loadout);

        // Save active preset index
        setActivePresetIndex(player, gem, index);

        player.sendMessage(
                Text.translatable("gems.loadout.loaded", loadout.name()).formatted(Formatting.GREEN),
                false
        );
        return true;
    }

    /**
     * Apply a loadout to the player.
     * Respects current energy gating for abilities.
     */
    public static void applyLoadout(ServerPlayerEntity player, GemLoadout loadout) {
        // Apply passives enabled state
        GemPlayerState.setPassivesEnabled(player, loadout.passivesEnabled());

        // Store ability order preference
        saveAbilityOrder(player, loadout.gem(), loadout.abilityOrder());

        // Store HUD layout preference
        saveHudLayout(player, loadout.hudLayout());
    }

    /**
     * Create a loadout from the player's current settings.
     */
    public static GemLoadout createFromCurrent(ServerPlayerEntity player, String name) {
        GemId gem = GemPlayerState.getActiveGem(player);
        GemDefinition def = GemRegistry.definition(gem);

        List<Identifier> abilityOrder = getAbilityOrder(player, gem);
        if (abilityOrder.isEmpty()) {
            abilityOrder = def.abilities();
        }

        boolean passivesEnabled = GemPlayerState.arePassivesEnabled(player);
        GemLoadout.HudLayout hudLayout = getHudLayout(player);

        return new GemLoadout(
                GemLoadout.sanitizeName(name),
                gem,
                abilityOrder,
                passivesEnabled,
                hudLayout
        );
    }

    // ========== Ability Order ==========

    private static final String KEY_ABILITY_ORDER = "abilityOrder";

    /**
     * Get the player's custom ability order for a gem.
     */
    public static List<Identifier> getAbilityOrder(ServerPlayerEntity player, GemId gem) {
        if (!canUseLoadouts(player)) {
            return GemRegistry.definition(gem).abilities();
        }
        NbtCompound root = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        NbtCompound orders = root.getCompound(KEY_ABILITY_ORDER).orElse(null);
        if (orders == null) {
            return GemRegistry.definition(gem).abilities();
        }

        NbtList list = orders.getList(gem.name()).orElse(null);
        if (list == null || list.isEmpty()) {
            return GemRegistry.definition(gem).abilities();
        }

        List<Identifier> result = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            String idStr = list.getString(i, "");
            if (!idStr.isEmpty()) {
                Identifier id = Identifier.tryParse(idStr);
                if (id != null) {
                    result.add(id);
                }
            }
        }
        return GemLoadout.sanitizeAbilityOrder(result, GemRegistry.definition(gem).abilities());
    }

    /**
     * Save the player's custom ability order for a gem.
     */
    public static void saveAbilityOrder(ServerPlayerEntity player, GemId gem, List<Identifier> order) {
        NbtCompound root = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        NbtCompound orders = root.getCompound(KEY_ABILITY_ORDER).orElse(new NbtCompound());

        List<Identifier> sanitized = GemLoadout.sanitizeAbilityOrder(order, GemRegistry.definition(gem).abilities());
        NbtList list = new NbtList();
        for (Identifier id : sanitized) {
            list.add(NbtString.of(id.toString()));
        }
        orders.put(gem.name(), list);
        root.put(KEY_ABILITY_ORDER, orders);
    }

    // ========== HUD Layout ==========

    private static final String KEY_HUD_LAYOUT = "hudLayout";

    /**
     * Get the player's HUD layout preferences.
     */
    public static GemLoadout.HudLayout getHudLayout(ServerPlayerEntity player) {
        if (!canUseLoadouts(player)) {
            return GemLoadout.HudLayout.defaults();
        }
        NbtCompound root = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        NbtCompound layout = root.getCompound(KEY_HUD_LAYOUT).orElse(null);
        if (layout == null) {
            return GemLoadout.HudLayout.defaults();
        }

        String posName = layout.getString("position", "TOP_LEFT");
        GemLoadout.HudPosition position;
        try {
            position = GemLoadout.HudPosition.valueOf(posName);
        } catch (IllegalArgumentException e) {
            position = GemLoadout.HudPosition.TOP_LEFT;
        }

        boolean showCooldowns = layout.getBoolean("showCooldowns").orElse(true);
        boolean showEnergy = layout.getBoolean("showEnergy").orElse(true);
        boolean compactMode = layout.getBoolean("compactMode").orElse(false);

        return new GemLoadout.HudLayout(position, showCooldowns, showEnergy, compactMode);
    }

    /**
     * Save the player's HUD layout preferences.
     */
    public static void saveHudLayout(ServerPlayerEntity player, GemLoadout.HudLayout layout) {
        NbtCompound root = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        NbtCompound nbt = new NbtCompound();
        nbt.putString("position", layout.position().name());
        nbt.putBoolean("showCooldowns", layout.showCooldowns());
        nbt.putBoolean("showEnergy", layout.showEnergy());
        nbt.putBoolean("compactMode", layout.compactMode());
        root.put(KEY_HUD_LAYOUT, nbt);
    }

    // ========== Active Preset Tracking ==========

    /**
     * Get the active preset index for a gem, or -1 if none.
     */
    public static int getActivePresetIndex(ServerPlayerEntity player, GemId gem) {
        NbtCompound root = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        NbtCompound active = root.getCompound(KEY_ACTIVE_PRESET).orElse(null);
        if (active == null) {
            return -1;
        }
        return active.getInt(gem.name(), -1);
    }

    /**
     * Set the active preset index for a gem.
     */
    public static void setActivePresetIndex(ServerPlayerEntity player, GemId gem, int index) {
        NbtCompound root = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        NbtCompound active = root.getCompound(KEY_ACTIVE_PRESET).orElse(new NbtCompound());
        if (index < 0) {
            active.remove(gem.name());
            if (active.isEmpty()) {
                root.remove(KEY_ACTIVE_PRESET);
            } else {
                root.put(KEY_ACTIVE_PRESET, active);
            }
            return;
        }
        active.putInt(gem.name(), index);
        root.put(KEY_ACTIVE_PRESET, active);
    }

    /**
     * Reset the current gem's loadout to defaults when loadouts are locked.
     */
    public static void resetToDefaults(ServerPlayerEntity player) {
        GemId gem = GemPlayerState.getActiveGem(player);
        GemDefinition def = GemRegistry.definition(gem);
        saveAbilityOrder(player, gem, def.abilities());
        saveHudLayout(player, GemLoadout.HudLayout.defaults());
        GemPlayerState.setPassivesEnabled(player, true);
        setActivePresetIndex(player, gem, -1);
    }

    // ========== Internal Helpers ==========

    private static void saveAllPresets(ServerPlayerEntity player, GemId gem, List<GemLoadout> presets) {
        NbtCompound root = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        NbtCompound loadouts = root.getCompound(KEY_LOADOUTS).orElse(new NbtCompound());

        NbtList list = new NbtList();
        for (GemLoadout loadout : presets) {
            list.add(toNbt(loadout));
        }
        loadouts.put(gem.name(), list);
        root.put(KEY_LOADOUTS, loadouts);
    }

    private static NbtCompound toNbt(GemLoadout loadout) {
        NbtCompound nbt = new NbtCompound();
        nbt.putString("name", loadout.name());
        nbt.putString("gem", loadout.gem().name());
        nbt.putBoolean("passivesEnabled", loadout.passivesEnabled());

        NbtList abilities = new NbtList();
        for (Identifier id : loadout.abilityOrder()) {
            abilities.add(NbtString.of(id.toString()));
        }
        nbt.put("abilityOrder", abilities);

        NbtCompound hudNbt = new NbtCompound();
        hudNbt.putString("position", loadout.hudLayout().position().name());
        hudNbt.putBoolean("showCooldowns", loadout.hudLayout().showCooldowns());
        hudNbt.putBoolean("showEnergy", loadout.hudLayout().showEnergy());
        hudNbt.putBoolean("compactMode", loadout.hudLayout().compactMode());
        nbt.put("hudLayout", hudNbt);

        return nbt;
    }

    private static GemLoadout fromNbt(NbtCompound nbt, GemId gem) {
        String name = nbt.getString("name", "Preset");
        boolean passivesEnabled = nbt.getBoolean("passivesEnabled").orElse(true);

        List<Identifier> abilityOrder = new ArrayList<>();
        NbtList abilities = nbt.getList("abilityOrder").orElse(null);
        if (abilities != null) {
            for (int i = 0; i < abilities.size(); i++) {
                String idStr = abilities.getString(i, "");
                if (!idStr.isEmpty()) {
                    Identifier id = Identifier.tryParse(idStr);
                    if (id != null) {
                        abilityOrder.add(id);
                    }
                }
            }
        }

        GemLoadout.HudLayout hudLayout = GemLoadout.HudLayout.defaults();
        NbtCompound hudNbt = nbt.getCompound("hudLayout").orElse(null);
        if (hudNbt != null) {
            String posName = hudNbt.getString("position", "TOP_LEFT");
            GemLoadout.HudPosition position;
            try {
                position = GemLoadout.HudPosition.valueOf(posName);
            } catch (IllegalArgumentException e) {
                position = GemLoadout.HudPosition.TOP_LEFT;
            }
            boolean showCooldowns = hudNbt.getBoolean("showCooldowns").orElse(true);
            boolean showEnergy = hudNbt.getBoolean("showEnergy").orElse(true);
            boolean compactMode = hudNbt.getBoolean("compactMode").orElse(false);
            hudLayout = new GemLoadout.HudLayout(position, showCooldowns, showEnergy, compactMode);
        }

        return new GemLoadout(name, gem, abilityOrder, passivesEnabled, hudLayout);
    }
}
