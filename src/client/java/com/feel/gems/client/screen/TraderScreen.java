package com.feel.gems.client.screen;

import com.feel.gems.GemsMod;
import com.feel.gems.client.ClientDisables;
import com.feel.gems.core.GemId;
import com.feel.gems.screen.TraderScreenHandler;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;




public final class TraderScreen extends HandledScreen<TraderScreenHandler> {
    private static final Text TITLE_TRADE = Text.translatable("screen." + GemsMod.MOD_ID + ".gem_trader.title");
    private static final Text TITLE_PURCHASE = Text.translatable("screen." + GemsMod.MOD_ID + ".purchase.title");
    private static final Text HINT_TRADE = Text.translatable("screen." + GemsMod.MOD_ID + ".gem_trader.hint");
    private static final Text HINT_PURCHASE = Text.translatable("screen." + GemsMod.MOD_ID + ".purchase.hint");

    private int panelLeft;
    private int panelRight;
    private int panelTop;
    private int panelBottom;
    private int gridStartX;
    private int gridStartY;
    private int titleY;
    private int hintY;
    private java.util.List<net.minecraft.text.OrderedText> hintLines = java.util.List.of();

    private List<GemId> visibleGems = List.of();

    public TraderScreen(TraderScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, TITLE_TRADE);
    }

    @Override
    protected void init() {
        super.init();
        clearChildren();

        int buttonWidth = 140;
        int buttonHeight = 20;
        int gap = 8;
        int columns = 2;

        visibleGems = computeVisibleGems();
        int rows = (visibleGems.size() + columns - 1) / columns;
        int gridWidth = columns * buttonWidth + (columns - 1) * gap;
        int gridHeight = rows * buttonHeight + (rows - 1) * gap;

        int centerX = this.width / 2;
        int centerY = this.height / 2;
        this.gridStartX = centerX - (gridWidth / 2);

        int maxTextWidth = Math.min(gridWidth + 40, this.width - 32);
        hintLines = this.textRenderer.wrapLines(hintText(), maxTextWidth);
        int hintHeight = hintLines.size() * this.textRenderer.fontHeight;
        titleY = 12;
        hintY = titleY + this.textRenderer.fontHeight + 4;
        int textBlockHeight = (hintY - titleY) + hintHeight;

        this.gridStartY = Math.max(titleY + textBlockHeight + 12, centerY - (gridHeight / 2));

        this.panelLeft = Math.max(8, gridStartX - 14);
        this.panelRight = Math.min(this.width - 8, gridStartX + gridWidth + 14);
        this.panelTop = Math.max(6, titleY - 6);
        this.panelBottom = Math.min(this.height - 6, gridStartY + gridHeight + 54);

        for (int i = 0; i < visibleGems.size(); i++) {
            GemId gemId = visibleGems.get(i);
            int row = i / columns;
            int col = i % columns;

            int x = this.gridStartX + col * (buttonWidth + gap);
            int y = this.gridStartY + row * (buttonHeight + gap);

            Text label = Text.translatable("item." + GemsMod.MOD_ID + "." + gemId.name().toLowerCase() + "_gem");
            int buttonId = gemId.ordinal();
            addDrawableChild(ButtonWidget.builder(label, btn -> select(buttonId)).dimensions(x, y, buttonWidth, buttonHeight).build());
        }

        int cancelWidth = gridWidth;
        addDrawableChild(ButtonWidget.builder(Text.translatable("gems.screen.button.cancel"), btn -> close()).dimensions(this.gridStartX, this.gridStartY + gridHeight + 18, cancelWidth, buttonHeight).build());
    }

    private List<GemId> computeVisibleGems() {
        List<GemId> out = new ArrayList<>();
        for (GemId gem : GemId.values()) {
            if (ClientDisables.isInitialized() && ClientDisables.isGemDisabled(gem)) {
                continue;
            }
            out.add(gem);
        }
        return List.copyOf(out);
    }

    private void select(int buttonId) {
        if (this.client == null || this.client.interactionManager == null) {
            return;
        }
        this.client.interactionManager.clickButton(this.handler.syncId, buttonId);
        close();
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        // No-op: we render our own title/hint.
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        context.fill(panelLeft, panelTop, panelRight, panelBottom, 0xB0000000);
        context.fill(panelLeft, panelTop, panelRight, panelTop + 1, 0x33FFFFFF);
        context.fill(panelLeft, panelBottom - 1, panelRight, panelBottom, 0x22111111);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(context, mouseX, mouseY);

        int centerX = this.width / 2;
        context.drawCenteredTextWithShadow(this.textRenderer, getTitle(), centerX, titleY, 0xFFFFFFFF);
        int textY = hintY;
        for (var line : hintLines) {
            context.drawCenteredTextWithShadow(this.textRenderer, line, centerX, textY, 0xA0A0A0);
            textY += this.textRenderer.fontHeight;
        }
    }

    @Override
    public Text getTitle() {
        return handler.isPurchaseMode() ? TITLE_PURCHASE : TITLE_TRADE;
    }

    private Text hintText() {
        return handler.isPurchaseMode() ? HINT_PURCHASE : HINT_TRADE;
    }
}
