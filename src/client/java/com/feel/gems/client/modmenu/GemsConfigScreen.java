package com.feel.gems.client.modmenu;

import com.feel.gems.client.GemsClientConfig;
import com.feel.gems.client.GemsClientConfigManager;
import com.feel.gems.config.GemsBalanceConfig;
import com.feel.gems.config.GemsConfigManager;
import com.feel.gems.config.GemsDisablesConfig;
import com.feel.gems.config.GemsDisablesConfigManager;
import com.feel.gems.net.ClientPassiveTogglePayload;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
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
    private GemsDisablesConfig disablesCfg;
    private String disablesLoadError = null;
    private boolean disablesDirty = false;

    private Category category = Category.GENERAL;
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
        var disablesResult = GemsDisablesConfigManager.loadOrCreateForUi();
        this.disablesCfg = disablesResult.config() != null ? disablesResult.config() : new GemsDisablesConfig();
        this.disablesLoadError = disablesResult.error();
        this.disablesDirty = false;
        this.validation.reset();
    }

    private void rebuild() {
        clearChildren();

        // Category tabs at top
        int tabX = 18;
        int tabY = 26;
        int tabW = 80;
        int tabH = 18;
        int tabGap = 4;

        for (int i = 0; i < Category.values().length; i++) {
            Category cat = Category.values()[i];
            int x = tabX + i * (tabW + tabGap);
            ButtonWidget tabBtn = addDrawableChild(ButtonWidget.builder(Text.literal(cat.label), btn -> {
                category = cat;
                // Switch to first section in this category
                Section[] catSections = Section.forCategory(cat);
                if (catSections.length > 0) {
                    section = catSections[0];
                }
                sidebarScrollPx = 0;
                scrollPx = 0;
                rebuild();
            }).dimensions(x, tabY, tabW, tabH).build());
            tabBtn.active = cat != category;
        }

        int sidebarX = 18;
        int sidebarY = 52;
        int sidebarW = 130;
        int sidebarH = 20;
        int sidebarGap = 4;

        int footerTop = this.height - 80;
        int sidebarViewH = Math.max(0, footerTop - sidebarY - 6);
        
        // Only show sections for current category
        Section[] categorySections = Section.forCategory(category);
        int sidebarContentH = categorySections.length * (sidebarH + sidebarGap) - sidebarGap;
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

        for (int i = 0; i < categorySections.length; i++) {
            Section s = categorySections[i];
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
                y = spacer(y);
                logicalY += 8;
                y = addStringRow("Mob blacklist (comma-separated ids)", y, labelX, labelW, fieldX, fieldW,
                        () -> joinList(cfg.mobBlacklist),
                        v -> cfg.mobBlacklist = parseList(v));
                logicalY += ROW_H;
            }
            case BONUS_POOL -> {
                y = addIntRow("Max bonus abilities", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.maxBonusAbilities, v -> cfg.bonusPool.maxBonusAbilities = v, 0, 10);
                logicalY += ROW_H;
                y = addIntRow("Max bonus passives", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.maxBonusPassives, v -> cfg.bonusPool.maxBonusPassives = v, 0, 10);
                logicalY += ROW_H;
                y = addBoolRow("Show bonuses in HUD", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.showBonusesInHud, v -> cfg.bonusPool.showBonusesInHud = v);
                logicalY += ROW_H;
                y = spacer(y);
                logicalY += 8;
                y = addFloatRow("Ability cooldown multiplier", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.bonusAbilityCooldownMultiplier, v -> cfg.bonusPool.bonusAbilityCooldownMultiplier = v, 0.1F, 10.0F);
                logicalY += ROW_H;
                y = addFloatRow("Ability damage multiplier", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.bonusAbilityDamageMultiplier, v -> cfg.bonusPool.bonusAbilityDamageMultiplier = v, 0.1F, 10.0F);
                logicalY += ROW_H;
                y = addFloatRow("Passive effect multiplier", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.bonusPassiveEffectMultiplier, v -> cfg.bonusPool.bonusPassiveEffectMultiplier = v, 0.1F, 10.0F);
                logicalY += ROW_H;
                y = spacer(y);
                logicalY += 8;
                y = addNoteRow("Bonus abilities", y, labelX, labelW);
                logicalY += ROW_H;
                y = addIntRow("Thunderstrike cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.thunderstrikeCooldownSeconds, v -> cfg.bonusPool.thunderstrikeCooldownSeconds = v, 0, 10000);
                logicalY += ROW_H;
                y = addFloatRow("Thunderstrike damage", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.thunderstrikeDamage, v -> cfg.bonusPool.thunderstrikeDamage = v, 0.0F, 1000.0F);
                logicalY += ROW_H;
                y = addIntRow("Thunderstrike range blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.thunderstrikeRangeBlocks, v -> cfg.bonusPool.thunderstrikeRangeBlocks = v, 0, 10000);
                logicalY += ROW_H;
                y = addIntRow("Frostbite cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.frostbiteCooldownSeconds, v -> cfg.bonusPool.frostbiteCooldownSeconds = v, 0, 10000);
                logicalY += ROW_H;
                y = addFloatRow("Frostbite damage", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.frostbiteDamage, v -> cfg.bonusPool.frostbiteDamage = v, 0.0F, 1000.0F);
                logicalY += ROW_H;
                y = addIntRow("Frostbite Freeze seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.frostbiteFreezeSeconds, v -> cfg.bonusPool.frostbiteFreezeSeconds = v, 0, 10000);
                logicalY += ROW_H;
                y = addIntRow("Frostbite range blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.frostbiteRangeBlocks, v -> cfg.bonusPool.frostbiteRangeBlocks = v, 0, 10000);
                logicalY += ROW_H;
                y = addIntRow("Earthshatter cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.earthshatterCooldownSeconds, v -> cfg.bonusPool.earthshatterCooldownSeconds = v, 0, 10000);
                logicalY += ROW_H;
                y = addFloatRow("Earthshatter damage", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.earthshatterDamage, v -> cfg.bonusPool.earthshatterDamage = v, 0.0F, 1000.0F);
                logicalY += ROW_H;
                y = addIntRow("Earthshatter radius blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.earthshatterRadiusBlocks, v -> cfg.bonusPool.earthshatterRadiusBlocks = v, 0, 10000);
                logicalY += ROW_H;
                y = addIntRow("Shadowstep cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.shadowstepCooldownSeconds, v -> cfg.bonusPool.shadowstepCooldownSeconds = v, 0, 10000);
                logicalY += ROW_H;
                y = addIntRow("Shadowstep distance blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.shadowstepDistanceBlocks, v -> cfg.bonusPool.shadowstepDistanceBlocks = v, 0, 10000);
                logicalY += ROW_H;
                y = addIntRow("Radiant Burst cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.radiantBurstCooldownSeconds, v -> cfg.bonusPool.radiantBurstCooldownSeconds = v, 0, 10000);
                logicalY += ROW_H;
                y = addFloatRow("Radiant Burst damage", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.radiantBurstDamage, v -> cfg.bonusPool.radiantBurstDamage = v, 0.0F, 1000.0F);
                logicalY += ROW_H;
                y = addIntRow("Radiant Burst radius blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.radiantBurstRadiusBlocks, v -> cfg.bonusPool.radiantBurstRadiusBlocks = v, 0, 10000);
                logicalY += ROW_H;
                y = addIntRow("Radiant Burst Blind seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.radiantBurstBlindSeconds, v -> cfg.bonusPool.radiantBurstBlindSeconds = v, 0, 10000);
                logicalY += ROW_H;
                y = addIntRow("Venomspray cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.venomsprayCooldownSeconds, v -> cfg.bonusPool.venomsprayCooldownSeconds = v, 0, 10000);
                logicalY += ROW_H;
                y = addIntRow("Venomspray Poison seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.venomsprayPoisonSeconds, v -> cfg.bonusPool.venomsprayPoisonSeconds = v, 0, 10000);
                logicalY += ROW_H;
                y = addIntRow("Venomspray Cone Angle Degrees", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.venomsprayConeAngleDegrees, v -> cfg.bonusPool.venomsprayConeAngleDegrees = v, 0, 10000);
                logicalY += ROW_H;
                y = addIntRow("Venomspray range blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.venomsprayRangeBlocks, v -> cfg.bonusPool.venomsprayRangeBlocks = v, 0, 10000);
                logicalY += ROW_H;
                y = addIntRow("Timewarp cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.timewarpCooldownSeconds, v -> cfg.bonusPool.timewarpCooldownSeconds = v, 0, 10000);
                logicalY += ROW_H;
                y = addIntRow("Timewarp duration seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.timewarpDurationSeconds, v -> cfg.bonusPool.timewarpDurationSeconds = v, 0, 10000);
                logicalY += ROW_H;
                y = addIntRow("Timewarp radius blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.timewarpRadiusBlocks, v -> cfg.bonusPool.timewarpRadiusBlocks = v, 0, 10000);
                logicalY += ROW_H;
                y = addIntRow("Timewarp Slowness amplifier", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.timewarpSlownessAmplifier, v -> cfg.bonusPool.timewarpSlownessAmplifier = v, 0, 10000);
                logicalY += ROW_H;
                y = addIntRow("Decoy Trap cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.decoyTrapCooldownSeconds, v -> cfg.bonusPool.decoyTrapCooldownSeconds = v, 0, 10000);
                logicalY += ROW_H;
                y = addFloatRow("Decoy Trap Explosion damage", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.decoyTrapExplosionDamage, v -> cfg.bonusPool.decoyTrapExplosionDamage = v, 0.0F, 1000.0F);
                logicalY += ROW_H;
                y = addIntRow("Decoy Trap Arm Time seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.decoyTrapArmTimeSeconds, v -> cfg.bonusPool.decoyTrapArmTimeSeconds = v, 0, 10000);
                logicalY += ROW_H;
                y = addIntRow("Decoy Trap max Active", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.decoyTrapMaxActive, v -> cfg.bonusPool.decoyTrapMaxActive = v, 0, 10000);
                logicalY += ROW_H;
                y = addIntRow("Decoy Trap Despawn seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.decoyTrapDespawnSeconds, v -> cfg.bonusPool.decoyTrapDespawnSeconds = v, 0, 10000);
                logicalY += ROW_H;
                y = addIntRow("Gravity Well cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.gravityWellCooldownSeconds, v -> cfg.bonusPool.gravityWellCooldownSeconds = v, 0, 10000);
                logicalY += ROW_H;
                y = addIntRow("Gravity Well duration seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.gravityWellDurationSeconds, v -> cfg.bonusPool.gravityWellDurationSeconds = v, 0, 10000);
                logicalY += ROW_H;
                y = addIntRow("Gravity Well radius blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.gravityWellRadiusBlocks, v -> cfg.bonusPool.gravityWellRadiusBlocks = v, 0, 10000);
                logicalY += ROW_H;
                y = addFloatRow("Gravity Well Pull strength", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.gravityWellPullStrength, v -> cfg.bonusPool.gravityWellPullStrength = v, 0.0F, 1000.0F);
                logicalY += ROW_H;
                y = addIntRow("Chain Lightning cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.chainLightningCooldownSeconds, v -> cfg.bonusPool.chainLightningCooldownSeconds = v, 0, 10000);
                logicalY += ROW_H;
                y = addFloatRow("Chain Lightning damage", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.chainLightningDamage, v -> cfg.bonusPool.chainLightningDamage = v, 0.0F, 1000.0F);
                logicalY += ROW_H;
                y = addIntRow("Chain Lightning max Bounces", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.chainLightningMaxBounces, v -> cfg.bonusPool.chainLightningMaxBounces = v, 0, 10000);
                logicalY += ROW_H;
                y = addIntRow("Chain Lightning Bounce range blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.chainLightningBounceRangeBlocks, v -> cfg.bonusPool.chainLightningBounceRangeBlocks = v, 0, 10000);
                logicalY += ROW_H;
                y = addIntRow("Magma Pool cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.magmaPoolCooldownSeconds, v -> cfg.bonusPool.magmaPoolCooldownSeconds = v, 0, 10000);
                logicalY += ROW_H;
                y = addFloatRow("Magma Pool damage per Second", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.magmaPoolDamagePerSecond, v -> cfg.bonusPool.magmaPoolDamagePerSecond = v, 0.0F, 1000.0F);
                logicalY += ROW_H;
                y = addIntRow("Magma Pool duration seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.magmaPoolDurationSeconds, v -> cfg.bonusPool.magmaPoolDurationSeconds = v, 0, 10000);
                logicalY += ROW_H;
                y = addIntRow("Magma Pool radius blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.magmaPoolRadiusBlocks, v -> cfg.bonusPool.magmaPoolRadiusBlocks = v, 0, 10000);
                logicalY += ROW_H;
                y = addIntRow("Ice Wall cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.iceWallCooldownSeconds, v -> cfg.bonusPool.iceWallCooldownSeconds = v, 0, 10000);
                logicalY += ROW_H;
                y = addIntRow("Ice Wall duration seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.iceWallDurationSeconds, v -> cfg.bonusPool.iceWallDurationSeconds = v, 0, 10000);
                logicalY += ROW_H;
                y = addIntRow("Ice Wall Width blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.iceWallWidthBlocks, v -> cfg.bonusPool.iceWallWidthBlocks = v, 0, 10000);
                logicalY += ROW_H;
                y = addIntRow("Ice Wall Height blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.iceWallHeightBlocks, v -> cfg.bonusPool.iceWallHeightBlocks = v, 0, 10000);
                logicalY += ROW_H;
                y = addIntRow("Wind Slash cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.windSlashCooldownSeconds, v -> cfg.bonusPool.windSlashCooldownSeconds = v, 0, 10000);
                logicalY += ROW_H;
                y = addFloatRow("Wind Slash damage", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.windSlashDamage, v -> cfg.bonusPool.windSlashDamage = v, 0.0F, 1000.0F);
                logicalY += ROW_H;
                y = addIntRow("Wind Slash range blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.windSlashRangeBlocks, v -> cfg.bonusPool.windSlashRangeBlocks = v, 0, 10000);
                logicalY += ROW_H;
                y = addIntRow("Curse Bolt cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.curseBoltCooldownSeconds, v -> cfg.bonusPool.curseBoltCooldownSeconds = v, 0, 10000);
                logicalY += ROW_H;
                y = addFloatRow("Curse Bolt damage", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.curseBoltDamage, v -> cfg.bonusPool.curseBoltDamage = v, 0.0F, 1000.0F);
                logicalY += ROW_H;
                y = addIntRow("Curse Bolt Effect duration seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.curseBoltEffectDurationSeconds, v -> cfg.bonusPool.curseBoltEffectDurationSeconds = v, 0, 10000);
                logicalY += ROW_H;
                y = addIntRow("Berserker Rage cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.berserkerRageCooldownSeconds, v -> cfg.bonusPool.berserkerRageCooldownSeconds = v, 0, 10000);
                logicalY += ROW_H;
                y = addIntRow("Berserker Rage duration seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.berserkerRageDurationSeconds, v -> cfg.bonusPool.berserkerRageDurationSeconds = v, 0, 10000);
                logicalY += ROW_H;
                y = addFloatRow("Berserker Rage damage boost percent", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.berserkerRageDamageBoostPercent, v -> cfg.bonusPool.berserkerRageDamageBoostPercent = v, 0.0F, 1000.0F);
                logicalY += ROW_H;
                y = addFloatRow("Berserker Rage damage Taken boost percent", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.berserkerRageDamageTakenBoostPercent, v -> cfg.bonusPool.berserkerRageDamageTakenBoostPercent = v, 0.0F, 1000.0F);
                logicalY += ROW_H;
                y = addIntRow("Ethereal Step cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.etherealStepCooldownSeconds, v -> cfg.bonusPool.etherealStepCooldownSeconds = v, 0, 10000);
                logicalY += ROW_H;
                y = addIntRow("Ethereal Step distance blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.etherealStepDistanceBlocks, v -> cfg.bonusPool.etherealStepDistanceBlocks = v, 0, 10000);
                logicalY += ROW_H;
                y = addIntRow("Arcane Missiles cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.arcaneMissilesCooldownSeconds, v -> cfg.bonusPool.arcaneMissilesCooldownSeconds = v, 0, 10000);
                logicalY += ROW_H;
                y = addFloatRow("Arcane Missiles damage per Missile", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.arcaneMissilesDamagePerMissile, v -> cfg.bonusPool.arcaneMissilesDamagePerMissile = v, 0.0F, 1000.0F);
                logicalY += ROW_H;
                y = addIntRow("Arcane Missiles count", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.arcaneMissilesCount, v -> cfg.bonusPool.arcaneMissilesCount = v, 0, 10000);
                logicalY += ROW_H;
                y = addIntRow("Life Tap cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.lifeTapCooldownSeconds, v -> cfg.bonusPool.lifeTapCooldownSeconds = v, 0, 10000);
                logicalY += ROW_H;
                y = addFloatRow("Life Tap health Cost", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.lifeTapHealthCost, v -> cfg.bonusPool.lifeTapHealthCost = v, 0.0F, 1000.0F);
                logicalY += ROW_H;
                y = addFloatRow("Life Tap cooldown reduction percent", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.lifeTapCooldownReductionPercent, v -> cfg.bonusPool.lifeTapCooldownReductionPercent = v, 0.0F, 1000.0F);
                logicalY += ROW_H;
                y = addIntRow("Doom Bolt cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.doomBoltCooldownSeconds, v -> cfg.bonusPool.doomBoltCooldownSeconds = v, 0, 10000);
                logicalY += ROW_H;
                y = addFloatRow("Doom Bolt damage", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.doomBoltDamage, v -> cfg.bonusPool.doomBoltDamage = v, 0.0F, 1000.0F);
                logicalY += ROW_H;
                y = addFloatRow("Doom Bolt Velocity", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.doomBoltVelocity, v -> cfg.bonusPool.doomBoltVelocity = v, 0.0F, 1000.0F);
                logicalY += ROW_H;
                y = addIntRow("Sanctuary cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.sanctuaryCooldownSeconds, v -> cfg.bonusPool.sanctuaryCooldownSeconds = v, 0, 10000);
                logicalY += ROW_H;
                y = addIntRow("Sanctuary duration seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.sanctuaryDurationSeconds, v -> cfg.bonusPool.sanctuaryDurationSeconds = v, 0, 10000);
                logicalY += ROW_H;
                y = addIntRow("Sanctuary radius blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.sanctuaryRadiusBlocks, v -> cfg.bonusPool.sanctuaryRadiusBlocks = v, 0, 10000);
                logicalY += ROW_H;
                y = addIntRow("Spectral Chains cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.spectralChainsCooldownSeconds, v -> cfg.bonusPool.spectralChainsCooldownSeconds = v, 0, 10000);
                logicalY += ROW_H;
                y = addIntRow("Spectral Chains duration seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.spectralChainsDurationSeconds, v -> cfg.bonusPool.spectralChainsDurationSeconds = v, 0, 10000);
                logicalY += ROW_H;
                y = addIntRow("Spectral Chains range blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.spectralChainsRangeBlocks, v -> cfg.bonusPool.spectralChainsRangeBlocks = v, 0, 10000);
                logicalY += ROW_H;
                y = addIntRow("Void Rift cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.voidRiftCooldownSeconds, v -> cfg.bonusPool.voidRiftCooldownSeconds = v, 0, 10000);
                logicalY += ROW_H;
                y = addFloatRow("Void Rift damage per Second", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.voidRiftDamagePerSecond, v -> cfg.bonusPool.voidRiftDamagePerSecond = v, 0.0F, 1000.0F);
                logicalY += ROW_H;
                y = addIntRow("Void Rift duration seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.voidRiftDurationSeconds, v -> cfg.bonusPool.voidRiftDurationSeconds = v, 0, 10000);
                logicalY += ROW_H;
                y = addIntRow("Void Rift radius blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.voidRiftRadiusBlocks, v -> cfg.bonusPool.voidRiftRadiusBlocks = v, 0, 10000);
                logicalY += ROW_H;
                y = addIntRow("Inferno Dash cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.infernoDashCooldownSeconds, v -> cfg.bonusPool.infernoDashCooldownSeconds = v, 0, 10000);
                logicalY += ROW_H;
                y = addFloatRow("Inferno Dash damage", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.infernoDashDamage, v -> cfg.bonusPool.infernoDashDamage = v, 0.0F, 1000.0F);
                logicalY += ROW_H;
                y = addIntRow("Inferno Dash distance blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.infernoDashDistanceBlocks, v -> cfg.bonusPool.infernoDashDistanceBlocks = v, 0, 10000);
                logicalY += ROW_H;
                y = addIntRow("Inferno Dash Fire duration seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.infernoDashFireDurationSeconds, v -> cfg.bonusPool.infernoDashFireDurationSeconds = v, 0, 10000);
                logicalY += ROW_H;
                y = addIntRow("Tidal Wave cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.tidalWaveCooldownSeconds, v -> cfg.bonusPool.tidalWaveCooldownSeconds = v, 0, 10000);
                logicalY += ROW_H;
                y = addFloatRow("Tidal Wave damage", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.tidalWaveDamage, v -> cfg.bonusPool.tidalWaveDamage = v, 0.0F, 1000.0F);
                logicalY += ROW_H;
                y = addIntRow("Tidal Wave range blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.tidalWaveRangeBlocks, v -> cfg.bonusPool.tidalWaveRangeBlocks = v, 0, 10000);
                logicalY += ROW_H;
                y = addIntRow("Tidal Wave Slow seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.tidalWaveSlowSeconds, v -> cfg.bonusPool.tidalWaveSlowSeconds = v, 0, 10000);
                logicalY += ROW_H;
                y = addIntRow("Starfall cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.starfallCooldownSeconds, v -> cfg.bonusPool.starfallCooldownSeconds = v, 0, 10000);
                logicalY += ROW_H;
                y = addFloatRow("Starfall damage per Hit", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.starfallDamagePerHit, v -> cfg.bonusPool.starfallDamagePerHit = v, 0.0F, 1000.0F);
                logicalY += ROW_H;
                y = addIntRow("Starfall Meteor count", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.starfallMeteorCount, v -> cfg.bonusPool.starfallMeteorCount = v, 0, 10000);
                logicalY += ROW_H;
                y = addIntRow("Starfall radius blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.starfallRadiusBlocks, v -> cfg.bonusPool.starfallRadiusBlocks = v, 0, 10000);
                logicalY += ROW_H;
                y = addIntRow("Bloodlust cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.bloodlustCooldownSeconds, v -> cfg.bonusPool.bloodlustCooldownSeconds = v, 0, 10000);
                logicalY += ROW_H;
                y = addIntRow("Bloodlust duration seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.bloodlustDurationSeconds, v -> cfg.bonusPool.bloodlustDurationSeconds = v, 0, 10000);
                logicalY += ROW_H;
                y = addFloatRow("Bloodlust Attack Speed per Kill", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.bloodlustAttackSpeedPerKill, v -> cfg.bonusPool.bloodlustAttackSpeedPerKill = v, 0.0F, 1000.0F);
                logicalY += ROW_H;
                y = addIntRow("Bloodlust max Stacks", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.bloodlustMaxStacks, v -> cfg.bonusPool.bloodlustMaxStacks = v, 0, 10000);
                logicalY += ROW_H;
                y = addIntRow("Crystal Cage cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.crystalCageCooldownSeconds, v -> cfg.bonusPool.crystalCageCooldownSeconds = v, 0, 10000);
                logicalY += ROW_H;
                y = addIntRow("Crystal Cage duration seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.crystalCageDurationSeconds, v -> cfg.bonusPool.crystalCageDurationSeconds = v, 0, 10000);
                logicalY += ROW_H;
                y = addIntRow("Crystal Cage range blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.crystalCageRangeBlocks, v -> cfg.bonusPool.crystalCageRangeBlocks = v, 0, 10000);
                logicalY += ROW_H;
                y = addIntRow("Phantasm cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.phantasmCooldownSeconds, v -> cfg.bonusPool.phantasmCooldownSeconds = v, 0, 10000);
                logicalY += ROW_H;
                y = addIntRow("Phantasm duration seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.phantasmDurationSeconds, v -> cfg.bonusPool.phantasmDurationSeconds = v, 0, 10000);
                logicalY += ROW_H;
                y = addFloatRow("Phantasm Explosion damage", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.phantasmExplosionDamage, v -> cfg.bonusPool.phantasmExplosionDamage = v, 0.0F, 1000.0F);
                logicalY += ROW_H;
                y = addIntRow("Sonic Boom cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.sonicBoomCooldownSeconds, v -> cfg.bonusPool.sonicBoomCooldownSeconds = v, 0, 10000);
                logicalY += ROW_H;
                y = addFloatRow("Sonic Boom damage", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.sonicBoomDamage, v -> cfg.bonusPool.sonicBoomDamage = v, 0.0F, 1000.0F);
                logicalY += ROW_H;
                y = addIntRow("Sonic Boom radius blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.sonicBoomRadiusBlocks, v -> cfg.bonusPool.sonicBoomRadiusBlocks = v, 0, 10000);
                logicalY += ROW_H;
                y = addFloatRow("Sonic Boom Knockback strength", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.sonicBoomKnockbackStrength, v -> cfg.bonusPool.sonicBoomKnockbackStrength = v, 0.0F, 1000.0F);
                logicalY += ROW_H;
                y = addIntRow("Vampiric Touch cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.vampiricTouchCooldownSeconds, v -> cfg.bonusPool.vampiricTouchCooldownSeconds = v, 0, 10000);
                logicalY += ROW_H;
                y = addFloatRow("Vampiric Touch damage", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.vampiricTouchDamage, v -> cfg.bonusPool.vampiricTouchDamage = v, 0.0F, 1000.0F);
                logicalY += ROW_H;
                y = addFloatRow("Vampiric Touch Heal percent", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.vampiricTouchHealPercent, v -> cfg.bonusPool.vampiricTouchHealPercent = v, 0.0F, 1000.0F);
                logicalY += ROW_H;
                y = addIntRow("Blinding Flash cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.blindingFlashCooldownSeconds, v -> cfg.bonusPool.blindingFlashCooldownSeconds = v, 0, 10000);
                logicalY += ROW_H;
                y = addIntRow("Blinding Flash Blind seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.blindingFlashBlindSeconds, v -> cfg.bonusPool.blindingFlashBlindSeconds = v, 0, 10000);
                logicalY += ROW_H;
                y = addIntRow("Blinding Flash radius blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.blindingFlashRadiusBlocks, v -> cfg.bonusPool.blindingFlashRadiusBlocks = v, 0, 10000);
                logicalY += ROW_H;
                y = addIntRow("Storm Call cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.stormCallCooldownSeconds, v -> cfg.bonusPool.stormCallCooldownSeconds = v, 0, 10000);
                logicalY += ROW_H;
                y = addFloatRow("Storm Call damage per Strike", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.stormCallDamagePerStrike, v -> cfg.bonusPool.stormCallDamagePerStrike = v, 0.0F, 1000.0F);
                logicalY += ROW_H;
                y = addIntRow("Storm Call duration seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.stormCallDurationSeconds, v -> cfg.bonusPool.stormCallDurationSeconds = v, 0, 10000);
                logicalY += ROW_H;
                y = addIntRow("Storm Call radius blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.stormCallRadiusBlocks, v -> cfg.bonusPool.stormCallRadiusBlocks = v, 0, 10000);
                logicalY += ROW_H;
                y = addIntRow("Storm Call Strikes per Second", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.stormCallStrikesPerSecond, v -> cfg.bonusPool.stormCallStrikesPerSecond = v, 0, 10000);
                logicalY += ROW_H;
                y = addIntRow("Quicksand cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.quicksandCooldownSeconds, v -> cfg.bonusPool.quicksandCooldownSeconds = v, 0, 10000);
                logicalY += ROW_H;
                y = addIntRow("Quicksand duration seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.quicksandDurationSeconds, v -> cfg.bonusPool.quicksandDurationSeconds = v, 0, 10000);
                logicalY += ROW_H;
                y = addIntRow("Quicksand radius blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.quicksandRadiusBlocks, v -> cfg.bonusPool.quicksandRadiusBlocks = v, 0, 10000);
                logicalY += ROW_H;
                y = addIntRow("Quicksand Slowness amplifier", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.quicksandSlownessAmplifier, v -> cfg.bonusPool.quicksandSlownessAmplifier = v, 0, 10000);
                logicalY += ROW_H;
                y = addIntRow("Searing Light cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.searingLightCooldownSeconds, v -> cfg.bonusPool.searingLightCooldownSeconds = v, 0, 10000);
                logicalY += ROW_H;
                y = addFloatRow("Searing Light damage", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.searingLightDamage, v -> cfg.bonusPool.searingLightDamage = v, 0.0F, 1000.0F);
                logicalY += ROW_H;
                y = addFloatRow("Searing Light Undead Bonus damage", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.searingLightUndeadBonusDamage, v -> cfg.bonusPool.searingLightUndeadBonusDamage = v, 0.0F, 1000.0F);
                logicalY += ROW_H;
                y = addIntRow("Searing Light range blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.searingLightRangeBlocks, v -> cfg.bonusPool.searingLightRangeBlocks = v, 0, 10000);
                logicalY += ROW_H;
                y = addIntRow("Spectral Blade cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.spectralBladeCooldownSeconds, v -> cfg.bonusPool.spectralBladeCooldownSeconds = v, 0, 10000);
                logicalY += ROW_H;
                y = addFloatRow("Spectral Blade damage", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.spectralBladeDamage, v -> cfg.bonusPool.spectralBladeDamage = v, 0.0F, 1000.0F);
                logicalY += ROW_H;
                y = addIntRow("Spectral Blade duration seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.spectralBladeDurationSeconds, v -> cfg.bonusPool.spectralBladeDurationSeconds = v, 0, 10000);
                logicalY += ROW_H;
                y = addIntRow("Nether Portal cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.netherPortalCooldownSeconds, v -> cfg.bonusPool.netherPortalCooldownSeconds = v, 0, 10000);
                logicalY += ROW_H;
                y = addIntRow("Nether Portal distance blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.netherPortalDistanceBlocks, v -> cfg.bonusPool.netherPortalDistanceBlocks = v, 0, 10000);
                logicalY += ROW_H;
                y = addIntRow("Entangle cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.entangleCooldownSeconds, v -> cfg.bonusPool.entangleCooldownSeconds = v, 0, 10000);
                logicalY += ROW_H;
                y = addIntRow("Entangle duration seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.entangleDurationSeconds, v -> cfg.bonusPool.entangleDurationSeconds = v, 0, 10000);
                logicalY += ROW_H;
                y = addIntRow("Entangle radius blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.entangleRadiusBlocks, v -> cfg.bonusPool.entangleRadiusBlocks = v, 0, 10000);
                logicalY += ROW_H;
                y = addIntRow("Mind Spike cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.mindSpikeCooldownSeconds, v -> cfg.bonusPool.mindSpikeCooldownSeconds = v, 0, 10000);
                logicalY += ROW_H;
                y = addFloatRow("Mind Spike damage per Second", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.mindSpikeDamagePerSecond, v -> cfg.bonusPool.mindSpikeDamagePerSecond = v, 0.0F, 1000.0F);
                logicalY += ROW_H;
                y = addIntRow("Mind Spike duration seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.mindSpikeDurationSeconds, v -> cfg.bonusPool.mindSpikeDurationSeconds = v, 0, 10000);
                logicalY += ROW_H;
                y = addIntRow("Mind Spike range blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.mindSpikeRangeBlocks, v -> cfg.bonusPool.mindSpikeRangeBlocks = v, 0, 10000);
                logicalY += ROW_H;
                y = addIntRow("Seismic Slam cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.seismicSlamCooldownSeconds, v -> cfg.bonusPool.seismicSlamCooldownSeconds = v, 0, 10000);
                logicalY += ROW_H;
                y = addFloatRow("Seismic Slam damage", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.seismicSlamDamage, v -> cfg.bonusPool.seismicSlamDamage = v, 0.0F, 1000.0F);
                logicalY += ROW_H;
                y = addIntRow("Seismic Slam radius blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.seismicSlamRadiusBlocks, v -> cfg.bonusPool.seismicSlamRadiusBlocks = v, 0, 10000);
                logicalY += ROW_H;
                y = addFloatRow("Seismic Slam Knockup strength", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.seismicSlamKnockupStrength, v -> cfg.bonusPool.seismicSlamKnockupStrength = v, 0.0F, 1000.0F);
                logicalY += ROW_H;
                y = addIntRow("Icicle Barrage cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.icicleBarrageCooldownSeconds, v -> cfg.bonusPool.icicleBarrageCooldownSeconds = v, 0, 10000);
                logicalY += ROW_H;
                y = addFloatRow("Icicle Barrage damage per Icicle", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.icicleBarrageDamagePerIcicle, v -> cfg.bonusPool.icicleBarrageDamagePerIcicle = v, 0.0F, 1000.0F);
                logicalY += ROW_H;
                y = addIntRow("Icicle Barrage count", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.icicleBarrageCount, v -> cfg.bonusPool.icicleBarrageCount = v, 0, 10000);
                logicalY += ROW_H;
                y = addIntRow("Icicle Barrage range blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.icicleBarrageRangeBlocks, v -> cfg.bonusPool.icicleBarrageRangeBlocks = v, 0, 10000);
                logicalY += ROW_H;
                y = addIntRow("Banishment cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.banishmentCooldownSeconds, v -> cfg.bonusPool.banishmentCooldownSeconds = v, 0, 10000);
                logicalY += ROW_H;
                y = addIntRow("Banishment distance blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.banishmentDistanceBlocks, v -> cfg.bonusPool.banishmentDistanceBlocks = v, 0, 10000);
                logicalY += ROW_H;
                y = addIntRow("Banishment range blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.banishmentRangeBlocks, v -> cfg.bonusPool.banishmentRangeBlocks = v, 0, 10000);
                logicalY += ROW_H;
                y = addIntRow("Corpse Explosion cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.corpseExplosionCooldownSeconds, v -> cfg.bonusPool.corpseExplosionCooldownSeconds = v, 0, 10000);
                logicalY += ROW_H;
                y = addFloatRow("Corpse Explosion damage", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.corpseExplosionDamage, v -> cfg.bonusPool.corpseExplosionDamage = v, 0.0F, 1000.0F);
                logicalY += ROW_H;
                y = addIntRow("Corpse Explosion radius blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.corpseExplosionRadiusBlocks, v -> cfg.bonusPool.corpseExplosionRadiusBlocks = v, 0, 10000);
                logicalY += ROW_H;
                y = addIntRow("Corpse Explosion Corpse range blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.corpseExplosionCorpseRangeBlocks, v -> cfg.bonusPool.corpseExplosionCorpseRangeBlocks = v, 0, 10000);
                logicalY += ROW_H;
                y = addIntRow("Corpse Explosion Mark duration seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.corpseExplosionMarkDurationSeconds, v -> cfg.bonusPool.corpseExplosionMarkDurationSeconds = v, 0, 10000);
                logicalY += ROW_H;
                y = addIntRow("Soul Swap cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.soulSwapCooldownSeconds, v -> cfg.bonusPool.soulSwapCooldownSeconds = v, 0, 10000);
                logicalY += ROW_H;
                y = addIntRow("Soul Swap range blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.soulSwapRangeBlocks, v -> cfg.bonusPool.soulSwapRangeBlocks = v, 0, 10000);
                logicalY += ROW_H;
                y = addIntRow("Mark Of Death cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.markOfDeathCooldownSeconds, v -> cfg.bonusPool.markOfDeathCooldownSeconds = v, 0, 10000);
                logicalY += ROW_H;
                y = addIntRow("Mark Of Death duration seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.markOfDeathDurationSeconds, v -> cfg.bonusPool.markOfDeathDurationSeconds = v, 0, 10000);
                logicalY += ROW_H;
                y = addFloatRow("Mark Of Death Bonus damage percent", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.markOfDeathBonusDamagePercent, v -> cfg.bonusPool.markOfDeathBonusDamagePercent = v, 0.0F, 1000.0F);
                logicalY += ROW_H;
                y = addIntRow("Mark Of Death range blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.markOfDeathRangeBlocks, v -> cfg.bonusPool.markOfDeathRangeBlocks = v, 0, 10000);
                logicalY += ROW_H;
                y = addIntRow("Iron Maiden cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.ironMaidenCooldownSeconds, v -> cfg.bonusPool.ironMaidenCooldownSeconds = v, 0, 10000);
                logicalY += ROW_H;
                y = addIntRow("Iron Maiden duration seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.ironMaidenDurationSeconds, v -> cfg.bonusPool.ironMaidenDurationSeconds = v, 0, 10000);
                logicalY += ROW_H;
                y = addFloatRow("Iron Maiden Reflect percent", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.ironMaidenReflectPercent, v -> cfg.bonusPool.ironMaidenReflectPercent = v, 0.0F, 1000.0F);
                logicalY += ROW_H;
                y = addIntRow("Warp Strike cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.warpStrikeCooldownSeconds, v -> cfg.bonusPool.warpStrikeCooldownSeconds = v, 0, 10000);
                logicalY += ROW_H;
                y = addFloatRow("Warp Strike damage", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.warpStrikeDamage, v -> cfg.bonusPool.warpStrikeDamage = v, 0.0F, 1000.0F);
                logicalY += ROW_H;
                y = addIntRow("Warp Strike range blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.warpStrikeRangeBlocks, v -> cfg.bonusPool.warpStrikeRangeBlocks = v, 0, 10000);
                logicalY += ROW_H;
                y = addIntRow("Vortex Strike cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.vortexStrikeCooldownSeconds, v -> cfg.bonusPool.vortexStrikeCooldownSeconds = v, 0, 10000);
                logicalY += ROW_H;
                y = addFloatRow("Vortex Strike damage", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.vortexStrikeDamage, v -> cfg.bonusPool.vortexStrikeDamage = v, 0.0F, 1000.0F);
                logicalY += ROW_H;
                y = addIntRow("Vortex Strike radius blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.vortexStrikeRadiusBlocks, v -> cfg.bonusPool.vortexStrikeRadiusBlocks = v, 0, 10000);
                logicalY += ROW_H;
                y = addFloatRow("Vortex Strike Pull strength", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.vortexStrikePullStrength, v -> cfg.bonusPool.vortexStrikePullStrength = v, 0.0F, 1000.0F);
                logicalY += ROW_H;
                y = addIntRow("Plague Cloud cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.plagueCloudCooldownSeconds, v -> cfg.bonusPool.plagueCloudCooldownSeconds = v, 0, 10000);
                logicalY += ROW_H;
                y = addIntRow("Plague Cloud duration seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.plagueCloudDurationSeconds, v -> cfg.bonusPool.plagueCloudDurationSeconds = v, 0, 10000);
                logicalY += ROW_H;
                y = addIntRow("Plague Cloud Poison amplifier", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.plagueCloudPoisonAmplifier, v -> cfg.bonusPool.plagueCloudPoisonAmplifier = v, 0, 10000);
                logicalY += ROW_H;
                y = addIntRow("Plague Cloud Weakness amplifier", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.plagueCloudWeaknessAmplifier, v -> cfg.bonusPool.plagueCloudWeaknessAmplifier = v, 0, 10000);
                logicalY += ROW_H;
                y = addIntRow("Overcharge cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.overchargeCooldownSeconds, v -> cfg.bonusPool.overchargeCooldownSeconds = v, 0, 10000);
                logicalY += ROW_H;
                y = addFloatRow("Overcharge damage multiplier", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.overchargeDamageMultiplier, v -> cfg.bonusPool.overchargeDamageMultiplier = v, 0.0F, 1000.0F);
                logicalY += ROW_H;
                y = addFloatRow("Overcharge health Cost", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.overchargeHealthCost, v -> cfg.bonusPool.overchargeHealthCost = v, 0.0F, 1000.0F);
                logicalY += ROW_H;
                y = addIntRow("Overcharge duration seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.overchargeDurationSeconds, v -> cfg.bonusPool.overchargeDurationSeconds = v, 0, 10000);
                logicalY += ROW_H;
                y = addIntRow("Gravity Crush cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.gravityCrushCooldownSeconds, v -> cfg.bonusPool.gravityCrushCooldownSeconds = v, 0, 10000);
                logicalY += ROW_H;
                y = addFloatRow("Gravity Crush damage", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.gravityCrushDamage, v -> cfg.bonusPool.gravityCrushDamage = v, 0.0F, 1000.0F);
                logicalY += ROW_H;
                y = addIntRow("Gravity Crush Root duration seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.gravityCrushRootDurationSeconds, v -> cfg.bonusPool.gravityCrushRootDurationSeconds = v, 0, 10000);
                logicalY += ROW_H;
                y = addIntRow("Gravity Crush range blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.gravityCrushRangeBlocks, v -> cfg.bonusPool.gravityCrushRangeBlocks = v, 0, 10000);
                logicalY += ROW_H;
                y = spacer(y);
                logicalY += 8;
                y = addNoteRow("Bonus passives", y, labelX, labelW);
                logicalY += ROW_H;
                y = addFloatRow("Thorns Aura damage percent", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.thornsAuraDamagePercent, v -> cfg.bonusPool.thornsAuraDamagePercent = v, 0.0F, 1000.0F);
                logicalY += ROW_H;
                y = addFloatRow("Lifesteal percent", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.lifestealPercent, v -> cfg.bonusPool.lifestealPercent = v, 0.0F, 1000.0F);
                logicalY += ROW_H;
                y = addFloatRow("Dodge chance percent", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.dodgeChancePercent, v -> cfg.bonusPool.dodgeChancePercent = v, 0.0F, 1000.0F);
                logicalY += ROW_H;
                y = addFloatRow("Critical Strike Bonus damage percent", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.criticalStrikeBonusDamagePercent, v -> cfg.bonusPool.criticalStrikeBonusDamagePercent = v, 0.0F, 1000.0F);
                logicalY += ROW_H;
                y = addFloatRow("Critical Strike chance Bonus", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.criticalStrikeChanceBonus, v -> cfg.bonusPool.criticalStrikeChanceBonus = v, 0.0F, 1000.0F);
                logicalY += ROW_H;
                y = addFloatRow("Mana Shield xp per damage", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.manaShieldXpPerDamage, v -> cfg.bonusPool.manaShieldXpPerDamage = v, 0.0F, 1000.0F);
                logicalY += ROW_H;
                y = addIntRow("Regeneration boost amplifier", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.regenerationBoostAmplifier, v -> cfg.bonusPool.regenerationBoostAmplifier = v, 0, 10000);
                logicalY += ROW_H;
                y = addFloatRow("Damage reduction percent", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.damageReductionPercent, v -> cfg.bonusPool.damageReductionPercent = v, 0.0F, 1000.0F);
                logicalY += ROW_H;
                y = addFloatRow("Attack Speed boost percent", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.attackSpeedBoostPercent, v -> cfg.bonusPool.attackSpeedBoostPercent = v, 0.0F, 1000.0F);
                logicalY += ROW_H;
                y = addFloatRow("Reach Extend blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.reachExtendBlocks, v -> cfg.bonusPool.reachExtendBlocks = v, 0.0F, 1000.0F);
                logicalY += ROW_H;
                y = addFloatRow("Impact Absorb percent", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.impactAbsorbPercent, v -> cfg.bonusPool.impactAbsorbPercent = v, 0.0F, 1000.0F);
                logicalY += ROW_H;
                y = addIntRow("Impact Absorb max absorption", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.impactAbsorbMaxAbsorption, v -> cfg.bonusPool.impactAbsorbMaxAbsorption = v, 0, 10000);
                logicalY += ROW_H;
                y = addIntRow("Adrenaline Surge duration seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.adrenalineSurgeDurationSeconds, v -> cfg.bonusPool.adrenalineSurgeDurationSeconds = v, 0, 10000);
                logicalY += ROW_H;
                y = addIntRow("Adrenaline Surge cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.adrenalineSurgeCooldownSeconds, v -> cfg.bonusPool.adrenalineSurgeCooldownSeconds = v, 0, 10000);
                logicalY += ROW_H;
                y = addFloatRow("Intimidate damage reduction percent", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.intimidateDamageReductionPercent, v -> cfg.bonusPool.intimidateDamageReductionPercent = v, 0.0F, 1000.0F);
                logicalY += ROW_H;
                y = addIntRow("Intimidate radius blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.intimidateRadiusBlocks, v -> cfg.bonusPool.intimidateRadiusBlocks = v, 0, 10000);
                logicalY += ROW_H;
                y = addIntRow("Evasive Roll cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.evasiveRollCooldownSeconds, v -> cfg.bonusPool.evasiveRollCooldownSeconds = v, 0, 10000);
                logicalY += ROW_H;
                y = addIntRow("Evasive Roll distance blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.evasiveRollDistanceBlocks, v -> cfg.bonusPool.evasiveRollDistanceBlocks = v, 0, 10000);
                logicalY += ROW_H;
                y = addFloatRow("Combat Meditate heal per Second", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.combatMeditateHealPerSecond, v -> cfg.bonusPool.combatMeditateHealPerSecond = v, 0.0F, 1000.0F);
                logicalY += ROW_H;
                y = addIntRow("Combat Meditate Delay seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.combatMeditateDelaySeconds, v -> cfg.bonusPool.combatMeditateDelaySeconds = v, 0, 10000);
                logicalY += ROW_H;
                y = addFloatRow("Weapon Mastery Bonus damage", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.weaponMasteryBonusDamage, v -> cfg.bonusPool.weaponMasteryBonusDamage = v, 0.0F, 1000.0F);
                logicalY += ROW_H;
                y = addFloatRow("Culling Blade Threshold percent", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.cullingBladeThresholdPercent, v -> cfg.bonusPool.cullingBladeThresholdPercent = v, 0.0F, 1000.0F);
                logicalY += ROW_H;
                y = addFloatRow("Thick Skin Projectile reduction percent", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.thickSkinProjectileReductionPercent, v -> cfg.bonusPool.thickSkinProjectileReductionPercent = v, 0.0F, 1000.0F);
                logicalY += ROW_H;
                y = addFloatRow("Xp boost percent", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.xpBoostPercent, v -> cfg.bonusPool.xpBoostPercent = v, 0.0F, 1000.0F);
                logicalY += ROW_H;
                y = addFloatRow("Hunger Resist reduction percent", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.hungerResistReductionPercent, v -> cfg.bonusPool.hungerResistReductionPercent = v, 0.0F, 1000.0F);
                logicalY += ROW_H;
                y = addBoolRow("Poison Immunity Full", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.poisonImmunityFull, v -> cfg.bonusPool.poisonImmunityFull = v);
                logicalY += ROW_H;
                y = addIntRow("Second Wind cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.secondWindCooldownSeconds, v -> cfg.bonusPool.secondWindCooldownSeconds = v, 0, 10000);
                logicalY += ROW_H;
                y = addFloatRow("Echo Strike chance percent", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.echoStrikeChancePercent, v -> cfg.bonusPool.echoStrikeChancePercent = v, 0.0F, 1000.0F);
                logicalY += ROW_H;
                y = addFloatRow("Chain Breaker duration reduction percent", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.chainBreakerDurationReductionPercent, v -> cfg.bonusPool.chainBreakerDurationReductionPercent = v, 0.0F, 1000.0F);
                logicalY += ROW_H;
                y = addFloatRow("Stone Skin Flat reduction", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.stoneSkinFlatReduction, v -> cfg.bonusPool.stoneSkinFlatReduction = v, 0.0F, 1000.0F);
                logicalY += ROW_H;
                y = addIntRow("Arcane Barrier cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.arcaneBarrierCooldownSeconds, v -> cfg.bonusPool.arcaneBarrierCooldownSeconds = v, 0, 10000);
                logicalY += ROW_H;
                y = addFloatRow("Arcane Barrier Absorb Amount", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.arcaneBarrierAbsorbAmount, v -> cfg.bonusPool.arcaneBarrierAbsorbAmount = v, 0.0F, 1000.0F);
                logicalY += ROW_H;
                y = addFloatRow("Predator Sense Threshold percent", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.predatorSenseThresholdPercent, v -> cfg.bonusPool.predatorSenseThresholdPercent = v, 0.0F, 1000.0F);
                logicalY += ROW_H;
                y = addIntRow("Predator Sense range blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.predatorSenseRangeBlocks, v -> cfg.bonusPool.predatorSenseRangeBlocks = v, 0, 10000);
                logicalY += ROW_H;
                y = addFloatRow("Battle Medic heal per Second", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.battleMedicHealPerSecond, v -> cfg.bonusPool.battleMedicHealPerSecond = v, 0.0F, 1000.0F);
                logicalY += ROW_H;
                y = addIntRow("Battle Medic radius blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.battleMedicRadiusBlocks, v -> cfg.bonusPool.battleMedicRadiusBlocks = v, 0, 10000);
                logicalY += ROW_H;
                y = addFloatRow("Last Stand Threshold percent", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.lastStandThresholdPercent, v -> cfg.bonusPool.lastStandThresholdPercent = v, 0.0F, 1000.0F);
                logicalY += ROW_H;
                y = addFloatRow("Last Stand damage boost percent", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.lastStandDamageBoostPercent, v -> cfg.bonusPool.lastStandDamageBoostPercent = v, 0.0F, 1000.0F);
                logicalY += ROW_H;
                y = addFloatRow("Executioner Threshold percent", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.executionerThresholdPercent, v -> cfg.bonusPool.executionerThresholdPercent = v, 0.0F, 1000.0F);
                logicalY += ROW_H;
                y = addFloatRow("Executioner Bonus damage percent", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.executionerBonusDamagePercent, v -> cfg.bonusPool.executionerBonusDamagePercent = v, 0.0F, 1000.0F);
                logicalY += ROW_H;
                y = addFloatRow("Bloodthirst heal On Kill", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.bloodthirstHealOnKill, v -> cfg.bonusPool.bloodthirstHealOnKill = v, 0.0F, 1000.0F);
                logicalY += ROW_H;
                y = addBoolRow("Steel Resolve Full Knockback Immunity", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.steelResolveFullKnockbackImmunity, v -> cfg.bonusPool.steelResolveFullKnockbackImmunity = v);
                logicalY += ROW_H;
                y = addFloatRow("Elemental Harmony reduction percent", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.elementalHarmonyReductionPercent, v -> cfg.bonusPool.elementalHarmonyReductionPercent = v, 0.0F, 1000.0F);
                logicalY += ROW_H;
                y = addFloatRow("Treasure Hunter Drop boost percent", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.treasureHunterDropBoostPercent, v -> cfg.bonusPool.treasureHunterDropBoostPercent = v, 0.0F, 1000.0F);
                logicalY += ROW_H;
                y = addFloatRow("Counter Strike damage multiplier", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.counterStrikeDamageMultiplier, v -> cfg.bonusPool.counterStrikeDamageMultiplier = v, 0.0F, 1000.0F);
                logicalY += ROW_H;
                y = addIntRow("Counter Strike window seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.counterStrikeWindowSeconds, v -> cfg.bonusPool.counterStrikeWindowSeconds = v, 0, 10000);
                logicalY += ROW_H;
                y = addFloatRow("Bulwark Block Effectiveness boost percent", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.bulwarkBlockEffectivenessBoostPercent, v -> cfg.bonusPool.bulwarkBlockEffectivenessBoostPercent = v, 0.0F, 1000.0F);
                logicalY += ROW_H;
                y = addFloatRow("Quick Recovery Debuff reduction percent", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.quickRecoveryDebuffReductionPercent, v -> cfg.bonusPool.quickRecoveryDebuffReductionPercent = v, 0.0F, 1000.0F);
                logicalY += ROW_H;
                y = addIntRow("Overfowing Vitality Bonus hearts", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.overfowingVitalityBonusHearts, v -> cfg.bonusPool.overfowingVitalityBonusHearts = v, 0, 10000);
                logicalY += ROW_H;
                y = addFloatRow("Magnetic Pull range multiplier", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.magneticPullRangeMultiplier, v -> cfg.bonusPool.magneticPullRangeMultiplier = v, 0.0F, 1000.0F);
                logicalY += ROW_H;
                y = addFloatRow("Vengeance Buff duration seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.vengeanceBuffDurationSeconds, v -> cfg.bonusPool.vengeanceBuffDurationSeconds = v, 0.0F, 1000.0F);
                logicalY += ROW_H;
                y = addFloatRow("Vengeance damage boost percent", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.vengeanceDamageBoostPercent, v -> cfg.bonusPool.vengeanceDamageBoostPercent = v, 0.0F, 1000.0F);
                logicalY += ROW_H;
                y = addFloatRow("Nemesis Bonus damage percent", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.nemesisBonusDamagePercent, v -> cfg.bonusPool.nemesisBonusDamagePercent = v, 0.0F, 1000.0F);
                logicalY += ROW_H;
                y = addFloatRow("Hunters Instinct Crit boost percent", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.huntersInstinctCritBoostPercent, v -> cfg.bonusPool.huntersInstinctCritBoostPercent = v, 0.0F, 1000.0F);
                logicalY += ROW_H;
                y = addFloatRow("Berserker Blood max Attack Speed boost", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.berserkerBloodMaxAttackSpeedBoost, v -> cfg.bonusPool.berserkerBloodMaxAttackSpeedBoost = v, 0.0F, 1000.0F);
                logicalY += ROW_H;
                y = addFloatRow("Opportunist Backstab Bonus percent", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.opportunistBackstabBonusPercent, v -> cfg.bonusPool.opportunistBackstabBonusPercent = v, 0.0F, 1000.0F);
                logicalY += ROW_H;
                y = addFloatRow("Ironclad armor boost percent", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.ironcladArmorBoostPercent, v -> cfg.bonusPool.ironcladArmorBoostPercent = v, 0.0F, 1000.0F);
                logicalY += ROW_H;
                y = addFloatRow("Mist Form Phase chance percent", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.mistFormPhaseChancePercent, v -> cfg.bonusPool.mistFormPhaseChancePercent = v, 0.0F, 1000.0F);
                logicalY += ROW_H;
                y = addIntRow("War Cry radius blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.warCryRadiusBlocks, v -> cfg.bonusPool.warCryRadiusBlocks = v, 0, 10000);
                logicalY += ROW_H;
                y = addIntRow("War Cry strength duration seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.warCryStrengthDurationSeconds, v -> cfg.bonusPool.warCryStrengthDurationSeconds = v, 0, 10000);
                logicalY += ROW_H;
                y = addIntRow("Siphon Soul Regen duration seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.siphonSoulRegenDurationSeconds, v -> cfg.bonusPool.siphonSoulRegenDurationSeconds = v, 0, 10000);
                logicalY += ROW_H;
                y = addFloatRow("Unbreakable Durability reduction percent", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.unbreakableDurabilityReductionPercent, v -> cfg.bonusPool.unbreakableDurabilityReductionPercent = v, 0.0F, 1000.0F);
                logicalY += ROW_H;
                y = addFloatRow("Focused Mind cooldown reduction percent", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.focusedMindCooldownReductionPercent, v -> cfg.bonusPool.focusedMindCooldownReductionPercent = v, 0.0F, 1000.0F);
                logicalY += ROW_H;
                y = addIntRow("Sixth Sense warning range blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.bonusPool.sixthSenseWarningRangeBlocks, v -> cfg.bonusPool.sixthSenseWarningRangeBlocks = v, 0, 10000);
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
                y = spacer(y);
                logicalY += 8;
                // Passives
                y = addIntRow("Flux Capacitor charge threshold", y, labelX, labelW, fieldX, fieldW, () -> cfg.flux.fluxCapacitorChargeThreshold, v -> cfg.flux.fluxCapacitorChargeThreshold = v, 0, 200);
                logicalY += ROW_H;
                y = addIntRow("Flux Capacitor absorption amplifier", y, labelX, labelW, fieldX, fieldW, () -> cfg.flux.fluxCapacitorAbsorptionAmplifier, v -> cfg.flux.fluxCapacitorAbsorptionAmplifier = v, 0, 10);
                logicalY += ROW_H;
                y = addIntRow("Flux Conductivity charge per damage", y, labelX, labelW, fieldX, fieldW, () -> cfg.flux.fluxConductivityChargePerDamage, v -> cfg.flux.fluxConductivityChargePerDamage = v, 0, 50);
                logicalY += ROW_H;
                y = addIntRow("Flux Conductivity max charge per hit", y, labelX, labelW, fieldX, fieldW, () -> cfg.flux.fluxConductivityMaxChargePerHit, v -> cfg.flux.fluxConductivityMaxChargePerHit = v, 0, 100);
                logicalY += ROW_H;
                y = addIntRow("Flux Insulation charge threshold", y, labelX, labelW, fieldX, fieldW, () -> cfg.flux.fluxInsulationChargeThreshold, v -> cfg.flux.fluxInsulationChargeThreshold = v, 0, 200);
                logicalY += ROW_H;
                y = addFloatRow("Flux Insulation damage multiplier", y, labelX, labelW, fieldX, fieldW, () -> cfg.flux.fluxInsulationDamageMultiplier, v -> cfg.flux.fluxInsulationDamageMultiplier = v, 0.0F, 2.0F);
                logicalY += ROW_H;
                y = spacer(y);
                logicalY += 8;
                // Flux Surge
                y = addIntRow("Flux Surge cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.flux.fluxSurgeCooldownSeconds, v -> cfg.flux.fluxSurgeCooldownSeconds = v, 0, 3600);
                logicalY += ROW_H;
                y = addIntRow("Flux Surge duration seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.flux.fluxSurgeDurationSeconds, v -> cfg.flux.fluxSurgeDurationSeconds = v, 0, 120);
                logicalY += ROW_H;
                y = addIntRow("Flux Surge speed amplifier", y, labelX, labelW, fieldX, fieldW, () -> cfg.flux.fluxSurgeSpeedAmplifier, v -> cfg.flux.fluxSurgeSpeedAmplifier = v, 0, 10);
                logicalY += ROW_H;
                y = addIntRow("Flux Surge resistance amplifier", y, labelX, labelW, fieldX, fieldW, () -> cfg.flux.fluxSurgeResistanceAmplifier, v -> cfg.flux.fluxSurgeResistanceAmplifier = v, 0, 10);
                logicalY += ROW_H;
                y = addIntRow("Flux Surge charge cost", y, labelX, labelW, fieldX, fieldW, () -> cfg.flux.fluxSurgeChargeCost, v -> cfg.flux.fluxSurgeChargeCost = v, 0, 200);
                logicalY += ROW_H;
                y = addIntRow("Flux Surge radius blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.flux.fluxSurgeRadiusBlocks, v -> cfg.flux.fluxSurgeRadiusBlocks = v, 0, 32);
                logicalY += ROW_H;
                y = addDoubleRow("Flux Surge knockback", y, labelX, labelW, fieldX, fieldW, () -> cfg.flux.fluxSurgeKnockback, v -> cfg.flux.fluxSurgeKnockback = v, 0.0D, 5.0D);
                logicalY += ROW_H;
                y = spacer(y);
                logicalY += 8;
                // Flux Discharge
                y = addIntRow("Flux Discharge cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.flux.fluxDischargeCooldownSeconds, v -> cfg.flux.fluxDischargeCooldownSeconds = v, 0, 3600);
                logicalY += ROW_H;
                y = addIntRow("Flux Discharge radius blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.flux.fluxDischargeRadiusBlocks, v -> cfg.flux.fluxDischargeRadiusBlocks = v, 0, 64);
                logicalY += ROW_H;
                y = addFloatRow("Flux Discharge base damage", y, labelX, labelW, fieldX, fieldW, () -> cfg.flux.fluxDischargeBaseDamage, v -> cfg.flux.fluxDischargeBaseDamage = v, 0.0F, 40.0F);
                logicalY += ROW_H;
                y = addFloatRow("Flux Discharge damage per charge", y, labelX, labelW, fieldX, fieldW, () -> cfg.flux.fluxDischargeDamagePerCharge, v -> cfg.flux.fluxDischargeDamagePerCharge = v, 0.0F, 2.0F);
                logicalY += ROW_H;
                y = addFloatRow("Flux Discharge max damage", y, labelX, labelW, fieldX, fieldW, () -> cfg.flux.fluxDischargeMaxDamage, v -> cfg.flux.fluxDischargeMaxDamage = v, 0.0F, 80.0F);
                logicalY += ROW_H;
                y = addIntRow("Flux Discharge min charge", y, labelX, labelW, fieldX, fieldW, () -> cfg.flux.fluxDischargeMinCharge, v -> cfg.flux.fluxDischargeMinCharge = v, 0, 200);
                logicalY += ROW_H;
                y = addDoubleRow("Flux Discharge knockback", y, labelX, labelW, fieldX, fieldW, () -> cfg.flux.fluxDischargeKnockback, v -> cfg.flux.fluxDischargeKnockback = v, 0.0D, 5.0D);
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
                y = spacer(y);
                logicalY += 8;
                y = addIntRow("Gust cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.puff.gustCooldownSeconds, v -> cfg.puff.gustCooldownSeconds = v, 0, 3600);
                logicalY += ROW_H;
                y = addIntRow("Gust radius blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.puff.gustRadiusBlocks, v -> cfg.puff.gustRadiusBlocks = v, 0, 64);
                logicalY += ROW_H;
                y = addDoubleRow("Gust up velocity Y", y, labelX, labelW, fieldX, fieldW, () -> cfg.puff.gustUpVelocityY, v -> cfg.puff.gustUpVelocityY = v, 0.0D, 5.0D);
                logicalY += ROW_H;
                y = addDoubleRow("Gust knockback", y, labelX, labelW, fieldX, fieldW, () -> cfg.puff.gustKnockback, v -> cfg.puff.gustKnockback = v, 0.0D, 5.0D);
                logicalY += ROW_H;
                y = addIntRow("Gust slowness duration seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.puff.gustSlownessDurationSeconds, v -> cfg.puff.gustSlownessDurationSeconds = v, 0, 60);
                logicalY += ROW_H;
                y = addIntRow("Gust slowness amplifier", y, labelX, labelW, fieldX, fieldW, () -> cfg.puff.gustSlownessAmplifier, v -> cfg.puff.gustSlownessAmplifier = v, 0, 10);
                logicalY += ROW_H;
                y = addIntRow("Gust slow falling duration seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.puff.gustSlowFallingDurationSeconds, v -> cfg.puff.gustSlowFallingDurationSeconds = v, 0, 60);
                logicalY += ROW_H;
                y = spacer(y);
                logicalY += 8;
                y = addIntRow("Windborne duration seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.puff.windborneDurationSeconds, v -> cfg.puff.windborneDurationSeconds = v, 0, 60);
                logicalY += ROW_H;
                y = addIntRow("Windborne slow falling amplifier", y, labelX, labelW, fieldX, fieldW, () -> cfg.puff.windborneSlowFallingAmplifier, v -> cfg.puff.windborneSlowFallingAmplifier = v, 0, 10);
                logicalY += ROW_H;
            }
            case SPEED -> {
                // Passives
                y = addDoubleRow("Momentum min speed", y, labelX, labelW, fieldX, fieldW, () -> cfg.speed.momentumMinSpeed, v -> cfg.speed.momentumMinSpeed = v, 0.0D, 1.0D);
                logicalY += ROW_H;
                y = addDoubleRow("Momentum max speed", y, labelX, labelW, fieldX, fieldW, () -> cfg.speed.momentumMaxSpeed, v -> cfg.speed.momentumMaxSpeed = v, 0.0D, 2.0D);
                logicalY += ROW_H;
                y = addFloatRow("Momentum min multiplier", y, labelX, labelW, fieldX, fieldW, () -> cfg.speed.momentumMinMultiplier, v -> cfg.speed.momentumMinMultiplier = v, 0.0F, 2.0F);
                logicalY += ROW_H;
                y = addFloatRow("Momentum max multiplier", y, labelX, labelW, fieldX, fieldW, () -> cfg.speed.momentumMaxMultiplier, v -> cfg.speed.momentumMaxMultiplier = v, 0.0F, 3.0F);
                logicalY += ROW_H;
                y = addIntRow("Frictionless speed amplifier", y, labelX, labelW, fieldX, fieldW, () -> cfg.speed.frictionlessSpeedAmplifier, v -> cfg.speed.frictionlessSpeedAmplifier = v, 0, 10);
                logicalY += ROW_H;
                y = addIntRow("Auto-step cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.speed.autoStepCooldownSeconds, v -> cfg.speed.autoStepCooldownSeconds = v, 0, 120);
                logicalY += ROW_H;
                y = addDoubleRow("Auto-step height bonus", y, labelX, labelW, fieldX, fieldW, () -> cfg.speed.autoStepHeightBonus, v -> cfg.speed.autoStepHeightBonus = v, 0.0D, 2.0D);
                logicalY += ROW_H;
                y = spacer(y);
                logicalY += 8;
                // Abilities
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
                y = spacer(y);
                logicalY += 8;
                y = addIntRow("Slipstream cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.speed.slipstreamCooldownSeconds, v -> cfg.speed.slipstreamCooldownSeconds = v, 0, 3600);
                logicalY += ROW_H;
                y = addIntRow("Slipstream duration seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.speed.slipstreamDurationSeconds, v -> cfg.speed.slipstreamDurationSeconds = v, 0, 120);
                logicalY += ROW_H;
                y = addIntRow("Slipstream length blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.speed.slipstreamLengthBlocks, v -> cfg.speed.slipstreamLengthBlocks = v, 0, 64);
                logicalY += ROW_H;
                y = addIntRow("Slipstream radius blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.speed.slipstreamRadiusBlocks, v -> cfg.speed.slipstreamRadiusBlocks = v, 0, 16);
                logicalY += ROW_H;
                y = addIntRow("Slipstream ally speed amplifier", y, labelX, labelW, fieldX, fieldW, () -> cfg.speed.slipstreamAllySpeedAmplifier, v -> cfg.speed.slipstreamAllySpeedAmplifier = v, 0, 10);
                logicalY += ROW_H;
                y = addIntRow("Slipstream enemy slowness amplifier", y, labelX, labelW, fieldX, fieldW, () -> cfg.speed.slipstreamEnemySlownessAmplifier, v -> cfg.speed.slipstreamEnemySlownessAmplifier = v, 0, 10);
                logicalY += ROW_H;
                y = addDoubleRow("Slipstream enemy knockback", y, labelX, labelW, fieldX, fieldW, () -> cfg.speed.slipstreamEnemyKnockback, v -> cfg.speed.slipstreamEnemyKnockback = v, 0.0D, 3.0D);
                logicalY += ROW_H;
                y = spacer(y);
                logicalY += 8;
                y = addIntRow("Afterimage cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.speed.afterimageCooldownSeconds, v -> cfg.speed.afterimageCooldownSeconds = v, 0, 3600);
                logicalY += ROW_H;
                y = addIntRow("Afterimage duration seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.speed.afterimageDurationSeconds, v -> cfg.speed.afterimageDurationSeconds = v, 0, 120);
                logicalY += ROW_H;
                y = addIntRow("Afterimage speed amplifier", y, labelX, labelW, fieldX, fieldW, () -> cfg.speed.afterimageSpeedAmplifier, v -> cfg.speed.afterimageSpeedAmplifier = v, 0, 10);
                logicalY += ROW_H;
                y = spacer(y);
                logicalY += 8;
                y = addIntRow("Tempo Shift cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.speed.tempoShiftCooldownSeconds, v -> cfg.speed.tempoShiftCooldownSeconds = v, 0, 3600);
                logicalY += ROW_H;
                y = addIntRow("Tempo Shift duration seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.speed.tempoShiftDurationSeconds, v -> cfg.speed.tempoShiftDurationSeconds = v, 0, 120);
                logicalY += ROW_H;
                y = addIntRow("Tempo Shift radius blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.speed.tempoShiftRadiusBlocks, v -> cfg.speed.tempoShiftRadiusBlocks = v, 0, 64);
                logicalY += ROW_H;
                y = addIntRow("Tempo Shift ally cooldown ticks/sec", y, labelX, labelW, fieldX, fieldW, () -> cfg.speed.tempoShiftAllyCooldownTicksPerSecond, v -> cfg.speed.tempoShiftAllyCooldownTicksPerSecond = v, 0, 40);
                logicalY += ROW_H;
                y = addIntRow("Tempo Shift enemy cooldown ticks/sec", y, labelX, labelW, fieldX, fieldW, () -> cfg.speed.tempoShiftEnemyCooldownTicksPerSecond, v -> cfg.speed.tempoShiftEnemyCooldownTicksPerSecond = v, 0, 40);
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
                y = spacer(y);
                logicalY += 8;
                // Passives
                y = addFloatRow("Adrenaline threshold hearts", y, labelX, labelW, fieldX, fieldW, () -> cfg.strength.adrenalineThresholdHearts, v -> cfg.strength.adrenalineThresholdHearts = v, 0.0F, 20.0F);
                logicalY += ROW_H;
                y = addIntRow("Adrenaline duration seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.strength.adrenalineDurationSeconds, v -> cfg.strength.adrenalineDurationSeconds = v, 0, 60);
                logicalY += ROW_H;
                y = addIntRow("Adrenaline resistance amplifier", y, labelX, labelW, fieldX, fieldW, () -> cfg.strength.adrenalineResistanceAmplifier, v -> cfg.strength.adrenalineResistanceAmplifier = v, 0, 10);
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
                y = addIntRow("Amplification bonus levels", y, labelX, labelW, fieldX, fieldW, () -> cfg.wealth.amplificationBonusLevels, v -> cfg.wealth.amplificationBonusLevels = v, 1, 5);
                logicalY += ROW_H;
                y = spacer(y);
                logicalY += 8;
                y = addIntRow("Rich Rush cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.wealth.richRushCooldownSeconds, v -> cfg.wealth.richRushCooldownSeconds = v, 0, 24 * 3600);
                logicalY += ROW_H;
                y = addIntRow("Rich Rush duration seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.wealth.richRushDurationSeconds, v -> cfg.wealth.richRushDurationSeconds = v, 0, 24 * 3600);
                logicalY += ROW_H;
                y = addIntRow("Rich Rush loot rolls", y, labelX, labelW, fieldX, fieldW, () -> cfg.wealth.richRushLootRolls, v -> cfg.wealth.richRushLootRolls = v, 1, 20);
                logicalY += ROW_H;
                y = addIntRow("Rich Rush Wither Skull guarantee", y, labelX, labelW, fieldX, fieldW, () -> cfg.wealth.richRushWitherSkullGuarantee, v -> cfg.wealth.richRushWitherSkullGuarantee = v, 0, 64);
                logicalY += ROW_H;
                y = addIntRow("Rich Rush Shulker Shell guarantee", y, labelX, labelW, fieldX, fieldW, () -> cfg.wealth.richRushShulkerShellGuarantee, v -> cfg.wealth.richRushShulkerShellGuarantee = v, 0, 64);
                logicalY += ROW_H;
                y = addIntRow("Rich Rush Nether Star guarantee", y, labelX, labelW, fieldX, fieldW, () -> cfg.wealth.richRushNetherStarGuarantee, v -> cfg.wealth.richRushNetherStarGuarantee = v, 0, 64);
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
                y = addStringRow("Summon costs (id=points, comma-separated)", y, labelX, labelW, fieldX, fieldW,
                        () -> joinIntMap(cfg.summoner.costs),
                        v -> cfg.summoner.costs = parseIntMap(v));
                logicalY += ROW_H;
                y = addStringRow("Slot 1 loadout (id=count, comma-separated)", y, labelX, labelW, fieldX, fieldW,
                        () -> joinSummonSpecs(cfg.summoner.slot1),
                        v -> cfg.summoner.slot1 = parseSummonSpecs(v));
                logicalY += ROW_H;
                y = addStringRow("Slot 2 loadout (id=count, comma-separated)", y, labelX, labelW, fieldX, fieldW,
                        () -> joinSummonSpecs(cfg.summoner.slot2),
                        v -> cfg.summoner.slot2 = parseSummonSpecs(v));
                logicalY += ROW_H;
                y = addStringRow("Slot 3 loadout (id=count, comma-separated)", y, labelX, labelW, fieldX, fieldW,
                        () -> joinSummonSpecs(cfg.summoner.slot3),
                        v -> cfg.summoner.slot3 = parseSummonSpecs(v));
                logicalY += ROW_H;
                y = addStringRow("Slot 4 loadout (id=count, comma-separated)", y, labelX, labelW, fieldX, fieldW,
                        () -> joinSummonSpecs(cfg.summoner.slot4),
                        v -> cfg.summoner.slot4 = parseSummonSpecs(v));
                logicalY += ROW_H;
                y = addStringRow("Slot 5 loadout (id=count, comma-separated)", y, labelX, labelW, fieldX, fieldW,
                        () -> joinSummonSpecs(cfg.summoner.slot5),
                        v -> cfg.summoner.slot5 = parseSummonSpecs(v));
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
                y = addIntRow("Orbital Laser mining cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.space.orbitalLaserMiningCooldownSeconds, v -> cfg.space.orbitalLaserMiningCooldownSeconds = v, 0, 3600);
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
            case SPY -> {
                y = addIntRow("Stillness seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.spy.stillnessSeconds, v -> cfg.spy.stillnessSeconds = v, 0, 60);
                logicalY += ROW_H;
                y = addFloatRow("Stillness move epsilon blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.spy.stillnessMoveEpsilonBlocks, v -> cfg.spy.stillnessMoveEpsilonBlocks = v, 0.0F, 1.0F);
                logicalY += ROW_H;
                y = addIntRow("Stillness invis refresh seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.spy.stillnessInvisRefreshSeconds, v -> cfg.spy.stillnessInvisRefreshSeconds = v, 0, 60);
                logicalY += ROW_H;
                y = addIntRow("Backstep cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.spy.backstepCooldownSeconds, v -> cfg.spy.backstepCooldownSeconds = v, 0, 3600);
                logicalY += ROW_H;
                y = addDoubleRow("Backstep velocity", y, labelX, labelW, fieldX, fieldW, () -> cfg.spy.backstepVelocity, v -> cfg.spy.backstepVelocity = v, 0.0D, 5.0D);
                logicalY += ROW_H;
                y = addDoubleRow("Backstep up velocity", y, labelX, labelW, fieldX, fieldW, () -> cfg.spy.backstepUpVelocity, v -> cfg.spy.backstepUpVelocity = v, 0.0D, 2.0D);
                logicalY += ROW_H;
                y = addFloatRow("Backstab bonus damage", y, labelX, labelW, fieldX, fieldW, () -> cfg.spy.backstabBonusDamage, v -> cfg.spy.backstabBonusDamage = v, 0.0F, 40.0F);
                logicalY += ROW_H;
                y = addIntRow("Backstab angle degrees", y, labelX, labelW, fieldX, fieldW, () -> cfg.spy.backstabAngleDegrees, v -> cfg.spy.backstabAngleDegrees = v, 0, 180);
                logicalY += ROW_H;
                y = spacer(y);
                logicalY += 8;
                y = addIntRow("Observe range blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.spy.observeRangeBlocks, v -> cfg.spy.observeRangeBlocks = v, 0, 128);
                logicalY += ROW_H;
                y = addIntRow("Observe window seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.spy.observeWindowSeconds, v -> cfg.spy.observeWindowSeconds = v, 0, 3600);
                logicalY += ROW_H;
                y = addIntRow("Steal required witness count", y, labelX, labelW, fieldX, fieldW, () -> cfg.spy.stealRequiredWitnessCount, v -> cfg.spy.stealRequiredWitnessCount = v, 1, 20);
                logicalY += ROW_H;
                y = addIntRow("Max stolen abilities", y, labelX, labelW, fieldX, fieldW, () -> cfg.spy.maxStolenAbilities, v -> cfg.spy.maxStolenAbilities = v, 1, 10);
                logicalY += ROW_H;
                y = spacer(y);
                logicalY += 8;
                y = addIntRow("Mimic Form cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.spy.mimicFormCooldownSeconds, v -> cfg.spy.mimicFormCooldownSeconds = v, 0, 3600);
                logicalY += ROW_H;
                y = addIntRow("Mimic Form duration seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.spy.mimicFormDurationSeconds, v -> cfg.spy.mimicFormDurationSeconds = v, 0, 120);
                logicalY += ROW_H;
                y = addFloatRow("Mimic Form bonus max health", y, labelX, labelW, fieldX, fieldW, () -> cfg.spy.mimicFormBonusMaxHealth, v -> cfg.spy.mimicFormBonusMaxHealth = v, 0.0F, 40.0F);
                logicalY += ROW_H;
                y = addFloatRow("Mimic Form speed multiplier", y, labelX, labelW, fieldX, fieldW, () -> cfg.spy.mimicFormSpeedMultiplier, v -> cfg.spy.mimicFormSpeedMultiplier = v, 0.1F, 3.0F);
                logicalY += ROW_H;
                y = spacer(y);
                logicalY += 8;
                y = addIntRow("Echo cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.spy.echoCooldownSeconds, v -> cfg.spy.echoCooldownSeconds = v, 0, 3600);
                logicalY += ROW_H;
                y = addIntRow("Echo window seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.spy.echoWindowSeconds, v -> cfg.spy.echoWindowSeconds = v, 0, 60);
                logicalY += ROW_H;
                y = addIntRow("Steal cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.spy.stealCooldownSeconds, v -> cfg.spy.stealCooldownSeconds = v, 0, 3600);
                logicalY += ROW_H;
                y = spacer(y);
                logicalY += 8;
                y = addIntRow("Smoke Bomb cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.spy.smokeBombCooldownSeconds, v -> cfg.spy.smokeBombCooldownSeconds = v, 0, 3600);
                logicalY += ROW_H;
                y = addIntRow("Smoke Bomb radius blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.spy.smokeBombRadiusBlocks, v -> cfg.spy.smokeBombRadiusBlocks = v, 0, 64);
                logicalY += ROW_H;
                y = addIntRow("Smoke Bomb duration seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.spy.smokeBombDurationSeconds, v -> cfg.spy.smokeBombDurationSeconds = v, 0, 60);
                logicalY += ROW_H;
                y = addIntRow("Smoke Bomb blindness amplifier", y, labelX, labelW, fieldX, fieldW, () -> cfg.spy.smokeBombBlindnessAmplifier, v -> cfg.spy.smokeBombBlindnessAmplifier = v, 0, 10);
                logicalY += ROW_H;
                y = addIntRow("Smoke Bomb slowness amplifier", y, labelX, labelW, fieldX, fieldW, () -> cfg.spy.smokeBombSlownessAmplifier, v -> cfg.spy.smokeBombSlownessAmplifier = v, 0, 10);
                logicalY += ROW_H;
                y = addIntRow("Stolen Cast cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.spy.stolenCastCooldownSeconds, v -> cfg.spy.stolenCastCooldownSeconds = v, 0, 3600);
                logicalY += ROW_H;
                y = spacer(y);
                logicalY += 8;
                y = addIntRow("Skinshift cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.spy.skinshiftCooldownSeconds, v -> cfg.spy.skinshiftCooldownSeconds = v, 0, 3600);
                logicalY += ROW_H;
                y = addIntRow("Skinshift duration seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.spy.skinshiftDurationSeconds, v -> cfg.spy.skinshiftDurationSeconds = v, 0, 600);
                logicalY += ROW_H;
                y = addIntRow("Skinshift range blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.spy.skinshiftRangeBlocks, v -> cfg.spy.skinshiftRangeBlocks = v, 0, 128);
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
            case VOID -> {
                y = addBoolRow("Block all status effects", y, labelX, labelW, fieldX, fieldW, () -> cfg.voidGem.blockAllStatusEffects, v -> cfg.voidGem.blockAllStatusEffects = v);
                logicalY += ROW_H;
                y = addNoteRow("Note: This also blocks vanilla effects (potions/beacons).", y, labelX, labelW);
                logicalY += ROW_H;
            }
            case CHAOS -> {
                y = addIntRow("Rotation seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.chaos.rotationSeconds, v -> cfg.chaos.rotationSeconds = v, 1, 3600);
                logicalY += ROW_H;
                y = addIntRow("Rotation ability cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.chaos.rotationAbilityCooldownSeconds, v -> cfg.chaos.rotationAbilityCooldownSeconds = v, 0, 3600);
                logicalY += ROW_H;
                y = spacer(y);
                logicalY += 8;
                y = addIntRow("Slot count", y, labelX, labelW, fieldX, fieldW, () -> cfg.chaos.slotCount, v -> cfg.chaos.slotCount = v, 1, 9);
                logicalY += ROW_H;
                y = addIntRow("Slot duration seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.chaos.slotDurationSeconds, v -> cfg.chaos.slotDurationSeconds = v, 1, 3600);
                logicalY += ROW_H;
                y = addIntRow("Slot ability cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.chaos.slotAbilityCooldownSeconds, v -> cfg.chaos.slotAbilityCooldownSeconds = v, 0, 3600);
                logicalY += ROW_H;
            }
            case PRISM -> {
                y = addIntRow("Max selected gem abilities", y, labelX, labelW, fieldX, fieldW, () -> cfg.prism.maxGemAbilities, v -> cfg.prism.maxGemAbilities = v, 0, 10);
                logicalY += ROW_H;
                y = addIntRow("Max selected gem passives", y, labelX, labelW, fieldX, fieldW, () -> cfg.prism.maxGemPassives, v -> cfg.prism.maxGemPassives = v, 0, 10);
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
            case DUELIST -> {
                // Passives
                y = addFloatRow("Riposte bonus damage multiplier", y, labelX, labelW, fieldX, fieldW, () -> cfg.duelist.riposteBonusDamageMultiplier, v -> cfg.duelist.riposteBonusDamageMultiplier = v, 1.0F, 5.0F);
                logicalY += ROW_H;
                y = addIntRow("Riposte window seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.duelist.riposteWindowSeconds, v -> cfg.duelist.riposteWindowSeconds = v, 0, 30);
                logicalY += ROW_H;
                y = addFloatRow("Focus bonus damage multiplier", y, labelX, labelW, fieldX, fieldW, () -> cfg.duelist.focusBonusDamageMultiplier, v -> cfg.duelist.focusBonusDamageMultiplier = v, 1.0F, 5.0F);
                logicalY += ROW_H;
                y = addIntRow("Focus radius blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.duelist.focusRadiusBlocks, v -> cfg.duelist.focusRadiusBlocks = v, 0, 64);
                logicalY += ROW_H;
                y = addFloatRow("Combat Stance speed multiplier", y, labelX, labelW, fieldX, fieldW, () -> cfg.duelist.combatStanceSpeedMultiplier, v -> cfg.duelist.combatStanceSpeedMultiplier = v, 1.0F, 3.0F);
                logicalY += ROW_H;
                y = spacer(y);
                logicalY += 8;
                // Abilities
                y = addIntRow("Lunge cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.duelist.lungeCooldownSeconds, v -> cfg.duelist.lungeCooldownSeconds = v, 0, 3600);
                logicalY += ROW_H;
                y = addIntRow("Lunge distance blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.duelist.lungeDistanceBlocks, v -> cfg.duelist.lungeDistanceBlocks = v, 0, 64);
                logicalY += ROW_H;
                y = addFloatRow("Lunge damage", y, labelX, labelW, fieldX, fieldW, () -> cfg.duelist.lungeDamage, v -> cfg.duelist.lungeDamage = v, 0.0F, 40.0F);
                logicalY += ROW_H;
                y = spacer(y);
                logicalY += 8;
                y = addIntRow("Parry cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.duelist.parryCooldownSeconds, v -> cfg.duelist.parryCooldownSeconds = v, 0, 3600);
                logicalY += ROW_H;
                y = addIntRow("Parry window ticks", y, labelX, labelW, fieldX, fieldW, () -> cfg.duelist.parryWindowTicks, v -> cfg.duelist.parryWindowTicks = v, 0, 100);
                logicalY += ROW_H;
                y = addIntRow("Parry stun seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.duelist.parryStunSeconds, v -> cfg.duelist.parryStunSeconds = v, 0, 30);
                logicalY += ROW_H;
                y = spacer(y);
                logicalY += 8;
                y = addIntRow("Rapid Strike cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.duelist.rapidStrikeCooldownSeconds, v -> cfg.duelist.rapidStrikeCooldownSeconds = v, 0, 3600);
                logicalY += ROW_H;
                y = addIntRow("Rapid Strike duration seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.duelist.rapidStrikeDurationSeconds, v -> cfg.duelist.rapidStrikeDurationSeconds = v, 0, 60);
                logicalY += ROW_H;
                y = spacer(y);
                logicalY += 8;
                y = addIntRow("Flourish cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.duelist.flourishCooldownSeconds, v -> cfg.duelist.flourishCooldownSeconds = v, 0, 3600);
                logicalY += ROW_H;
                y = addIntRow("Flourish radius blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.duelist.flourishRadiusBlocks, v -> cfg.duelist.flourishRadiusBlocks = v, 0, 32);
                logicalY += ROW_H;
                y = addFloatRow("Flourish damage", y, labelX, labelW, fieldX, fieldW, () -> cfg.duelist.flourishDamage, v -> cfg.duelist.flourishDamage = v, 0.0F, 40.0F);
                logicalY += ROW_H;
                y = spacer(y);
                logicalY += 8;
                y = addIntRow("Mirror Match cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.duelist.mirrorMatchCooldownSeconds, v -> cfg.duelist.mirrorMatchCooldownSeconds = v, 0, 3600);
                logicalY += ROW_H;
                y = addIntRow("Mirror Match duration seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.duelist.mirrorMatchDurationSeconds, v -> cfg.duelist.mirrorMatchDurationSeconds = v, 0, 120);
                logicalY += ROW_H;
                y = addIntRow("Mirror Match range blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.duelist.mirrorMatchRangeBlocks, v -> cfg.duelist.mirrorMatchRangeBlocks = v, 0, 64);
                logicalY += ROW_H;
                y = spacer(y);
                logicalY += 8;
                y = addIntRow("Blade Dance cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.duelist.bladeDanceCooldownSeconds, v -> cfg.duelist.bladeDanceCooldownSeconds = v, 0, 3600);
                logicalY += ROW_H;
                y = addIntRow("Blade Dance duration seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.duelist.bladeDanceDurationSeconds, v -> cfg.duelist.bladeDanceDurationSeconds = v, 0, 60);
                logicalY += ROW_H;
                y = addFloatRow("Blade Dance starting multiplier", y, labelX, labelW, fieldX, fieldW, () -> cfg.duelist.bladeDanceStartingMultiplier, v -> cfg.duelist.bladeDanceStartingMultiplier = v, 0.5F, 3.0F);
                logicalY += ROW_H;
                y = addFloatRow("Blade Dance increase per hit", y, labelX, labelW, fieldX, fieldW, () -> cfg.duelist.bladeDanceIncreasePerHit, v -> cfg.duelist.bladeDanceIncreasePerHit = v, 0.0F, 1.0F);
                logicalY += ROW_H;
                y = addFloatRow("Blade Dance max multiplier", y, labelX, labelW, fieldX, fieldW, () -> cfg.duelist.bladeDanceMaxMultiplier, v -> cfg.duelist.bladeDanceMaxMultiplier = v, 1.0F, 10.0F);
                logicalY += ROW_H;
                y = addIntRow("Blade Dance reset seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.duelist.bladeDanceResetSeconds, v -> cfg.duelist.bladeDanceResetSeconds = v, 0, 60);
                logicalY += ROW_H;
            }
            case HUNTER -> {
                // Passives
                y = addFloatRow("Prey Mark bonus damage multiplier", y, labelX, labelW, fieldX, fieldW, () -> cfg.hunter.preyMarkBonusDamageMultiplier, v -> cfg.hunter.preyMarkBonusDamageMultiplier = v, 1.0F, 5.0F);
                logicalY += ROW_H;
                y = addIntRow("Prey Mark duration seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.hunter.preyMarkDurationSeconds, v -> cfg.hunter.preyMarkDurationSeconds = v, 0, 600);
                logicalY += ROW_H;
                y = addIntRow("Tracker's Eye range blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.hunter.trackersEyeRangeBlocks, v -> cfg.hunter.trackersEyeRangeBlocks = v, 0, 128);
                logicalY += ROW_H;
                y = addIntRow("Trophy Hunter duration seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.hunter.trophyHunterDurationSeconds, v -> cfg.hunter.trophyHunterDurationSeconds = v, 0, 600);
                logicalY += ROW_H;
                y = spacer(y);
                logicalY += 8;
                // Abilities
                y = addIntRow("Hunting Trap cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.hunter.huntingTrapCooldownSeconds, v -> cfg.hunter.huntingTrapCooldownSeconds = v, 0, 3600);
                logicalY += ROW_H;
                y = addIntRow("Hunting Trap root seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.hunter.huntingTrapRootSeconds, v -> cfg.hunter.huntingTrapRootSeconds = v, 0, 60);
                logicalY += ROW_H;
                y = addFloatRow("Hunting Trap damage", y, labelX, labelW, fieldX, fieldW, () -> cfg.hunter.huntingTrapDamage, v -> cfg.hunter.huntingTrapDamage = v, 0.0F, 200.0F);
                logicalY += ROW_H;
                y = addIntRow("Hunting Trap lifetime seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.hunter.huntingTrapLifetimeSeconds, v -> cfg.hunter.huntingTrapLifetimeSeconds = v, 0, 600);
                logicalY += ROW_H;
                y = spacer(y);
                logicalY += 8;
                y = addIntRow("Pounce cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.hunter.pounceCooldownSeconds, v -> cfg.hunter.pounceCooldownSeconds = v, 0, 3600);
                logicalY += ROW_H;
                y = addIntRow("Pounce range blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.hunter.pounceRangeBlocks, v -> cfg.hunter.pounceRangeBlocks = v, 0, 64);
                logicalY += ROW_H;
                y = addFloatRow("Pounce damage", y, labelX, labelW, fieldX, fieldW, () -> cfg.hunter.pounceDamage, v -> cfg.hunter.pounceDamage = v, 0.0F, 200.0F);
                logicalY += ROW_H;
                y = spacer(y);
                logicalY += 8;
                y = addIntRow("Net Shot cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.hunter.netShotCooldownSeconds, v -> cfg.hunter.netShotCooldownSeconds = v, 0, 3600);
                logicalY += ROW_H;
                y = addIntRow("Net Shot slow seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.hunter.netShotSlowSeconds, v -> cfg.hunter.netShotSlowSeconds = v, 0, 60);
                logicalY += ROW_H;
                y = addIntRow("Net Shot range blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.hunter.netShotRangeBlocks, v -> cfg.hunter.netShotRangeBlocks = v, 0, 64);
                logicalY += ROW_H;
                y = spacer(y);
                logicalY += 8;
                y = addIntRow("Crippling cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.hunter.cripplingCooldownSeconds, v -> cfg.hunter.cripplingCooldownSeconds = v, 0, 3600);
                logicalY += ROW_H;
                y = addFloatRow("Crippling slow multiplier", y, labelX, labelW, fieldX, fieldW, () -> cfg.hunter.cripplingSlowMultiplier, v -> cfg.hunter.cripplingSlowMultiplier = v, 0.0F, 1.0F);
                logicalY += ROW_H;
                y = addIntRow("Crippling duration seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.hunter.cripplingDurationSeconds, v -> cfg.hunter.cripplingDurationSeconds = v, 0, 60);
                logicalY += ROW_H;
                y = addIntRow("Crippling range blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.hunter.cripplingRangeBlocks, v -> cfg.hunter.cripplingRangeBlocks = v, 0, 64);
                logicalY += ROW_H;
                y = spacer(y);
                logicalY += 8;
                y = addIntRow("Pack Tactics cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.hunter.packTacticsCooldownSeconds, v -> cfg.hunter.packTacticsCooldownSeconds = v, 0, 3600);
                logicalY += ROW_H;
                y = addFloatRow("Pack Tactics bonus damage multiplier", y, labelX, labelW, fieldX, fieldW, () -> cfg.hunter.packTacticsBonusDamageMultiplier, v -> cfg.hunter.packTacticsBonusDamageMultiplier = v, 1.0F, 5.0F);
                logicalY += ROW_H;
                y = addIntRow("Pack Tactics duration seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.hunter.packTacticsDurationSeconds, v -> cfg.hunter.packTacticsDurationSeconds = v, 0, 120);
                logicalY += ROW_H;
                y = addIntRow("Pack Tactics radius blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.hunter.packTacticsRadiusBlocks, v -> cfg.hunter.packTacticsRadiusBlocks = v, 0, 64);
                logicalY += ROW_H;
                y = spacer(y);
                logicalY += 8;
                y = addIntRow("Six-Pack Pain cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.hunter.sixPackPainCooldownSeconds, v -> cfg.hunter.sixPackPainCooldownSeconds = v, 0, 3600);
                logicalY += ROW_H;
                y = addIntRow("Six-Pack Pain clone count", y, labelX, labelW, fieldX, fieldW, () -> cfg.hunter.sixPackPainCloneCount, v -> cfg.hunter.sixPackPainCloneCount = v, 1, 10);
                logicalY += ROW_H;
                y = addIntRow("Six-Pack Pain duration seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.hunter.sixPackPainDurationSeconds, v -> cfg.hunter.sixPackPainDurationSeconds = v, 0, 600);
                logicalY += ROW_H;
                y = addFloatRow("Six-Pack Pain health per clone", y, labelX, labelW, fieldX, fieldW, () -> cfg.hunter.sixPackPainHealthPerClone, v -> cfg.hunter.sixPackPainHealthPerClone = v, 1.0F, 100.0F);
                logicalY += ROW_H;
                y = addIntRow("Six-Pack Pain close target range", y, labelX, labelW, fieldX, fieldW, () -> cfg.hunter.sixPackPainCloseTargetRangeBlocks, v -> cfg.hunter.sixPackPainCloseTargetRangeBlocks = v, 1, 64);
                logicalY += ROW_H;
                y = addIntRow("Six-Pack Pain wide target range", y, labelX, labelW, fieldX, fieldW, () -> cfg.hunter.sixPackPainWideTargetRangeBlocks, v -> cfg.hunter.sixPackPainWideTargetRangeBlocks = v, 1, 128);
                logicalY += ROW_H;
                y = addIntRow("Six-Pack Pain buff duration ticks", y, labelX, labelW, fieldX, fieldW, () -> cfg.hunter.sixPackPainBuffDurationTicks, v -> cfg.hunter.sixPackPainBuffDurationTicks = v, 0, 6000);
                logicalY += ROW_H;
                y = addIntRow("Six-Pack Pain debuff duration ticks", y, labelX, labelW, fieldX, fieldW, () -> cfg.hunter.sixPackPainDebuffDurationTicks, v -> cfg.hunter.sixPackPainDebuffDurationTicks = v, 0, 6000);
                logicalY += ROW_H;
                y = spacer(y);
                logicalY += 8;
                // Origin Tracking
                y = addIntRow("Origin Tracking cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.hunter.originTrackingCooldownSeconds, v -> cfg.hunter.originTrackingCooldownSeconds = v, 0, 3600);
                logicalY += ROW_H;
                y = addIntRow("Origin Tracking duration seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.hunter.originTrackingDurationSeconds, v -> cfg.hunter.originTrackingDurationSeconds = v, 0, 600);
                logicalY += ROW_H;
            }
            case SENTINEL -> {
                // Passives
                y = addFloatRow("Guardian Aura damage reduction", y, labelX, labelW, fieldX, fieldW, () -> cfg.sentinel.guardianAuraDamageReduction, v -> cfg.sentinel.guardianAuraDamageReduction = v, 0.0F, 1.0F);
                logicalY += ROW_H;
                y = addIntRow("Guardian Aura radius blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.sentinel.guardianAuraRadiusBlocks, v -> cfg.sentinel.guardianAuraRadiusBlocks = v, 0, 64);
                logicalY += ROW_H;
                y = addIntRow("Fortress stand still seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.sentinel.fortressStandStillSeconds, v -> cfg.sentinel.fortressStandStillSeconds = v, 0, 60);
                logicalY += ROW_H;
                y = addIntRow("Fortress resistance amplifier", y, labelX, labelW, fieldX, fieldW, () -> cfg.sentinel.fortressResistanceAmplifier, v -> cfg.sentinel.fortressResistanceAmplifier = v, 0, 10);
                logicalY += ROW_H;
                y = addFloatRow("Retribution Thorns damage multiplier", y, labelX, labelW, fieldX, fieldW, () -> cfg.sentinel.retributionThornsDamageMultiplier, v -> cfg.sentinel.retributionThornsDamageMultiplier = v, 0.0F, 2.0F);
                logicalY += ROW_H;
                y = spacer(y);
                logicalY += 8;
                // Abilities
                y = addIntRow("Shield Wall cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.sentinel.shieldWallCooldownSeconds, v -> cfg.sentinel.shieldWallCooldownSeconds = v, 0, 3600);
                logicalY += ROW_H;
                y = addIntRow("Shield Wall duration seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.sentinel.shieldWallDurationSeconds, v -> cfg.sentinel.shieldWallDurationSeconds = v, 0, 60);
                logicalY += ROW_H;
                y = addIntRow("Shield Wall width blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.sentinel.shieldWallWidthBlocks, v -> cfg.sentinel.shieldWallWidthBlocks = v, 0, 20);
                logicalY += ROW_H;
                y = addIntRow("Shield Wall height blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.sentinel.shieldWallHeightBlocks, v -> cfg.sentinel.shieldWallHeightBlocks = v, 0, 10);
                logicalY += ROW_H;
                y = spacer(y);
                logicalY += 8;
                y = addIntRow("Taunt cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.sentinel.tauntCooldownSeconds, v -> cfg.sentinel.tauntCooldownSeconds = v, 0, 3600);
                logicalY += ROW_H;
                y = addIntRow("Taunt duration seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.sentinel.tauntDurationSeconds, v -> cfg.sentinel.tauntDurationSeconds = v, 0, 60);
                logicalY += ROW_H;
                y = addIntRow("Taunt radius blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.sentinel.tauntRadiusBlocks, v -> cfg.sentinel.tauntRadiusBlocks = v, 0, 64);
                logicalY += ROW_H;
                y = addFloatRow("Taunt damage reduction", y, labelX, labelW, fieldX, fieldW, () -> cfg.sentinel.tauntDamageReduction, v -> cfg.sentinel.tauntDamageReduction = v, 0.0F, 1.0F);
                logicalY += ROW_H;
                y = spacer(y);
                logicalY += 8;
                y = addIntRow("Intervention cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.sentinel.interventionCooldownSeconds, v -> cfg.sentinel.interventionCooldownSeconds = v, 0, 3600);
                logicalY += ROW_H;
                y = addIntRow("Intervention range blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.sentinel.interventionRangeBlocks, v -> cfg.sentinel.interventionRangeBlocks = v, 0, 128);
                logicalY += ROW_H;
                y = spacer(y);
                logicalY += 8;
                y = addIntRow("Rally Cry cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.sentinel.rallyCryCooldownSeconds, v -> cfg.sentinel.rallyCryCooldownSeconds = v, 0, 3600);
                logicalY += ROW_H;
                y = addFloatRow("Rally Cry heal hearts", y, labelX, labelW, fieldX, fieldW, () -> cfg.sentinel.rallyCryHealHearts, v -> cfg.sentinel.rallyCryHealHearts = v, 0.0F, 20.0F);
                logicalY += ROW_H;
                y = addIntRow("Rally Cry resistance duration seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.sentinel.rallyCryResistanceDurationSeconds, v -> cfg.sentinel.rallyCryResistanceDurationSeconds = v, 0, 60);
                logicalY += ROW_H;
                y = addIntRow("Rally Cry radius blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.sentinel.rallyCryRadiusBlocks, v -> cfg.sentinel.rallyCryRadiusBlocks = v, 0, 64);
                logicalY += ROW_H;
                y = spacer(y);
                logicalY += 8;
                y = addIntRow("Lockdown cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.sentinel.lockdownCooldownSeconds, v -> cfg.sentinel.lockdownCooldownSeconds = v, 0, 3600);
                logicalY += ROW_H;
                y = addIntRow("Lockdown duration seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.sentinel.lockdownDurationSeconds, v -> cfg.sentinel.lockdownDurationSeconds = v, 0, 60);
                logicalY += ROW_H;
                y = addIntRow("Lockdown radius blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.sentinel.lockdownRadiusBlocks, v -> cfg.sentinel.lockdownRadiusBlocks = v, 0, 64);
                logicalY += ROW_H;
            }
            case TRICKSTER -> {
                // Passives
                y = addFloatRow("Sleight of Hand chance", y, labelX, labelW, fieldX, fieldW, () -> cfg.trickster.sleightOfHandChance, v -> cfg.trickster.sleightOfHandChance = v, 0.0F, 1.0F);
                logicalY += ROW_H;
                y = addFloatRow("Slippery chance", y, labelX, labelW, fieldX, fieldW, () -> cfg.trickster.slipperyChance, v -> cfg.trickster.slipperyChance = v, 0.0F, 1.0F);
                logicalY += ROW_H;
                y = spacer(y);
                logicalY += 8;
                // Abilities
                y = addIntRow("Shadow Swap cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.trickster.shadowSwapCooldownSeconds, v -> cfg.trickster.shadowSwapCooldownSeconds = v, 0, 3600);
                logicalY += ROW_H;
                y = addIntRow("Shadow Swap range blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.trickster.shadowSwapRangeBlocks, v -> cfg.trickster.shadowSwapRangeBlocks = v, 1, 128);
                logicalY += ROW_H;
                y = spacer(y);
                logicalY += 8;
                y = addIntRow("Mirage cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.trickster.mirageCooldownSeconds, v -> cfg.trickster.mirageCooldownSeconds = v, 0, 3600);
                logicalY += ROW_H;
                y = addIntRow("Mirage duration seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.trickster.mirageDurationSeconds, v -> cfg.trickster.mirageDurationSeconds = v, 0, 60);
                logicalY += ROW_H;
                y = addIntRow("Mirage clone count", y, labelX, labelW, fieldX, fieldW, () -> cfg.trickster.mirageCloneCount, v -> cfg.trickster.mirageCloneCount = v, 0, 10);
                logicalY += ROW_H;
                y = spacer(y);
                logicalY += 8;
                y = addIntRow("Glitch Step cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.trickster.glitchStepCooldownSeconds, v -> cfg.trickster.glitchStepCooldownSeconds = v, 0, 3600);
                logicalY += ROW_H;
                y = addIntRow("Glitch Step distance blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.trickster.glitchStepDistanceBlocks, v -> cfg.trickster.glitchStepDistanceBlocks = v, 0, 64);
                logicalY += ROW_H;
                y = addFloatRow("Glitch Step afterimg damage", y, labelX, labelW, fieldX, fieldW, () -> cfg.trickster.glitchStepAfterimgDamage, v -> cfg.trickster.glitchStepAfterimgDamage = v, 0.0F, 40.0F);
                logicalY += ROW_H;
                y = addIntRow("Glitch Step afterimg radius blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.trickster.glitchStepAfterimgRadiusBlocks, v -> cfg.trickster.glitchStepAfterimgRadiusBlocks = v, 0, 32);
                logicalY += ROW_H;
                y = spacer(y);
                logicalY += 8;
                y = addIntRow("Puppet Master cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.trickster.puppetMasterCooldownSeconds, v -> cfg.trickster.puppetMasterCooldownSeconds = v, 0, 3600);
                logicalY += ROW_H;
                y = addIntRow("Puppet Master duration seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.trickster.puppetMasterDurationSeconds, v -> cfg.trickster.puppetMasterDurationSeconds = v, 0, 30);
                logicalY += ROW_H;
                y = addIntRow("Puppet Master range blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.trickster.puppetMasterRangeBlocks, v -> cfg.trickster.puppetMasterRangeBlocks = v, 0, 64);
                logicalY += ROW_H;
                y = spacer(y);
                logicalY += 8;
                y = addIntRow("Mind Games cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.trickster.mindGamesCooldownSeconds, v -> cfg.trickster.mindGamesCooldownSeconds = v, 0, 3600);
                logicalY += ROW_H;
                y = addIntRow("Mind Games duration seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.trickster.mindGamesDurationSeconds, v -> cfg.trickster.mindGamesDurationSeconds = v, 0, 30);
                logicalY += ROW_H;
                y = addIntRow("Mind Games range blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.trickster.mindGamesRangeBlocks, v -> cfg.trickster.mindGamesRangeBlocks = v, 0, 64);
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
                y = addFloatRow("Chrono Charm cooldown multiplier", y, labelX, labelW, fieldX, fieldW, () -> cfg.legendary.chronoCharmCooldownMultiplier, v -> cfg.legendary.chronoCharmCooldownMultiplier = v, 0.05F, 1.0F);
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
                y = addIntRow("Duelist's Rapier cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.legendary.duelistsRapierCooldownSeconds, v -> cfg.legendary.duelistsRapierCooldownSeconds = v, 0, 3600);
                logicalY += ROW_H;
                y = addIntRow("Duelist's Rapier parry window ticks", y, labelX, labelW, fieldX, fieldW, () -> cfg.legendary.duelistsRapierParryWindowTicks, v -> cfg.legendary.duelistsRapierParryWindowTicks = v, 0, 60);
                logicalY += ROW_H;
                y = addFloatRow("Duelist's Rapier crit damage multiplier", y, labelX, labelW, fieldX, fieldW, () -> cfg.legendary.duelistsRapierCritDamageMultiplier, v -> cfg.legendary.duelistsRapierCritDamageMultiplier = v, 0.0F, 10.0F);
                logicalY += ROW_H;
                y = spacer(y);
                logicalY += 8;
                y = addIntRow("Challenger's Gauntlet cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.legendary.challengersGauntletCooldownSeconds, v -> cfg.legendary.challengersGauntletCooldownSeconds = v, 0, 3600);
                logicalY += ROW_H;
                y = addIntRow("Challenger's Gauntlet range blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.legendary.challengersGauntletRangeBlocks, v -> cfg.legendary.challengersGauntletRangeBlocks = v, 1, 64);
                logicalY += ROW_H;
                y = spacer(y);
                logicalY += 8;
                y = addIntRow("Reversal Mirror duration seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.legendary.reversalMirrorDurationSeconds, v -> cfg.legendary.reversalMirrorDurationSeconds = v, 0, 60);
                logicalY += ROW_H;
                y = addIntRow("Reversal Mirror cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.legendary.reversalMirrorCooldownSeconds, v -> cfg.legendary.reversalMirrorCooldownSeconds = v, 0, 3600);
                logicalY += ROW_H;
                y = spacer(y);
                logicalY += 8;
                y = addIntRow("Gladiator's Mark duration seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.legendary.gladiatorsMarkDurationSeconds, v -> cfg.legendary.gladiatorsMarkDurationSeconds = v, 0, 3600);
                logicalY += ROW_H;
                y = addIntRow("Gladiator's Mark cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.legendary.gladiatorsMarkCooldownSeconds, v -> cfg.legendary.gladiatorsMarkCooldownSeconds = v, 0, 3600);
                logicalY += ROW_H;
                y = addIntRow("Gladiator's Mark range blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.legendary.gladiatorsMarkRangeBlocks, v -> cfg.legendary.gladiatorsMarkRangeBlocks = v, 1, 64);
                logicalY += ROW_H;
                y = addFloatRow("Gladiator's Mark damage multiplier", y, labelX, labelW, fieldX, fieldW, () -> cfg.legendary.gladiatorsMarkDamageMultiplier, v -> cfg.legendary.gladiatorsMarkDamageMultiplier = v, 0.0F, 10.0F);
                logicalY += ROW_H;
                y = spacer(y);
                logicalY += 8;
                y = addIntRow("Soul Shackle duration seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.legendary.soulShackleDurationSeconds, v -> cfg.legendary.soulShackleDurationSeconds = v, 0, 3600);
                logicalY += ROW_H;
                y = addIntRow("Soul Shackle cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.legendary.soulShackleCooldownSeconds, v -> cfg.legendary.soulShackleCooldownSeconds = v, 0, 3600);
                logicalY += ROW_H;
                y = addIntRow("Soul Shackle range blocks", y, labelX, labelW, fieldX, fieldW, () -> cfg.legendary.soulShackleRangeBlocks, v -> cfg.legendary.soulShackleRangeBlocks = v, 1, 64);
                logicalY += ROW_H;
                y = addFloatRow("Soul Shackle split ratio", y, labelX, labelW, fieldX, fieldW, () -> cfg.legendary.soulShackleSplitRatio, v -> cfg.legendary.soulShackleSplitRatio = v, 0.0F, 1.0F);
                logicalY += ROW_H;
                y = spacer(y);
                logicalY += 8;
                y = addIntRow("Experience Blade max sharpness", y, labelX, labelW, fieldX, fieldW, () -> cfg.legendary.experienceBladeMaxSharpness, v -> cfg.legendary.experienceBladeMaxSharpness = v, 0, 255);
                logicalY += ROW_H;
                y = addIntRow("Experience Blade sharpness per tier", y, labelX, labelW, fieldX, fieldW, () -> cfg.legendary.experienceBladeSharpnessPerTier, v -> cfg.legendary.experienceBladeSharpnessPerTier = v, 1, 50);
                logicalY += ROW_H;
                y = addIntRow("Experience Blade xp levels per tier", y, labelX, labelW, fieldX, fieldW, () -> cfg.legendary.experienceBladeXpLevelsPerTier, v -> cfg.legendary.experienceBladeXpLevelsPerTier = v, 1, 100);
                logicalY += ROW_H;
                y = spacer(y);
                logicalY += 8;
                y = addIntRow("Trophy Necklace max stolen passives", y, labelX, labelW, fieldX, fieldW, () -> cfg.legendary.trophyNecklaceMaxPassives, v -> cfg.legendary.trophyNecklaceMaxPassives = v, 0, 64);
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
                y = addStringRow("Recipe gem requirements (recipe=gem, comma-separated)", y, labelX, labelW, fieldX, fieldW,
                        () -> joinMap(cfg.legendary.recipeGemRequirements),
                        v -> cfg.legendary.recipeGemRequirements = parseMap(v));
                logicalY += ROW_H;
            }
            case MASTERY -> {
                y = addBoolRow("Enabled", y, labelX, labelW, fieldX, fieldW, () -> cfg.mastery.enabled, v -> cfg.mastery.enabled = v);
                logicalY += ROW_H;
                y = addBoolRow("Show aura particles", y, labelX, labelW, fieldX, fieldW, () -> cfg.mastery.showAuraParticles, v -> cfg.mastery.showAuraParticles = v);
                logicalY += ROW_H;
            }
            case RIVALRY -> {
                y = addBoolRow("Enabled", y, labelX, labelW, fieldX, fieldW, () -> cfg.rivalry.enabled, v -> cfg.rivalry.enabled = v);
                logicalY += ROW_H;
                y = addDoubleRow("Damage multiplier", y, labelX, labelW, fieldX, fieldW, () -> cfg.rivalry.damageMultiplier, v -> cfg.rivalry.damageMultiplier = v, 0.0D, 10.0D);
                logicalY += ROW_H;
                y = addBoolRow("Show in HUD", y, labelX, labelW, fieldX, fieldW, () -> cfg.rivalry.showInHud, v -> cfg.rivalry.showInHud = v);
                logicalY += ROW_H;
            }
            case LOADOUTS -> {
                y = addBoolRow("Enabled", y, labelX, labelW, fieldX, fieldW, () -> cfg.loadouts.enabled, v -> cfg.loadouts.enabled = v);
                logicalY += ROW_H;
                y = addIntRow("Unlock energy", y, labelX, labelW, fieldX, fieldW, () -> cfg.loadouts.unlockEnergy, v -> cfg.loadouts.unlockEnergy = v, 0, 10);
                logicalY += ROW_H;
                y = addIntRow("Max presets per gem", y, labelX, labelW, fieldX, fieldW, () -> cfg.loadouts.maxPresetsPerGem, v -> cfg.loadouts.maxPresetsPerGem = v, 0, 20);
                logicalY += ROW_H;
            }
            case SYNERGIES -> {
                y = addBoolRow("Enabled", y, labelX, labelW, fieldX, fieldW, () -> cfg.synergies.enabled, v -> cfg.synergies.enabled = v);
                logicalY += ROW_H;
                y = addIntRow("Window seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.synergies.windowSeconds, v -> cfg.synergies.windowSeconds = v, 0, 60);
                logicalY += ROW_H;
                y = addIntRow("Cooldown seconds", y, labelX, labelW, fieldX, fieldW, () -> cfg.synergies.cooldownSeconds, v -> cfg.synergies.cooldownSeconds = v, 0, 3600);
                logicalY += ROW_H;
                y = addBoolRow("Show notifications", y, labelX, labelW, fieldX, fieldW, () -> cfg.synergies.showNotifications, v -> cfg.synergies.showNotifications = v);
                logicalY += ROW_H;
            }
            case AUGMENTS -> {
                y = addIntRow("Gem max slots", y, labelX, labelW, fieldX, fieldW, () -> cfg.augments.gemMaxSlots, v -> cfg.augments.gemMaxSlots = v, 0, 16);
                logicalY += ROW_H;
                y = addIntRow("Legendary max slots", y, labelX, labelW, fieldX, fieldW, () -> cfg.augments.legendaryMaxSlots, v -> cfg.augments.legendaryMaxSlots = v, 0, 16);
                logicalY += ROW_H;
                y = spacer(y);
                logicalY += 8;
                y = addIntRow("Rarity common weight", y, labelX, labelW, fieldX, fieldW, () -> cfg.augments.rarityCommonWeight, v -> cfg.augments.rarityCommonWeight = v, 0, 1000);
                logicalY += ROW_H;
                y = addIntRow("Rarity rare weight", y, labelX, labelW, fieldX, fieldW, () -> cfg.augments.rarityRareWeight, v -> cfg.augments.rarityRareWeight = v, 0, 1000);
                logicalY += ROW_H;
                y = addIntRow("Rarity epic weight", y, labelX, labelW, fieldX, fieldW, () -> cfg.augments.rarityEpicWeight, v -> cfg.augments.rarityEpicWeight = v, 0, 1000);
                logicalY += ROW_H;
                y = spacer(y);
                logicalY += 8;
                y = addFloatRow("Common magnitude min", y, labelX, labelW, fieldX, fieldW, () -> cfg.augments.commonMagnitudeMin, v -> cfg.augments.commonMagnitudeMin = v, 0.0F, 10.0F);
                logicalY += ROW_H;
                y = addFloatRow("Common magnitude max", y, labelX, labelW, fieldX, fieldW, () -> cfg.augments.commonMagnitudeMax, v -> cfg.augments.commonMagnitudeMax = v, 0.0F, 10.0F);
                logicalY += ROW_H;
                y = addFloatRow("Rare magnitude min", y, labelX, labelW, fieldX, fieldW, () -> cfg.augments.rareMagnitudeMin, v -> cfg.augments.rareMagnitudeMin = v, 0.0F, 10.0F);
                logicalY += ROW_H;
                y = addFloatRow("Rare magnitude max", y, labelX, labelW, fieldX, fieldW, () -> cfg.augments.rareMagnitudeMax, v -> cfg.augments.rareMagnitudeMax = v, 0.0F, 10.0F);
                logicalY += ROW_H;
                y = addFloatRow("Epic magnitude min", y, labelX, labelW, fieldX, fieldW, () -> cfg.augments.epicMagnitudeMin, v -> cfg.augments.epicMagnitudeMin = v, 0.0F, 10.0F);
                logicalY += ROW_H;
                y = addFloatRow("Epic magnitude max", y, labelX, labelW, fieldX, fieldW, () -> cfg.augments.epicMagnitudeMax, v -> cfg.augments.epicMagnitudeMax = v, 0.0F, 10.0F);
                logicalY += ROW_H;
            }
            case DISABLES -> {
                y = addDisablesStringRow("Disabled gems (comma-separated)", y, labelX, labelW, fieldX, fieldW,
                        () -> joinList(disablesCfg.disabledGems),
                        v -> disablesCfg.disabledGems = parseList(v));
                logicalY += ROW_H;
                y = addDisablesStringRow("Disabled abilities (comma-separated)", y, labelX, labelW, fieldX, fieldW,
                        () -> joinList(disablesCfg.disabledAbilities),
                        v -> disablesCfg.disabledAbilities = parseList(v));
                logicalY += ROW_H;
                y = addDisablesStringRow("Disabled passives (comma-separated)", y, labelX, labelW, fieldX, fieldW,
                        () -> joinList(disablesCfg.disabledPassives),
                        v -> disablesCfg.disabledPassives = parseList(v));
                logicalY += ROW_H;
                y = addDisablesStringRow("Disabled bonus abilities (comma-separated)", y, labelX, labelW, fieldX, fieldW,
                        () -> joinList(disablesCfg.disabledBonusAbilities),
                        v -> disablesCfg.disabledBonusAbilities = parseList(v));
                logicalY += ROW_H;
                y = addDisablesStringRow("Disabled bonus passives (comma-separated)", y, labelX, labelW, fieldX, fieldW,
                        () -> joinList(disablesCfg.disabledBonusPassives),
                        v -> disablesCfg.disabledBonusPassives = parseList(v));
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
            disablesCfg = new GemsDisablesConfig();
            disablesDirty = true;
            rebuild();
        }).dimensions(footerX + 136, footerY, 110, 20).build());
        ButtonWidget copyPathButton = addDrawableChild(ButtonWidget.builder(Text.literal("Copy config paths"), b -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null) {
                client.keyboard.setClipboard("Balance: " + balancePath() + "\nDisables: " + disablesPath());
            }
        }).dimensions(footerX + 252, footerY, 140, 20).build());

        footerY += 24;

        ButtonWidget dumpButton = addDrawableChild(ButtonWidget.builder(Text.literal("Dump effective"), b -> ClientCommandSender.sendCommand("gems dumpBalance")).dimensions(footerX, footerY, 130, 20).build());
        ButtonWidget doneButton = addDrawableChild(ButtonWidget.builder(Text.literal("Done"), b -> close()).dimensions(footerX + 136, footerY, 110, 20).build());

        saveButton.active = (canEdit && (dirty || disablesDirty) && validation.isValid()) || clientDirty;
        saveReloadButton.active = canEdit && (dirty || disablesDirty) && validation.isValid();
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
            saveButton.active = (canEdit && (dirty || disablesDirty) && validation.isValid()) || clientDirty;
        }
        if (saveReloadButton != null) {
            saveReloadButton.active = canEdit && (dirty || disablesDirty) && validation.isValid();
        }
    }

    private void save(boolean reload) {
        boolean saveServer = dirty && canEdit && validation.isValid();
        boolean saveDisables = disablesDirty && canEdit;
        boolean saveClient = clientDirty;
        if (!saveServer && !saveDisables && !saveClient) {
            return;
        }
        if (saveServer) {
            GemsConfigManager.writeBalanceForUi(cfg);
            dirty = false;
            if (reload) {
                ClientCommandSender.sendCommand("gems reloadBalance");
            }
        }
        if (saveDisables) {
            GemsDisablesConfigManager.writeDisablesForUi(disablesCfg);
            disablesDirty = false;
            if (reload) {
                ClientCommandSender.sendCommand("gems reloadDisables");
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
            case SPY -> cfg.spy = new GemsBalanceConfig.Spy();
            case BEACON -> cfg.beacon = new GemsBalanceConfig.Beacon();
            case AIR -> cfg.air = new GemsBalanceConfig.Air();
            case VOID -> cfg.voidGem = new GemsBalanceConfig.VoidGem();
            case CHAOS -> cfg.chaos = new GemsBalanceConfig.Chaos();
            case PRISM -> cfg.prism = new GemsBalanceConfig.Prism();
            case DUELIST -> cfg.duelist = new GemsBalanceConfig.Duelist();
            case HUNTER -> cfg.hunter = new GemsBalanceConfig.Hunter();
            case SENTINEL -> cfg.sentinel = new GemsBalanceConfig.Sentinel();
            case TRICKSTER -> cfg.trickster = new GemsBalanceConfig.Trickster();
            case LEGENDARY -> cfg.legendary = new GemsBalanceConfig.Legendary();
            case BONUS_POOL -> cfg.bonusPool = new GemsBalanceConfig.BonusPool();
            case MASTERY -> cfg.mastery = new GemsBalanceConfig.Mastery();
            case RIVALRY -> cfg.rivalry = new GemsBalanceConfig.Rivalry();
            case LOADOUTS -> cfg.loadouts = new GemsBalanceConfig.Loadouts();
            case SYNERGIES -> cfg.synergies = new GemsBalanceConfig.Synergies();
            case AUGMENTS -> cfg.augments = new GemsBalanceConfig.Augments();
            case DISABLES -> {
                disablesCfg = new GemsDisablesConfig();
                disablesDirty = true;
            }
        }
        if (section != Section.CLIENT && section != Section.DISABLES) {
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

    private int addDisablesStringRow(String label, int y, int labelX, int labelW, int fieldX, int fieldW, Supplier<String> getter, Consumer<String> setter) {
        addDrawableChild(ButtonWidget.builder(Text.literal(label), b -> {
        }).dimensions(labelX, y, labelW, 20).build()).active = false;
        TextFieldWidget field = new TextFieldWidget(this.textRenderer, fieldX, y, fieldW, 20, Text.empty());
        field.setText(getter.get());
        field.setEditable(canEdit);
        field.setChangedListener(s -> {
            setter.accept(s);
            disablesDirty = true;
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

    private static String joinList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        for (String value : values) {
            if (value == null) {
                continue;
            }
            String trimmed = value.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (out.length() > 0) {
                out.append(", ");
            }
            out.append(trimmed);
        }
        return out.toString();
    }

    private static List<String> parseList(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        for (String part : value.split(",")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                out.add(trimmed);
            }
        }
        return out;
    }

    private static String joinMap(Map<String, String> map) {
        if (map == null || map.isEmpty()) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        for (Map.Entry<String, String> entry : new TreeMap<>(map).entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (key == null || key.isBlank() || value == null || value.isBlank()) {
                continue;
            }
            if (out.length() > 0) {
                out.append(", ");
            }
            out.append(key.trim()).append('=').append(value.trim());
        }
        return out.toString();
    }

    private static Map<String, String> parseMap(String value) {
        Map<String, String> out = new TreeMap<>();
        if (value == null || value.isBlank()) {
            return out;
        }
        for (String part : value.split(",")) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            int idx = trimmed.indexOf('=');
            if (idx <= 0 || idx == trimmed.length() - 1) {
                continue;
            }
            String key = trimmed.substring(0, idx).trim();
            String val = trimmed.substring(idx + 1).trim();
            if (!key.isEmpty() && !val.isEmpty()) {
                out.put(key, val);
            }
        }
        return out;
    }

    private static String joinIntMap(Map<String, Integer> map) {
        if (map == null || map.isEmpty()) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        for (Map.Entry<String, Integer> entry : new TreeMap<>(map).entrySet()) {
            String key = entry.getKey();
            Integer value = entry.getValue();
            if (key == null || key.isBlank() || value == null) {
                continue;
            }
            if (out.length() > 0) {
                out.append(", ");
            }
            out.append(key.trim()).append('=').append(value);
        }
        return out.toString();
    }

    private static Map<String, Integer> parseIntMap(String value) {
        Map<String, Integer> out = new TreeMap<>();
        if (value == null || value.isBlank()) {
            return out;
        }
        for (String part : value.split(",")) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            int idx = trimmed.indexOf('=');
            if (idx <= 0 || idx == trimmed.length() - 1) {
                continue;
            }
            String key = trimmed.substring(0, idx).trim();
            String val = trimmed.substring(idx + 1).trim();
            if (key.isEmpty() || val.isEmpty()) {
                continue;
            }
            Integer parsed = tryParseInt(val);
            if (parsed == null) {
                continue;
            }
            out.put(key, Math.max(0, parsed));
        }
        return out;
    }

    private static String joinSummonSpecs(List<GemsBalanceConfig.Summoner.SummonSpec> specs) {
        if (specs == null || specs.isEmpty()) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        for (GemsBalanceConfig.Summoner.SummonSpec spec : specs) {
            if (spec == null || spec.entityId == null || spec.entityId.isBlank()) {
                continue;
            }
            if (out.length() > 0) {
                out.append(", ");
            }
            out.append(spec.entityId.trim()).append('=').append(Math.max(1, spec.count));
        }
        return out.toString();
    }

    private static List<GemsBalanceConfig.Summoner.SummonSpec> parseSummonSpecs(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        List<GemsBalanceConfig.Summoner.SummonSpec> out = new ArrayList<>();
        for (String part : value.split(",")) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            String id = trimmed;
            int count = 1;
            int idx = trimmed.indexOf('=');
            if (idx > 0) {
                id = trimmed.substring(0, idx).trim();
                String rawCount = trimmed.substring(idx + 1).trim();
                Integer parsed = tryParseInt(rawCount);
                if (parsed != null) {
                    count = Math.max(1, parsed);
                }
            }
            if (!id.isEmpty()) {
                out.add(new GemsBalanceConfig.Summoner.SummonSpec(id, count));
            }
        }
        return out;
    }

    private static Path balancePath() {
        return GemsConfigManager.balancePathForUi();
    }

    private static Path disablesPath() {
        return GemsDisablesConfigManager.disablesPathForUi();
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
        context.drawCenteredTextWithShadow(textRenderer, this.title, centerX, 18, 0xFFFFFFFF);

        String balancePath = "Balance: " + balancePath().toString();
        String disablesPath = "Disables: " + disablesPath().toString();
        context.drawCenteredTextWithShadow(textRenderer, Text.literal(balancePath), centerX, 32, 0xA0A0A0);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal(disablesPath), centerX, 44, 0xA0A0A0);

        if (loadError != null) {
            context.drawCenteredTextWithShadow(textRenderer, Text.literal(loadError), centerX, this.height - 18, 0xFF5555);
        }
        if (disablesLoadError != null) {
            context.drawCenteredTextWithShadow(textRenderer, Text.literal(disablesLoadError), centerX, this.height - 30, 0xFF5555);
        }

        if (!canEdit) {
            context.drawCenteredTextWithShadow(textRenderer, Text.literal("Config edits require op permissions on multiplayer servers."), centerX, this.height - 56, 0xFFAA00);
        } else if (!validation.isValid()) {
            context.drawCenteredTextWithShadow(textRenderer, Text.literal("Fix invalid values to enable Save."), centerX, this.height - 42, 0xFF5555);
        } else if (dirty || disablesDirty) {
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

    private enum Category {
        GENERAL("General"),
        ELEMENTAL("Elemental"),
        COMBAT("Combat"),
        MISC("Misc");

        private final String label;

        Category(String label) {
            this.label = label;
        }
    }

    private enum Section {
        // General
        CLIENT("Client", Category.GENERAL),
        VISUAL("Visual", Category.GENERAL),
        SYSTEMS("Systems", Category.GENERAL),
        BONUS_POOL("Bonus Pool", Category.GENERAL),
        LEGENDARY("Legendary", Category.GENERAL),
        MASTERY("Mastery", Category.GENERAL),
        RIVALRY("Rivalry", Category.GENERAL),
        LOADOUTS("Loadouts", Category.GENERAL),
        SYNERGIES("Synergies", Category.GENERAL),
        AUGMENTS("Augments", Category.GENERAL),
        DISABLES("Disables", Category.GENERAL),
        // Elemental
        ASTRA("Astra", Category.ELEMENTAL),
        FIRE("Fire", Category.ELEMENTAL),
        FLUX("Flux", Category.ELEMENTAL),
        LIFE("Life", Category.ELEMENTAL),
        PUFF("Puff", Category.ELEMENTAL),
        SPEED("Speed", Category.ELEMENTAL),
        STRENGTH("Strength", Category.ELEMENTAL),
        WEALTH("Wealth", Category.ELEMENTAL),
        SPACE("Space", Category.ELEMENTAL),
        AIR("Air", Category.ELEMENTAL),
        // Combat
        TERROR("Terror", Category.COMBAT),
        SUMMONER("Summoner", Category.COMBAT),
        REAPER("Reaper", Category.COMBAT),
        PILLAGER("Pillager", Category.COMBAT),
        DUELIST("Duelist", Category.COMBAT),
        HUNTER("Hunter", Category.COMBAT),
        SENTINEL("Sentinel", Category.COMBAT),
        TRICKSTER("Trickster", Category.COMBAT),
        // Misc
        SPY("Spy", Category.MISC),
        BEACON("Beacon", Category.MISC),
        VOID("Void", Category.MISC),
        CHAOS("Chaos", Category.MISC),
        PRISM("Prism", Category.MISC);

        private final String label;
        private final Category category;

        Section(String label, Category category) {
            this.label = label;
            this.category = category;
        }

        static Section[] forCategory(Category cat) {
            return java.util.Arrays.stream(values())
                    .filter(s -> s.category == cat)
                    .toArray(Section[]::new);
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
        if (cfg.spy == null) {
            cfg.spy = new GemsBalanceConfig.Spy();
        }
        if (cfg.beacon == null) {
            cfg.beacon = new GemsBalanceConfig.Beacon();
        }
        if (cfg.air == null) {
            cfg.air = new GemsBalanceConfig.Air();
        }
        if (cfg.voidGem == null) {
            cfg.voidGem = new GemsBalanceConfig.VoidGem();
        }
        if (cfg.chaos == null) {
            cfg.chaos = new GemsBalanceConfig.Chaos();
        }
        if (cfg.prism == null) {
            cfg.prism = new GemsBalanceConfig.Prism();
        }
        if (cfg.duelist == null) {
            cfg.duelist = new GemsBalanceConfig.Duelist();
        }
        if (cfg.hunter == null) {
            cfg.hunter = new GemsBalanceConfig.Hunter();
        }
        if (cfg.sentinel == null) {
            cfg.sentinel = new GemsBalanceConfig.Sentinel();
        }
        if (cfg.trickster == null) {
            cfg.trickster = new GemsBalanceConfig.Trickster();
        }
        if (cfg.legendary == null) {
            cfg.legendary = new GemsBalanceConfig.Legendary();
        }
        if (cfg.bonusPool == null) {
            cfg.bonusPool = new GemsBalanceConfig.BonusPool();
        }
        if (cfg.mastery == null) {
            cfg.mastery = new GemsBalanceConfig.Mastery();
        }
        if (cfg.rivalry == null) {
            cfg.rivalry = new GemsBalanceConfig.Rivalry();
        }
        if (cfg.loadouts == null) {
            cfg.loadouts = new GemsBalanceConfig.Loadouts();
        }
        if (cfg.synergies == null) {
            cfg.synergies = new GemsBalanceConfig.Synergies();
        }
        if (cfg.augments == null) {
            cfg.augments = new GemsBalanceConfig.Augments();
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
