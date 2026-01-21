package com.feel.gems.mixin.accessor;

import java.util.Optional;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradedItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(TradeOffer.class)
public interface TradeOfferAccessor {
    @Accessor("secondBuyItem")
    Optional<TradedItem> gems$getSecondBuyItem();

    @Accessor("secondBuyItem")
    @Mutable
    void gems$setSecondBuyItem(Optional<TradedItem> secondBuyItem);
}
