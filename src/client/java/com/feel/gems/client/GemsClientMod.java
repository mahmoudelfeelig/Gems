package com.feel.gems.client;

import com.feel.gems.item.ModItems;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.client.item.CompassAnglePredicateProvider;
import net.minecraft.client.item.ModelPredicateProviderRegistry;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LodestoneTrackerComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;




public final class GemsClientMod implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientNetworking.register();
        GemsKeybinds.register();
        com.feel.gems.client.hud.GemsHud.register();
        com.feel.gems.client.hud.GemsTooltips.register();
        HandledScreens.register(com.feel.gems.screen.ModScreenHandlers.TRADER, com.feel.gems.client.screen.TraderScreen::new);

        ModelPredicateProviderRegistry.register(
                com.feel.gems.item.ModItems.TRACKER_COMPASS,
                Identifier.of("minecraft", "angle"),
                new CompassAnglePredicateProvider((world, stack, entity) -> {
                    LodestoneTrackerComponent tracker = stack.get(DataComponentTypes.LODESTONE_TRACKER);
                    if (tracker == null) {
                        return null;
                    }
                    return tracker.target().orElse(null);
                })
        );

        ModelPredicateProviderRegistry.register(
                ModItems.HUNTERS_SIGHT_BOW,
                Identifier.of("minecraft", "pull"),
                (stack, world, entity, seed) -> {
                    if (!(entity instanceof LivingEntity living)) {
                        return 0.0F;
                    }
                    if (!living.isUsingItem() || living.getActiveItem() != stack) {
                        return 0.0F;
                    }
                    int use = stack.getMaxUseTime(living) - living.getItemUseTimeLeft();
                    return use / 20.0F;
                }
        );
        ModelPredicateProviderRegistry.register(
                ModItems.HUNTERS_SIGHT_BOW,
                Identifier.of("minecraft", "pulling"),
                (stack, world, entity, seed) ->
                        entity instanceof LivingEntity living && living.isUsingItem() && living.getActiveItem() == stack ? 1.0F : 0.0F
        );
    }
}
