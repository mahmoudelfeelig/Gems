package com.feel.gems.item.legendary;

import com.feel.gems.legendary.LegendaryItem;
import com.feel.gems.screen.GemSeerScreenHandler;
import com.feel.gems.util.GemsTooltipFormat;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import java.util.function.Consumer;

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

        player.openHandledScreen(new ExtendedScreenHandlerFactory<GemSeerScreenHandler.OpeningData>() {
            private GemSeerScreenHandler.OpeningData data;

            @Override
            public Text getDisplayName() {
                return Text.translatable("screen.gems.gem_seer.title");
            }

            @Override
            public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity p) {
                if (!(p instanceof ServerPlayerEntity serverPlayer)) {
                    return new GemSeerScreenHandler(syncId, inv);
                }
                GemSeerScreenHandler.OpeningData openingData = ensureData(serverPlayer);
                return new GemSeerScreenHandler(syncId, inv, openingData);
            }

            @Override
            public GemSeerScreenHandler.OpeningData getScreenOpeningData(ServerPlayerEntity player) {
                return ensureData(player);
            }

            private GemSeerScreenHandler.OpeningData ensureData(ServerPlayerEntity serverPlayer) {
                if (data == null) {
                    data = GemSeerScreenHandler.buildOpeningData(serverPlayer);
                }
                return data;
            }
        });

        return ActionResult.SUCCESS;
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return true;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, TooltipDisplayComponent displayComponent, Consumer<Text> tooltip, TooltipType type) {
        GemsTooltipFormat.appendDescription(tooltip, Text.translatable("item.gems.gem_seer.desc"));
    }
}
