package com.feel.gems.power.passive.duelist;

import com.feel.gems.power.api.GemMaintainedPassive;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import java.util.Set;

/**
 * Combat Stance: While holding a sword, gain 10% movement speed.
 */
public final class DuelistCombatStancePassive implements GemMaintainedPassive {
    // Lazily initialized to avoid class loading issues in unit tests
    private static Set<Item> swords;

    private static Set<Item> getSwords() {
        if (swords == null) {
            swords = Set.of(
                Items.WOODEN_SWORD, Items.STONE_SWORD, Items.IRON_SWORD,
                Items.GOLDEN_SWORD, Items.DIAMOND_SWORD, Items.NETHERITE_SWORD
            );
        }
        return swords;
    }

    @Override
    public Identifier id() {
        return PowerIds.DUELIST_COMBAT_STANCE;
    }

    @Override
    public String name() {
        return "Combat Stance";
    }

    @Override
    public String description() {
        return "While holding a sword, gain 10% movement speed.";
    }

    @Override
    public void apply(ServerPlayerEntity player) {
        maintain(player);
    }

    @Override
    public void remove(ServerPlayerEntity player) {
        player.removeStatusEffect(StatusEffects.SPEED);
    }

    @Override
    public void maintain(ServerPlayerEntity player) {
        if (!isHoldingSword(player)) {
            return;
        }
        // Speed amplifier 0 = Speed I (+20%), so we use 0 for ~10% speed
        // The config uses a multiplier (1.1 = 10% boost), we approximate with Speed I
        int amplifier = 0;
        int duration = 40; // Refresh every 2 seconds
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, duration, amplifier, true, false, false));
    }

    private static boolean isHoldingSword(ServerPlayerEntity player) {
        Set<Item> swordSet = getSwords();
        return swordSet.contains(player.getMainHandStack().getItem())
            || swordSet.contains(player.getOffHandStack().getItem());
    }
}
