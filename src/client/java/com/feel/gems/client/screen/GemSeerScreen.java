package com.feel.gems.client.screen;

import java.util.ArrayList;
import java.util.List;

import static com.feel.gems.client.screen.GemsScreenConstants.BUTTON_HEIGHT;
import static com.feel.gems.client.screen.GemsScreenConstants.CLOSE_BUTTON_WIDTH;
import static com.feel.gems.client.screen.GemsScreenConstants.COLOR_GRAY;
import static com.feel.gems.client.screen.GemsScreenConstants.COLOR_WHITE;
import static com.feel.gems.client.screen.GemsScreenConstants.CONTENT_START_Y;
import static com.feel.gems.client.screen.GemsScreenConstants.ENTRIES_PER_PAGE;
import static com.feel.gems.client.screen.GemsScreenConstants.NAV_BUTTON_WIDTH;
import static com.feel.gems.client.screen.GemsScreenConstants.SPACING;
import static com.feel.gems.client.screen.GemsScreenConstants.SUBTITLE_Y;
import static com.feel.gems.client.screen.GemsScreenConstants.TITLE_Y;
import static com.feel.gems.client.screen.GemsScreenConstants.closeButtonY;
import static com.feel.gems.client.screen.GemsScreenConstants.navButtonY;
import static com.feel.gems.client.screen.GemsScreenConstants.panelWidth;
import com.feel.gems.screen.GemSeerScreenHandler;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Client screen for the Gem Seer item.
 * Shows a list of online players with their active and owned gems.
 */
public final class GemSeerScreen extends HandledScreen<GemSeerScreenHandler> {
    private static final Text TITLE = Text.translatable("gems.screen.gem_seer.title").formatted(Formatting.GOLD);
    private static final Text HINT = Text.translatable("gems.screen.gem_seer.hint");
    private static final int ROW_HEIGHT = BUTTON_HEIGHT + SPACING;

    private final List<Row> visibleRows = new ArrayList<>();
    private int page = 0;
    private int entriesPerPage = ENTRIES_PER_PAGE;

    public GemSeerScreen(GemSeerScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, TITLE);
    }

    private record Row(GemSeerScreenHandler.PlayerInfo info, ButtonWidget button) {
    }

    @Override
    protected void init() {
        super.init();
        rebuild();
    }

    private void rebuild() {
        clearChildren();
        visibleRows.clear();

        int centerX = this.width / 2;
        int panelW = panelWidth(this.width);
        int entryX = centerX - panelW / 2;
        int listY = CONTENT_START_Y;
        entriesPerPage = ENTRIES_PER_PAGE;

        List<GemSeerScreenHandler.PlayerInfo> players = handler.getPlayerInfos();
        int maxPage = players.isEmpty() ? 0 : Math.max(0, (players.size() - 1) / entriesPerPage);
        page = Math.max(0, Math.min(maxPage, page));

        if (players.isEmpty()) {
            addDrawableChild(ButtonWidget.builder(Text.translatable("gems.screen.gem_seer.no_players").formatted(Formatting.GRAY), btn -> {
            }).dimensions(entryX, listY, panelW, BUTTON_HEIGHT).build()).active = false;
        } else {
            int start = page * entriesPerPage;
            int end = Math.min(players.size(), start + entriesPerPage);

            int y = listY;
            for (int i = start; i < end; i++) {
                GemSeerScreenHandler.PlayerInfo info = players.get(i);

                Text label = Text.literal(info.name()).formatted(info.online() ? Formatting.GREEN : Formatting.GRAY);

                int buttonId = i;
                ButtonWidget button = ButtonWidget.builder(label, btn -> select(buttonId))
                        .dimensions(entryX, y, panelW, BUTTON_HEIGHT)
                        .build();
                addDrawableChild(button);
                visibleRows.add(new Row(info, button));

                y += ROW_HEIGHT;
            }
        }

        int navY = navButtonY(this.height);
        ButtonWidget prev = ButtonWidget.builder(Text.translatable("gems.screen.button.prev"), btn -> changePage(-1))
                .dimensions(centerX - NAV_BUTTON_WIDTH - SPACING, navY, NAV_BUTTON_WIDTH, BUTTON_HEIGHT)
                .build();
        ButtonWidget next = ButtonWidget.builder(Text.translatable("gems.screen.button.next"), btn -> changePage(1))
                .dimensions(centerX + SPACING, navY, NAV_BUTTON_WIDTH, BUTTON_HEIGHT)
                .build();
        prev.active = page > 0;
        next.active = (page + 1) * entriesPerPage < players.size();
        addDrawableChild(prev);
        addDrawableChild(next);

        addDrawableChild(ButtonWidget.builder(Text.translatable("gems.screen.button.close"), btn -> close())
                .dimensions(centerX - CLOSE_BUTTON_WIDTH / 2, closeButtonY(this.height), CLOSE_BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
    }

    private void select(int buttonId) {
        if (this.client == null || this.client.interactionManager == null) {
            return;
        }
        this.client.interactionManager.clickButton(this.handler.syncId, buttonId);
    }

    private void changePage(int delta) {
        List<GemSeerScreenHandler.PlayerInfo> players = handler.getPlayerInfos();
        if (players.isEmpty()) {
            return;
        }
        int maxPage = Math.max(0, (players.size() - 1) / entriesPerPage);
        page = Math.max(0, Math.min(maxPage, page + delta));
        rebuild();
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        // No-op: we render our own title/hint.
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        // No custom panel background; keep the same transparent look as Tracker Compass.
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(context, mouseX, mouseY);

        int centerX = this.width / 2;
        context.drawCenteredTextWithShadow(this.textRenderer, TITLE, centerX, TITLE_Y, COLOR_WHITE);

        List<GemSeerScreenHandler.PlayerInfo> players = handler.getPlayerInfos();
        if (players.isEmpty()) {
            return;
        }

        context.drawCenteredTextWithShadow(this.textRenderer, HINT, centerX, SUBTITLE_Y, COLOR_GRAY);

        Row hovered = getHoveredRow(mouseX, mouseY);
        if (hovered != null) {
            context.drawTooltip(this.textRenderer, tooltipFor(hovered.info), mouseX, mouseY);
        }
    }

    private Row getHoveredRow(int mouseX, int mouseY) {
        for (Row row : visibleRows) {
            if (row.button.isMouseOver(mouseX, mouseY)) {
                return row;
            }
        }
        return null;
    }

    private List<Text> tooltipFor(GemSeerScreenHandler.PlayerInfo info) {
        List<Text> tooltip = new ArrayList<>();
        tooltip.add(Text.literal(info.name()).formatted(Formatting.WHITE));
        tooltip.add(Text.translatable("gems.screen.gem_seer.active_gem").formatted(Formatting.GRAY)
                .append(Text.literal(GemSeerScreenHandler.formatGemName(info.activeGem()))
                        .formatted(GemSeerScreenHandler.gemColor(info.activeGem()))));
        tooltip.add(Text.translatable("gems.screen.gem_seer.energy").formatted(Formatting.GRAY)
                .append(Text.literal("[" + info.energy() + "/10]").formatted(GemSeerScreenHandler.energyColor(info.energy()))));
        tooltip.add(Text.translatable("gems.screen.gem_seer.owned_gems").formatted(Formatting.GRAY)
                .append(Text.literal(formatOwnedGems(info)).formatted(Formatting.WHITE)));
        return tooltip;
    }

    private static String formatOwnedGems(GemSeerScreenHandler.PlayerInfo info) {
        if (info.ownedGems() == null || info.ownedGems().isEmpty()) {
            return "None";
        }
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (var gem : info.ownedGems()) {
            if (!first) sb.append(", ");
            sb.append(GemSeerScreenHandler.formatGemName(gem));
            first = false;
        }
        return sb.toString();
    }

}
