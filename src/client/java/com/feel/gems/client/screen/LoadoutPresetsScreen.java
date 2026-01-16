package com.feel.gems.client.screen;

import com.feel.gems.client.ClientGemState;
import com.feel.gems.core.GemId;
import com.feel.gems.loadout.GemLoadout;
import com.feel.gems.net.LoadoutDeletePayload;
import com.feel.gems.net.LoadoutLoadPayload;
import com.feel.gems.net.LoadoutOpenRequestPayload;
import com.feel.gems.net.LoadoutSavePayload;
import com.feel.gems.net.LoadoutScreenPayload;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

/**
 * Client UI for managing per-gem loadout presets.
 */
public final class LoadoutPresetsScreen extends GemsScreenBase {
    private static final int ROW_HEIGHT = 24;
    private static final int BUTTON_HEIGHT = 20;
    private static final int SETTINGS_ROW_HEIGHT = 22;

    private final LoadoutScreenPayload payload;
    private final List<ButtonWidget> loadButtons = new ArrayList<>();
    private final List<ButtonWidget> deleteButtons = new ArrayList<>();

    private TextFieldWidget nameField;
    private ButtonWidget saveButton;
    private ButtonWidget closeButton;
    private ButtonWidget prevGemButton;
    private ButtonWidget nextGemButton;
    private CyclingButtonWidget<Boolean> passivesToggle;
    private CyclingButtonWidget<GemLoadout.HudPosition> hudPosToggle;
    private CyclingButtonWidget<Boolean> showCooldownsToggle;
    private CyclingButtonWidget<Boolean> showEnergyToggle;
    private CyclingButtonWidget<Boolean> compactModeToggle;

    private boolean passivesEnabled = true;
    private GemLoadout.HudPosition hudPosition = GemLoadout.HudPosition.TOP_LEFT;
    private boolean showCooldowns = true;
    private boolean showEnergy = true;
    private boolean compactMode = false;

    private int listTop;
    private int labelX;
    private int loadX;
    private int deleteX;
    private int panelLeft;
    private int panelRight;
    private int panelTop;
    private int panelBottom;

    public LoadoutPresetsScreen(LoadoutScreenPayload payload) {
        super(Text.translatable("gems.screen.loadout_presets.title"));
        this.payload = payload;
    }

    @Override
    protected void init() {
        super.init();
        clearChildren();

        int centerX = this.width / 2;
        int panelWidth = Math.min(380, this.width - 40);
        panelLeft = centerX - (panelWidth / 2);
        panelRight = centerX + (panelWidth / 2);
        panelTop = 18;
        panelBottom = this.height - 36;

        int top = 28;
        int settingsTop = top + 50;
        int settingsRows = 5;
        listTop = settingsTop + (settingsRows * SETTINGS_ROW_HEIGHT) + 8;
        labelX = panelLeft + 10;
        loadX = panelRight - 130;
        deleteX = panelRight - 60;

        nameField = new TextFieldWidget(this.textRenderer, panelLeft + 10, top + 26, panelWidth - 150, 18, Text.translatable("gems.screen.loadout_presets.name"));
        LoadoutScreenPayload.Preset activePreset = getActivePreset();
        if (activePreset != null) {
            applyPresetToEditor(activePreset);
        }
        nameField.setText(activePreset != null && !activePreset.name().isBlank() ? activePreset.name() : "Preset");
        addDrawableChild(nameField);

        saveButton = ButtonWidget.builder(Text.translatable("gems.screen.loadout_presets.save"), btn -> savePreset())
                .dimensions(panelRight - 130, top + 24, 120, BUTTON_HEIGHT)
                .build();
        addDrawableChild(saveButton);

        prevGemButton = ButtonWidget.builder(Text.literal("<"), btn -> requestGem(offsetGem(-1)))
            .dimensions(panelLeft + 10, top, 20, BUTTON_HEIGHT)
                .build();
        addDrawableChild(prevGemButton);

        nextGemButton = ButtonWidget.builder(Text.literal(">"), btn -> requestGem(offsetGem(1)))
            .dimensions(panelLeft + 140, top, 20, BUTTON_HEIGHT)
                .build();
        addDrawableChild(nextGemButton);

        closeButton = ButtonWidget.builder(Text.translatable("gems.screen.button.close"), btn -> close())
                .dimensions(panelRight - 80, this.height - 28, 70, BUTTON_HEIGHT)
                .build();
        addDrawableChild(closeButton);

        int labelW = 140;
        int fieldW = panelWidth - labelW - 30;
        int labelX = panelLeft + 10;
        int fieldX = panelLeft + 10 + labelW + 10;
        int rowY = settingsTop;

        addLabel(labelX, rowY, labelW, "Passives Enabled");
        passivesToggle = addToggle(fieldX, rowY, fieldW, passivesEnabled, v -> passivesEnabled = v);
        rowY += SETTINGS_ROW_HEIGHT;

        addLabel(labelX, rowY, labelW, "HUD Position");
        hudPosToggle = CyclingButtonWidget.builder(pos -> Text.literal(pos.name()), hudPosition)
            .values(java.util.Arrays.asList(GemLoadout.HudPosition.values()))
            .build(fieldX, rowY, fieldW, BUTTON_HEIGHT, Text.empty(), (b, v) -> hudPosition = v);
        addDrawableChild(hudPosToggle);
        rowY += SETTINGS_ROW_HEIGHT;

        addLabel(labelX, rowY, labelW, "Show Cooldowns");
        showCooldownsToggle = addToggle(fieldX, rowY, fieldW, showCooldowns, v -> showCooldowns = v);
        rowY += SETTINGS_ROW_HEIGHT;

        addLabel(labelX, rowY, labelW, "Show Energy");
        showEnergyToggle = addToggle(fieldX, rowY, fieldW, showEnergy, v -> showEnergy = v);
        rowY += SETTINGS_ROW_HEIGHT;

        addLabel(labelX, rowY, labelW, "Compact Mode");
        compactModeToggle = addToggle(fieldX, rowY, fieldW, compactMode, v -> compactMode = v);

        buildPresetButtons(panelRight);
    }

    private void buildPresetButtons(int panelRight) {
        loadButtons.clear();
        deleteButtons.clear();

        int maxPresets = Math.max(1, payload.maxPresets());
        for (int i = 0; i < maxPresets; i++) {
            int y = listTop + (i * ROW_HEIGHT);
            int index = i;

            ButtonWidget loadBtn = ButtonWidget.builder(Text.translatable("gems.screen.loadout_presets.load"), btn -> loadPreset(index))
                    .dimensions(loadX, y - 2, 60, BUTTON_HEIGHT)
                    .build();
            ButtonWidget deleteBtn = ButtonWidget.builder(Text.translatable("gems.screen.loadout_presets.delete"), btn -> deletePreset(index))
                    .dimensions(deleteX, y - 2, 50, BUTTON_HEIGHT)
                    .build();

            boolean hasPreset = index < payload.presets().size();
            loadBtn.active = hasPreset;
            deleteBtn.active = hasPreset;

            loadButtons.add(loadBtn);
            deleteButtons.add(deleteBtn);
            addDrawableChild(loadBtn);
            addDrawableChild(deleteBtn);
        }
    }

    private void savePreset() {
        if (!ClientGemState.isInitialized()) {
            return;
        }
        String name = nameField.getText();
        ClientPlayNetworking.send(new LoadoutSavePayload(
                payload.gem(),
                name == null ? "" : name,
                List.of(),
                passivesEnabled,
                hudPosition,
                showCooldowns,
                showEnergy,
                compactMode
        ));
        requestGem(payload.gem());
    }

    private void loadPreset(int index) {
        ClientPlayNetworking.send(new LoadoutLoadPayload(payload.gem(), index));
        requestGem(payload.gem());
    }

    private void deletePreset(int index) {
        ClientPlayNetworking.send(new LoadoutDeletePayload(payload.gem(), index));
        requestGem(payload.gem());
    }

    private void requestGem(GemId gem) {
        ClientPlayNetworking.send(new LoadoutOpenRequestPayload(gem));
    }

    private GemId offsetGem(int delta) {
        GemId[] gems = GemId.values();
        int index = 0;
        for (int i = 0; i < gems.length; i++) {
            if (gems[i] == payload.gem()) {
                index = i;
                break;
            }
        }
        int next = (index + delta + gems.length) % gems.length;
        return gems[next];
    }

    @Override
    public boolean keyPressed(KeyInput key) {
        if (nameField != null && nameField.keyPressed(key)) {
            return true;
        }
        return super.keyPressed(key);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        drawPanel(context, panelLeft, panelTop, panelRight, panelBottom);

        int centerX = this.width / 2;
        renderBase(context);

        String gemLabel = Text.translatable("gems.screen.loadout_presets.gem", payload.gem().name()).getString();
        context.drawTextWithShadow(this.textRenderer, gemLabel, (this.width / 2) - 20, 30, 0xA0A0A0);

        int maxPresets = Math.max(1, payload.maxPresets());
        for (int i = 0; i < maxPresets; i++) {
            String label = Text.translatable("gems.screen.loadout_presets.empty").getString();
            if (i < payload.presets().size()) {
                LoadoutScreenPayload.Preset preset = payload.presets().get(i);
                label = preset.name();
            }
            if (i == payload.activeIndex()) {
                label = label + " " + Text.translatable("gems.screen.loadout_presets.active").getString();
            }
            context.drawTextWithShadow(this.textRenderer, (i + 1) + ". " + label, labelX, listTop + (i * ROW_HEIGHT), 0xFFFFFF);
        }

    }

    private LoadoutScreenPayload.Preset getActivePreset() {
        if (payload.presets().isEmpty()) {
            return null;
        }
        int idx = Math.max(0, Math.min(payload.activeIndex(), payload.presets().size() - 1));
        return payload.presets().get(idx);
    }

    private void applyPresetToEditor(LoadoutScreenPayload.Preset preset) {
        passivesEnabled = preset.passivesEnabled();
        hudPosition = preset.hudPosition();
        showCooldowns = preset.showCooldowns();
        showEnergy = preset.showEnergy();
        compactMode = preset.compactMode();
    }

    private void addLabel(int x, int y, int w, String text) {
        ButtonWidget label = ButtonWidget.builder(Text.literal(text), b -> {})
                .dimensions(x, y, w, BUTTON_HEIGHT)
                .build();
        label.active = false;
        addDrawableChild(label);
    }

    private CyclingButtonWidget<Boolean> addToggle(int x, int y, int w, boolean initial, java.util.function.Consumer<Boolean> setter) {
        CyclingButtonWidget<Boolean> btn = CyclingButtonWidget.onOffBuilder(initial)
                .build(x, y, w, BUTTON_HEIGHT, Text.empty(), (b, v) -> setter.accept(v));
        addDrawableChild(btn);
        return btn;
    }
}
