package com.feel.gems.client.screen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import static com.feel.gems.client.screen.GemsScreenConstants.*;

/**
 * Base screen for consistent Gems UI layout.
 */
public abstract class GemsScreenBase extends Screen {
    protected GemsScreenBase(Text title) {
        super(title);
    }

    protected void renderBase(DrawContext context) {
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, TITLE_Y, COLOR_WHITE);
    }

    protected ButtonWidget addCloseButton() {
        int x = (this.width - CLOSE_BUTTON_WIDTH) / 2;
        int y = closeButtonY(this.height);
        return addDrawableChild(ButtonWidget.builder(Text.translatable("gems.screen.button.close"), btn -> close())
                .dimensions(x, y, CLOSE_BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
    }

    protected void drawPanel(DrawContext context, int left, int top, int right, int bottom) {
        context.fill(left, top, right, bottom, COLOR_PANEL_BG);
        context.fill(left, top, right, top + 1, COLOR_PANEL_BORDER);
        context.fill(left, bottom - 1, right, bottom, COLOR_PANEL_BORDER);
        context.fill(left, top, left + 1, bottom, COLOR_PANEL_BORDER);
        context.fill(right - 1, top, right, bottom, COLOR_PANEL_BORDER);
    }
}
