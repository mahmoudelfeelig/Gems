package com.feel.gems.power.ability.strength;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemAbility;
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


public final class BountyHuntingAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.BOUNTY_HUNTING;
    }

    @Override
    public String name() {
        return "Bounty Hunting";
    }

    @Override
    public String description() {
        return "Consumes an item and tracks its original owner for a short time.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().strength().bountyCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        ItemStack stack = player.getMainHandStack();
        if (stack.isEmpty()) {
            player.sendMessage(Text.literal("Hold an item to track."), true);
            return false;
        }
        UUID owner = AbilityRuntime.getOwner(stack);
        if (owner == null) {
            player.sendMessage(Text.literal("That item has no recorded owner."), true);
            return false;
        }
        if (owner.equals(player.getUuid())) {
            player.sendMessage(Text.literal("That's your own item."), true);
            return false;
        }
        var target = player.getEntityWorld().getServer().getPlayerManager().getPlayer(owner);
        if (target == null) {
            player.sendMessage(Text.literal("Owner is offline."), true);
            return false;
        }

        stack.decrement(1);
        AbilityRuntime.startBounty(player, owner, GemsBalance.v().strength().bountyDurationTicks());
        AbilityFeedback.sound(player, SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 0.9F, 0.9F);
        AbilityFeedback.burst(player, ParticleTypes.COMPOSTER, 10, 0.25D);
        player.sendMessage(Text.literal("Tracking " + target.getName().getString()), true);
        return true;
    }
}
