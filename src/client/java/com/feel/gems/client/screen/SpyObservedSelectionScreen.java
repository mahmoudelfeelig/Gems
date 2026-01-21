package com.feel.gems.client.screen;

import static com.feel.gems.client.screen.GemsScreenConstants.*;

import com.feel.gems.net.SpyObservedScreenPayload;
import com.feel.gems.net.SpyObservedSelectPayload;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

/**
 * Client UI for selecting observed Spy abilities for Echo/Steal.
 */
public final class SpyObservedSelectionScreen extends GemsScreenBase {
    private final List<SpyObservedScreenPayload.ObservedEntry> entries;
    private final List<SpyObservedScreenPayload.StolenEntry> stolen;
    private final Identifier selectedEchoId;
    private final Identifier selectedStealId;
    private final Identifier selectedStolenCastId;
    private int page = 0;
    private Tab tab = Tab.ECHO;

    public SpyObservedSelectionScreen(SpyObservedScreenPayload payload) {
        super(Text.translatable("gems.screen.spy_observed.title"));
        this.entries = new ArrayList<>(payload.observed());
        this.stolen = new ArrayList<>(payload.stolen());
        this.selectedEchoId = payload.selectedEchoId();
        this.selectedStealId = payload.selectedStealId();
        this.selectedStolenCastId = payload.selectedStolenCastId();
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

        List<EntryView> visible = entriesForTab();
        int totalPg = totalPages(visible.size(), ENTRIES_PER_PAGE);
        page = clampPage(page, totalPg);

        int start = page * ENTRIES_PER_PAGE;
        int end = Math.min(visible.size(), start + ENTRIES_PER_PAGE);
        int listY = CONTENT_START_Y + BUTTON_HEIGHT + SPACING;

        addTabButtons(centerX, entryX, panelW);

        if (visible.isEmpty()) {
            addDrawableChild(ButtonWidget.builder(Text.translatable("gems.screen.spy_observed.none").formatted(Formatting.GRAY), btn -> {})
                    .dimensions(entryX, listY + 8, panelW, BUTTON_HEIGHT)
                    .build()).active = false;
        } else {
            for (int i = start; i < end; i++) {
                EntryView view = visible.get(i);
                boolean isSelected = view.selected;
                String labelText = view.label;

                Formatting color = view.color;
                if (isSelected) {
                    color = Formatting.GOLD;
                }
                Text label = Text.literal(labelText).formatted(color);

                ButtonWidget button = addDrawableChild(ButtonWidget.builder(label, btn -> select(view.id))
                        .dimensions(entryX, listY + (i - start) * (BUTTON_HEIGHT + SPACING), panelW, BUTTON_HEIGHT)
                        .build());
                button.active = view.clickable;
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

    private void addTabButtons(int centerX, int entryX, int panelW) {
        int tabW = (panelW - (SPACING * 2)) / 3;
        int tabY = CONTENT_START_Y;

        addDrawableChild(ButtonWidget.builder(tabLabel(Tab.ECHO), btn -> switchTab(Tab.ECHO))
                .dimensions(entryX, tabY, tabW, BUTTON_HEIGHT)
                .build()).active = tab != Tab.ECHO;

        addDrawableChild(ButtonWidget.builder(tabLabel(Tab.STEAL), btn -> switchTab(Tab.STEAL))
                .dimensions(entryX + tabW + SPACING, tabY, tabW, BUTTON_HEIGHT)
                .build()).active = tab != Tab.STEAL;

        addDrawableChild(ButtonWidget.builder(tabLabel(Tab.STOLEN_CAST), btn -> switchTab(Tab.STOLEN_CAST))
                .dimensions(entryX + (tabW + SPACING) * 2, tabY, tabW, BUTTON_HEIGHT)
                .build()).active = tab != Tab.STOLEN_CAST;
    }

    private Text tabLabel(Tab target) {
        return switch (target) {
            case ECHO -> Text.translatable("gems.screen.spy_observed.tab.echo");
            case STEAL -> Text.translatable("gems.screen.spy_observed.tab.steal");
            case STOLEN_CAST -> Text.translatable("gems.screen.spy_observed.tab.stolen_cast");
        };
    }

    private void switchTab(Tab next) {
        if (tab == next) {
            return;
        }
        tab = next;
        page = 0;
        rebuild();
    }

    private List<EntryView> entriesForTab() {
        List<EntryView> out = new ArrayList<>();
        if (tab == Tab.STOLEN_CAST) {
            for (SpyObservedScreenPayload.StolenEntry entry : stolen) {
                boolean selected = entry.id().equals(selectedStolenCastId);
                out.add(new EntryView(entry.id(), entry.name(), Formatting.AQUA, true, selected));
            }
            return out;
        }

        for (SpyObservedScreenPayload.ObservedEntry entry : entries) {
            if (tab == Tab.ECHO && !entry.canEcho()) {
                continue;
            }
            if (tab == Tab.STEAL && !entry.canSteal()) {
                continue;
            }
            boolean selected = tab == Tab.ECHO
                    ? entry.id().equals(selectedEchoId)
                    : entry.id().equals(selectedStealId);
            String label = entry.name() + " x" + entry.count();
            Formatting color = tab == Tab.STEAL ? Formatting.GREEN : Formatting.AQUA;
            out.add(new EntryView(entry.id(), label, color, true, selected));
        }
        return out;
    }

    private void changePage(int delta) {
        int maxPage = Math.max(0, (entriesForTab().size() - 1) / ENTRIES_PER_PAGE);
        page = Math.max(0, Math.min(maxPage, page + delta));
        rebuild();
    }

    private void select(Identifier id) {
        int tabId = switch (tab) {
            case ECHO -> SpyObservedSelectPayload.TAB_ECHO;
            case STEAL -> SpyObservedSelectPayload.TAB_STEAL;
            case STOLEN_CAST -> SpyObservedSelectPayload.TAB_STOLEN_CAST;
        };
        ClientPlayNetworking.send(new SpyObservedSelectPayload(tabId, id));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        int centerX = this.width / 2;
        renderBase(context);
        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.translatable("gems.screen.spy_observed.subtitle"),
                centerX, SUBTITLE_Y, COLOR_GRAY);
    }

    private enum Tab {
        ECHO,
        STEAL,
        STOLEN_CAST
    }

    private record EntryView(Identifier id, String label, Formatting color, boolean clickable, boolean selected) {
    }
}
