package com.feel.gems.power.ability.wealth;

import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import com.feel.gems.screen.PocketsScreenHandler;
import com.feel.gems.screen.PocketsStorage;
import com.feel.gems.item.GemOwnership;
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
        return "Opens an extra inventory.";
    }

    @Override
    public int cooldownTicks() {
        return 0;
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        SimpleInventory pockets = PocketsStorage.load(player);
        GemOwnership.purgeInventory(player.getEntityWorld().getServer(), pockets);
        NamedScreenHandlerFactory factory = new NamedScreenHandlerFactory() {
            @Override
            public Text getDisplayName() {
                return Text.translatable("gems.ability.wealth.pockets.title");
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
