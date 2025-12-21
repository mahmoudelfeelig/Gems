package com.feel.gems.client.screen;

import com.feel.gems.GemsMod;
import com.feel.gems.core.GemId;
import com.feel.gems.screen.TraderScreenHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;

public final class TraderScreen extends HandledScreen<TraderScreenHandler> {
    private static final Text TITLE = Text.translatable("screen." + GemsMod.MOD_ID + ".trader.title");

    private int panelLeft;
    private int panelRight;
    private int panelTop;
    private int panelBottom;

    public TraderScreen(TraderScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, TITLE);
    }

    @Override
    protected void init() {
        super.init();

        int buttonWidth = 140;
        int buttonHeight = 20;
        int gap = 8;
        int columns = 2;

        GemId[] gems = GemId.values();
        int rows = (gems.length + columns - 1) / columns;
        int gridWidth = columns * buttonWidth + (columns - 1) * gap;
        int gridHeight = rows * buttonHeight + (rows - 1) * gap;

        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int gridStartX = centerX - (gridWidth / 2);
        int gridStartY = Math.max(60, centerY - (gridHeight / 2) - 10);

        this.panelLeft = gridStartX - 14;
        this.panelRight = gridStartX + gridWidth + 14;
        this.panelTop = gridStartY - 32;
        this.panelBottom = gridStartY + gridHeight + 54;

        for (int i = 0; i < gems.length; i++) {
            GemId gemId = gems[i];
            int row = i / columns;
            int col = i % columns;

            int x = gridStartX + col * (buttonWidth + gap);
            int y = gridStartY + row * (buttonHeight + gap);

            Text label = Text.translatable("item." + GemsMod.MOD_ID + "." + gemId.name().toLowerCase() + "_gem");
            int buttonId = i;
            addDrawableChild(ButtonWidget.builder(label, btn -> select(buttonId)).dimensions(x, y, buttonWidth, buttonHeight).build());
        }

        int cancelWidth = gridWidth;
        addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"), btn -> close()).dimensions(gridStartX, gridStartY + gridHeight + 18, cancelWidth, buttonHeight).build());
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
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(context, mouseX, mouseY);

        int centerX = this.width / 2;
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, centerX, panelTop + 8, 0xFFFFFF);
        context.drawCenteredTextWithShadow(
            this.textRenderer,
            Text.literal("Pick a gem to activate. Trading for a new gem consumes a Gem Trader."),
            centerX,
            panelTop + 22,
            0xA0A0A0
        );
    }
}
