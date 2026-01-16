package com.feel.gems.gametest.rivalry;

import com.feel.gems.gametest.util.GemsGameTestUtil;
import com.feel.gems.rivalry.RivalryManager;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.test.TestContext;

public final class RivalryGameTests {
    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void assignsTargetAndAppliesDamageBonus(TestContext context) {
        ServerPlayerEntity p1 = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        ServerPlayerEntity p2 = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(p1);
        GemsGameTestUtil.forceSurvival(p2);

        RivalryManager.assignTarget(p1);
        if (!RivalryManager.isRivalryTarget(p1, p2)) {
            context.throwGameTestException("Expected p2 to be assigned as p1 rivalry target");
            return;
        }

        float base = 4.0f;
        float expected = base * RivalryManager.getDamageMultiplier();
        float actual = RivalryManager.applyRivalryBonus(p1, p2, base);
        if (Math.abs(actual - expected) > 0.0001f) {
            context.throwGameTestException("Rivalry bonus damage mismatch: expected " + expected + " got " + actual);
            return;
        }

        GemsGameTestUtil.complete(context);
    }
}
