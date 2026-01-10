package com.feel.gems.client.screen;

import com.feel.gems.screen.GemSeerScreenHandler;
import java.util.ArrayList;
import java.util.List;
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

    private static final int BUTTON_HEIGHT = 20;
    private static final int ROW_EXTRA_HEIGHT = 12;
    private static final int ROW_GAP = 4;
    private static final int ROW_HEIGHT = BUTTON_HEIGHT + ROW_EXTRA_HEIGHT + ROW_GAP;

    private final List<Row> visibleRows = new ArrayList<>();
    private int page = 0;
    private int entriesPerPage = 6;

    private int panelLeft;
    private int panelRight;
    private int panelTop;
    private int panelBottom;
    private int titleY;
    private int hintY;
    private int listX;
    private int listY;
    private int listWidth;

    public GemSeerScreen(GemSeerScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, TITLE);
    }

    private record Row(GemSeerScreenHandler.PlayerInfo info, ButtonWidget button, int ownedLineY) {
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
        int panelWidth = Math.min(340, this.width - 32);

        titleY = 12;
        hintY = 28;

        panelLeft = centerX - (panelWidth / 2);
        panelRight = centerX + (panelWidth / 2);
        panelTop = 6;
        panelBottom = this.height - 6;

        int bottomY = this.height - 44;
        int listTop = 54;
        int availableListHeight = Math.max(ROW_HEIGHT, bottomY - listTop - 10);
        entriesPerPage = Math.max(1, Math.min(10, availableListHeight / ROW_HEIGHT));

        List<GemSeerScreenHandler.PlayerInfo> players = handler.getPlayerInfos();

        listX = panelLeft + 12;
        listWidth = panelWidth - 24;
        listY = listTop;

        if (players.isEmpty()) {
            addDrawableChild(ButtonWidget.builder(Text.translatable("gems.screen.gem_seer.no_players").formatted(Formatting.GRAY), btn -> {
            }).dimensions(listX, listY, listWidth, BUTTON_HEIGHT).build()).active = false;

            addDrawableChild(ButtonWidget.builder(Text.translatable("gems.screen.button.close"), btn -> close())
                    .dimensions(centerX - 70, bottomY + BUTTON_HEIGHT + 6, 140, BUTTON_HEIGHT)
                    .build());
            return;
        }

        int maxPage = Math.max(0, (players.size() - 1) / entriesPerPage);
        page = Math.max(0, Math.min(maxPage, page));

        int start = page * entriesPerPage;
        int end = Math.min(players.size(), start + entriesPerPage);

        int y = listY;
        for (int i = start; i < end; i++) {
            GemSeerScreenHandler.PlayerInfo info = players.get(i);

            Text label = Text.literal(info.name()).formatted(Formatting.WHITE)
                    .append(Text.literal(" - ").formatted(Formatting.GRAY))
                    .append(Text.literal(GemSeerScreenHandler.formatGemName(info.activeGem()))
                            .formatted(GemSeerScreenHandler.gemColor(info.activeGem())))
                    .append(Text.literal(" [" + info.energy() + "/10]").formatted(GemSeerScreenHandler.energyColor(info.energy())));

            int buttonId = i;
            ButtonWidget button = ButtonWidget.builder(label, btn -> select(buttonId))
                    .dimensions(listX, y, listWidth, BUTTON_HEIGHT)
                    .build();
            addDrawableChild(button);
            visibleRows.add(new Row(info, button, y + BUTTON_HEIGHT + 2));

            y += ROW_HEIGHT;
        }

        int smallWidth = 70;
        ButtonWidget prev = ButtonWidget.builder(Text.translatable("gems.screen.button.prev"), btn -> changePage(-1))
                .dimensions(centerX - smallWidth - 8, bottomY, smallWidth, BUTTON_HEIGHT)
                .build();
        ButtonWidget next = ButtonWidget.builder(Text.translatable("gems.screen.button.next"), btn -> changePage(1))
                .dimensions(centerX + 8, bottomY, smallWidth, BUTTON_HEIGHT)
                .build();
        prev.active = page > 0;
        next.active = (page + 1) * entriesPerPage < players.size();
        addDrawableChild(prev);
        addDrawableChild(next);

        addDrawableChild(ButtonWidget.builder(Text.translatable("gems.screen.button.close"), btn -> close())
                .dimensions(centerX - 70, bottomY + BUTTON_HEIGHT + 6, 140, BUTTON_HEIGHT)
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
        context.fill(panelLeft, panelTop, panelRight, panelBottom, 0xB0000000);
        context.fill(panelLeft, panelTop, panelRight, panelTop + 1, 0x33FFFFFF);
        context.fill(panelLeft, panelBottom - 1, panelRight, panelBottom, 0x22111111);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(context, mouseX, mouseY);

        int centerX = this.width / 2;
        context.drawCenteredTextWithShadow(this.textRenderer, TITLE, centerX, titleY, 0xFFFFFF);

        List<GemSeerScreenHandler.PlayerInfo> players = handler.getPlayerInfos();
        if (players.isEmpty()) {
            return;
        }

        context.drawCenteredTextWithShadow(this.textRenderer, HINT, centerX, hintY, 0xA0A0A0);
        int maxPage = Math.max(0, (players.size() - 1) / entriesPerPage) + 1;
        context.drawCenteredTextWithShadow(this.textRenderer, "Page " + (page + 1) + " / " + maxPage, centerX, hintY + 10, 0x808080);

        for (Row row : visibleRows) {
            String ownedRaw = formatOwnedGems(row.info);
            String ownedTruncated = truncateToWidth(ownedRaw, listWidth - 8);
            Text ownedLine = Text.translatable("gems.screen.gem_seer.owned_gems").formatted(Formatting.GRAY)
                    .append(Text.literal(ownedTruncated).formatted(Formatting.WHITE));
            context.drawTextWithShadow(this.textRenderer, ownedLine, listX + 4, row.ownedLineY, 0xA0A0A0);
        }

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

    private String truncateToWidth(String text, int maxWidth) {
        if (this.textRenderer.getWidth(text) <= maxWidth) {
            return text;
        }
        final String ellipsis = "...";
        int ellipsisWidth = this.textRenderer.getWidth(ellipsis);
        int allowed = Math.max(0, maxWidth - ellipsisWidth);

        int end = text.length();
        while (end > 0 && this.textRenderer.getWidth(text.substring(0, end)) > allowed) {
            end--;
        }
        return (end <= 0) ? ellipsis : text.substring(0, end) + ellipsis;
    }
}
