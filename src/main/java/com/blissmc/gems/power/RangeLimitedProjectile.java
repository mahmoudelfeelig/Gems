package com.blissmc.gems.power;

import net.minecraft.util.math.Vec3d;

public interface RangeLimitedProjectile {
    void gems$setRangeLimit(Vec3d origin, double maxDistanceBlocks);
}

