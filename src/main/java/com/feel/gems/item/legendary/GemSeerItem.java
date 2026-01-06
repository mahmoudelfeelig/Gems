package com.feel.gems.item.legendary;

import com.feel.gems.legendary.LegendaryItem;
import com.feel.gems.screen.GemSeerScreenHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

/**
 * Gem Seer - A legendary item that reveals information about any online player's gems.
 * Opens a selection screen to choose a player, then displays their active gem, energy level, and owned gems.
 * Does not have a cooldown and is not consumed on use.
 */
public final class GemSeerItem extends Item implements LegendaryItem {
    private static final String LEGENDARY_ID = "gem_seer";

    public GemSeerItem(Settings settings) {
        super(settings);
    }

    @Override
    public String legendaryId() {
        return LEGENDARY_ID;
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        if (world.isClient()) {
            return ActionResult.SUCCESS;
        }

        if (!(user instanceof ServerPlayerEntity player)) {
            return ActionResult.PASS;
        }

        player.openHandledScreen(new NamedScreenHandlerFactory() {
            @Override
            public Text getDisplayName() {
                return Text.translatable("screen.gems.gem_seer.title");
            }

            @Override
            public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity p) {
                return new GemSeerScreenHandler(syncId, inv);
            }
        });

        return ActionResult.SUCCESS;
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return true;
    }
}
