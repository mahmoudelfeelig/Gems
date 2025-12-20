package com.feel.gems.item;

import com.feel.gems.core.GemId;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public final class TraderItem extends Item {
    public TraderItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (world.isClient) {
            return TypedActionResult.pass(stack);
        }
        if (!(user instanceof ServerPlayerEntity player)) {
            return TypedActionResult.pass(stack);
        }

        MutableText message = Text.literal("Choose a gem: ").formatted(Formatting.GOLD);
        for (GemId gemId : GemId.values()) {
            String arg = gemId.name().toLowerCase();
            MutableText option = Text.literal("[" + arg + "]")
                    .formatted(Formatting.AQUA)
                    .styled(style -> style
                            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/gems trade " + arg))
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("Trade for " + gemId.name()))));
            message.append(option).append(Text.literal(" "));
        }
        player.sendMessage(message, false);
        return TypedActionResult.success(stack);
    }
}
