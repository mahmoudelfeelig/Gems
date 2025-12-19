package com.blissmc.gems.power;

import com.blissmc.gems.screen.PocketsScreenHandler;
import com.blissmc.gems.screen.PocketsStorage;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class PocketsAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.POCKETS;
    }

    @Override
    public String name() {
        return "Pockets";
    }

    @Override
    public String description() {
        return "Opens a 9-slot extra inventory.";
    }

    @Override
    public int cooldownTicks() {
        return 0;
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        SimpleInventory pockets = PocketsStorage.load(player);
        NamedScreenHandlerFactory factory = new NamedScreenHandlerFactory() {
            @Override
            public Text getDisplayName() {
                return Text.literal("Pockets");
            }

            @Override
            public net.minecraft.screen.ScreenHandler createMenu(int syncId, net.minecraft.entity.player.PlayerInventory inv, net.minecraft.entity.player.PlayerEntity p) {
                return new PocketsScreenHandler(syncId, inv, player, pockets);
            }
        };
        player.openHandledScreen(factory);
        AbilityFeedback.sound(player, SoundEvents.BLOCK_CHEST_OPEN, 0.8F, 1.1F);
        return true;
    }
}
