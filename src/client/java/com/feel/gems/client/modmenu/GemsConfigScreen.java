package com.feel.gems.client.modmenu;

import com.feel.gems.client.GemsClientConfig;
import com.feel.gems.client.GemsClientConfigManager;
import com.feel.gems.config.GemsBalanceConfig;
import com.feel.gems.config.GemsConfigManager;
import com.feel.gems.net.ClientPassiveTogglePayload;
import java.nio.file.Path;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;




public final class GemsConfigScreen extends Screen {
    private static final int ROW_H = 22;

    private final Screen parent;

    private GemsBalanceConfig cfg;
    private String loadError = null;
    private boolean canEdit = false;
    private GemsClientConfig clientCfg;
    private boolean clientDirty = false;

    private Section section = Section.VISUAL;
    private boolean dirty = false;
    private final ValidationTracker validation = new ValidationTracker();

    private ButtonWidget saveButton;
    private ButtonWidget saveReloadButton;

    private int scrollPx = 0;
    private int maxScrollPx = 0;

    private int sidebarScrollPx = 0;
    private int sidebarMaxScrollPx = 0;
    private int sidebarX = 0;
    private int sidebarY = 0;
    private int sidebarW = 0;
    private int sidebarViewH = 0;

    public GemsConfigScreen(Screen parent) {
        super(Text.literal("Gems Config"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        load();
        rebuild();
    }

    private void load() {
        var result = GemsConfigManager.loadOrCreateForUi();
        this.cfg = normalize(result.config() != null ? result.config() : new GemsBalanceConfig());
        this.loadError = result.error();
        this.dirty = false;
        this.scrollPx = 0;
        this.sidebarScrollPx = 0;
        this.canEdit = canEditConfig();
        this.clientCfg = GemsClientConfigManager.loadOrCreate();
        this.clientDirty = false;
        this.validation.reset();
    }

    private void rebuild() {
        clearChildren();

        int sidebarX = 18;
        int sidebarY = 52;
        int sidebarW = 130;
        int sidebarH = 20;
        int sidebarGap = 4;

        int footerTop = this.height - 80;
        int sidebarViewH = Math.max(0, footerTop - sidebarY - 6);
        int sidebarContentH = Section.values().length * (sidebarH + sidebarGap) - sidebarGap;
        sidebarMaxScrollPx = Math.max(0, sidebarContentH - sidebarViewH);
        int prevSidebarScroll = sidebarScrollPx;
        sidebarScrollPx = clampInt(sidebarScrollPx, 0, sidebarMaxScrollPx);
        if (prevSidebarScroll != sidebarScrollPx) {
            rebuild();
            return;
        }
        this.sidebarX = sidebarX;
        this.sidebarY = sidebarY;
        this.sidebarW = sidebarW;
        this.sidebarViewH = sidebarViewH;

        for (int i = 0; i < Section.values().length; i++) {
            Section s = Section.values()[i];
            int y = sidebarY + i * (sidebarH + sidebarGap) - sidebarScrollPx;
            ButtonWidget b = addDrawableChild(ButtonWidget.builder(Text.literal(s.label), btn -> {
                section = s;
                scrollPx = 0;
                rebuild();
            }).dimensions(sidebarX, y, sidebarW, sidebarH).build());
            b.active = s != section;
        }

        int contentX = sidebarX + sidebarW + 20;
        int fieldsTop = 52;

        int labelX = contentX;
        int available = this.width - contentX - 18;
        int labelW = Math.max(120, available - 120 - 10);
        int maxLabelW = available - 80 - 10;
        if (maxLabelW > 0) {
            labelW = Math.min(labelW, maxLabelW);
        }
        int fieldX = contentX + labelW + 10;
        int fieldW = Math.max(80, Math.min(160, this.width - fieldX - 18));

        int y = fieldsTop - scrollPx;
        int logicalY = fieldsTop;

        validation.reset();

        switch (section) {
            case CLIENT -> {
                y = addClientBoolRow("Enable gem passives", y, labelX, labelW, fieldX, fieldW, () -> clientCfg.passivesEnabled, v -> clientCfg.passivesEnabled = v);
                logicalY += ROW_H;
                y = addClientControlModeRow("Control mode", y, labelX, labelW, fieldX, fieldW, () -> clientCfg.controlMode, v -> clientCfg.controlMode = v);
                logicalY += ROW_H;
            }
            case VISUAL -> {
                y = addBoolRow("Enable particles", y, labelX, labelW, fieldX, fieldW, () -> cfg.visual.enableParticles, v -> cfg.visual.enableParticles = v);
                logicalY += ROW_H;
                y = addBoolRow("Enable sounds", y, labelX, labelW, fieldX, fieldW, () -> cfg.visual.enableSounds, v -> cfg.visual.enableSounds = v);
                logicalY += ROW_H;
                y = addIntRow("Particle scale percent", y, labelX, labelW, fieldX, fieldW, () -> cfg.visual.particleScalePercent, v -> cfg.visual.particleScalePercent = v, 0, 200);
                logicalY += ROW_H;
                y = addIntRow("Max particles per call", y, labelX, labelW, fieldX, fieldW, () -> cfg.visual.maxParticlesPerCall, v -> cfg.visual.maxParticlesPerCall = v, 0, 2048);
                logicalY += ROW_H;
                y = addIntRow("Max beam steps", y, labelX, labelW, fieldX, fieldW, () -> cfg.visual.maxBeamSteps, v -> cfg.visual.maxBeamSteps = v, 0, 2048);
                logicalY += ROW_H;
                y = addIntRow("Max ring points", y, labelX, labelW, fieldX, fieldW, () -> cfg.visual.maxRingPoints, v -> cfg.visual.maxRingPoints = v, 0, 2048);
                logicalY += ROW_H;
            }
            case SYSTEMS -> {
                y = addIntRow("Min max hearts", y, labelX, labelW, fieldX, fieldW, () -> cfg.systems.minMaxHearts, v -> cfg.systems.minMaxHearts = v, 1, 20);
                logicalY += ROW_H;
                y = addIntRow("Assassin trigger hearts", y, labelX, labelW, fieldX, fieldW, () -> cfg.systems.assassinTriggerHearts, v -> cfg.systems.assassinTriggerHearts = v, 1, 20);
                logicalY += ROW_H;
                y = addIntRow("Assassin max hearts", y, labelX, labelW, fieldX, fieldW, () -> cfg.systems.assassinMaxHearts, v -> cfg.systems.assassinMaxHearts = v, 1, 20);
                logicalY += ROW_H;
                y = addIntRow("Assassin elimination hearts threshold", y, labelX, labelW, fieldX, fieldW, () -> cfg.systems.assassinEliminationHeartsThreshold, v -> cfg.systems.assassinEliminationHeartsThreshold = v, 0, 20);
                logicalY += ROW_H;
                y = addIntRow("Assassin vs assassin victim hearts loss", y, labelX, labelW, fieldX, fieldW, () -> cfg.systems.assassinVsAssassinVictimHeartsLoss, v -> cfg.systems.assassinVsAssassinVictimHeartsLoss = v, 0, 20);
                logicalY += ROW_H;
                y = addIntRow("Assassin vs assassin killer hearts gain", y, labelX, labelW, fieldX, fieldW, () -> cfg.systems.assassinVsAssassinKillerHeartsGain, v -> cfg.systems.assassinVsAssassinKillerHeartsGain = v, 0, 20);
                logicalY += ROW_H;
                y = addDoubleRow("Controlled follow start blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.systems.controlledFollowStartBlocks, v -> cfg.systems.controlledFollowStartBlocks = v, 0.0D, 128.0D);
                logicalY += ROW_H;
                y = addDoubleRow("Controlled follow stop blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.systems.controlledFollowStopBlocks, v -> cfg.systems.controlledFollowStopBlocks = v, 0.0D, 128.0D);
                logicalY += ROW_H;
                y = addDoubleRow("Controlled follow speed", y, labelX, labelW, fieldX, fieldW, () -> cfg.systems.controlledFollowSpeed, v -> cfg.systems.controlledFollowSpeed = v, 0.0D, 3.0D);
                logicalY += ROW_H;
            }
            case ASTRA -> {
                y = addIntRow("Shadow Anchor window seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.astra.shadowAnchorWindowSeconds, v -> cfg.astra.shadowAnchorWindowSeconds = v, 0, 60);
                logicalY += ROW_H;
                y = addIntRow("Shadow Anchor post-cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.astra.shadowAnchorPostCooldownSeconds, v -> cfg.astra.shadowAnchorPostCooldownSeconds = v, 0, 3600);
                logicalY += ROW_H;
                y = spacer(y);
                logicalY += 8;
                y = addIntRow("Dimensional Void cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.astra.dimensionalVoidCooldownSeconds, v -> cfg.astra.dimensionalVoidCooldownSeconds = v, 0, 3600);
                logicalY += ROW_H;
                y = addIntRow("Dimensional Void duration seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.astra.dimensionalVoidDurationSeconds, v -> cfg.astra.dimensionalVoidDurationSeconds = v, 0, 60);
                logicalY += ROW_H;
                y = addIntRow("Dimensional Void radius blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.astra.dimensionalVoidRadiusBlocks, v -> cfg.astra.dimensionalVoidRadiusBlocks = v, 0, 32);
                logicalY += ROW_H;
                y = spacer(y);
                logicalY += 8;
                y = addIntRow("Astral Daggers cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.astra.astralDaggersCooldownSeconds, v -> cfg.astra.astralDaggersCooldownSeconds = v, 0, 3600);
                logicalY += ROW_H;
                y = addIntRow("Astral Daggers count", y, labelX, labelW, fieldX, fieldW, () -> cfg.astra.astralDaggersCount, v -> cfg.astra.astralDaggersCount = v, 1, 30);
                logicalY += ROW_H;
                y = addFloatRow("Astral Daggers damage", y, labelX, labelW, fieldX, fieldW, () -> cfg.astra.astralDaggersDamage, v -> cfg.astra.astralDaggersDamage = v, 0.0F, 40.0F);
                logicalY += ROW_H;
                y = addFloatRow("Astral Daggers velocity", y, labelX, labelW, fieldX, fieldW, () -> cfg.astra.astralDaggersVelocity, v -> cfg.astra.astralDaggersVelocity = v, 0.1F, 8.0F);
                logicalY += ROW_H;
                y = addFloatRow("Astral Daggers spread", y, labelX, labelW, fieldX, fieldW, () -> cfg.astra.astralDaggersSpread, v -> cfg.astra.astralDaggersSpread = v, 0.0F, 0.5F);
                logicalY += ROW_H;
                y = spacer(y);
                logicalY += 8;
                y = addIntRow("Unbounded cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.astra.unboundedCooldownSeconds, v -> cfg.astra.unboundedCooldownSeconds = v, 0, 3600);
                logicalY += ROW_H;
                y = addIntRow("Unbounded duration seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.astra.unboundedDurationSeconds, v -> cfg.astra.unboundedDurationSeconds = v, 0, 10);
                logicalY += ROW_H;
                y = spacer(y);
                logicalY += 8;
                y = addIntRow("Astral Camera cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.astra.astralCameraCooldownSeconds, v -> cfg.astra.astralCameraCooldownSeconds = v, 0, 3600);
                logicalY += ROW_H;
                y = addIntRow("Astral Camera duration seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.astra.astralCameraDurationSeconds, v -> cfg.astra.astralCameraDurationSeconds = v, 0, 60);
                logicalY += ROW_H;
                y = spacer(y);
                logicalY += 8;
                y = addIntRow("Spook cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.astra.spookCooldownSeconds, v -> cfg.astra.spookCooldownSeconds = v, 0, 3600);
                logicalY += ROW_H;
                y = addIntRow("Spook radius blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.astra.spookRadiusBlocks, v -> cfg.astra.spookRadiusBlocks = v, 0, 32);
                logicalY += ROW_H;
                y = addIntRow("Spook duration seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.astra.spookDurationSeconds, v -> cfg.astra.spookDurationSeconds = v, 0, 60);
                logicalY += ROW_H;
                y = spacer(y);
                logicalY += 8;
                y = addIntRow("Tag cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.astra.tagCooldownSeconds, v -> cfg.astra.tagCooldownSeconds = v, 0, 3600);
                logicalY += ROW_H;
                y = addIntRow("Tag range blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.astra.tagRangeBlocks, v -> cfg.astra.tagRangeBlocks = v, 0, 128);
                logicalY += ROW_H;
                y = addIntRow("Tag duration seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.astra.tagDurationSeconds, v -> cfg.astra.tagDurationSeconds = v, 0, 120);
                logicalY += ROW_H;
                y = spacer(y);
                logicalY += 8;
                y = addFloatRow("Soul healing (hearts)", y, labelX, labelW, fieldX, fieldW, () -> cfg.astra.soulHealingHearts, v -> cfg.astra.soulHealingHearts = v, 0.0F, 40.0F);
                logicalY += ROW_H;
                y = addDoubleRow("Soul release forward blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.astra.soulReleaseForwardBlocks, v -> cfg.astra.soulReleaseForwardBlocks = v, 0.0D, 32.0D);
                logicalY += ROW_H;
                y = addDoubleRow("Soul release up blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.astra.soulReleaseUpBlocks, v -> cfg.astra.soulReleaseUpBlocks = v, 0.0D, 16.0D);
                logicalY += ROW_H;
            }
            case FIRE -> {
                y = addIntRow("Cosy Campfire cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.fire.cosyCampfireCooldownSeconds, v -> cfg.fire.cosyCampfireCooldownSeconds = v, 0, 3600);
                logicalY += ROW_H;
                y = addIntRow("Cosy Campfire duration seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.fire.cosyCampfireDurationSeconds, v -> cfg.fire.cosyCampfireDurationSeconds = v, 0, 120);
                logicalY += ROW_H;
                y = addIntRow("Cosy Campfire radius blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.fire.cosyCampfireRadiusBlocks, v -> cfg.fire.cosyCampfireRadiusBlocks = v, 0, 32);
                logicalY += ROW_H;
                y = addIntRow("Cosy Campfire regen amplifier", y, labelX, labelW, fieldX, fieldW, () -> cfg.fire.cosyCampfireRegenAmplifier, v -> cfg.fire.cosyCampfireRegenAmplifier = v, 0, 10);
                logicalY += ROW_H;
                y = spacer(y);
                logicalY += 8;
                y = addIntRow("Heat Haze cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.fire.heatHazeCooldownSeconds, v -> cfg.fire.heatHazeCooldownSeconds = v, 0, 3600);
                logicalY += ROW_H;
                y = addIntRow("Heat Haze duration seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.fire.heatHazeDurationSeconds, v -> cfg.fire.heatHazeDurationSeconds = v, 0, 120);
                logicalY += ROW_H;
                y = addIntRow("Heat Haze radius blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.fire.heatHazeRadiusBlocks, v -> cfg.fire.heatHazeRadiusBlocks = v, 0, 32);
                logicalY += ROW_H;
                y = addIntRow("Heat Haze enemy mining fatigue amplifier", y, labelX, labelW, fieldX, fieldW, () -> cfg.fire.heatHazeEnemyMiningFatigueAmplifier, v -> cfg.fire.heatHazeEnemyMiningFatigueAmplifier = v, 0, 10);
                logicalY += ROW_H;
                y = addIntRow("Heat Haze enemy weakness amplifier", y, labelX, labelW, fieldX, fieldW, () -> cfg.fire.heatHazeEnemyWeaknessAmplifier, v -> cfg.fire.heatHazeEnemyWeaknessAmplifier = v, 0, 10);
                logicalY += ROW_H;
                y = spacer(y);
                logicalY += 8;
                y = addIntRow("Fireball charge-up seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.fire.fireballChargeUpSeconds, v -> cfg.fire.fireballChargeUpSeconds = v, 0, 20);
                logicalY += ROW_H;
                y = addIntRow("Fireball charge-down seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.fire.fireballChargeDownSeconds, v -> cfg.fire.fireballChargeDownSeconds = v, 0, 20);
                logicalY += ROW_H;
                y = addIntRow("Fireball internal cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.fire.fireballInternalCooldownSeconds, v -> cfg.fire.fireballInternalCooldownSeconds = v, 0, 3600);
                logicalY += ROW_H;
                y = addIntRow("Fireball max distance blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.fire.fireballMaxDistanceBlocks, v -> cfg.fire.fireballMaxDistanceBlocks = v, 10, 256);
                logicalY += ROW_H;
                y = spacer(y);
                logicalY += 8;
                y = addIntRow("Meteor Shower cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.fire.meteorShowerCooldownSeconds, v -> cfg.fire.meteorShowerCooldownSeconds = v, 0, 3600);
                logicalY += ROW_H;
                y = addIntRow("Meteor Shower target range blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.fire.meteorShowerTargetRangeBlocks, v -> cfg.fire.meteorShowerTargetRangeBlocks = v, 1, 128);
                logicalY += ROW_H;
                y = addIntRow("Meteor Shower count", y, labelX, labelW, fieldX, fieldW, () -> cfg.fire.meteorShowerCount, v -> cfg.fire.meteorShowerCount = v, 0, 50);
                logicalY += ROW_H;
                y = addIntRow("Meteor Shower line length blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.fire.meteorShowerSpreadBlocks, v -> cfg.fire.meteorShowerSpreadBlocks = v, 0, 48);
                logicalY += ROW_H;
                y = addIntRow("Meteor Shower height blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.fire.meteorShowerHeightBlocks, v -> cfg.fire.meteorShowerHeightBlocks = v, 1, 256);
                logicalY += ROW_H;
                y = addFloatRow("Meteor Shower velocity", y, labelX, labelW, fieldX, fieldW, () -> cfg.fire.meteorShowerVelocity, v -> cfg.fire.meteorShowerVelocity = v, 0.1F, 6.0F);
                logicalY += ROW_H;
                y = addIntRow("Meteor Shower explosion power", y, labelX, labelW, fieldX, fieldW, () -> cfg.fire.meteorShowerExplosionPower, v -> cfg.fire.meteorShowerExplosionPower = v, 1, 6);
                logicalY += ROW_H;
            }
            case FLUX -> {
                y = addIntRow("Flux Beam cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.flux.fluxBeamCooldownSeconds, v -> cfg.flux.fluxBeamCooldownSeconds = v, 0, 3600);
                logicalY += ROW_H;
                y = addIntRow("Flux Beam range blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.flux.fluxBeamRangeBlocks, v -> cfg.flux.fluxBeamRangeBlocks = v, 1, 256);
                logicalY += ROW_H;
                y = addFloatRow("Flux Beam min damage", y, labelX, labelW, fieldX, fieldW, () -> cfg.flux.fluxBeamMinDamage, v -> cfg.flux.fluxBeamMinDamage = v, 0.0F, 40.0F);
                logicalY += ROW_H;
                y = addFloatRow("Flux Beam max damage at 100%", y, labelX, labelW, fieldX, fieldW, () -> cfg.flux.fluxBeamMaxDamageAt100, v -> cfg.flux.fluxBeamMaxDamageAt100 = v, 0.0F, 80.0F);
                logicalY += ROW_H;
                y = addFloatRow("Flux Beam max damage at 200%", y, labelX, labelW, fieldX, fieldW, () -> cfg.flux.fluxBeamMaxDamageAt200, v -> cfg.flux.fluxBeamMaxDamageAt200 = v, 0.0F, 120.0F);
                logicalY += ROW_H;
                y = addIntRow("Flux Beam armor damage at 100%", y, labelX, labelW, fieldX, fieldW, () -> cfg.flux.fluxBeamArmorDamageAt100, v -> cfg.flux.fluxBeamArmorDamageAt100 = v, 0, 2000);
                logicalY += ROW_H;
                y = addIntRow("Flux Beam armor damage per percent", y, labelX, labelW, fieldX, fieldW, () -> cfg.flux.fluxBeamArmorDamagePerPercent, v -> cfg.flux.fluxBeamArmorDamagePerPercent = v, 0, 100);
                logicalY += ROW_H;
                y = spacer(y);
                logicalY += 8;
                y = addIntRow("Static Burst cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.flux.staticBurstCooldownSeconds, v -> cfg.flux.staticBurstCooldownSeconds = v, 0, 3600);
                logicalY += ROW_H;
                y = addIntRow("Static Burst radius blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.flux.staticBurstRadiusBlocks, v -> cfg.flux.staticBurstRadiusBlocks = v, 0, 32);
                logicalY += ROW_H;
                y = addFloatRow("Static Burst max damage", y, labelX, labelW, fieldX, fieldW, () -> cfg.flux.staticBurstMaxDamage, v -> cfg.flux.staticBurstMaxDamage = v, 0.0F, 80.0F);
                logicalY += ROW_H;
                y = addIntRow("Static Burst store window seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.flux.staticBurstStoreWindowSeconds, v -> cfg.flux.staticBurstStoreWindowSeconds = v, 0, 600);
                logicalY += ROW_H;
                y = spacer(y);
                logicalY += 8;
                y = addIntRow("Charge: Diamond Block percent", y, labelX, labelW, fieldX, fieldW, () -> cfg.flux.chargeDiamondBlock, v -> cfg.flux.chargeDiamondBlock = v, 0, 200);
                logicalY += ROW_H;
                y = addIntRow("Charge: Gold Block percent", y, labelX, labelW, fieldX, fieldW, () -> cfg.flux.chargeGoldBlock, v -> cfg.flux.chargeGoldBlock = v, 0, 200);
                logicalY += ROW_H;
                y = addIntRow("Charge: Copper Block percent", y, labelX, labelW, fieldX, fieldW, () -> cfg.flux.chargeCopperBlock, v -> cfg.flux.chargeCopperBlock = v, 0, 200);
                logicalY += ROW_H;
                y = addIntRow("Charge: Emerald Block percent", y, labelX, labelW, fieldX, fieldW, () -> cfg.flux.chargeEmeraldBlock, v -> cfg.flux.chargeEmeraldBlock = v, 0, 200);
                logicalY += ROW_H;
                y = addIntRow("Charge: Amethyst Block percent", y, labelX, labelW, fieldX, fieldW, () -> cfg.flux.chargeAmethystBlock, v -> cfg.flux.chargeAmethystBlock = v, 0, 200);
                logicalY += ROW_H;
                y = addIntRow("Charge: Netherite Scrap percent", y, labelX, labelW, fieldX, fieldW, () -> cfg.flux.chargeNetheriteScrap, v -> cfg.flux.chargeNetheriteScrap = v, 0, 200);
                logicalY += ROW_H;
                y = addIntRow("Charge: Enchanted diamond item percent", y, labelX, labelW, fieldX, fieldW, () -> cfg.flux.chargeEnchantedDiamondItem, v -> cfg.flux.chargeEnchantedDiamondItem = v, 0, 200);
                logicalY += ROW_H;
                y = spacer(y);
                logicalY += 8;
                y = addIntRow("Overcharge delay seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.flux.overchargeDelaySeconds, v -> cfg.flux.overchargeDelaySeconds = v, 0, 60);
                logicalY += ROW_H;
                y = addIntRow("Overcharge percent per second", y, labelX, labelW, fieldX, fieldW, () -> cfg.flux.overchargePerSecond, v -> cfg.flux.overchargePerSecond = v, 0, 100);
                logicalY += ROW_H;
                y = addFloatRow("Overcharge self-damage per second", y, labelX, labelW, fieldX, fieldW, () -> cfg.flux.overchargeSelfDamagePerSecond, v -> cfg.flux.overchargeSelfDamagePerSecond = v, 0.0F, 20.0F);
                logicalY += ROW_H;
            }
            case LIFE -> {
                y = addIntRow("Vitality Vortex cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.life.vitalityVortexCooldownSeconds, v -> cfg.life.vitalityVortexCooldownSeconds = v, 0, 3600);
                logicalY += ROW_H;
                y = addIntRow("Vitality Vortex radius blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.life.vitalityVortexRadiusBlocks, v -> cfg.life.vitalityVortexRadiusBlocks = v, 0, 32);
                logicalY += ROW_H;
                y = addIntRow("Vitality Vortex duration seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.life.vitalityVortexDurationSeconds, v -> cfg.life.vitalityVortexDurationSeconds = v, 0, 120);
                logicalY += ROW_H;
                y = addIntRow("Vitality Vortex scan radius blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.life.vitalityVortexScanRadiusBlocks, v -> cfg.life.vitalityVortexScanRadiusBlocks = v, 1, 6);
                logicalY += ROW_H;
                y = addIntRow("Vitality Vortex verdant threshold", y, labelX, labelW, fieldX, fieldW, () -> cfg.life.vitalityVortexVerdantThreshold, v -> cfg.life.vitalityVortexVerdantThreshold = v, 0, 64);
                logicalY += ROW_H;
                y = addFloatRow("Vitality Vortex ally heal", y, labelX, labelW, fieldX, fieldW, () -> cfg.life.vitalityVortexAllyHeal, v -> cfg.life.vitalityVortexAllyHeal = v, 0.0F, 40.0F);
                logicalY += ROW_H;
                y = spacer(y);
                logicalY += 8;
                y = addIntRow("Health Drain cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.life.healthDrainCooldownSeconds, v -> cfg.life.healthDrainCooldownSeconds = v, 0, 3600);
                logicalY += ROW_H;
                y = addIntRow("Health Drain range blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.life.healthDrainRangeBlocks, v -> cfg.life.healthDrainRangeBlocks = v, 0, 128);
                logicalY += ROW_H;
                y = addFloatRow("Health Drain amount", y, labelX, labelW, fieldX, fieldW, () -> cfg.life.healthDrainAmount, v -> cfg.life.healthDrainAmount = v, 0.0F, 40.0F);
                logicalY += ROW_H;
                y = spacer(y);
                logicalY += 8;
                y = addIntRow("Life Swap cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.life.lifeSwapCooldownSeconds, v -> cfg.life.lifeSwapCooldownSeconds = v, 0, 3600);
                logicalY += ROW_H;
                y = addIntRow("Life Swap range blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.life.lifeSwapRangeBlocks, v -> cfg.life.lifeSwapRangeBlocks = v, 0, 128);
                logicalY += ROW_H;
                y = addFloatRow("Life Swap min hearts", y, labelX, labelW, fieldX, fieldW, () -> cfg.life.lifeSwapMinHearts, v -> cfg.life.lifeSwapMinHearts = v, 1.0F, 20.0F);
                logicalY += ROW_H;
                y = spacer(y);
                logicalY += 8;
                y = addIntRow("Life Circle cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.life.lifeCircleCooldownSeconds, v -> cfg.life.lifeCircleCooldownSeconds = v, 0, 3600);
                logicalY += ROW_H;
                y = addIntRow("Life Circle duration seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.life.lifeCircleDurationSeconds, v -> cfg.life.lifeCircleDurationSeconds = v, 0, 120);
                logicalY += ROW_H;
                y = addIntRow("Life Circle radius blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.life.lifeCircleRadiusBlocks, v -> cfg.life.lifeCircleRadiusBlocks = v, 0, 32);
                logicalY += ROW_H;
                y = addDoubleRow("Life Circle max health delta", y, labelX, labelW, fieldX, fieldW, () -> cfg.life.lifeCircleMaxHealthDelta, v -> cfg.life.lifeCircleMaxHealthDelta = v, 0.0D, 40.0D);
                logicalY += ROW_H;
                y = spacer(y);
                logicalY += 8;
                y = addIntRow("Heart Lock cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.life.heartLockCooldownSeconds, v -> cfg.life.heartLockCooldownSeconds = v, 0, 3600);
                logicalY += ROW_H;
                y = addIntRow("Heart Lock duration seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.life.heartLockDurationSeconds, v -> cfg.life.heartLockDurationSeconds = v, 0, 60);
                logicalY += ROW_H;
                y = addIntRow("Heart Lock range blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.life.heartLockRangeBlocks, v -> cfg.life.heartLockRangeBlocks = v, 0, 128);
                logicalY += ROW_H;
            }
            case PUFF -> {
                y = addIntRow("Double Jump cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.puff.doubleJumpCooldownSeconds, v -> cfg.puff.doubleJumpCooldownSeconds = v, 0, 3600);
                logicalY += ROW_H;
                y = addDoubleRow("Double Jump velocity Y", y, labelX, labelW, fieldX, fieldW, () -> cfg.puff.doubleJumpVelocityY, v -> cfg.puff.doubleJumpVelocityY = v, 0.0D, 3.0D);
                logicalY += ROW_H;
                y = spacer(y);
                logicalY += 8;
                y = addIntRow("Dash cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.puff.dashCooldownSeconds, v -> cfg.puff.dashCooldownSeconds = v, 0, 3600);
                logicalY += ROW_H;
                y = addDoubleRow("Dash velocity", y, labelX, labelW, fieldX, fieldW, () -> cfg.puff.dashVelocity, v -> cfg.puff.dashVelocity = v, 0.0D, 5.0D);
                logicalY += ROW_H;
                y = addFloatRow("Dash damage", y, labelX, labelW, fieldX, fieldW, () -> cfg.puff.dashDamage, v -> cfg.puff.dashDamage = v, 0.0F, 40.0F);
                logicalY += ROW_H;
                y = addDoubleRow("Dash hit range blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.puff.dashHitRangeBlocks, v -> cfg.puff.dashHitRangeBlocks = v, 0.5D, 16.0D);
                logicalY += ROW_H;
                y = spacer(y);
                logicalY += 8;
                y = addIntRow("Breezy Bash cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.puff.breezyBashCooldownSeconds, v -> cfg.puff.breezyBashCooldownSeconds = v, 0, 3600);
                logicalY += ROW_H;
                y = addIntRow("Breezy Bash range blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.puff.breezyBashRangeBlocks, v -> cfg.puff.breezyBashRangeBlocks = v, 0, 64);
                logicalY += ROW_H;
                y = addDoubleRow("Breezy Bash up velocity Y", y, labelX, labelW, fieldX, fieldW, () -> cfg.puff.breezyBashUpVelocityY, v -> cfg.puff.breezyBashUpVelocityY = v, 0.0D, 5.0D);
                logicalY += ROW_H;
                y = addDoubleRow("Breezy Bash knockback", y, labelX, labelW, fieldX, fieldW, () -> cfg.puff.breezyBashKnockback, v -> cfg.puff.breezyBashKnockback = v, 0.0D, 5.0D);
                logicalY += ROW_H;
                y = addFloatRow("Breezy Bash initial damage", y, labelX, labelW, fieldX, fieldW, () -> cfg.puff.breezyBashInitialDamage, v -> cfg.puff.breezyBashInitialDamage = v, 0.0F, 40.0F);
                logicalY += ROW_H;
                y = addFloatRow("Breezy Bash impact damage", y, labelX, labelW, fieldX, fieldW, () -> cfg.puff.breezyBashImpactDamage, v -> cfg.puff.breezyBashImpactDamage = v, 0.0F, 80.0F);
                logicalY += ROW_H;
                y = addIntRow("Breezy Bash impact window seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.puff.breezyBashImpactWindowSeconds, v -> cfg.puff.breezyBashImpactWindowSeconds = v, 0, 60);
                logicalY += ROW_H;
                y = spacer(y);
                logicalY += 8;
                y = addIntRow("Group Bash cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.puff.groupBashCooldownSeconds, v -> cfg.puff.groupBashCooldownSeconds = v, 0, 3600);
                logicalY += ROW_H;
                y = addIntRow("Group Bash radius blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.puff.groupBashRadiusBlocks, v -> cfg.puff.groupBashRadiusBlocks = v, 0, 64);
                logicalY += ROW_H;
                y = addDoubleRow("Group Bash knockback", y, labelX, labelW, fieldX, fieldW, () -> cfg.puff.groupBashKnockback, v -> cfg.puff.groupBashKnockback = v, 0.0D, 5.0D);
                logicalY += ROW_H;
                y = addDoubleRow("Group Bash up velocity Y", y, labelX, labelW, fieldX, fieldW, () -> cfg.puff.groupBashUpVelocityY, v -> cfg.puff.groupBashUpVelocityY = v, 0.0D, 5.0D);
                logicalY += ROW_H;
            }
            case SPEED -> {
                y = addIntRow("Arc Shot cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.speed.arcShotCooldownSeconds, v -> cfg.speed.arcShotCooldownSeconds = v, 0, 3600);
                logicalY += ROW_H;
                y = addIntRow("Arc Shot range blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.speed.arcShotRangeBlocks, v -> cfg.speed.arcShotRangeBlocks = v, 0, 256);
                logicalY += ROW_H;
                y = addDoubleRow("Arc Shot radius blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.speed.arcShotRadiusBlocks, v -> cfg.speed.arcShotRadiusBlocks = v, 0.0D, 16.0D);
                logicalY += ROW_H;
                y = addIntRow("Arc Shot max targets", y, labelX, labelW, fieldX, fieldW, () -> cfg.speed.arcShotMaxTargets, v -> cfg.speed.arcShotMaxTargets = v, 1, 10);
                logicalY += ROW_H;
                y = addFloatRow("Arc Shot damage", y, labelX, labelW, fieldX, fieldW, () -> cfg.speed.arcShotDamage, v -> cfg.speed.arcShotDamage = v, 0.0F, 40.0F);
                logicalY += ROW_H;
                y = spacer(y);
                logicalY += 8;
                y = addIntRow("Speed Storm cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.speed.speedStormCooldownSeconds, v -> cfg.speed.speedStormCooldownSeconds = v, 0, 3600);
                logicalY += ROW_H;
                y = addIntRow("Speed Storm duration seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.speed.speedStormDurationSeconds, v -> cfg.speed.speedStormDurationSeconds = v, 0, 120);
                logicalY += ROW_H;
                y = addIntRow("Speed Storm radius blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.speed.speedStormRadiusBlocks, v -> cfg.speed.speedStormRadiusBlocks = v, 0, 32);
                logicalY += ROW_H;
                y = addIntRow("Speed Storm ally speed amplifier", y, labelX, labelW, fieldX, fieldW, () -> cfg.speed.speedStormAllySpeedAmplifier, v -> cfg.speed.speedStormAllySpeedAmplifier = v, 0, 10);
                logicalY += ROW_H;
                y = addIntRow("Speed Storm ally haste amplifier", y, labelX, labelW, fieldX, fieldW, () -> cfg.speed.speedStormAllyHasteAmplifier, v -> cfg.speed.speedStormAllyHasteAmplifier = v, 0, 10);
                logicalY += ROW_H;
                y = addIntRow("Speed Storm enemy slowness amplifier", y, labelX, labelW, fieldX, fieldW, () -> cfg.speed.speedStormEnemySlownessAmplifier, v -> cfg.speed.speedStormEnemySlownessAmplifier = v, 0, 10);
                logicalY += ROW_H;
                y = addIntRow("Speed Storm enemy mining fatigue amplifier", y, labelX, labelW, fieldX, fieldW, () -> cfg.speed.speedStormEnemyMiningFatigueAmplifier, v -> cfg.speed.speedStormEnemyMiningFatigueAmplifier = v, 0, 10);
                logicalY += ROW_H;
                y = spacer(y);
                logicalY += 8;
                y = addIntRow("Terminal Velocity cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.speed.terminalVelocityCooldownSeconds, v -> cfg.speed.terminalVelocityCooldownSeconds = v, 0, 3600);
                logicalY += ROW_H;
                y = addIntRow("Terminal Velocity duration seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.speed.terminalVelocityDurationSeconds, v -> cfg.speed.terminalVelocityDurationSeconds = v, 0, 120);
                logicalY += ROW_H;
                y = addIntRow("Terminal Velocity speed amplifier", y, labelX, labelW, fieldX, fieldW, () -> cfg.speed.terminalVelocitySpeedAmplifier, v -> cfg.speed.terminalVelocitySpeedAmplifier = v, 0, 10);
                logicalY += ROW_H;
                y = addIntRow("Terminal Velocity haste amplifier", y, labelX, labelW, fieldX, fieldW, () -> cfg.speed.terminalVelocityHasteAmplifier, v -> cfg.speed.terminalVelocityHasteAmplifier = v, 0, 10);
                logicalY += ROW_H;
            }
            case STRENGTH -> {
                y = addIntRow("Nullify cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.strength.nullifyCooldownSeconds, v -> cfg.strength.nullifyCooldownSeconds = v, 0, 3600);
                logicalY += ROW_H;
                y = addIntRow("Nullify radius blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.strength.nullifyRadiusBlocks, v -> cfg.strength.nullifyRadiusBlocks = v, 0, 32);
                logicalY += ROW_H;
                y = spacer(y);
                logicalY += 8;
                y = addIntRow("Frailer cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.strength.frailerCooldownSeconds, v -> cfg.strength.frailerCooldownSeconds = v, 0, 3600);
                logicalY += ROW_H;
                y = addIntRow("Frailer range blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.strength.frailerRangeBlocks, v -> cfg.strength.frailerRangeBlocks = v, 0, 128);
                logicalY += ROW_H;
                y = addIntRow("Frailer duration seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.strength.frailerDurationSeconds, v -> cfg.strength.frailerDurationSeconds = v, 0, 120);
                logicalY += ROW_H;
                y = spacer(y);
                logicalY += 8;
                y = addIntRow("Bounty Hunting cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.strength.bountyCooldownSeconds, v -> cfg.strength.bountyCooldownSeconds = v, 0, 3600);
                logicalY += ROW_H;
                y = addIntRow("Bounty Hunting duration seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.strength.bountyDurationSeconds, v -> cfg.strength.bountyDurationSeconds = v, 0, 3600);
                logicalY += ROW_H;
                y = spacer(y);
                logicalY += 8;
                y = addIntRow("Chad Strength cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.strength.chadCooldownSeconds, v -> cfg.strength.chadCooldownSeconds = v, 0, 3600);
                logicalY += ROW_H;
                y = addIntRow("Chad Strength duration seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.strength.chadDurationSeconds, v -> cfg.strength.chadDurationSeconds = v, 0, 3600);
                logicalY += ROW_H;
                y = addIntRow("Chad Strength every hits", y, labelX, labelW, fieldX, fieldW, () -> cfg.strength.chadEveryHits, v -> cfg.strength.chadEveryHits = v, 1, 20);
                logicalY += ROW_H;
                y = addFloatRow("Chad Strength bonus damage", y, labelX, labelW, fieldX, fieldW, () -> cfg.strength.chadBonusDamage, v -> cfg.strength.chadBonusDamage = v, 0.0F, 80.0F);
                logicalY += ROW_H;
            }
            case WEALTH -> {
                y = addIntRow("Fumble cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.wealth.fumbleCooldownSeconds, v -> cfg.wealth.fumbleCooldownSeconds = v, 0, 3600);
                logicalY += ROW_H;
                y = addIntRow("Fumble duration seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.wealth.fumbleDurationSeconds, v -> cfg.wealth.fumbleDurationSeconds = v, 0, 120);
                logicalY += ROW_H;
                y = addIntRow("Fumble radius blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.wealth.fumbleRadiusBlocks, v -> cfg.wealth.fumbleRadiusBlocks = v, 0, 32);
                logicalY += ROW_H;
                y = spacer(y);
                logicalY += 8;
                y = addIntRow("Item Lock cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.wealth.hotbarLockCooldownSeconds, v -> cfg.wealth.hotbarLockCooldownSeconds = v, 0, 3600);
                logicalY += ROW_H;
                y = addIntRow("Item Lock duration seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.wealth.hotbarLockDurationSeconds, v -> cfg.wealth.hotbarLockDurationSeconds = v, 0, 120);
                logicalY += ROW_H;
                y = addIntRow("Item Lock range blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.wealth.hotbarLockRangeBlocks, v -> cfg.wealth.hotbarLockRangeBlocks = v, 0, 128);
                logicalY += ROW_H;
                y = spacer(y);
                logicalY += 8;
                y = addIntRow("Amplification cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.wealth.amplificationCooldownSeconds, v -> cfg.wealth.amplificationCooldownSeconds = v, 0, 3600);
                logicalY += ROW_H;
                y = addIntRow("Amplification duration seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.wealth.amplificationDurationSeconds, v -> cfg.wealth.amplificationDurationSeconds = v, 0, 600);
                logicalY += ROW_H;
                y = spacer(y);
                logicalY += 8;
                y = addIntRow("Rich Rush cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.wealth.richRushCooldownSeconds, v -> cfg.wealth.richRushCooldownSeconds = v, 0, 24 * 3600);
                logicalY += ROW_H;
                y = addIntRow("Rich Rush duration seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.wealth.richRushDurationSeconds, v -> cfg.wealth.richRushDurationSeconds = v, 0, 24 * 3600);
                logicalY += ROW_H;
                y = addIntRow("Pockets rows", y, labelX, labelW, fieldX, fieldW, () -> cfg.wealth.pocketsRows, v -> cfg.wealth.pocketsRows = v, 1, 6);
                logicalY += ROW_H;
            }
            case TERROR -> {
                y = addIntRow("Dread Aura radius blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.terror.dreadAuraRadiusBlocks, v -> cfg.terror.dreadAuraRadiusBlocks = v, 0, 64);
                logicalY += ROW_H;
                y = addIntRow("Dread Aura amplifier", y, labelX, labelW, fieldX, fieldW, () -> cfg.terror.dreadAuraAmplifier, v -> cfg.terror.dreadAuraAmplifier = v, 0, 10);
                logicalY += ROW_H;
                y = addIntRow("Blood Price duration seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.terror.bloodPriceDurationSeconds, v -> cfg.terror.bloodPriceDurationSeconds = v, 0, 120);
                logicalY += ROW_H;
                y = addIntRow("Blood Price strength amplifier", y, labelX, labelW, fieldX, fieldW, () -> cfg.terror.bloodPriceStrengthAmplifier, v -> cfg.terror.bloodPriceStrengthAmplifier = v, 0, 10);
                logicalY += ROW_H;
                y = addIntRow("Blood Price resistance amplifier", y, labelX, labelW, fieldX, fieldW, () -> cfg.terror.bloodPriceResistanceAmplifier, v -> cfg.terror.bloodPriceResistanceAmplifier = v, 0, 10);
                logicalY += ROW_H;
                y = spacer(y);
                logicalY += 8;
                y = addIntRow("Terror Trade cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.terror.terrorTradeCooldownSeconds, v -> cfg.terror.terrorTradeCooldownSeconds = v, 0, 3600);
                logicalY += ROW_H;
                y = addIntRow("Terror Trade range blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.terror.terrorTradeRangeBlocks, v -> cfg.terror.terrorTradeRangeBlocks = v, 0, 128);
                logicalY += ROW_H;
                y = addIntRow("Terror Trade max uses", y, labelX, labelW, fieldX, fieldW, () -> cfg.terror.terrorTradeMaxUses, v -> cfg.terror.terrorTradeMaxUses = v, 0, 20);
                logicalY += ROW_H;
                y = addIntRow("Terror Trade hearts cost", y, labelX, labelW, fieldX, fieldW, () -> cfg.terror.terrorTradeHeartsCost, v -> cfg.terror.terrorTradeHeartsCost = v, 0, 20);
                logicalY += ROW_H;
                y = addIntRow("Terror Trade permanent energy penalty", y, labelX, labelW, fieldX, fieldW, () -> cfg.terror.terrorTradePermanentEnergyPenalty, v -> cfg.terror.terrorTradePermanentEnergyPenalty = v, 0, 10);
                logicalY += ROW_H;
                y = addIntRow("Terror Trade normal target hearts penalty", y, labelX, labelW, fieldX, fieldW, () -> cfg.terror.terrorTradeNormalTargetHeartsPenalty, v -> cfg.terror.terrorTradeNormalTargetHeartsPenalty = v, 0, 20);
                logicalY += ROW_H;
                y = addIntRow("Terror Trade normal target energy penalty", y, labelX, labelW, fieldX, fieldW, () -> cfg.terror.terrorTradeNormalTargetEnergyPenalty, v -> cfg.terror.terrorTradeNormalTargetEnergyPenalty = v, 0, 10);
                logicalY += ROW_H;
                y = spacer(y);
                logicalY += 8;
                y = addIntRow("Panic Ring cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.terror.panicRingCooldownSeconds, v -> cfg.terror.panicRingCooldownSeconds = v, 0, 3600);
                logicalY += ROW_H;
                y = addIntRow("Panic Ring TNT count", y, labelX, labelW, fieldX, fieldW, () -> cfg.terror.panicRingTntCount, v -> cfg.terror.panicRingTntCount = v, 0, 50);
                logicalY += ROW_H;
                y = addIntRow("Panic Ring fuse ticks", y, labelX, labelW, fieldX, fieldW, () -> cfg.terror.panicRingFuseTicks, v -> cfg.terror.panicRingFuseTicks = v, 0, 200);
                logicalY += ROW_H;
                y = addDoubleRow("Panic Ring radius blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.terror.panicRingRadiusBlocks, v -> cfg.terror.panicRingRadiusBlocks = v, 0.0D, 8.0D);
                logicalY += ROW_H;
                y = spacer(y);
                logicalY += 8;
                y = addIntRow("Rig cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.terror.rigCooldownSeconds, v -> cfg.terror.rigCooldownSeconds = v, 0, 3600);
                logicalY += ROW_H;
                y = addIntRow("Rig range blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.terror.rigRangeBlocks, v -> cfg.terror.rigRangeBlocks = v, 0, 128);
                logicalY += ROW_H;
                y = addIntRow("Rig duration seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.terror.rigDurationSeconds, v -> cfg.terror.rigDurationSeconds = v, 0, 600);
                logicalY += ROW_H;
                y = addIntRow("Rig fuse ticks", y, labelX, labelW, fieldX, fieldW, () -> cfg.terror.rigFuseTicks, v -> cfg.terror.rigFuseTicks = v, 0, 200);
                logicalY += ROW_H;
                y = addIntRow("Rig TNT count", y, labelX, labelW, fieldX, fieldW, () -> cfg.terror.rigTntCount, v -> cfg.terror.rigTntCount = v, 0, 50);
                logicalY += ROW_H;
                y = spacer(y);
                logicalY += 8;
                y = addIntRow("Remote Charge arm window seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.terror.remoteChargeArmWindowSeconds, v -> cfg.terror.remoteChargeArmWindowSeconds = v, 0, 120);
                logicalY += ROW_H;
                y = addIntRow("Remote Charge detonate window seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.terror.remoteChargeDetonateWindowSeconds, v -> cfg.terror.remoteChargeDetonateWindowSeconds = v, 0, 600);
                logicalY += ROW_H;
                y = addIntRow("Remote Charge fuse ticks", y, labelX, labelW, fieldX, fieldW, () -> cfg.terror.remoteChargeFuseTicks, v -> cfg.terror.remoteChargeFuseTicks = v, 0, 200);
                logicalY += ROW_H;
                y = addIntRow("Remote Charge cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.terror.remoteChargeCooldownSeconds, v -> cfg.terror.remoteChargeCooldownSeconds = v, 0, 3600);
                logicalY += ROW_H;
                y = spacer(y);
                logicalY += 8;
                y = addIntRow("Breach Charge cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.terror.breachChargeCooldownSeconds, v -> cfg.terror.breachChargeCooldownSeconds = v, 0, 3600);
                logicalY += ROW_H;
                y = addIntRow("Breach Charge range blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.terror.breachChargeRangeBlocks, v -> cfg.terror.breachChargeRangeBlocks = v, 0, 128);
                logicalY += ROW_H;
                y = addIntRow("Breach Charge fuse ticks", y, labelX, labelW, fieldX, fieldW, () -> cfg.terror.breachChargeFuseTicks, v -> cfg.terror.breachChargeFuseTicks = v, 0, 200);
                logicalY += ROW_H;
                y = addFloatRow("Breach Charge explosion power", y, labelX, labelW, fieldX, fieldW, () -> cfg.terror.breachChargeExplosionPower, v -> cfg.terror.breachChargeExplosionPower = v, 0.1F, 10.0F);
                logicalY += ROW_H;
            }
            case SUMMONER -> {
                y = addIntRow("Summon point cap", y, labelX, labelW, fieldX, fieldW, () -> cfg.summoner.maxPoints, v -> cfg.summoner.maxPoints = v, 0, 200);
                logicalY += ROW_H;
                y = addIntRow("Max active summons", y, labelX, labelW, fieldX, fieldW, () -> cfg.summoner.maxActiveSummons, v -> cfg.summoner.maxActiveSummons = v, 0, 200);
                logicalY += ROW_H;
                y = addIntRow("Summon lifetime seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.summoner.summonLifetimeSeconds, v -> cfg.summoner.summonLifetimeSeconds = v, 0, 3600);
                logicalY += ROW_H;
                y = addIntRow("Command range blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.summoner.commandRangeBlocks, v -> cfg.summoner.commandRangeBlocks = v, 0, 128);
                logicalY += ROW_H;
                y = addIntRow("Summon cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.summoner.summonSlotCooldownSeconds, v -> cfg.summoner.summonSlotCooldownSeconds = v, 0, 3600);
                logicalY += ROW_H;
                y = addIntRow("Recall cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.summoner.recallCooldownSeconds, v -> cfg.summoner.recallCooldownSeconds = v, 0, 3600);
                logicalY += ROW_H;
                y = addIntRow("Commander's Mark duration seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.summoner.commandersMarkDurationSeconds, v -> cfg.summoner.commandersMarkDurationSeconds = v, 0, 120);
                logicalY += ROW_H;
                y = addIntRow("Commander's Mark strength amplifier", y, labelX, labelW, fieldX, fieldW, () -> cfg.summoner.commandersMarkStrengthAmplifier, v -> cfg.summoner.commandersMarkStrengthAmplifier = v, 0, 10);
                logicalY += ROW_H;
                y = addFloatRow("Summon bonus health", y, labelX, labelW, fieldX, fieldW, () -> cfg.summoner.summonBonusHealth, v -> cfg.summoner.summonBonusHealth = v, 0.0F, 40.0F);
                logicalY += ROW_H;
                y = addDoubleRow("Summon spawn forward blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.summoner.summonSpawnForwardBlocks, v -> cfg.summoner.summonSpawnForwardBlocks = v, 0.0D, 16.0D);
                logicalY += ROW_H;
                y = addDoubleRow("Summon spawn up blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.summoner.summonSpawnUpBlocks, v -> cfg.summoner.summonSpawnUpBlocks = v, 0.0D, 8.0D);
                logicalY += ROW_H;
                y = addDoubleRow("Summon spawn ring base blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.summoner.summonSpawnRingBaseBlocks, v -> cfg.summoner.summonSpawnRingBaseBlocks = v, 0.0D, 4.0D);
                logicalY += ROW_H;
                y = addDoubleRow("Summon spawn ring step blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.summoner.summonSpawnRingStepBlocks, v -> cfg.summoner.summonSpawnRingStepBlocks = v, 0.0D, 4.0D);
                logicalY += ROW_H;
                y = addIntRow("Summon spawn ring layers", y, labelX, labelW, fieldX, fieldW, () -> cfg.summoner.summonSpawnRingLayers, v -> cfg.summoner.summonSpawnRingLayers = v, 1, 8);
                logicalY += ROW_H;
                y = addIntRow("Summon spawn ring segments", y, labelX, labelW, fieldX, fieldW, () -> cfg.summoner.summonSpawnRingSegments, v -> cfg.summoner.summonSpawnRingSegments = v, 1, 16);
                logicalY += ROW_H;
                y = spacer(y);
                logicalY += 8;
                y = addNoteRow("Edit summon costs/loadouts in balance.json", y, labelX, labelW);
                logicalY += ROW_H;
                y = addNoteRow("Edit mob blacklist in balance.json", y, labelX, labelW);
                logicalY += ROW_H;
            }
            case SPACE -> {
                y = addFloatRow("Lunar min multiplier", y, labelX, labelW, fieldX, fieldW, () -> cfg.space.lunarMinMultiplier, v -> cfg.space.lunarMinMultiplier = v, 0.1F, 5.0F);
                logicalY += ROW_H;
                y = addFloatRow("Lunar max multiplier", y, labelX, labelW, fieldX, fieldW, () -> cfg.space.lunarMaxMultiplier, v -> cfg.space.lunarMaxMultiplier = v, 0.1F, 5.0F);
                logicalY += ROW_H;
                y = addFloatRow("Starshield projectile damage multiplier", y, labelX, labelW, fieldX, fieldW, () -> cfg.space.starshieldProjectileDamageMultiplier, v -> cfg.space.starshieldProjectileDamageMultiplier = v, 0.1F, 2.0F);
                logicalY += ROW_H;
                y = spacer(y);
                logicalY += 8;
                y = addIntRow("Orbital Laser cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.space.orbitalLaserCooldownSeconds, v -> cfg.space.orbitalLaserCooldownSeconds = v, 0, 3600);
                logicalY += ROW_H;
                y = addIntRow("Orbital Laser range blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.space.orbitalLaserRangeBlocks, v -> cfg.space.orbitalLaserRangeBlocks = v, 0, 256);
                logicalY += ROW_H;
                y = addIntRow("Orbital Laser delay seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.space.orbitalLaserDelaySeconds, v -> cfg.space.orbitalLaserDelaySeconds = v, 0, 20);
                logicalY += ROW_H;
                y = addIntRow("Orbital Laser radius blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.space.orbitalLaserRadiusBlocks, v -> cfg.space.orbitalLaserRadiusBlocks = v, 0, 32);
                logicalY += ROW_H;
                y = addFloatRow("Orbital Laser damage", y, labelX, labelW, fieldX, fieldW, () -> cfg.space.orbitalLaserDamage, v -> cfg.space.orbitalLaserDamage = v, 0.0F, 80.0F);
                logicalY += ROW_H;
                y = addIntRow("Orbital Laser mining radius blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.space.orbitalLaserMiningRadiusBlocks, v -> cfg.space.orbitalLaserMiningRadiusBlocks = v, 0, 8);
                logicalY += ROW_H;
                y = addFloatRow("Orbital Laser mining hardness cap", y, labelX, labelW, fieldX, fieldW, () -> cfg.space.orbitalLaserMiningHardnessCap, v -> cfg.space.orbitalLaserMiningHardnessCap = v, 0.0F, 300.0F);
                logicalY += ROW_H;
                y = addIntRow("Orbital Laser mining max blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.space.orbitalLaserMiningMaxBlocks, v -> cfg.space.orbitalLaserMiningMaxBlocks = v, 0, 512);
                logicalY += ROW_H;
                y = spacer(y);
                logicalY += 8;
                y = addIntRow("Gravity Field cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.space.gravityFieldCooldownSeconds, v -> cfg.space.gravityFieldCooldownSeconds = v, 0, 3600);
                logicalY += ROW_H;
                y = addIntRow("Gravity Field duration seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.space.gravityFieldDurationSeconds, v -> cfg.space.gravityFieldDurationSeconds = v, 0, 120);
                logicalY += ROW_H;
                y = addIntRow("Gravity Field radius blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.space.gravityFieldRadiusBlocks, v -> cfg.space.gravityFieldRadiusBlocks = v, 0, 64);
                logicalY += ROW_H;
                y = addFloatRow("Gravity Field ally gravity multiplier", y, labelX, labelW, fieldX, fieldW, () -> cfg.space.gravityFieldAllyGravityMultiplier, v -> cfg.space.gravityFieldAllyGravityMultiplier = v, 0.1F, 2.0F);
                logicalY += ROW_H;
                y = addFloatRow("Gravity Field enemy gravity multiplier", y, labelX, labelW, fieldX, fieldW, () -> cfg.space.gravityFieldEnemyGravityMultiplier, v -> cfg.space.gravityFieldEnemyGravityMultiplier = v, 0.1F, 2.0F);
                logicalY += ROW_H;
                y = spacer(y);
                logicalY += 8;
                y = addIntRow("Black Hole cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.space.blackHoleCooldownSeconds, v -> cfg.space.blackHoleCooldownSeconds = v, 0, 3600);
                logicalY += ROW_H;
                y = addIntRow("Black Hole duration seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.space.blackHoleDurationSeconds, v -> cfg.space.blackHoleDurationSeconds = v, 0, 120);
                logicalY += ROW_H;
                y = addIntRow("Black Hole radius blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.space.blackHoleRadiusBlocks, v -> cfg.space.blackHoleRadiusBlocks = v, 0, 64);
                logicalY += ROW_H;
                y = addFloatRow("Black Hole pull strength", y, labelX, labelW, fieldX, fieldW, () -> cfg.space.blackHolePullStrength, v -> cfg.space.blackHolePullStrength = v, 0.0F, 2.0F);
                logicalY += ROW_H;
                y = addFloatRow("Black Hole damage per second", y, labelX, labelW, fieldX, fieldW, () -> cfg.space.blackHoleDamagePerSecond, v -> cfg.space.blackHoleDamagePerSecond = v, 0.0F, 40.0F);
                logicalY += ROW_H;
                y = spacer(y);
                logicalY += 8;
                y = addIntRow("White Hole cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.space.whiteHoleCooldownSeconds, v -> cfg.space.whiteHoleCooldownSeconds = v, 0, 3600);
                logicalY += ROW_H;
                y = addIntRow("White Hole duration seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.space.whiteHoleDurationSeconds, v -> cfg.space.whiteHoleDurationSeconds = v, 0, 120);
                logicalY += ROW_H;
                y = addIntRow("White Hole radius blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.space.whiteHoleRadiusBlocks, v -> cfg.space.whiteHoleRadiusBlocks = v, 0, 64);
                logicalY += ROW_H;
                y = addFloatRow("White Hole push strength", y, labelX, labelW, fieldX, fieldW, () -> cfg.space.whiteHolePushStrength, v -> cfg.space.whiteHolePushStrength = v, 0.0F, 2.0F);
                logicalY += ROW_H;
                y = addFloatRow("White Hole damage per second", y, labelX, labelW, fieldX, fieldW, () -> cfg.space.whiteHoleDamagePerSecond, v -> cfg.space.whiteHoleDamagePerSecond = v, 0.0F, 40.0F);
                logicalY += ROW_H;
            }
            case REAPER -> {
                y = addFloatRow("Undead Ward damage multiplier", y, labelX, labelW, fieldX, fieldW, () -> cfg.reaper.undeadWardDamageMultiplier, v -> cfg.reaper.undeadWardDamageMultiplier = v, 0.1F, 2.0F);
                logicalY += ROW_H;
                y = addIntRow("Harvest regen duration seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.reaper.harvestRegenDurationSeconds, v -> cfg.reaper.harvestRegenDurationSeconds = v, 0, 60);
                logicalY += ROW_H;
                y = addIntRow("Harvest regen amplifier", y, labelX, labelW, fieldX, fieldW, () -> cfg.reaper.harvestRegenAmplifier, v -> cfg.reaper.harvestRegenAmplifier = v, 0, 10);
                logicalY += ROW_H;
                y = spacer(y);
                logicalY += 8;
                y = addIntRow("Grave Steed cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.reaper.graveSteedCooldownSeconds, v -> cfg.reaper.graveSteedCooldownSeconds = v, 0, 3600);
                logicalY += ROW_H;
                y = addIntRow("Grave Steed duration seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.reaper.graveSteedDurationSeconds, v -> cfg.reaper.graveSteedDurationSeconds = v, 0, 3600);
                logicalY += ROW_H;
                y = addFloatRow("Grave Steed decay damage/sec", y, labelX, labelW, fieldX, fieldW, () -> cfg.reaper.graveSteedDecayDamagePerSecond, v -> cfg.reaper.graveSteedDecayDamagePerSecond = v, 0.0F, 20.0F);
                logicalY += ROW_H;
                y = spacer(y);
                logicalY += 8;
                y = addIntRow("Withering Strikes cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.reaper.witheringStrikesCooldownSeconds, v -> cfg.reaper.witheringStrikesCooldownSeconds = v, 0, 3600);
                logicalY += ROW_H;
                y = addIntRow("Withering Strikes duration seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.reaper.witheringStrikesDurationSeconds, v -> cfg.reaper.witheringStrikesDurationSeconds = v, 0, 120);
                logicalY += ROW_H;
                y = addIntRow("Wither duration seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.reaper.witheringStrikesWitherDurationSeconds, v -> cfg.reaper.witheringStrikesWitherDurationSeconds = v, 0, 60);
                logicalY += ROW_H;
                y = addIntRow("Wither amplifier", y, labelX, labelW, fieldX, fieldW, () -> cfg.reaper.witheringStrikesWitherAmplifier, v -> cfg.reaper.witheringStrikesWitherAmplifier = v, 0, 10);
                logicalY += ROW_H;
                y = spacer(y);
                logicalY += 8;
                y = addIntRow("Death Oath cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.reaper.deathOathCooldownSeconds, v -> cfg.reaper.deathOathCooldownSeconds = v, 0, 3600);
                logicalY += ROW_H;
                y = addIntRow("Death Oath duration seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.reaper.deathOathDurationSeconds, v -> cfg.reaper.deathOathDurationSeconds = v, 0, 120);
                logicalY += ROW_H;
                y = addIntRow("Death Oath range blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.reaper.deathOathRangeBlocks, v -> cfg.reaper.deathOathRangeBlocks = v, 0, 256);
                logicalY += ROW_H;
                y = addFloatRow("Death Oath self damage/sec", y, labelX, labelW, fieldX, fieldW, () -> cfg.reaper.deathOathSelfDamagePerSecond, v -> cfg.reaper.deathOathSelfDamagePerSecond = v, 0.0F, 20.0F);
                logicalY += ROW_H;
                y = addFloatRow("Death Oath bonus damage", y, labelX, labelW, fieldX, fieldW, () -> cfg.reaper.deathOathBonusDamage, v -> cfg.reaper.deathOathBonusDamage = v, 0.0F, 40.0F);
                logicalY += ROW_H;
                y = spacer(y);
                logicalY += 8;
                y = addIntRow("Retribution cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.reaper.retributionCooldownSeconds, v -> cfg.reaper.retributionCooldownSeconds = v, 0, 3600);
                logicalY += ROW_H;
                y = addIntRow("Retribution duration seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.reaper.retributionDurationSeconds, v -> cfg.reaper.retributionDurationSeconds = v, 0, 120);
                logicalY += ROW_H;
                y = addFloatRow("Retribution damage multiplier", y, labelX, labelW, fieldX, fieldW, () -> cfg.reaper.retributionDamageMultiplier, v -> cfg.reaper.retributionDamageMultiplier = v, 0.0F, 5.0F);
                logicalY += ROW_H;
                y = spacer(y);
                logicalY += 8;
                y = addIntRow("Scythe Sweep cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.reaper.scytheSweepCooldownSeconds, v -> cfg.reaper.scytheSweepCooldownSeconds = v, 0, 3600);
                logicalY += ROW_H;
                y = addIntRow("Scythe Sweep range blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.reaper.scytheSweepRangeBlocks, v -> cfg.reaper.scytheSweepRangeBlocks = v, 0, 16);
                logicalY += ROW_H;
                y = addIntRow("Scythe Sweep arc degrees", y, labelX, labelW, fieldX, fieldW, () -> cfg.reaper.scytheSweepArcDegrees, v -> cfg.reaper.scytheSweepArcDegrees = v, 0, 180);
                logicalY += ROW_H;
                y = addFloatRow("Scythe Sweep damage", y, labelX, labelW, fieldX, fieldW, () -> cfg.reaper.scytheSweepDamage, v -> cfg.reaper.scytheSweepDamage = v, 0.0F, 40.0F);
                logicalY += ROW_H;
                y = addDoubleRow("Scythe Sweep knockback", y, labelX, labelW, fieldX, fieldW, () -> cfg.reaper.scytheSweepKnockback, v -> cfg.reaper.scytheSweepKnockback = v, 0.0D, 5.0D);
                logicalY += ROW_H;
                y = spacer(y);
                logicalY += 8;
                y = addIntRow("Blood Charge cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.reaper.bloodChargeCooldownSeconds, v -> cfg.reaper.bloodChargeCooldownSeconds = v, 0, 3600);
                logicalY += ROW_H;
                y = addIntRow("Blood Charge max charge seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.reaper.bloodChargeMaxChargeSeconds, v -> cfg.reaper.bloodChargeMaxChargeSeconds = v, 0, 60);
                logicalY += ROW_H;
                y = addFloatRow("Blood Charge self damage/sec", y, labelX, labelW, fieldX, fieldW, () -> cfg.reaper.bloodChargeSelfDamagePerSecond, v -> cfg.reaper.bloodChargeSelfDamagePerSecond = v, 0.0F, 20.0F);
                logicalY += ROW_H;
                y = addFloatRow("Blood Charge max multiplier", y, labelX, labelW, fieldX, fieldW, () -> cfg.reaper.bloodChargeMaxMultiplier, v -> cfg.reaper.bloodChargeMaxMultiplier = v, 0.0F, 5.0F);
                logicalY += ROW_H;
                y = addIntRow("Blood Charge buff duration seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.reaper.bloodChargeBuffDurationSeconds, v -> cfg.reaper.bloodChargeBuffDurationSeconds = v, 0, 120);
                logicalY += ROW_H;
                y = spacer(y);
                logicalY += 8;
                y = addIntRow("Shadow Clone cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.reaper.shadowCloneCooldownSeconds, v -> cfg.reaper.shadowCloneCooldownSeconds = v, 0, 3600);
                logicalY += ROW_H;
                y = addIntRow("Shadow Clone duration seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.reaper.shadowCloneDurationSeconds, v -> cfg.reaper.shadowCloneDurationSeconds = v, 0, 120);
                logicalY += ROW_H;
                y = addFloatRow("Shadow Clone max health", y, labelX, labelW, fieldX, fieldW, () -> cfg.reaper.shadowCloneMaxHealth, v -> cfg.reaper.shadowCloneMaxHealth = v, 0.0F, 200.0F);
                logicalY += ROW_H;
                y = addIntRow("Shadow Clone count", y, labelX, labelW, fieldX, fieldW, () -> cfg.reaper.shadowCloneCount, v -> cfg.reaper.shadowCloneCount = v, 0, 50);
                logicalY += ROW_H;
                y = addStringRow("Shadow Clone entity id", y, labelX, labelW, fieldX, fieldW, () -> cfg.reaper.shadowCloneEntityId, v -> cfg.reaper.shadowCloneEntityId = v);
                logicalY += ROW_H;
            }
            case PILLAGER -> {
                y = addFloatRow("Raiders Training velocity multiplier", y, labelX, labelW, fieldX, fieldW, () -> cfg.pillager.raidersTrainingProjectileVelocityMultiplier, v -> cfg.pillager.raidersTrainingProjectileVelocityMultiplier = v, 0.1F, 3.0F);
                logicalY += ROW_H;
                y = addIntRow("Shieldbreaker disable cooldown ticks", y, labelX, labelW, fieldX, fieldW, () -> cfg.pillager.shieldbreakerDisableCooldownTicks, v -> cfg.pillager.shieldbreakerDisableCooldownTicks = v, 0, 200);
                logicalY += ROW_H;
                y = addFloatRow("Illager Discipline threshold hearts", y, labelX, labelW, fieldX, fieldW, () -> cfg.pillager.illagerDisciplineThresholdHearts, v -> cfg.pillager.illagerDisciplineThresholdHearts = v, 0.0F, 20.0F);
                logicalY += ROW_H;
                y = addIntRow("Illager Discipline resistance duration seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.pillager.illagerDisciplineResistanceDurationSeconds, v -> cfg.pillager.illagerDisciplineResistanceDurationSeconds = v, 0, 60);
                logicalY += ROW_H;
                y = addIntRow("Illager Discipline resistance amplifier", y, labelX, labelW, fieldX, fieldW, () -> cfg.pillager.illagerDisciplineResistanceAmplifier, v -> cfg.pillager.illagerDisciplineResistanceAmplifier = v, 0, 10);
                logicalY += ROW_H;
                y = addIntRow("Illager Discipline cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.pillager.illagerDisciplineCooldownSeconds, v -> cfg.pillager.illagerDisciplineCooldownSeconds = v, 0, 3600);
                logicalY += ROW_H;
                y = spacer(y);
                logicalY += 8;
                y = addIntRow("Fangs cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.pillager.fangsCooldownSeconds, v -> cfg.pillager.fangsCooldownSeconds = v, 0, 3600);
                logicalY += ROW_H;
                y = addIntRow("Fangs range blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.pillager.fangsRangeBlocks, v -> cfg.pillager.fangsRangeBlocks = v, 0, 128);
                logicalY += ROW_H;
                y = addIntRow("Fangs count", y, labelX, labelW, fieldX, fieldW, () -> cfg.pillager.fangsCount, v -> cfg.pillager.fangsCount = v, 0, 60);
                logicalY += ROW_H;
                y = addFloatRow("Fangs spacing blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.pillager.fangsSpacingBlocks, v -> cfg.pillager.fangsSpacingBlocks = v, 0.1F, 5.0F);
                logicalY += ROW_H;
                y = addIntRow("Fangs warmup step ticks", y, labelX, labelW, fieldX, fieldW, () -> cfg.pillager.fangsWarmupStepTicks, v -> cfg.pillager.fangsWarmupStepTicks = v, 0, 20);
                logicalY += ROW_H;
                y = spacer(y);
                logicalY += 8;
                y = addIntRow("Ravage cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.pillager.ravageCooldownSeconds, v -> cfg.pillager.ravageCooldownSeconds = v, 0, 3600);
                logicalY += ROW_H;
                y = addIntRow("Ravage range blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.pillager.ravageRangeBlocks, v -> cfg.pillager.ravageRangeBlocks = v, 0, 32);
                logicalY += ROW_H;
                y = addFloatRow("Ravage damage", y, labelX, labelW, fieldX, fieldW, () -> cfg.pillager.ravageDamage, v -> cfg.pillager.ravageDamage = v, 0.0F, 40.0F);
                logicalY += ROW_H;
                y = addDoubleRow("Ravage knockback", y, labelX, labelW, fieldX, fieldW, () -> cfg.pillager.ravageKnockback, v -> cfg.pillager.ravageKnockback = v, 0.0D, 5.0D);
                logicalY += ROW_H;
                y = spacer(y);
                logicalY += 8;
                y = addIntRow("Vindicator Break cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.pillager.vindicatorBreakCooldownSeconds, v -> cfg.pillager.vindicatorBreakCooldownSeconds = v, 0, 3600);
                logicalY += ROW_H;
                y = addIntRow("Vindicator Break duration seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.pillager.vindicatorBreakDurationSeconds, v -> cfg.pillager.vindicatorBreakDurationSeconds = v, 0, 120);
                logicalY += ROW_H;
                y = addIntRow("Vindicator Break strength amplifier", y, labelX, labelW, fieldX, fieldW, () -> cfg.pillager.vindicatorBreakStrengthAmplifier, v -> cfg.pillager.vindicatorBreakStrengthAmplifier = v, 0, 10);
                logicalY += ROW_H;
                y = addIntRow("Vindicator Break shield disable ticks", y, labelX, labelW, fieldX, fieldW, () -> cfg.pillager.vindicatorBreakShieldDisableCooldownTicks, v -> cfg.pillager.vindicatorBreakShieldDisableCooldownTicks = v, 0, 200);
                logicalY += ROW_H;
                y = spacer(y);
                logicalY += 8;
                y = addIntRow("Volley cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.pillager.volleyCooldownSeconds, v -> cfg.pillager.volleyCooldownSeconds = v, 0, 3600);
                logicalY += ROW_H;
                y = addIntRow("Volley duration seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.pillager.volleyDurationSeconds, v -> cfg.pillager.volleyDurationSeconds = v, 0, 120);
                logicalY += ROW_H;
                y = addIntRow("Volley period ticks", y, labelX, labelW, fieldX, fieldW, () -> cfg.pillager.volleyPeriodTicks, v -> cfg.pillager.volleyPeriodTicks = v, 1, 40);
                logicalY += ROW_H;
                y = addIntRow("Volley arrows per shot", y, labelX, labelW, fieldX, fieldW, () -> cfg.pillager.volleyArrowsPerShot, v -> cfg.pillager.volleyArrowsPerShot = v, 1, 10);
                logicalY += ROW_H;
                y = addFloatRow("Volley arrow damage", y, labelX, labelW, fieldX, fieldW, () -> cfg.pillager.volleyArrowDamage, v -> cfg.pillager.volleyArrowDamage = v, 0.0F, 40.0F);
                logicalY += ROW_H;
                y = addFloatRow("Volley arrow velocity", y, labelX, labelW, fieldX, fieldW, () -> cfg.pillager.volleyArrowVelocity, v -> cfg.pillager.volleyArrowVelocity = v, 0.0F, 10.0F);
                logicalY += ROW_H;
                y = addFloatRow("Volley arrow inaccuracy", y, labelX, labelW, fieldX, fieldW, () -> cfg.pillager.volleyArrowInaccuracy, v -> cfg.pillager.volleyArrowInaccuracy = v, 0.0F, 10.0F);
                logicalY += ROW_H;
                y = spacer(y);
                logicalY += 8;
                y = addIntRow("Warhorn cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.pillager.warhornCooldownSeconds, v -> cfg.pillager.warhornCooldownSeconds = v, 0, 3600);
                logicalY += ROW_H;
                y = addIntRow("Warhorn radius blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.pillager.warhornRadiusBlocks, v -> cfg.pillager.warhornRadiusBlocks = v, 0, 64);
                logicalY += ROW_H;
                y = addIntRow("Warhorn duration seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.pillager.warhornDurationSeconds, v -> cfg.pillager.warhornDurationSeconds = v, 0, 60);
                logicalY += ROW_H;
                y = addIntRow("Warhorn ally speed amplifier", y, labelX, labelW, fieldX, fieldW, () -> cfg.pillager.warhornAllySpeedAmplifier, v -> cfg.pillager.warhornAllySpeedAmplifier = v, 0, 10);
                logicalY += ROW_H;
                y = addIntRow("Warhorn ally resistance amplifier", y, labelX, labelW, fieldX, fieldW, () -> cfg.pillager.warhornAllyResistanceAmplifier, v -> cfg.pillager.warhornAllyResistanceAmplifier = v, 0, 10);
                logicalY += ROW_H;
                y = addIntRow("Warhorn enemy slowness amplifier", y, labelX, labelW, fieldX, fieldW, () -> cfg.pillager.warhornEnemySlownessAmplifier, v -> cfg.pillager.warhornEnemySlownessAmplifier = v, 0, 10);
                logicalY += ROW_H;
                y = addIntRow("Warhorn enemy weakness amplifier", y, labelX, labelW, fieldX, fieldW, () -> cfg.pillager.warhornEnemyWeaknessAmplifier, v -> cfg.pillager.warhornEnemyWeaknessAmplifier = v, 0, 10);
                logicalY += ROW_H;
                y = spacer(y);
                logicalY += 8;
                y = addIntRow("Snare cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.pillager.snareCooldownSeconds, v -> cfg.pillager.snareCooldownSeconds = v, 0, 3600);
                logicalY += ROW_H;
                y = addIntRow("Snare range blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.pillager.snareRangeBlocks, v -> cfg.pillager.snareRangeBlocks = v, 0, 128);
                logicalY += ROW_H;
                y = addIntRow("Snare duration seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.pillager.snareDurationSeconds, v -> cfg.pillager.snareDurationSeconds = v, 0, 60);
                logicalY += ROW_H;
                y = addIntRow("Snare slowness amplifier", y, labelX, labelW, fieldX, fieldW, () -> cfg.pillager.snareSlownessAmplifier, v -> cfg.pillager.snareSlownessAmplifier = v, 0, 10);
                logicalY += ROW_H;
            }
            case SPY_MIMIC -> {
                y = addIntRow("Stillness seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.spyMimic.stillnessSeconds, v -> cfg.spyMimic.stillnessSeconds = v, 0, 60);
                logicalY += ROW_H;
                y = addFloatRow("Stillness move epsilon blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.spyMimic.stillnessMoveEpsilonBlocks, v -> cfg.spyMimic.stillnessMoveEpsilonBlocks = v, 0.0F, 1.0F);
                logicalY += ROW_H;
                y = addIntRow("Stillness invis refresh seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.spyMimic.stillnessInvisRefreshSeconds, v -> cfg.spyMimic.stillnessInvisRefreshSeconds = v, 0, 60);
                logicalY += ROW_H;
                y = addIntRow("Backstep cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.spyMimic.backstepCooldownSeconds, v -> cfg.spyMimic.backstepCooldownSeconds = v, 0, 3600);
                logicalY += ROW_H;
                y = addDoubleRow("Backstep velocity", y, labelX, labelW, fieldX, fieldW, () -> cfg.spyMimic.backstepVelocity, v -> cfg.spyMimic.backstepVelocity = v, 0.0D, 5.0D);
                logicalY += ROW_H;
                y = addDoubleRow("Backstep up velocity", y, labelX, labelW, fieldX, fieldW, () -> cfg.spyMimic.backstepUpVelocity, v -> cfg.spyMimic.backstepUpVelocity = v, 0.0D, 2.0D);
                logicalY += ROW_H;
                y = addFloatRow("Backstab bonus damage", y, labelX, labelW, fieldX, fieldW, () -> cfg.spyMimic.backstabBonusDamage, v -> cfg.spyMimic.backstabBonusDamage = v, 0.0F, 40.0F);
                logicalY += ROW_H;
                y = addIntRow("Backstab angle degrees", y, labelX, labelW, fieldX, fieldW, () -> cfg.spyMimic.backstabAngleDegrees, v -> cfg.spyMimic.backstabAngleDegrees = v, 0, 180);
                logicalY += ROW_H;
                y = spacer(y);
                logicalY += 8;
                y = addIntRow("Observe range blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.spyMimic.observeRangeBlocks, v -> cfg.spyMimic.observeRangeBlocks = v, 0, 128);
                logicalY += ROW_H;
                y = addIntRow("Observe window seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.spyMimic.observeWindowSeconds, v -> cfg.spyMimic.observeWindowSeconds = v, 0, 3600);
                logicalY += ROW_H;
                y = addIntRow("Steal required witness count", y, labelX, labelW, fieldX, fieldW, () -> cfg.spyMimic.stealRequiredWitnessCount, v -> cfg.spyMimic.stealRequiredWitnessCount = v, 1, 20);
                logicalY += ROW_H;
                y = addIntRow("Max stolen abilities", y, labelX, labelW, fieldX, fieldW, () -> cfg.spyMimic.maxStolenAbilities, v -> cfg.spyMimic.maxStolenAbilities = v, 1, 10);
                logicalY += ROW_H;
                y = spacer(y);
                logicalY += 8;
                y = addIntRow("Mimic Form cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.spyMimic.mimicFormCooldownSeconds, v -> cfg.spyMimic.mimicFormCooldownSeconds = v, 0, 3600);
                logicalY += ROW_H;
                y = addIntRow("Mimic Form duration seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.spyMimic.mimicFormDurationSeconds, v -> cfg.spyMimic.mimicFormDurationSeconds = v, 0, 120);
                logicalY += ROW_H;
                y = addFloatRow("Mimic Form bonus max health", y, labelX, labelW, fieldX, fieldW, () -> cfg.spyMimic.mimicFormBonusMaxHealth, v -> cfg.spyMimic.mimicFormBonusMaxHealth = v, 0.0F, 40.0F);
                logicalY += ROW_H;
                y = addFloatRow("Mimic Form speed multiplier", y, labelX, labelW, fieldX, fieldW, () -> cfg.spyMimic.mimicFormSpeedMultiplier, v -> cfg.spyMimic.mimicFormSpeedMultiplier = v, 0.1F, 3.0F);
                logicalY += ROW_H;
                y = spacer(y);
                logicalY += 8;
                y = addIntRow("Echo cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.spyMimic.echoCooldownSeconds, v -> cfg.spyMimic.echoCooldownSeconds = v, 0, 3600);
                logicalY += ROW_H;
                y = addIntRow("Echo window seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.spyMimic.echoWindowSeconds, v -> cfg.spyMimic.echoWindowSeconds = v, 0, 60);
                logicalY += ROW_H;
                y = addIntRow("Steal cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.spyMimic.stealCooldownSeconds, v -> cfg.spyMimic.stealCooldownSeconds = v, 0, 3600);
                logicalY += ROW_H;
                y = spacer(y);
                logicalY += 8;
                y = addIntRow("Smoke Bomb cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.spyMimic.smokeBombCooldownSeconds, v -> cfg.spyMimic.smokeBombCooldownSeconds = v, 0, 3600);
                logicalY += ROW_H;
                y = addIntRow("Smoke Bomb radius blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.spyMimic.smokeBombRadiusBlocks, v -> cfg.spyMimic.smokeBombRadiusBlocks = v, 0, 64);
                logicalY += ROW_H;
                y = addIntRow("Smoke Bomb duration seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.spyMimic.smokeBombDurationSeconds, v -> cfg.spyMimic.smokeBombDurationSeconds = v, 0, 60);
                logicalY += ROW_H;
                y = addIntRow("Smoke Bomb blindness amplifier", y, labelX, labelW, fieldX, fieldW, () -> cfg.spyMimic.smokeBombBlindnessAmplifier, v -> cfg.spyMimic.smokeBombBlindnessAmplifier = v, 0, 10);
                logicalY += ROW_H;
                y = addIntRow("Smoke Bomb slowness amplifier", y, labelX, labelW, fieldX, fieldW, () -> cfg.spyMimic.smokeBombSlownessAmplifier, v -> cfg.spyMimic.smokeBombSlownessAmplifier = v, 0, 10);
                logicalY += ROW_H;
                y = addIntRow("Stolen Cast cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.spyMimic.stolenCastCooldownSeconds, v -> cfg.spyMimic.stolenCastCooldownSeconds = v, 0, 3600);
                logicalY += ROW_H;
                y = spacer(y);
                logicalY += 8;
                y = addIntRow("Skinshift cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.spyMimic.skinshiftCooldownSeconds, v -> cfg.spyMimic.skinshiftCooldownSeconds = v, 0, 3600);
                logicalY += ROW_H;
                y = addIntRow("Skinshift duration seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.spyMimic.skinshiftDurationSeconds, v -> cfg.spyMimic.skinshiftDurationSeconds = v, 0, 600);
                logicalY += ROW_H;
                y = addIntRow("Skinshift range blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.spyMimic.skinshiftRangeBlocks, v -> cfg.spyMimic.skinshiftRangeBlocks = v, 0, 128);
                logicalY += ROW_H;
            }
            case BEACON -> {
                y = addIntRow("Core radius blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.beacon.coreRadiusBlocks, v -> cfg.beacon.coreRadiusBlocks = v, 0, 64);
                logicalY += ROW_H;
                y = addIntRow("Core pulse period seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.beacon.corePulsePeriodSeconds, v -> cfg.beacon.corePulsePeriodSeconds = v, 0, 60);
                logicalY += ROW_H;
                y = addIntRow("Core regen duration seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.beacon.coreRegenDurationSeconds, v -> cfg.beacon.coreRegenDurationSeconds = v, 0, 60);
                logicalY += ROW_H;
                y = addIntRow("Core regen amplifier", y, labelX, labelW, fieldX, fieldW, () -> cfg.beacon.coreRegenAmplifier, v -> cfg.beacon.coreRegenAmplifier = v, 0, 10);
                logicalY += ROW_H;
                y = spacer(y);
                logicalY += 8;
                y = addIntRow("Stabilize radius blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.beacon.stabilizeRadiusBlocks, v -> cfg.beacon.stabilizeRadiusBlocks = v, 0, 64);
                logicalY += ROW_H;
                y = addIntRow("Stabilize reduce ticks/sec", y, labelX, labelW, fieldX, fieldW, () -> cfg.beacon.stabilizeReduceTicksPerSecond, v -> cfg.beacon.stabilizeReduceTicksPerSecond = v, 0, 200);
                logicalY += ROW_H;
                y = spacer(y);
                logicalY += 8;
                y = addIntRow("Rally radius blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.beacon.rallyRadiusBlocks, v -> cfg.beacon.rallyRadiusBlocks = v, 0, 64);
                logicalY += ROW_H;
                y = addIntRow("Rally absorption hearts", y, labelX, labelW, fieldX, fieldW, () -> cfg.beacon.rallyAbsorptionHearts, v -> cfg.beacon.rallyAbsorptionHearts = v, 0, 40);
                logicalY += ROW_H;
                y = addIntRow("Rally duration seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.beacon.rallyDurationSeconds, v -> cfg.beacon.rallyDurationSeconds = v, 0, 120);
                logicalY += ROW_H;
                y = spacer(y);
                logicalY += 8;
                y = addIntRow("Aura cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.beacon.auraCooldownSeconds, v -> cfg.beacon.auraCooldownSeconds = v, 0, 3600);
                logicalY += ROW_H;
                y = addIntRow("Aura duration seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.beacon.auraDurationSeconds, v -> cfg.beacon.auraDurationSeconds = v, 0, 120);
                logicalY += ROW_H;
                y = addIntRow("Aura radius blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.beacon.auraRadiusBlocks, v -> cfg.beacon.auraRadiusBlocks = v, 0, 64);
                logicalY += ROW_H;
                y = addIntRow("Aura refresh seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.beacon.auraRefreshSeconds, v -> cfg.beacon.auraRefreshSeconds = v, 0, 60);
                logicalY += ROW_H;
                y = addIntRow("Aura speed amplifier", y, labelX, labelW, fieldX, fieldW, () -> cfg.beacon.auraSpeedAmplifier, v -> cfg.beacon.auraSpeedAmplifier = v, 0, 10);
                logicalY += ROW_H;
                y = addIntRow("Aura haste amplifier", y, labelX, labelW, fieldX, fieldW, () -> cfg.beacon.auraHasteAmplifier, v -> cfg.beacon.auraHasteAmplifier = v, 0, 10);
                logicalY += ROW_H;
                y = addIntRow("Aura resistance amplifier", y, labelX, labelW, fieldX, fieldW, () -> cfg.beacon.auraResistanceAmplifier, v -> cfg.beacon.auraResistanceAmplifier = v, 0, 10);
                logicalY += ROW_H;
                y = addIntRow("Aura jump amplifier", y, labelX, labelW, fieldX, fieldW, () -> cfg.beacon.auraJumpAmplifier, v -> cfg.beacon.auraJumpAmplifier = v, 0, 10);
                logicalY += ROW_H;
                y = addIntRow("Aura strength amplifier", y, labelX, labelW, fieldX, fieldW, () -> cfg.beacon.auraStrengthAmplifier, v -> cfg.beacon.auraStrengthAmplifier = v, 0, 10);
                logicalY += ROW_H;
                y = addIntRow("Aura regen amplifier", y, labelX, labelW, fieldX, fieldW, () -> cfg.beacon.auraRegenAmplifier, v -> cfg.beacon.auraRegenAmplifier = v, 0, 10);
                logicalY += ROW_H;
            }
            case AIR -> {
                y = addFloatRow("Aerial Guard fall damage multiplier", y, labelX, labelW, fieldX, fieldW, () -> cfg.air.aerialGuardFallDamageMultiplier, v -> cfg.air.aerialGuardFallDamageMultiplier = v, 0.0F, 2.0F);
                logicalY += ROW_H;
                y = addFloatRow("Aerial Guard damage multiplier", y, labelX, labelW, fieldX, fieldW, () -> cfg.air.aerialGuardDamageMultiplier, v -> cfg.air.aerialGuardDamageMultiplier = v, 0.0F, 2.0F);
                logicalY += ROW_H;
                y = addFloatRow("Aerial Guard knockback multiplier", y, labelX, labelW, fieldX, fieldW, () -> cfg.air.aerialGuardKnockbackMultiplier, v -> cfg.air.aerialGuardKnockbackMultiplier = v, 0.0F, 2.0F);
                logicalY += ROW_H;
                y = addDoubleRow("Wind Shear knockback", y, labelX, labelW, fieldX, fieldW, () -> cfg.air.windShearKnockback, v -> cfg.air.windShearKnockback = v, 0.0D, 5.0D);
                logicalY += ROW_H;
                y = addIntRow("Wind Shear slowness duration seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.air.windShearSlownessDurationSeconds, v -> cfg.air.windShearSlownessDurationSeconds = v, 0, 60);
                logicalY += ROW_H;
                y = addIntRow("Wind Shear slowness amplifier", y, labelX, labelW, fieldX, fieldW, () -> cfg.air.windShearSlownessAmplifier, v -> cfg.air.windShearSlownessAmplifier = v, 0, 10);
                logicalY += ROW_H;
                y = spacer(y);
                logicalY += 8;
                y = addIntRow("Wind Jump cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.air.windJumpCooldownSeconds, v -> cfg.air.windJumpCooldownSeconds = v, 0, 3600);
                logicalY += ROW_H;
                y = addDoubleRow("Wind Jump vertical velocity", y, labelX, labelW, fieldX, fieldW, () -> cfg.air.windJumpVerticalVelocity, v -> cfg.air.windJumpVerticalVelocity = v, 0.0D, 5.0D);
                logicalY += ROW_H;
                y = addDoubleRow("Wind Jump forward velocity", y, labelX, labelW, fieldX, fieldW, () -> cfg.air.windJumpForwardVelocity, v -> cfg.air.windJumpForwardVelocity = v, 0.0D, 5.0D);
                logicalY += ROW_H;
                y = spacer(y);
                logicalY += 8;
                y = addIntRow("Gale Slam cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.air.galeSlamCooldownSeconds, v -> cfg.air.galeSlamCooldownSeconds = v, 0, 3600);
                logicalY += ROW_H;
                y = addIntRow("Gale Slam window seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.air.galeSlamWindowSeconds, v -> cfg.air.galeSlamWindowSeconds = v, 0, 120);
                logicalY += ROW_H;
                y = addIntRow("Gale Slam radius blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.air.galeSlamRadiusBlocks, v -> cfg.air.galeSlamRadiusBlocks = v, 0, 32);
                logicalY += ROW_H;
                y = addFloatRow("Gale Slam bonus damage", y, labelX, labelW, fieldX, fieldW, () -> cfg.air.galeSlamBonusDamage, v -> cfg.air.galeSlamBonusDamage = v, 0.0F, 40.0F);
                logicalY += ROW_H;
                y = addDoubleRow("Gale Slam knockback", y, labelX, labelW, fieldX, fieldW, () -> cfg.air.galeSlamKnockback, v -> cfg.air.galeSlamKnockback = v, 0.0D, 5.0D);
                logicalY += ROW_H;
                y = spacer(y);
                logicalY += 8;
                y = addIntRow("Crosswind cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.air.crosswindCooldownSeconds, v -> cfg.air.crosswindCooldownSeconds = v, 0, 3600);
                logicalY += ROW_H;
                y = addIntRow("Crosswind range blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.air.crosswindRangeBlocks, v -> cfg.air.crosswindRangeBlocks = v, 0, 128);
                logicalY += ROW_H;
                y = addIntRow("Crosswind radius blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.air.crosswindRadiusBlocks, v -> cfg.air.crosswindRadiusBlocks = v, 0, 32);
                logicalY += ROW_H;
                y = addFloatRow("Crosswind damage", y, labelX, labelW, fieldX, fieldW, () -> cfg.air.crosswindDamage, v -> cfg.air.crosswindDamage = v, 0.0F, 40.0F);
                logicalY += ROW_H;
                y = addDoubleRow("Crosswind knockback", y, labelX, labelW, fieldX, fieldW, () -> cfg.air.crosswindKnockback, v -> cfg.air.crosswindKnockback = v, 0.0D, 5.0D);
                logicalY += ROW_H;
                y = addIntRow("Crosswind slowness duration seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.air.crosswindSlownessDurationSeconds, v -> cfg.air.crosswindSlownessDurationSeconds = v, 0, 60);
                logicalY += ROW_H;
                y = addIntRow("Crosswind slowness amplifier", y, labelX, labelW, fieldX, fieldW, () -> cfg.air.crosswindSlownessAmplifier, v -> cfg.air.crosswindSlownessAmplifier = v, 0, 10);
                logicalY += ROW_H;
                y = spacer(y);
                logicalY += 8;
                y = addIntRow("Dash cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.air.dashCooldownSeconds, v -> cfg.air.dashCooldownSeconds = v, 0, 3600);
                logicalY += ROW_H;
                y = addDoubleRow("Dash velocity", y, labelX, labelW, fieldX, fieldW, () -> cfg.air.dashVelocity, v -> cfg.air.dashVelocity = v, 0.0D, 5.0D);
                logicalY += ROW_H;
                y = addDoubleRow("Dash up velocity", y, labelX, labelW, fieldX, fieldW, () -> cfg.air.dashUpVelocity, v -> cfg.air.dashUpVelocity = v, 0.0D, 2.0D);
                logicalY += ROW_H;
                y = addIntRow("Dash I-frame duration seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.air.dashIFrameDurationSeconds, v -> cfg.air.dashIFrameDurationSeconds = v, 0, 10);
                logicalY += ROW_H;
                y = addIntRow("Dash I-frame resistance amplifier", y, labelX, labelW, fieldX, fieldW, () -> cfg.air.dashIFrameResistanceAmplifier, v -> cfg.air.dashIFrameResistanceAmplifier = v, 0, 10);
                logicalY += ROW_H;
                y = spacer(y);
                logicalY += 8;
                y = addIntRow("Air mace breach level", y, labelX, labelW, fieldX, fieldW, () -> cfg.air.airMaceBreachLevel, v -> cfg.air.airMaceBreachLevel = v, 0, 10);
                logicalY += ROW_H;
                y = addIntRow("Air mace wind burst level", y, labelX, labelW, fieldX, fieldW, () -> cfg.air.airMaceWindBurstLevel, v -> cfg.air.airMaceWindBurstLevel = v, 0, 10);
                logicalY += ROW_H;
                y = addIntRow("Air mace mending level", y, labelX, labelW, fieldX, fieldW, () -> cfg.air.airMaceMendingLevel, v -> cfg.air.airMaceMendingLevel = v, 0, 1);
                logicalY += ROW_H;
                y = addIntRow("Air mace unbreaking level", y, labelX, labelW, fieldX, fieldW, () -> cfg.air.airMaceUnbreakingLevel, v -> cfg.air.airMaceUnbreakingLevel = v, 0, 10);
                logicalY += ROW_H;
                y = addIntRow("Air mace fire aspect level", y, labelX, labelW, fieldX, fieldW, () -> cfg.air.airMaceFireAspectLevel, v -> cfg.air.airMaceFireAspectLevel = v, 0, 10);
                logicalY += ROW_H;
            }
            case LEGENDARY -> {
                y = addIntRow("Legendary craft seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.legendary.craftSeconds, v -> cfg.legendary.craftSeconds = v, 0, 36000);
                logicalY += ROW_H;
                y = addIntRow("Legendary craft max per item (0 = unlimited)", y, labelX, labelW, fieldX, fieldW, () -> cfg.legendary.craftMaxPerItem, v -> cfg.legendary.craftMaxPerItem = v, 0, 100);
                logicalY += ROW_H;
                y = addIntRow("Legendary craft max active per item (0 = unlimited)", y, labelX, labelW, fieldX, fieldW, () -> cfg.legendary.craftMaxActivePerItem, v -> cfg.legendary.craftMaxActivePerItem = v, 0, 100);
                logicalY += ROW_H;
                y = addIntRow("Tracker refresh seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.legendary.trackerRefreshSeconds, v -> cfg.legendary.trackerRefreshSeconds = v, 0, 60);
                logicalY += ROW_H;
                y = addIntRow("Tracker max distance blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.legendary.trackerMaxDistanceBlocks, v -> cfg.legendary.trackerMaxDistanceBlocks = v, 0, 100000);
                logicalY += ROW_H;
                y = addIntRow("Recall cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.legendary.recallCooldownSeconds, v -> cfg.legendary.recallCooldownSeconds = v, 0, 3600);
                logicalY += ROW_H;
                y = spacer(y);
                logicalY += 8;
                y = addIntRow("Hypno hold seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.legendary.hypnoHoldSeconds, v -> cfg.legendary.hypnoHoldSeconds = v, 0, 10);
                logicalY += ROW_H;
                y = addIntRow("Hypno range blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.legendary.hypnoRangeBlocks, v -> cfg.legendary.hypnoRangeBlocks = v, 0, 128);
                logicalY += ROW_H;
                y = addIntRow("Hypno view range blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.legendary.hypnoViewRangeBlocks, v -> cfg.legendary.hypnoViewRangeBlocks = v, 0, 64);
                logicalY += ROW_H;
                y = addFloatRow("Hypno heal hearts", y, labelX, labelW, fieldX, fieldW, () -> cfg.legendary.hypnoHealHearts, v -> cfg.legendary.hypnoHealHearts = v, 0.0F, 40.0F);
                logicalY += ROW_H;
                y = addIntRow("Hypno max controlled", y, labelX, labelW, fieldX, fieldW, () -> cfg.legendary.hypnoMaxControlled, v -> cfg.legendary.hypnoMaxControlled = v, 0, 50);
                logicalY += ROW_H;
                y = addIntRow("Hypno duration seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.legendary.hypnoDurationSeconds, v -> cfg.legendary.hypnoDurationSeconds = v, 0, 3600);
                logicalY += ROW_H;
                y = spacer(y);
                logicalY += 8;
                y = addIntRow("Earthsplitter radius blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.legendary.earthsplitterRadiusBlocks, v -> cfg.legendary.earthsplitterRadiusBlocks = v, 0, 5);
                logicalY += ROW_H;
                y = addIntRow("Earthsplitter tunnel length blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.legendary.earthsplitterTunnelLengthBlocks, v -> cfg.legendary.earthsplitterTunnelLengthBlocks = v, 0, 64);
                logicalY += ROW_H;
                y = spacer(y);
                logicalY += 8;
                y = addIntRow("Blood Oath sharpness cap", y, labelX, labelW, fieldX, fieldW, () -> cfg.legendary.bloodOathSharpnessCap, v -> cfg.legendary.bloodOathSharpnessCap = v, 0, 20);
                logicalY += ROW_H;
                y = addIntRow("Demolition cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.legendary.demolitionCooldownSeconds, v -> cfg.legendary.demolitionCooldownSeconds = v, 0, 3600);
                logicalY += ROW_H;
                y = addIntRow("Demolition cooldown scale percent", y, labelX, labelW, fieldX, fieldW, () -> cfg.legendary.demolitionCooldownScalePercent, v -> cfg.legendary.demolitionCooldownScalePercent = v, 0, 200);
                logicalY += ROW_H;
                y = addIntRow("Demolition fuse ticks", y, labelX, labelW, fieldX, fieldW, () -> cfg.legendary.demolitionFuseTicks, v -> cfg.legendary.demolitionFuseTicks = v, 0, 200);
                logicalY += ROW_H;
                y = addIntRow("Demolition range blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.legendary.demolitionRangeBlocks, v -> cfg.legendary.demolitionRangeBlocks = v, 0, 128);
                logicalY += ROW_H;
                y = addFloatRow("Demolition explosion power", y, labelX, labelW, fieldX, fieldW, () -> cfg.legendary.demolitionExplosionPower, v -> cfg.legendary.demolitionExplosionPower = v, 0.1F, 10.0F);
                logicalY += ROW_H;
                y = addIntRow("Demolition TNT count", y, labelX, labelW, fieldX, fieldW, () -> cfg.legendary.demolitionTntCount, v -> cfg.legendary.demolitionTntCount = v, 0, 50);
                logicalY += ROW_H;
                y = addIntRow("Hunter aim range blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.legendary.hunterAimRangeBlocks, v -> cfg.legendary.hunterAimRangeBlocks = v, 0, 256);
                logicalY += ROW_H;
                y = addIntRow("Hunter aim timeout seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.legendary.hunterAimTimeoutSeconds, v -> cfg.legendary.hunterAimTimeoutSeconds = v, 0, 300);
                logicalY += ROW_H;
                y = addFloatRow("Hunter aim assist strength", y, labelX, labelW, fieldX, fieldW, () -> cfg.legendary.hunterAimAssistStrength, v -> cfg.legendary.hunterAimAssistStrength = v, 0.0F, 3.0F);
                logicalY += ROW_H;
                y = addIntRow("Third Strike window seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.legendary.thirdStrikeWindowSeconds, v -> cfg.legendary.thirdStrikeWindowSeconds = v, 0, 60);
                logicalY += ROW_H;
                y = addFloatRow("Third Strike bonus damage", y, labelX, labelW, fieldX, fieldW, () -> cfg.legendary.thirdStrikeBonusDamage, v -> cfg.legendary.thirdStrikeBonusDamage = v, 0.0F, 40.0F);
                logicalY += ROW_H;
                y = addFloatRow("Vampiric heal amount", y, labelX, labelW, fieldX, fieldW, () -> cfg.legendary.vampiricHealAmount, v -> cfg.legendary.vampiricHealAmount = v, 0.0F, 40.0F);
                logicalY += ROW_H;
                y = spacer(y);
                logicalY += 8;
                y = addIntRow("Supreme helmet night vision amp", y, labelX, labelW, fieldX, fieldW, () -> cfg.legendary.supremeHelmetNightVisionAmplifier, v -> cfg.legendary.supremeHelmetNightVisionAmplifier = v, 0, 10);
                logicalY += ROW_H;
                y = addIntRow("Supreme helmet water breathing amp", y, labelX, labelW, fieldX, fieldW, () -> cfg.legendary.supremeHelmetWaterBreathingAmplifier, v -> cfg.legendary.supremeHelmetWaterBreathingAmplifier = v, 0, 10);
                logicalY += ROW_H;
                y = addIntRow("Supreme chest strength amp", y, labelX, labelW, fieldX, fieldW, () -> cfg.legendary.supremeChestStrengthAmplifier, v -> cfg.legendary.supremeChestStrengthAmplifier = v, 0, 10);
                logicalY += ROW_H;
                y = addIntRow("Supreme leggings fire res amp", y, labelX, labelW, fieldX, fieldW, () -> cfg.legendary.supremeLeggingsFireResAmplifier, v -> cfg.legendary.supremeLeggingsFireResAmplifier = v, 0, 10);
                logicalY += ROW_H;
                y = addIntRow("Supreme boots speed amp", y, labelX, labelW, fieldX, fieldW, () -> cfg.legendary.supremeBootsSpeedAmplifier, v -> cfg.legendary.supremeBootsSpeedAmplifier = v, 0, 10);
                logicalY += ROW_H;
                y = addIntRow("Supreme set resistance amp", y, labelX, labelW, fieldX, fieldW, () -> cfg.legendary.supremeSetResistanceAmplifier, v -> cfg.legendary.supremeSetResistanceAmplifier = v, 0, 10);
                logicalY += ROW_H;
                y = spacer(y);
                logicalY += 8;
                y = addNoteRow("Edit recipe gem requirements in balance.json", y, labelX, labelW);
                logicalY += ROW_H;
            }
        }

        int viewHeight = footerTop - fieldsTop - 6;
        maxScrollPx = Math.max(0, (logicalY - fieldsTop) - viewHeight);
        int prevScroll = scrollPx;
        scrollPx = clampInt(scrollPx, 0, maxScrollPx);
        if (prevScroll != scrollPx) {
            rebuild();
            return;
        }

        int footerX = contentX;
        int footerY = footerTop;

        saveButton = addDrawableChild(ButtonWidget.builder(Text.literal("Save"), b -> save(false)).dimensions(footerX, footerY, 110, 20).build());
        saveReloadButton = addDrawableChild(ButtonWidget.builder(Text.literal("Save + Reload"), b -> save(true)).dimensions(footerX + 116, footerY, 130, 20).build());
        ButtonWidget reloadButton = addDrawableChild(ButtonWidget.builder(Text.literal("Reload from disk"), b -> {
            load();
            rebuild();
        }).dimensions(footerX + 252, footerY, 140, 20).build());

        footerY += 24;

        ButtonWidget resetSectionButton = addDrawableChild(ButtonWidget.builder(Text.literal("Reset section"), b -> {
            resetSection();
            rebuild();
        }).dimensions(footerX, footerY, 130, 20).build());
        ButtonWidget resetAllButton = addDrawableChild(ButtonWidget.builder(Text.literal("Reset all"), b -> {
            cfg = normalize(new GemsBalanceConfig());
            dirty = true;
            rebuild();
        }).dimensions(footerX + 136, footerY, 110, 20).build());
        ButtonWidget copyPathButton = addDrawableChild(ButtonWidget.builder(Text.literal("Copy config path"), b -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null) {
                client.keyboard.setClipboard(balancePath().toString());
            }
        }).dimensions(footerX + 252, footerY, 140, 20).build());

        footerY += 24;

        ButtonWidget dumpButton = addDrawableChild(ButtonWidget.builder(Text.literal("Dump effective"), b -> ClientCommandSender.sendCommand("gems dumpBalance")).dimensions(footerX, footerY, 130, 20).build());
        ButtonWidget doneButton = addDrawableChild(ButtonWidget.builder(Text.literal("Done"), b -> close()).dimensions(footerX + 136, footerY, 110, 20).build());

        saveButton.active = (canEdit && dirty && validation.isValid()) || clientDirty;
        saveReloadButton.active = canEdit && dirty && validation.isValid();
        resetSectionButton.active = canEdit;
        resetAllButton.active = canEdit;
        dumpButton.active = canEdit;
        reloadButton.active = true;
        copyPathButton.active = true;
        doneButton.active = true;

        updateButtonState();
    }

    private void updateButtonState() {
        if (saveButton != null) {
            saveButton.active = (canEdit && dirty && validation.isValid()) || clientDirty;
        }
        if (saveReloadButton != null) {
            saveReloadButton.active = canEdit && dirty && validation.isValid();
        }
    }

    private void save(boolean reload) {
        boolean saveServer = dirty && canEdit && validation.isValid();
        boolean saveClient = clientDirty;
        if (!saveServer && !saveClient) {
            return;
        }
        if (saveServer) {
            GemsConfigManager.writeBalanceForUi(cfg);
            dirty = false;
            if (reload) {
                ClientCommandSender.sendCommand("gems reloadBalance");
            }
        }
        if (saveClient) {
            GemsClientConfigManager.save(clientCfg);
            sendPassiveToggle();
            clientDirty = false;
        }
        rebuild();
    }

    private void sendPassiveToggle() {
        if (MinecraftClient.getInstance().getNetworkHandler() == null) {
            return;
        }
        ClientPlayNetworking.send(new ClientPassiveTogglePayload(clientCfg.passivesEnabled));
    }

    private void resetSection() {
        switch (section) {
            case CLIENT -> {
                clientCfg = new GemsClientConfig();
                clientDirty = true;
            }
            case VISUAL -> cfg.visual = new GemsBalanceConfig.Visual();
            case SYSTEMS -> cfg.systems = new GemsBalanceConfig.Systems();
            case ASTRA -> cfg.astra = new GemsBalanceConfig.Astra();
            case FIRE -> cfg.fire = new GemsBalanceConfig.Fire();
            case FLUX -> cfg.flux = new GemsBalanceConfig.Flux();
            case LIFE -> cfg.life = new GemsBalanceConfig.Life();
            case PUFF -> cfg.puff = new GemsBalanceConfig.Puff();
            case SPEED -> cfg.speed = new GemsBalanceConfig.Speed();
            case STRENGTH -> cfg.strength = new GemsBalanceConfig.Strength();
            case WEALTH -> cfg.wealth = new GemsBalanceConfig.Wealth();
            case TERROR -> cfg.terror = new GemsBalanceConfig.Terror();
            case SUMMONER -> cfg.summoner = new GemsBalanceConfig.Summoner();
            case SPACE -> cfg.space = new GemsBalanceConfig.Space();
            case REAPER -> cfg.reaper = new GemsBalanceConfig.Reaper();
            case PILLAGER -> cfg.pillager = new GemsBalanceConfig.Pillager();
            case SPY_MIMIC -> cfg.spyMimic = new GemsBalanceConfig.SpyMimic();
            case BEACON -> cfg.beacon = new GemsBalanceConfig.Beacon();
            case AIR -> cfg.air = new GemsBalanceConfig.Air();
            case LEGENDARY -> cfg.legendary = new GemsBalanceConfig.Legendary();
        }
        if (section != Section.CLIENT) {
            dirty = true;
        }
    }

    private static int spacer(int y) {
        return y + 8;
    }

    private int addBoolRow(String label, int y, int labelX, int labelW, int fieldX, int fieldW, Supplier<Boolean> getter, Consumer<Boolean> setter) {
        addDrawableChild(ButtonWidget.builder(Text.literal(label), b -> {
        }).dimensions(labelX, y, labelW, 20).build()).active = false;

        CyclingButtonWidget<Boolean> btn = CyclingButtonWidget.onOffBuilder(getter.get())
                .build(fieldX, y, fieldW, 20, Text.empty(), (b, v) -> {
                    setter.accept(v);
                    dirty = true;
                    updateButtonState();
                });
        btn.active = canEdit;
        addDrawableChild(btn);
        return y + ROW_H;
    }

    private int addClientBoolRow(String label, int y, int labelX, int labelW, int fieldX, int fieldW, Supplier<Boolean> getter, Consumer<Boolean> setter) {
        addDrawableChild(ButtonWidget.builder(Text.literal(label), b -> {
        }).dimensions(labelX, y, labelW, 20).build()).active = false;

        CyclingButtonWidget<Boolean> btn = CyclingButtonWidget.onOffBuilder(getter.get())
                .build(fieldX, y, fieldW, 20, Text.empty(), (b, v) -> {
                    setter.accept(v);
                    clientDirty = true;
                    updateButtonState();
                });
        btn.active = true;
        addDrawableChild(btn);
        return y + ROW_H;
    }

		    private int addClientControlModeRow(String label, int y, int labelX, int labelW, int fieldX, int fieldW, Supplier<GemsClientConfig.ControlMode> getter, Consumer<GemsClientConfig.ControlMode> setter) {
		        addDrawableChild(ButtonWidget.builder(Text.literal(label), b -> {
		        }).dimensions(labelX, y, labelW, 20).build()).active = false;
		
		        CyclingButtonWidget<GemsClientConfig.ControlMode> btn = CyclingButtonWidget.builder(mode -> Text.literal(mode.name()), getter.get())
		                .values(java.util.Arrays.asList(GemsClientConfig.ControlMode.values()))
		                .build(fieldX, y, fieldW, 20, Text.empty(), (b, v) -> {
		                    setter.accept(v);
		                    clientDirty = true;
		                    updateButtonState();
	                });
	        btn.active = true;
	        addDrawableChild(btn);
	        return y + ROW_H;
	    }

    private int addIntRow(String label, int y, int labelX, int labelW, int fieldX, int fieldW, Supplier<Integer> getter, Consumer<Integer> setter, int min, int max) {
        addDrawableChild(ButtonWidget.builder(labelWithRange(label, intRange(min, max)), b -> {
        }).dimensions(labelX, y, labelW, 20).build()).active = false;
        ValidationTracker.Flag flag = validation.flag();
        TextFieldWidget field = new TextFieldWidget(this.textRenderer, fieldX, y, fieldW, 20, Text.empty());
        field.setText(Integer.toString(getter.get()));
        field.setEditable(canEdit);
        field.setChangedListener(s -> {
            Integer parsed = tryParseInt(s);
            boolean ok = parsed != null;
            flag.setOk(ok);
            if (ok) {
                setter.accept(clampInt(parsed, min, max));
                dirty = true;
            }
            updateButtonState();
        });
        addDrawableChild(field);
        return y + ROW_H;
    }

    private int addStringRow(String label, int y, int labelX, int labelW, int fieldX, int fieldW, Supplier<String> getter, Consumer<String> setter) {
        addDrawableChild(ButtonWidget.builder(Text.literal(label), b -> {
        }).dimensions(labelX, y, labelW, 20).build()).active = false;
        TextFieldWidget field = new TextFieldWidget(this.textRenderer, fieldX, y, fieldW, 20, Text.empty());
        field.setText(getter.get());
        field.setEditable(canEdit);
        field.setChangedListener(s -> {
            setter.accept(s);
            dirty = true;
            updateButtonState();
        });
        addDrawableChild(field);
        return y + ROW_H;
    }

    private int addNoteRow(String label, int y, int labelX, int labelW) {
        addDrawableChild(ButtonWidget.builder(Text.literal(label), b -> {
        }).dimensions(labelX, y, labelW, 20).build()).active = false;
        return y + ROW_H;
    }

    private int addFloatRow(String label, int y, int labelX, int labelW, int fieldX, int fieldW, Supplier<Float> getter, Consumer<Float> setter, float min, float max) {
        addDrawableChild(ButtonWidget.builder(labelWithRange(label, floatRange(min, max)), b -> {
        }).dimensions(labelX, y, labelW, 20).build()).active = false;
        ValidationTracker.Flag flag = validation.flag();
        TextFieldWidget field = new TextFieldWidget(this.textRenderer, fieldX, y, fieldW, 20, Text.empty());
        field.setText(String.format(Locale.ROOT, "%.3f", getter.get()));
        field.setEditable(canEdit);
        field.setChangedListener(s -> {
            Float parsed = tryParseFloat(s);
            boolean ok = parsed != null;
            flag.setOk(ok);
            if (ok) {
                setter.accept(clampFloat(parsed, min, max));
                dirty = true;
            }
            updateButtonState();
        });
        addDrawableChild(field);
        return y + ROW_H;
    }

    private int addDoubleRow(String label, int y, int labelX, int labelW, int fieldX, int fieldW, Supplier<Double> getter, Consumer<Double> setter, double min, double max) {
        addDrawableChild(ButtonWidget.builder(labelWithRange(label, doubleRange(min, max)), b -> {
        }).dimensions(labelX, y, labelW, 20).build()).active = false;
        ValidationTracker.Flag flag = validation.flag();
        TextFieldWidget field = new TextFieldWidget(this.textRenderer, fieldX, y, fieldW, 20, Text.empty());
        field.setText(String.format(Locale.ROOT, "%.3f", getter.get()));
        field.setEditable(canEdit);
        field.setChangedListener(s -> {
            Double parsed = tryParseDouble(s);
            boolean ok = parsed != null;
            flag.setOk(ok);
            if (ok) {
                setter.accept(clampDouble(parsed, min, max));
                dirty = true;
            }
            updateButtonState();
        });
        addDrawableChild(field);
        return y + ROW_H;
    }

    private static Integer tryParseInt(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static Float tryParseFloat(String s) {
        try {
            return Float.parseFloat(s.trim());
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static Double tryParseDouble(String s) {
        try {
            return Double.parseDouble(s.trim());
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static int clampInt(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    private static float clampFloat(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }

    private static double clampDouble(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    private static Text labelWithRange(String label, String range) {
        return Text.literal(label).append(Text.literal(" [" + range + "]").formatted(Formatting.DARK_GRAY));
    }

    private static String intRange(int min, int max) {
        return min + ".." + max;
    }

    private static String floatRange(float min, float max) {
        return formatNumber(min) + ".." + formatNumber(max);
    }

    private static String doubleRange(double min, double max) {
        return formatNumber(min) + ".." + formatNumber(max);
    }

    private static String formatNumber(double v) {
        String s = String.format(Locale.ROOT, "%.3f", v);
        int end = s.length();
        while (end > 0 && s.charAt(end - 1) == '0') {
            end--;
        }
        if (end > 0 && s.charAt(end - 1) == '.') {
            end--;
        }
        return s.substring(0, Math.max(1, end));
    }

    private static Path balancePath() {
        return GemsConfigManager.balancePathForUi();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (sidebarMaxScrollPx > 0
                && mouseX >= sidebarX && mouseX < sidebarX + sidebarW
                && mouseY >= sidebarY && mouseY < sidebarY + sidebarViewH) {
            int delta = (int) Math.round(verticalAmount * 12.0D);
            if (delta != 0) {
                sidebarScrollPx = clampInt(sidebarScrollPx - delta, 0, sidebarMaxScrollPx);
                rebuild();
                return true;
            }
        }
        if (maxScrollPx <= 0) {
            return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }
        int delta = (int) Math.round(verticalAmount * 12.0D);
        if (delta == 0) {
            return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }
        scrollPx = clampInt(scrollPx - delta, 0, maxScrollPx);
        rebuild();
        return true;
    }

    @Override
    public void close() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null) {
            client.setScreen(parent);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        int centerX = this.width / 2;
        context.drawCenteredTextWithShadow(textRenderer, this.title, centerX, 18, 0xFFFFFF);

        String path = balancePath().toString();
        context.drawCenteredTextWithShadow(textRenderer, Text.literal(path), centerX, 32, 0xA0A0A0);

        if (loadError != null) {
            context.drawCenteredTextWithShadow(textRenderer, Text.literal(loadError), centerX, this.height - 18, 0xFF5555);
        }

        if (!canEdit) {
            context.drawCenteredTextWithShadow(textRenderer, Text.literal("Config edits require op permissions on multiplayer servers."), centerX, this.height - 56, 0xFFAA00);
        } else if (!validation.isValid()) {
            context.drawCenteredTextWithShadow(textRenderer, Text.literal("Fix invalid values to enable Save."), centerX, this.height - 42, 0xFF5555);
        } else if (dirty) {
            context.drawCenteredTextWithShadow(textRenderer, Text.literal("Unsaved changes."), centerX, this.height - 42, 0xFFFF55);
        }
    }

    private static boolean canEditConfig() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null) {
            return false;
        }
        return client.isInSingleplayer();
    }

    private enum Section {
        CLIENT("Client"),
        VISUAL("Visual"),
        SYSTEMS("Systems"),
        ASTRA("Astra"),
        FIRE("Fire"),
        FLUX("Flux"),
        LIFE("Life"),
        PUFF("Puff"),
        SPEED("Speed"),
        STRENGTH("Strength"),
        WEALTH("Wealth"),
        TERROR("Terror"),
        SUMMONER("Summoner"),
        SPACE("Space"),
        REAPER("Reaper"),
        PILLAGER("Pillager"),
        SPY_MIMIC("Spy/Mimic"),
        BEACON("Beacon"),
        AIR("Air"),
        LEGENDARY("Legendary");

        private final String label;

        Section(String label) {
            this.label = label;
        }
    }

    private static GemsBalanceConfig normalize(GemsBalanceConfig cfg) {
        if (cfg.visual == null) {
            cfg.visual = new GemsBalanceConfig.Visual();
        }
        if (cfg.systems == null) {
            cfg.systems = new GemsBalanceConfig.Systems();
        }
        if (cfg.fire == null) {
            cfg.fire = new GemsBalanceConfig.Fire();
        }
        if (cfg.flux == null) {
            cfg.flux = new GemsBalanceConfig.Flux();
        }
        if (cfg.astra == null) {
            cfg.astra = new GemsBalanceConfig.Astra();
        }
        if (cfg.life == null) {
            cfg.life = new GemsBalanceConfig.Life();
        }
        if (cfg.puff == null) {
            cfg.puff = new GemsBalanceConfig.Puff();
        }
        if (cfg.speed == null) {
            cfg.speed = new GemsBalanceConfig.Speed();
        }
        if (cfg.strength == null) {
            cfg.strength = new GemsBalanceConfig.Strength();
        }
        if (cfg.wealth == null) {
            cfg.wealth = new GemsBalanceConfig.Wealth();
        }
        if (cfg.terror == null) {
            cfg.terror = new GemsBalanceConfig.Terror();
        }
        if (cfg.summoner == null) {
            cfg.summoner = new GemsBalanceConfig.Summoner();
        }
        if (cfg.space == null) {
            cfg.space = new GemsBalanceConfig.Space();
        }
        if (cfg.reaper == null) {
            cfg.reaper = new GemsBalanceConfig.Reaper();
        }
        if (cfg.pillager == null) {
            cfg.pillager = new GemsBalanceConfig.Pillager();
        }
        if (cfg.spyMimic == null) {
            cfg.spyMimic = new GemsBalanceConfig.SpyMimic();
        }
        if (cfg.beacon == null) {
            cfg.beacon = new GemsBalanceConfig.Beacon();
        }
        if (cfg.air == null) {
            cfg.air = new GemsBalanceConfig.Air();
        }
        if (cfg.legendary == null) {
            cfg.legendary = new GemsBalanceConfig.Legendary();
        }
        return cfg;
    }

    private static final class ValidationTracker {
        private int invalid;

        void reset() {
            invalid = 0;
        }

        boolean isValid() {
            return invalid == 0;
        }

        Flag flag() {
            return new Flag(this);
        }

        private void onChange(boolean wasOk, boolean isOk) {
            if (wasOk == isOk) {
                return;
            }
            if (isOk) {
                invalid = Math.max(0, invalid - 1);
            } else {
                invalid++;
            }
        }

        private static final class Flag {
            private final ValidationTracker tracker;
            private boolean ok = true;

            private Flag(ValidationTracker tracker) {
                this.tracker = tracker;
            }

            void setOk(boolean ok) {
                boolean prev = this.ok;
                this.ok = ok;
                tracker.onChange(prev, ok);
            }
        }
    }
}
