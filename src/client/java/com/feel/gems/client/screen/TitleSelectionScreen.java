package com.feel.gems.client.screen;

import static com.feel.gems.client.screen.GemsScreenConstants.*;

import com.feel.gems.core.GemId;
import com.feel.gems.net.TitleSelectionScreenPayload;
import com.feel.gems.net.TitleSelectionSelectPayload;
import com.feel.gems.screen.GemSeerScreenHandler;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class TitleSelectionScreen extends GemsScreenBase {
    private final List<TitleSelectionScreenPayload.Entry> entries;
    private Tab tab = Tab.UNLOCKED;
    private int page = 0;

    public TitleSelectionScreen(TitleSelectionScreenPayload payload) {
        super(Text.translatable("gems.screen.title_selection.title"));
        this.entries = new ArrayList<>(payload.entries());
        this.entries.sort(Comparator.comparingInt(TitleSelectionScreen::sortKey));
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
        int tabY = SUBTITLE_Y + 12;

        ButtonWidget unlockedTab = ButtonWidget.builder(Text.translatable("gems.screen.title_selection.tab.unlocked"), btn -> switchTab(Tab.UNLOCKED))
                .dimensions(centerX - TAB_WIDTH - TAB_GAP, tabY, TAB_WIDTH, TAB_HEIGHT)
                .build();
        ButtonWidget allTab = ButtonWidget.builder(Text.translatable("gems.screen.title_selection.tab.all"), btn -> switchTab(Tab.ALL))
                .dimensions(centerX + TAB_GAP, tabY, TAB_WIDTH, TAB_HEIGHT)
                .build();
        unlockedTab.active = tab != Tab.UNLOCKED;
        allTab.active = tab != Tab.ALL;
        addDrawableChild(unlockedTab);
        addDrawableChild(allTab);

        List<TitleSelectionScreenPayload.Entry> visible = visibleEntries();
        int totalPg = totalPages(visible.size(), ENTRIES_PER_PAGE);
        page = clampPage(page, totalPg);

        int start = page * ENTRIES_PER_PAGE;
        int end = Math.min(visible.size(), start + ENTRIES_PER_PAGE);
        int y = tabY + TAB_HEIGHT + SPACING * 2;

        if (visible.isEmpty()) {
            addDrawableChild(ButtonWidget.builder(Text.translatable("gems.screen.title_selection.none").formatted(Formatting.GRAY), btn -> {})
                    .dimensions(entryX, y, panelW, BUTTON_HEIGHT)
                    .build()).active = false;
        } else {
            for (int i = start; i < end; i++) {
                TitleSelectionScreenPayload.Entry entry = visible.get(i);
                int buttonY = y + (i - start) * (BUTTON_HEIGHT + SPACING);
                Text label = labelFor(entry);
                ButtonWidget button = addDrawableChild(ButtonWidget.builder(label, btn -> select(entry.id()))
                        .dimensions(entryX, buttonY, panelW, BUTTON_HEIGHT)
                        .build());
                if (entry.gemOrdinal() < 0) {
                    button.active = entry.unlocked();
                } else if (!entry.unlocked() && !entry.selected()) {
                    button.active = false;
                }
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
        next.active = (page + 1) * ENTRIES_PER_PAGE < visible.size();
        addDrawableChild(prev);
        addDrawableChild(next);

        int closeY = closeButtonY(height);
        addDrawableChild(ButtonWidget.builder(Text.translatable("gems.screen.button.clear"), btn -> clearSelection())
                .dimensions(centerX - NAV_BUTTON_WIDTH - SPACING, closeY, NAV_BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("gems.screen.button.close"), btn -> close())
                .dimensions(centerX + SPACING, closeY, NAV_BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
    }

    private Text labelFor(TitleSelectionScreenPayload.Entry entry) {
        if (entry.gemOrdinal() < 0) {
            MutableText label = Text.translatable("gems.screen.title_selection.general").formatted(Formatting.GRAY)
                    .append(Text.translatable(entry.displayKey()).formatted(Formatting.GOLD));
            if (entry.selected()) {
                label.append(Text.translatable("gems.screen.title_selection.selected").formatted(Formatting.GOLD));
            } else if (entry.unlocked()) {
                label.append(Text.translatable("gems.screen.title_selection.leader").formatted(Formatting.GOLD));
            } else {
                label.append(Text.translatable("gems.screen.title_selection.locked").formatted(Formatting.DARK_GRAY));
            }
            return label;
        }
        GemId gem = safeGem(entry.gemOrdinal());
        String gemName = GemSeerScreenHandler.formatGemName(gem);

        MutableText label = Text.literal(gemName + " - ").formatted(Formatting.GRAY)
                .append(Text.translatable(entry.displayKey()).formatted(GemSeerScreenHandler.gemColor(gem)))
                .append(Text.literal(" [" + entry.usage() + "/" + entry.threshold() + "]").formatted(Formatting.DARK_GRAY));

        if (entry.selected()) {
            label.append(Text.translatable("gems.screen.title_selection.selected").formatted(Formatting.GOLD));
        } else if (!entry.unlocked()) {
            label.append(Text.translatable("gems.screen.title_selection.locked").formatted(Formatting.DARK_GRAY));
        }

        if (entry.forcedSelected()) {
            label.append(Text.translatable("gems.screen.title_selection.admin").formatted(Formatting.DARK_GRAY));
        }

        return label;
    }

    private void changePage(int delta) {
        int maxPage = Math.max(0, (visibleEntries().size() - 1) / ENTRIES_PER_PAGE);
        page = Math.max(0, Math.min(maxPage, page + delta));
        rebuild();
    }

    private void select(String id) {
        ClientPlayNetworking.send(new TitleSelectionSelectPayload(id));
    }

    private void clearSelection() {
        ClientPlayNetworking.send(new TitleSelectionSelectPayload(""));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        renderBase(context);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.translatable("gems.screen.title_selection.subtitle"),
                this.width / 2, SUBTITLE_Y, COLOR_GRAY);

        if (!entries.isEmpty()) {
            int maxPage = totalPages(visibleEntries().size(), ENTRIES_PER_PAGE);
            String pageText = "Page " + (page + 1) + " / " + maxPage;
            int pageY = SUBTITLE_Y + 12 + TAB_HEIGHT + 2;
            context.drawCenteredTextWithShadow(this.textRenderer, pageText, this.width / 2, pageY, COLOR_GRAY);
        }
    }

    private void switchTab(Tab next) {
        if (tab == next) {
            return;
        }
        tab = next;
        page = 0;
        rebuild();
    }

    private List<TitleSelectionScreenPayload.Entry> visibleEntries() {
        if (tab == Tab.ALL) {
            return entries;
        }
        List<TitleSelectionScreenPayload.Entry> filtered = new ArrayList<>();
        for (TitleSelectionScreenPayload.Entry entry : entries) {
            if (entry.unlocked() || entry.selected() || entry.forcedSelected()) {
                filtered.add(entry);
            }
        }
        return filtered;
    }

    private static int sortKey(TitleSelectionScreenPayload.Entry entry) {
        int gem = Math.max(0, entry.gemOrdinal());
        int threshold = Math.max(0, entry.threshold());
        if (entry.gemOrdinal() < 0) {
            return -10000 + threshold;
        }
        return gem * 10000 + threshold;
    }

    private static GemId safeGem(int ordinal) {
        GemId[] values = GemId.values();
        if (ordinal < 0 || ordinal >= values.length) {
            return GemId.ASTRA;
        }
        return values[ordinal];
    }

    private enum Tab {
        UNLOCKED,
        ALL
    }
}
