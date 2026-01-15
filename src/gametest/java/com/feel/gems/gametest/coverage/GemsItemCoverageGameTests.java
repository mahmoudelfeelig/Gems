package com.feel.gems.gametest.coverage;

import com.feel.gems.gametest.util.GemsGameTestUtil;
import java.util.EnumSet;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.TestContext;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

public final class GemsItemCoverageGameTests {
    private static void teleport(ServerPlayerEntity player, ServerWorld world, double x, double y, double z) {
        player.teleport(world, x, y, z, EnumSet.noneOf(PositionFlag.class), 0.0F, 0.0F, false);
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void allModItemsUseWithoutCrashing(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z);

        context.runAtTick(5L, () -> {
            for (Identifier id : Registries.ITEM.getIds()) {
                if (!"gems".equals(id.getNamespace())) {
                    continue;
                }
                Item item = Registries.ITEM.get(id);
                if (item == null) {
                    continue;
                }
                try {
                    ItemStack stack = new ItemStack(item);
                    player.setStackInHand(Hand.MAIN_HAND, stack);
                    item.use(world, player, Hand.MAIN_HAND);
                } catch (Throwable t) {
                    context.throwGameTestException("Item crashed: " + id + " -> " + t.getMessage());
                    return;
                }
            }
            context.complete();
        });
    }
}
