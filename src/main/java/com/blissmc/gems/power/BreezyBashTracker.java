package com.blissmc.gems.power;

import com.blissmc.gems.config.GemsBalance;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class BreezyBashTracker {
    private static final Map<UUID, Bash> ACTIVE = new HashMap<>();

    private BreezyBashTracker() {
    }

    public static void track(ServerPlayerEntity caster, ServerPlayerEntity target, int windowTicks) {
        if (windowTicks <= 0) {
            return;
        }
        long until = target.getServerWorld().getTime() + windowTicks;
        ACTIVE.put(target.getUuid(), new Bash(caster.getUuid(), until, false));
    }

    public static void tick(MinecraftServer server) {
        if (ACTIVE.isEmpty()) {
            return;
        }

        Iterator<Map.Entry<UUID, Bash>> it = ACTIVE.entrySet().iterator();
        while (it.hasNext()) {
            var entry = it.next();
            UUID targetId = entry.getKey();
            Bash bash = entry.getValue();

            ServerPlayerEntity target = server.getPlayerManager().getPlayer(targetId);
            if (target == null) {
                it.remove();
                continue;
            }

            long now = target.getServerWorld().getTime();
            if (now > bash.until) {
                it.remove();
                continue;
            }

            boolean onGround = target.isOnGround();
            if (onGround && !bash.wasOnGround) {
                ServerPlayerEntity caster = server.getPlayerManager().getPlayer(bash.caster);
                var source = caster != null ? caster.getDamageSources().playerAttack(caster) : target.getDamageSources().magic();
                target.damage(source, GemsBalance.v().puff().breezyBashImpactDamage());
                it.remove();
                continue;
            }

            bash.wasOnGround = onGround;
        }
    }

    private static final class Bash {
        private final UUID caster;
        private final long until;
        private boolean wasOnGround;

        private Bash(UUID caster, long until, boolean wasOnGround) {
            this.caster = caster;
            this.until = until;
            this.wasOnGround = wasOnGround;
        }
    }
}
