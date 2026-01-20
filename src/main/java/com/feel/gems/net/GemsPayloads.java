package com.feel.gems.net;

import com.feel.gems.net.payloads.ShadowCloneSyncPayload;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;




public final class GemsPayloads {
    private static boolean registered = false;

    private GemsPayloads() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        PayloadTypeRegistry.playC2S().register(ActivateAbilityPayload.ID, ActivateAbilityPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ActivateBonusAbilityPayload.ID, ActivateBonusAbilityPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(SoulReleasePayload.ID, SoulReleasePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(FluxChargePayload.ID, FluxChargePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ClientPassiveTogglePayload.ID, ClientPassiveTogglePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(LoadoutSavePayload.ID, LoadoutSavePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(LoadoutLoadPayload.ID, LoadoutLoadPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(LoadoutDeletePayload.ID, LoadoutDeletePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(LoadoutOpenRequestPayload.ID, LoadoutOpenRequestPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(SummonerLoadoutOpenRequestPayload.ID, SummonerLoadoutOpenRequestPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(SummonerLoadoutSavePayload.ID, SummonerLoadoutSavePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(TrackerCompassSelectPayload.ID, TrackerCompassSelectPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(BonusSelectionOpenRequestPayload.ID, BonusSelectionOpenRequestPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(BonusSelectionClaimPayload.ID, BonusSelectionClaimPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(PrismSelectionOpenRequestPayload.ID, PrismSelectionOpenRequestPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(PrismSelectionClaimPayload.ID, PrismSelectionClaimPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(SpyObservedOpenRequestPayload.ID, SpyObservedOpenRequestPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(SpyObservedSelectPayload.ID, SpyObservedSelectPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(TrophyNecklaceOpenRequestPayload.ID, TrophyNecklaceOpenRequestPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(TrophyNecklaceClaimPayload.ID, TrophyNecklaceClaimPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(AugmentOpenRequestPayload.ID, AugmentOpenRequestPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(AugmentRemovePayload.ID, AugmentRemovePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(InscriptionOpenRequestPayload.ID, InscriptionOpenRequestPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(InscriptionRemovePayload.ID, InscriptionRemovePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(TitleSelectionOpenRequestPayload.ID, TitleSelectionOpenRequestPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(TitleSelectionSelectPayload.ID, TitleSelectionSelectPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(StateSyncPayload.ID, StateSyncPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(CooldownSnapshotPayload.ID, CooldownSnapshotPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(AbilityCooldownPayload.ID, AbilityCooldownPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ExtraStatePayload.ID, ExtraStatePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(AbilityOrderSyncPayload.ID, AbilityOrderSyncPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(HudLayoutPayload.ID, HudLayoutPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(SummonerLoadoutScreenPayload.ID, SummonerLoadoutScreenPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(LoadoutScreenPayload.ID, LoadoutScreenPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(TrackerCompassScreenPayload.ID, TrackerCompassScreenPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(SpySkinshiftPayload.ID, SpySkinshiftPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ServerDisablesPayload.ID, ServerDisablesPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ChaosStatePayload.ID, ChaosStatePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ChaosSlotPayload.ID, ChaosSlotPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(BonusSelectionScreenPayload.ID, BonusSelectionScreenPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(BonusAbilitiesSyncPayload.ID, BonusAbilitiesSyncPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(PrismSelectionScreenPayload.ID, PrismSelectionScreenPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(PrismAbilitiesSyncPayload.ID, PrismAbilitiesSyncPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(SpyObservedScreenPayload.ID, SpyObservedScreenPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(TrophyNecklaceScreenPayload.ID, TrophyNecklaceScreenPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(AugmentScreenPayload.ID, AugmentScreenPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(InscriptionScreenPayload.ID, InscriptionScreenPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(TitleSelectionScreenPayload.ID, TitleSelectionScreenPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ShadowCloneSyncPayload.ID, ShadowCloneSyncPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(TricksterControlPayload.ID, TricksterControlPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(RivalrySyncPayload.ID, RivalrySyncPayload.CODEC);
        registered = true;
    }
}
