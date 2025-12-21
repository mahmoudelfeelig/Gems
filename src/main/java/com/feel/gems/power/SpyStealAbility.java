package com.feel.gems.power;

import com.feel.gems.config.GemsBalance;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public final class SpyStealAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.SPY_STEAL;
    }

    @Override
    public String name() {
        return "Steal";
    }

    @Override
    public String description() {
        return "After observing an ability enough times, steal it permanently (until you switch gems).";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().spyMimic().stealCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        return SpyMimicSystem.stealLastSeen(player);
    }
}

