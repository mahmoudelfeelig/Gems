package com.feel.gems.mixin;

import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.GemPowers;
import com.feel.gems.mixin.accessor.TradeOfferAccessor;
import java.util.Optional;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;
import net.minecraft.village.TradedItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(VillagerEntity.class)
public final class VillagerEntityWealthDiscountMixin {
    @Unique
    private TradeOfferList gems$wealthOffersBackup;

    @Inject(method = "prepareOffersFor", at = @At("TAIL"))
    private void gems$applyWealthDiscount(PlayerEntity player, CallbackInfo ci) {
        if (!(player instanceof ServerPlayerEntity serverPlayer)) {
            return;
        }
        if (!GemPowers.isPassiveActive(serverPlayer, PowerIds.WEALTH_CURED_PRICES)) {
            return;
        }
        VillagerEntity villager = (VillagerEntity) (Object) this;
        TradeOfferList offers = villager.getOffers();
        if (gems$wealthOffersBackup == null) {
            gems$wealthOffersBackup = offers.copy();
        }
        for (TradeOffer offer : offers) {
            int current = offer.getDisplayedFirstBuyItem().getCount();
            if (current > 1) {
                offer.increaseSpecialPrice(1 - current);
            }
            Optional<TradedItem> second = offer.getSecondBuyItem();
            if (second.isPresent() && second.get().count() > 1) {
                TradedItem original = second.get();
                TradedItem adjusted = new TradedItem(original.item(), 1, original.components());
                ((TradeOfferAccessor) offer).gems$setSecondBuyItem(Optional.of(adjusted));
            }
        }
    }

    @Inject(method = "resetCustomer", at = @At("TAIL"))
    private void gems$restoreWealthOffers(CallbackInfo ci) {
        if (gems$wealthOffersBackup == null) {
            return;
        }
        VillagerEntity villager = (VillagerEntity) (Object) this;
        villager.setOffers(gems$wealthOffersBackup);
        gems$wealthOffersBackup = null;
    }
}
