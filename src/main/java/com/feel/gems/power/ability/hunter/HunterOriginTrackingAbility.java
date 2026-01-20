package com.feel.gems.power.ability.hunter;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.legendary.LegendaryPlayerTracker;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.gem.spy.SpySystem;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import com.feel.gems.power.runtime.AbilityRuntime;
import java.util.UUID;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;


/**
 * Origin Tracking - Track the FIRST owner of an item (who crafted/found it).
 * Complements Bounty Hunting (Strength) which tracks the LAST owner.
 */
public final class HunterOriginTrackingAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.HUNTER_ORIGIN_TRACKING;
    }

    @Override
    public String name() {
        return "Origin Tracking";
    }

    @Override
    public String description() {
        return "Track the original owner of an item (who first crafted or found it).";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().hunter().originTrackingCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        ItemStack stack = player.getMainHandStack();
        if (stack.isEmpty()) {
            player.sendMessage(Text.translatable("gems.ability.hunter.origin.hold_item"), true);
            return false;
        }
        UUID firstOwner = AbilityRuntime.getFirstOwner(stack);
        if (firstOwner == null) {
            player.sendMessage(Text.translatable("gems.ability.hunter.origin.no_origin"), true);
            return false;
        }
        if (firstOwner.equals(player.getUuid())) {
            player.sendMessage(Text.translatable("gems.ability.hunter.origin.own_item"), true);
            return false;
        }
        var server = player.getEntityWorld().getServer();
        if (server == null) {
            return false;
        }
        if (SpySystem.hidesTracking(server, firstOwner)) {
            player.sendMessage(Text.translatable("gems.tracking.hidden"), true);
            return false;
        }
        LegendaryPlayerTracker.Snapshot snapshot = LegendaryPlayerTracker.snapshot(server, firstOwner);
        if (snapshot == null) {
            String name = AbilityRuntime.getFirstOwnerName(stack);
            if (name != null && !name.isEmpty()) {
                player.sendMessage(Text.translatable("gems.ability.hunter.origin.owner_offline_named", name), true);
            } else {
                player.sendMessage(Text.translatable("gems.ability.hunter.origin.owner_offline"), true);
            }
            return false;
        }

        // Don't consume the item - origin tracking is non-destructive
        AbilityRuntime.startBounty(player, firstOwner, GemsBalance.v().hunter().originTrackingDurationTicks());
        AbilityFeedback.sound(player, SoundEvents.ENTITY_FOX_SNIFF, 0.8F, 1.0F);
        AbilityFeedback.burst(player, ParticleTypes.GLOW, 15, 0.3D);
        player.sendMessage(Text.translatable("gems.ability.hunter.origin.tracking", snapshot.name()), true);
        return true;
    }
}
