package com.feel.gems.client.modmenu;

import com.feel.gems.config.GemsBalanceConfig;
import com.feel.gems.config.GemsConfigManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.nio.file.Path;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class GemsConfigScreen extends Screen {
    private static final int ROW_H = 22;

    private final Screen parent;

    private GemsBalanceConfig cfg;
    private String loadError = null;
    private boolean canEdit = false;

    private Section section = Section.VISUAL;
    private boolean dirty = false;
    private final ValidationTracker validation = new ValidationTracker();

    private ButtonWidget saveButton;
    private ButtonWidget saveReloadButton;

    private int scrollPx = 0;
    private int maxScrollPx = 0;

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
        this.canEdit = canEditConfig();
        this.validation.reset();
    }

    private void rebuild() {
        clearChildren();

        int sidebarX = 18;
        int sidebarY = 52;
        int sidebarW = 130;
        int sidebarH = 20;
        int sidebarGap = 4;

        for (int i = 0; i < Section.values().length; i++) {
            Section s = Section.values()[i];
            int y = sidebarY + i * (sidebarH + sidebarGap);
            ButtonWidget b = addDrawableChild(ButtonWidget.builder(Text.literal(s.label), btn -> {
                section = s;
                scrollPx = 0;
                rebuild();
            }).dimensions(sidebarX, y, sidebarW, sidebarH).build());
            b.active = s != section;
        }

        int contentX = sidebarX + sidebarW + 20;
        int fieldsTop = 52;
        int footerTop = this.height - 80;

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
                y = addIntRow("Meteor Shower count", y, labelX, labelW, fieldX, fieldW, () -> cfg.fire.meteorShowerCount, v -> cfg.fire.meteorShowerCount = v, 0, 50);
                logicalY += ROW_H;
                y = addIntRow("Meteor Shower spread blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.fire.meteorShowerSpreadBlocks, v -> cfg.fire.meteorShowerSpreadBlocks = v, 0, 48);
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

        saveButton.active = canEdit && dirty && validation.isValid();
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
            saveButton.active = canEdit && dirty && validation.isValid();
        }
        if (saveReloadButton != null) {
            saveReloadButton.active = canEdit && dirty && validation.isValid();
        }
    }

    private void save(boolean reload) {
        if (!canEdit) {
            return;
        }
        if (!validation.isValid()) {
            return;
        }
        GemsConfigManager.writeBalanceForUi(cfg);
        dirty = false;
        if (reload) {
            ClientCommandSender.sendCommand("gems reloadBalance");
        }
        rebuild();
    }

    private void resetSection() {
        switch (section) {
            case VISUAL -> cfg.visual = new GemsBalanceConfig.Visual();
            case ASTRA -> cfg.astra = new GemsBalanceConfig.Astra();
            case FIRE -> cfg.fire = new GemsBalanceConfig.Fire();
            case FLUX -> cfg.flux = new GemsBalanceConfig.Flux();
            case LIFE -> cfg.life = new GemsBalanceConfig.Life();
            case PUFF -> cfg.puff = new GemsBalanceConfig.Puff();
            case SPEED -> cfg.speed = new GemsBalanceConfig.Speed();
            case STRENGTH -> cfg.strength = new GemsBalanceConfig.Strength();
            case WEALTH -> cfg.wealth = new GemsBalanceConfig.Wealth();
        }
        dirty = true;
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

    private int addIntRow(String label, int y, int labelX, int labelW, int fieldX, int fieldW, Supplier<Integer> getter, Consumer<Integer> setter, int min, int max) {
        addDrawableChild(ButtonWidget.builder(Text.literal(label), b -> {
        }).dimensions(labelX, y, labelW, 20).build()).active = false;
        ValidationTracker.Flag flag = validation.flag();
        TextFieldWidget field = new TextFieldWidget(this.textRenderer, fieldX, y, fieldW, 20, Text.empty());
        field.setText(Integer.toString(getter.get()));
        field.setEditable(canEdit);
        field.setSelectable(canEdit);
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

    private int addFloatRow(String label, int y, int labelX, int labelW, int fieldX, int fieldW, Supplier<Float> getter, Consumer<Float> setter, float min, float max) {
        addDrawableChild(ButtonWidget.builder(Text.literal(label), b -> {
        }).dimensions(labelX, y, labelW, 20).build()).active = false;
        ValidationTracker.Flag flag = validation.flag();
        TextFieldWidget field = new TextFieldWidget(this.textRenderer, fieldX, y, fieldW, 20, Text.empty());
        field.setText(String.format(Locale.ROOT, "%.3f", getter.get()));
        field.setEditable(canEdit);
        field.setSelectable(canEdit);
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
        addDrawableChild(ButtonWidget.builder(Text.literal(label), b -> {
        }).dimensions(labelX, y, labelW, 20).build()).active = false;
        ValidationTracker.Flag flag = validation.flag();
        TextFieldWidget field = new TextFieldWidget(this.textRenderer, fieldX, y, fieldW, 20, Text.empty());
        field.setText(String.format(Locale.ROOT, "%.3f", getter.get()));
        field.setEditable(canEdit);
        field.setSelectable(canEdit);
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

    private static Path balancePath() {
        return GemsConfigManager.balancePathForUi();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
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
        if (client.isInSingleplayer()) {
            return true;
        }
        if (client.getNetworkHandler() == null) {
            return false;
        }
        var entry = client.getNetworkHandler().getPlayerListEntry(client.player.getUuid());
        return entry != null && entry.getPermissionLevel() >= 2;
    }

    private enum Section {
        VISUAL("Visual"),
        ASTRA("Astra"),
        FIRE("Fire"),
        FLUX("Flux"),
        LIFE("Life"),
        PUFF("Puff"),
        SPEED("Speed"),
        STRENGTH("Strength"),
        WEALTH("Wealth");

        private final String label;

        Section(String label) {
            this.label = label;
        }
    }

    private static GemsBalanceConfig normalize(GemsBalanceConfig cfg) {
        if (cfg.visual == null) {
            cfg.visual = new GemsBalanceConfig.Visual();
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
