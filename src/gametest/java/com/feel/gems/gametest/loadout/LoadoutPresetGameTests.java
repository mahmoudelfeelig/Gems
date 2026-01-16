package com.feel.gems.gametest.loadout;

import com.feel.gems.core.GemId;
import com.feel.gems.core.GemRegistry;
import com.feel.gems.gametest.util.GemsGameTestUtil;
import com.feel.gems.loadout.GemLoadout;
import com.feel.gems.loadout.LoadoutManager;
import com.feel.gems.state.GemPlayerState;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.TestContext;
import net.minecraft.util.Identifier;

public final class LoadoutPresetGameTests {
    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void saveAndLoadPresetRestoresSettings(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.FIRE);
        GemPlayerState.setEnergy(player, 6);

        List<Identifier> abilities = GemRegistry.definition(GemId.FIRE).abilities();
        List<Identifier> customOrder = abilities.size() > 1 ? List.of(abilities.get(1), abilities.get(0)) : abilities;
        LoadoutManager.saveAbilityOrder(player, GemId.FIRE, customOrder);
        LoadoutManager.saveHudLayout(player, new GemLoadout.HudLayout(GemLoadout.HudPosition.BOTTOM_RIGHT, false, false, true));
        GemPlayerState.setPassivesEnabled(player, false);

        GemLoadout loadout = LoadoutManager.createFromCurrent(player, "Test");
        int index = LoadoutManager.savePreset(player, loadout);
        if (index != 0) {
            context.throwGameTestException("Expected first preset index 0 but got " + index);
            return;
        }

        LoadoutManager.saveAbilityOrder(player, GemId.FIRE, abilities);
        LoadoutManager.saveHudLayout(player, GemLoadout.HudLayout.defaults());
        GemPlayerState.setPassivesEnabled(player, true);

        if (!LoadoutManager.loadPreset(player, GemId.FIRE, 0)) {
            context.throwGameTestException("Failed to load preset");
            return;
        }

        if (GemPlayerState.arePassivesEnabled(player)) {
            context.throwGameTestException("Expected passives to be disabled after loading preset");
            return;
        }

        if (!LoadoutManager.getAbilityOrder(player, GemId.FIRE).equals(customOrder)) {
            context.throwGameTestException("Ability order not restored by preset");
            return;
        }

        GemLoadout.HudLayout hud = LoadoutManager.getHudLayout(player);
        if (hud.position() != GemLoadout.HudPosition.BOTTOM_RIGHT || hud.showCooldowns() || hud.showEnergy() || !hud.compactMode()) {
            context.throwGameTestException("HUD layout not restored by preset");
            return;
        }

        if (LoadoutManager.getActivePresetIndex(player, GemId.FIRE) != 0) {
            context.throwGameTestException("Active preset index was not set after load");
            return;
        }

        GemsGameTestUtil.complete(context);
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void cannotSavePresetBelowEnergy(TestContext context) {
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.ASTRA);
        GemPlayerState.setEnergy(player, 0);

        GemLoadout loadout = LoadoutManager.createFromCurrent(player, "Locked");
        int index = LoadoutManager.savePreset(player, loadout);
        if (index >= 0) {
            context.throwGameTestException("Preset saved despite insufficient energy");
            return;
        }
        GemsGameTestUtil.complete(context);
    }
}
