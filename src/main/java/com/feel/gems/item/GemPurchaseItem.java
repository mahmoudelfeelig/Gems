package com.feel.gems.item;

import com.feel.gems.screen.TraderScreenHandler;
import com.feel.gems.state.GemsPersistentDataHolder;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;




public final class GemPurchaseItem extends Item {
    private static final String KEY_PENDING = "gemPurchasePending";

    public GemPurchaseItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        if (world.isClient()) {
            return ActionResult.SUCCESS;
        }
        if (!(user instanceof ServerPlayerEntity player)) {
            return ActionResult.PASS;
        }

        markPending(player);
        player.openHandledScreen(new net.minecraft.screen.NamedScreenHandlerFactory() {
            @Override
            public Text getDisplayName() {
                return Text.translatable("screen.gems.purchase.title");
            }

            @Override
            public net.minecraft.screen.ScreenHandler createMenu(int syncId, net.minecraft.entity.player.PlayerInventory inv, PlayerEntity p) {
                return new TraderScreenHandler(syncId, inv);
            }
        });
        return ActionResult.SUCCESS;
    }

    public static boolean consumePending(ServerPlayerEntity player) {
        NbtCompound data = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        if (!data.getBoolean(KEY_PENDING, false)) {
            return false;
        }
        data.remove(KEY_PENDING);
        return true;
    }

    private static void markPending(ServerPlayerEntity player) {
        NbtCompound data = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        data.putBoolean(KEY_PENDING, true);
    }
}
