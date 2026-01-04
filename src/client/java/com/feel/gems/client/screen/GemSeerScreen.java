package com.feel.gems.client.screen;

import com.feel.gems.GemsMod;
import com.feel.gems.core.GemId;
import com.feel.gems.screen.GemSeerScreenHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

/**
 * Client screen for the Gem Seer item.
 * Shows a list of all online players to select and view their gem info.
 */
public final class GemSeerScreen extends HandledScreen<GemSeerScreenHandler> {
    private static final Text TITLE = Text.literal("Gem Seer").formatted(Formatting.GOLD);
    private static final Text HINT = Text.literal("Select a player to view their gem info");
    
    private int panelLeft;
    private int panelRight;
    private int panelTop;
    private int panelBottom;
    private int gridStartX;
    private int gridStartY;
    private int titleY;
    private int hintY;

    public GemSeerScreen(GemSeerScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, TITLE);
    }

    @Override
    protected void init() {
        super.init();
        clearChildren();

        int buttonWidth = 180;
        int buttonHeight = 20;
        int gap = 4;
        int columns = 2;

        List<GemSeerScreenHandler.PlayerInfo> players = handler.getPlayerInfos();
        
        if (players.isEmpty()) {
            // No other players online
            titleY = this.height / 2 - 20;
            hintY = this.height / 2;
            
            panelLeft = this.width / 2 - 120;
            panelRight = this.width / 2 + 120;
            panelTop = titleY - 10;
            panelBottom = hintY + 30;
            
            addDrawableChild(ButtonWidget.builder(Text.literal("Close"), btn -> close())
                    .dimensions(this.width / 2 - 50, hintY + 40, 100, buttonHeight)
                    .build());
            return;
        }

        int rows = (players.size() + columns - 1) / columns;
        int gridWidth = columns * buttonWidth + (columns - 1) * gap;
        int gridHeight = rows * buttonHeight + (rows - 1) * gap;

        int centerX = this.width / 2;
        int centerY = this.height / 2;
        this.gridStartX = centerX - (gridWidth / 2);

        titleY = 16;
        hintY = titleY + this.textRenderer.fontHeight + 4;
        this.gridStartY = Math.max(hintY + this.textRenderer.fontHeight + 16, centerY - (gridHeight / 2));

        this.panelLeft = Math.max(8, gridStartX - 14);
        this.panelRight = Math.min(this.width - 8, gridStartX + gridWidth + 14);
        this.panelTop = Math.max(6, titleY - 10);
        this.panelBottom = Math.min(this.height - 6, gridStartY + gridHeight + 54);

        for (int i = 0; i < players.size(); i++) {
            GemSeerScreenHandler.PlayerInfo info = players.get(i);
            int row = i / columns;
            int col = i % columns;

            int x = this.gridStartX + col * (buttonWidth + gap);
            int y = this.gridStartY + row * (buttonHeight + gap);

            Text label = Text.literal(info.name()).formatted(Formatting.WHITE)
                    .append(Text.literal(" - ").formatted(Formatting.GRAY))
                    .append(Text.literal(GemSeerScreenHandler.formatGemName(info.activeGem()))
                            .formatted(GemSeerScreenHandler.gemColor(info.activeGem())));
            
            int buttonId = i;
            addDrawableChild(ButtonWidget.builder(label, btn -> select(buttonId))
                    .dimensions(x, y, buttonWidth, buttonHeight)
                    .build());
        }

        int closeWidth = Math.min(gridWidth, 200);
        addDrawableChild(ButtonWidget.builder(Text.literal("Close"), btn -> close())
                .dimensions(centerX - closeWidth / 2, this.gridStartY + gridHeight + 18, closeWidth, buttonHeight)
                .build());
    }

    private void select(int buttonId) {
        if (this.client == null || this.client.interactionManager == null) {
            return;
        }
        this.client.interactionManager.clickButton(this.handler.syncId, buttonId);
        // Don't close the screen - allow viewing multiple players
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
        context.drawCenteredTextWithShadow(this.textRenderer, TITLE, centerX, titleY, 0xFFFFFF);
        
        List<GemSeerScreenHandler.PlayerInfo> players = handler.getPlayerInfos();
        if (players.isEmpty()) {
            context.drawCenteredTextWithShadow(this.textRenderer, 
                    Text.literal("No other players online").formatted(Formatting.GRAY), 
                    centerX, hintY, 0xA0A0A0);
        } else {
            context.drawCenteredTextWithShadow(this.textRenderer, HINT, centerX, hintY, 0xA0A0A0);
        }
    }
}
