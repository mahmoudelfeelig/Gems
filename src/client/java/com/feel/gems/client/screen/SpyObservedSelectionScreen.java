package com.feel.gems.client.screen;

import com.feel.gems.net.SpyObservedScreenPayload;
import com.feel.gems.net.SpyObservedSelectPayload;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

/**
 * Client UI for selecting observed Spy abilities for Echo/Steal.
 */
public final class SpyObservedSelectionScreen extends Screen {
    private static final int ENTRIES_PER_PAGE = 8;

    private final List<SpyObservedScreenPayload.ObservedEntry> entries;
    private final Identifier selectedId;
    private int page = 0;

    public SpyObservedSelectionScreen(SpyObservedScreenPayload payload) {
        super(Text.translatable("gems.screen.spy_observed.title"));
        this.entries = new ArrayList<>(payload.observed());
        this.selectedId = payload.selectedId();
    }

    @Override
    protected void init() {
        super.init();
        rebuild();
    }

    private void rebuild() {
        clearChildren();

        int centerX = this.width / 2;
        int topY = 32;
        int panelWidth = Math.min(300, this.width - 32);
        int buttonHeight = 20;
        int spacing = 4;

        int start = page * ENTRIES_PER_PAGE;
        int end = Math.min(entries.size(), start + ENTRIES_PER_PAGE);
        int y = topY + 24;

        if (entries.isEmpty()) {
            addDrawableChild(ButtonWidget.builder(Text.translatable("gems.screen.spy_observed.none").formatted(Formatting.GRAY), btn -> {})
                    .dimensions(centerX - (panelWidth / 2), y, panelWidth, buttonHeight)
                    .build()).active = false;
        } else {
            for (int i = start; i < end; i++) {
                SpyObservedScreenPayload.ObservedEntry entry = entries.get(i);
                boolean isSelected = entry.id().equals(selectedId);
                String status = statusLabel(entry);
                String labelText = entry.name() + " x" + entry.count() + status;

                Formatting color = entry.canSteal() ? Formatting.GREEN : (entry.canEcho() ? Formatting.AQUA : Formatting.GRAY);
                if (isSelected) {
                    labelText = "\u2714 " + labelText;
                }
                Text label = Text.literal(labelText).formatted(color);

                addDrawableChild(ButtonWidget.builder(label, btn -> select(entry.id()))
                        .dimensions(centerX - (panelWidth / 2), y + (i - start) * (buttonHeight + spacing), panelWidth, buttonHeight)
                        .build());
            }
        }

        int bottomY = this.height - 64;
        int smallWidth = 70;
        int maxPage = Math.max(0, (entries.size() - 1) / ENTRIES_PER_PAGE);

        ButtonWidget prev = ButtonWidget.builder(Text.translatable("gems.screen.button.prev"), btn -> changePage(-1))
                .dimensions(centerX - smallWidth - 8, bottomY, smallWidth, buttonHeight)
                .build();
        ButtonWidget next = ButtonWidget.builder(Text.translatable("gems.screen.button.next"), btn -> changePage(1))
                .dimensions(centerX + 8, bottomY, smallWidth, buttonHeight)
                .build();
        prev.active = page > 0;
        next.active = page < maxPage;
        addDrawableChild(prev);
        addDrawableChild(next);

        addDrawableChild(ButtonWidget.builder(Text.translatable("gems.screen.button.close"), btn -> close())
                .dimensions(centerX - 50, bottomY + buttonHeight + 6, 100, buttonHeight)
                .build());
    }

    private String statusLabel(SpyObservedScreenPayload.ObservedEntry entry) {
        if (entry.canEcho() && entry.canSteal()) {
            return " [Echo+Steal]";
        }
        if (entry.canSteal()) {
            return " [Steal]";
        }
        if (entry.canEcho()) {
            return " [Echo]";
        }
        return "";
    }

    private void changePage(int delta) {
        int maxPage = Math.max(0, (entries.size() - 1) / ENTRIES_PER_PAGE);
        page = Math.max(0, Math.min(maxPage, page + delta));
        rebuild();
    }

    private void select(Identifier id) {
        ClientPlayNetworking.send(new SpyObservedSelectPayload(id));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        int centerX = this.width / 2;
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, centerX, 12, 0xFFFFFF);
        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.translatable("gems.screen.spy_observed.subtitle"),
                centerX, 22, 0xA0A0A0);
    }
}
