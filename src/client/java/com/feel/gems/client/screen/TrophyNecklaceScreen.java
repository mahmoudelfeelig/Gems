package com.feel.gems.client.screen;

import static com.feel.gems.client.screen.GemsScreenConstants.*;

import com.feel.gems.net.TrophyNecklaceClaimPayload;
import com.feel.gems.net.TrophyNecklaceScreenPayload;
import com.feel.gems.net.TrophyNecklaceScreenPayload.OfferedEntry;
import com.feel.gems.net.TrophyNecklaceScreenPayload.StolenEntry;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

public final class TrophyNecklaceScreen extends GemsScreenBase {
    private enum Tab {
        OFFERED,
        STOLEN
    }

    private final String targetName;
    private final List<OfferedEntry> offered;
    private final List<StolenEntry> stolen;
    private final int maxStolenPassives;

    private Tab tab = Tab.OFFERED;
    private int page = 0;

    public TrophyNecklaceScreen(TrophyNecklaceScreenPayload payload) {
        super(Text.translatable("gems.screen.trophy_necklace.title"));
        this.targetName = payload.targetName();
        this.offered = new ArrayList<>(payload.offeredPassives());
        this.stolen = new ArrayList<>(payload.stolenPassives());
        this.maxStolenPassives = payload.maxStolenPassives();
    }

    @Override
    protected void init() {
        rebuild();
    }

    private void rebuild() {
        clearChildren();

        int centerX = width / 2;
        int panelW = panelWidth(width);
        int entryX = centerX - panelW / 2;

        int stolenCount = stolen.size();

        int tabY = tabY();
        addTabs(centerX, tabY);

        int totalPages = totalPages(currentEntries().size(), ENTRIES_PER_PAGE);
        page = clampPage(page, totalPages);

        int start = page * ENTRIES_PER_PAGE;
        int end = Math.min(start + ENTRIES_PER_PAGE, currentEntries().size());

        int entryY = tabY + TAB_HEIGHT + (SPACING * 2);
        if (tab == Tab.OFFERED) {
            for (int i = start; i < end; i++) {
                OfferedEntry entry = offered.get(i);
                boolean alreadyStolen = entry.alreadyStolen();
                String suffix = alreadyStolen
                        ? " (Already stolen)"
                        : " (Available)";
                Text label = Text.literal(entry.name() + suffix).formatted(alreadyStolen ? Formatting.DARK_GRAY : Formatting.WHITE);

                Identifier id = entry.id();
                ButtonWidget btn = ButtonWidget.builder(label, b -> steal(id))
                        .dimensions(entryX, entryY, panelW, ENTRY_HEIGHT)
                        .build();
                if (alreadyStolen || stolenCount >= maxStolenPassives) {
                    btn.active = false;
                }
                addDrawableChild(btn);
                entryY += ENTRY_HEIGHT + SPACING;
            }
        } else {
            for (int i = start; i < end; i++) {
                StolenEntry entry = stolen.get(i);
                boolean enabled = entry.enabled();
                String suffix = enabled ? " (Enabled)" : " (Disabled)";
                Text label = Text.literal(entry.name() + suffix).formatted(enabled ? Formatting.GREEN : Formatting.GRAY);

                Identifier id = entry.id();
                ButtonWidget btn = ButtonWidget.builder(label, b -> toggleEnabled(id, enabled))
                        .dimensions(entryX, entryY, panelW, ENTRY_HEIGHT)
                        .build();
                addDrawableChild(btn);
                entryY += ENTRY_HEIGHT + SPACING;
            }
        }

        // Pagination
        int navY = navButtonY(height);
        if (page > 0) {
            addDrawableChild(ButtonWidget.builder(Text.translatable("gems.screen.button.prev"), b -> changePage(-1))
                    .dimensions(centerX - NAV_BUTTON_WIDTH - SPACING, navY, NAV_BUTTON_WIDTH, BUTTON_HEIGHT)
                    .build());
        }
        if (page < totalPages - 1) {
            addDrawableChild(ButtonWidget.builder(Text.translatable("gems.screen.button.next"), b -> changePage(1))
                    .dimensions(centerX + SPACING, navY, NAV_BUTTON_WIDTH, BUTTON_HEIGHT)
                    .build());
        }

        addDrawableChild(ButtonWidget.builder(Text.translatable("gems.screen.button.close"), b -> close())
                .dimensions(centerX - CLOSE_BUTTON_WIDTH / 2, closeButtonY(height), CLOSE_BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
    }

    private void changePage(int delta) {
        page += delta;
        rebuild();
    }

    private void steal(Identifier passiveId) {
        ClientPlayNetworking.send(new TrophyNecklaceClaimPayload(passiveId, TrophyNecklaceClaimPayload.Action.STEAL));
    }

    private void toggleEnabled(Identifier passiveId, boolean enabled) {
        TrophyNecklaceClaimPayload.Action action = enabled
                ? TrophyNecklaceClaimPayload.Action.DISABLE
                : TrophyNecklaceClaimPayload.Action.ENABLE;
        ClientPlayNetworking.send(new TrophyNecklaceClaimPayload(passiveId, action));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        renderBase(context);
        context.drawCenteredTextWithShadow(textRenderer,
                Text.translatable("gems.screen.trophy_necklace.subtitle", targetName, maxStolenPassives),
                width / 2, SUBTITLE_Y, COLOR_GRAY);
        String status = "Available: " + offered.size() + " | Stolen: " + stolen.size() + "/" + maxStolenPassives;
        context.drawCenteredTextWithShadow(textRenderer, status, width / 2, SUBTITLE_Y + 12, COLOR_GRAY);
        String tabLabel = tab == Tab.OFFERED ? "Available passives" : "Stolen passives";
        context.drawCenteredTextWithShadow(textRenderer, tabLabel, width / 2, SUBTITLE_Y + 24, COLOR_GRAY);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private void addTabs(int centerX, int tabY) {
        ButtonWidget offeredTab = ButtonWidget.builder(Text.translatable("gems.screen.trophy_necklace.tab.offered"), btn -> {
            tab = Tab.OFFERED;
            page = 0;
            rebuild();
        }).dimensions(centerX - TAB_WIDTH - TAB_GAP, tabY, TAB_WIDTH, TAB_HEIGHT).build();
        ButtonWidget stolenTab = ButtonWidget.builder(Text.translatable("gems.screen.trophy_necklace.tab.stolen"), btn -> {
            tab = Tab.STOLEN;
            page = 0;
            rebuild();
        }).dimensions(centerX + TAB_GAP, tabY, TAB_WIDTH, TAB_HEIGHT).build();
        offeredTab.active = tab != Tab.OFFERED;
        stolenTab.active = tab != Tab.STOLEN;
        addDrawableChild(offeredTab);
        addDrawableChild(stolenTab);
    }

    private List<?> currentEntries() {
        return tab == Tab.OFFERED ? offered : stolen;
    }

    private int tabY() {
        return SUBTITLE_Y + 36;
    }
}

