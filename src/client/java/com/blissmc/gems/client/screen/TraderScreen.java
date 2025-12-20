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

    public TraderScreen(TraderScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, TITLE);
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        int buttonWidth = 110;
        int buttonHeight = 20;
        int gap = 6;

        GemId[] gems = GemId.values();
        for (int i = 0; i < gems.length; i++) {
            GemId gemId = gems[i];

            int row = i / 2;
            int col = i % 2;

            int x = centerX + (col == 0 ? -(buttonWidth + gap) : gap);
            int y = centerY - 48 + row * (buttonHeight + 6);

            Text label = Text.translatable("item." + GemsMod.MOD_ID + "." + gemId.name().toLowerCase() + "_gem");
            int buttonId = i;
            addDrawableChild(ButtonWidget.builder(label, btn -> select(buttonId)).dimensions(x, y, buttonWidth, buttonHeight).build());
        }

        addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"), btn -> close()).dimensions(centerX - 55, centerY + 64, 110, 20).build());
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
        // Intentionally empty: this UI uses plain buttons without a container background texture.
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(context, mouseX, mouseY);

        int centerX = this.width / 2;
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, centerX, 18, 0xFFFFFF);
        context.drawCenteredTextWithShadow(
                this.textRenderer,
                Text.literal("Pick a gem to activate. Trading for a new gem consumes a Gem Trader."),
                centerX,
                34,
                0xA0A0A0
        );
    }
}
