package com.feel.gems.power.api;

import net.minecraft.server.network.ServerPlayerEntity;




/**
 * A passive that needs periodic maintenance (kept deliberately low-frequency).
 */
public interface GemMaintainedPassive extends GemPassive {
    void maintain(ServerPlayerEntity player);
}

