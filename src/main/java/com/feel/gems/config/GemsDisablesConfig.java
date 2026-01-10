package com.feel.gems.config;

import java.util.List;

/**
 * Server-side gameplay toggles for disabling content.
 *
 * <p>These are intended to be enforced server-side; clients only use the synced values for UI hiding.</p>
 */
public final class GemsDisablesConfig {
    /**
     * Disabled gems by enum name (case-insensitive), e.g. "summoner".
     */
    public List<String> disabledGems = List.of();

    /**
     * Disabled abilities by id, e.g. "gems:astral_daggers".
     */
    public List<String> disabledAbilities = List.of();

    /**
     * Disabled passives by id, e.g. "gems:soul_capture".
     */
    public List<String> disabledPassives = List.of();

    /**
     * Disabled bonus abilities by id, e.g. "gems:bonus_thunderstrike".
     * These are the special abilities claimable at max energy.
     */
    public List<String> disabledBonusAbilities = List.of();

    /**
     * Disabled bonus passives by id, e.g. "gems:bonus_thorns_aura".
     * These are the special passives claimable at max energy.
     */
    public List<String> disabledBonusPassives = List.of();
}

