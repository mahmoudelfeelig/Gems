package com.feel.gems.client.screen;

import static com.feel.gems.client.screen.GemsScreenConstants.*;

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
        int panelW = panelWidth(width);
        int entryX = centerX - panelW / 2;

        int totalPg = totalPages(entries.size(), ENTRIES_PER_PAGE);
        page = clampPage(page, totalPg);

        int start = page * ENTRIES_PER_PAGE;
        int end = Math.min(entries.size(), start + ENTRIES_PER_PAGE);
        int y = CONTENT_START_Y;

        if (entries.isEmpty()) {
            addDrawableChild(ButtonWidget.builder(Text.translatable("gems.screen.spy_observed.none").formatted(Formatting.GRAY), btn -> {})
                    .dimensions(entryX, y, panelW, BUTTON_HEIGHT)
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
                        .dimensions(entryX, y + (i - start) * (BUTTON_HEIGHT + SPACING), panelW, BUTTON_HEIGHT)
                        .build());
            }
        }

        int navY = navButtonY(height);

        ButtonWidget prev = ButtonWidget.builder(Text.translatable("gems.screen.button.prev"), btn -> changePage(-1))
                .dimensions(centerX - NAV_BUTTON_WIDTH - SPACING, navY, NAV_BUTTON_WIDTH, BUTTON_HEIGHT)
                .build();
        ButtonWidget next = ButtonWidget.builder(Text.translatable("gems.screen.button.next"), btn -> changePage(1))
                .dimensions(centerX + SPACING, navY, NAV_BUTTON_WIDTH, BUTTON_HEIGHT)
                .build();
        prev.active = page > 0;
        next.active = page < totalPg - 1;
        addDrawableChild(prev);
        addDrawableChild(next);

        addDrawableChild(ButtonWidget.builder(Text.translatable("gems.screen.button.close"), btn -> close())
                .dimensions(centerX - CLOSE_BUTTON_WIDTH / 2, closeButtonY(height), CLOSE_BUTTON_WIDTH, BUTTON_HEIGHT)
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
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, centerX, TITLE_Y, COLOR_WHITE);
        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.translatable("gems.screen.spy_observed.subtitle"),
                centerX, SUBTITLE_Y, COLOR_GRAY);
    }
}
