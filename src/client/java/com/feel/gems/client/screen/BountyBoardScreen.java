package com.feel.gems.client.screen;

import static com.feel.gems.client.screen.GemsScreenConstants.*;

import com.feel.gems.net.BountyBoardPayload;
import com.feel.gems.net.BountyPlacePayload;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class BountyBoardScreen extends GemsScreenBase {
    private enum Tab {
        ACTIVE,
        PLACE
    }

    private final List<BountyBoardPayload.Entry> bounties;
    private final List<BountyBoardPayload.PlayerEntry> players;
    private final UUID viewerId;
    private final int maxAdditionalHearts;
    private final int maxAdditionalEnergy;

    private Tab tab = Tab.ACTIVE;
    private int page = 0;
    private int playerPage = 0;
    private UUID selectedTarget;
    private int selectedHearts;
    private int selectedEnergy;

    public BountyBoardScreen(BountyBoardPayload payload) {
        super(Text.translatable("gems.screen.bounty.title"));
        this.viewerId = payload.viewerId();
        this.maxAdditionalHearts = payload.maxAdditionalHearts();
        this.maxAdditionalEnergy = payload.maxAdditionalEnergy();
        this.bounties = new ArrayList<>(payload.bounties());
        this.players = new ArrayList<>(payload.players());
        this.bounties.sort(Comparator.comparing((BountyBoardPayload.Entry e) -> e.targetName().toLowerCase(Locale.ROOT))
                .thenComparing(e -> e.placerName().toLowerCase(Locale.ROOT)));
        this.players.sort(Comparator.comparing(p -> p.name().toLowerCase(Locale.ROOT)));
    }

    @Override
    protected void init() {
        super.init();
        rebuild();
    }

    private void rebuild() {
        clearChildren();
        addTabs();

        if (tab == Tab.ACTIVE) {
            buildActiveTab();
        } else {
            buildPlaceTab();
        }

        addCloseButton();
    }

    private void addTabs() {
        int centerX = width / 2;
        int tabY = SUBTITLE_Y + 2;
        ButtonWidget active = ButtonWidget.builder(Text.translatable("gems.screen.bounty.tab.active"), btn -> {
            tab = Tab.ACTIVE;
            rebuild();
        }).dimensions(centerX - TAB_WIDTH - TAB_GAP, tabY, TAB_WIDTH, TAB_HEIGHT).build();
        ButtonWidget place = ButtonWidget.builder(Text.translatable("gems.screen.bounty.tab.place"), btn -> {
            tab = Tab.PLACE;
            rebuild();
        }).dimensions(centerX + TAB_GAP, tabY, TAB_WIDTH, TAB_HEIGHT).build();
        active.active = tab != Tab.ACTIVE;
        place.active = tab != Tab.PLACE;
        addDrawableChild(active);
        addDrawableChild(place);
    }

    private void buildActiveTab() {
        int panelW = panelWidth(width);
        int entryX = (width - panelW) / 2;
        int totalPg = totalPages(bounties.size(), ENTRIES_PER_PAGE);
        page = clampPage(page, totalPg);

        int start = page * ENTRIES_PER_PAGE;
        int end = Math.min(bounties.size(), start + ENTRIES_PER_PAGE);
        int y = CONTENT_START_Y;

        if (bounties.isEmpty()) {
            addDrawableChild(ButtonWidget.builder(Text.translatable("gems.screen.bounty.empty").formatted(Formatting.GRAY), btn -> {
            }).dimensions(entryX, y, panelW, BUTTON_HEIGHT).build()).active = false;
        } else {
            for (int i = start; i < end; i++) {
                BountyBoardPayload.Entry entry = bounties.get(i);
                String label = entry.targetName() + " | " + entry.placerName()
                        + " | " + entry.hearts() + "H " + entry.energy() + "E";
                int buttonY = y + (i - start) * (BUTTON_HEIGHT + SPACING);
                addDrawableChild(ButtonWidget.builder(Text.literal(label), btn -> {
                }).dimensions(entryX, buttonY, panelW, BUTTON_HEIGHT).build()).active = false;
            }
        }

        addNavButtons(bounties.size(), page, delta -> {
            page = clampPage(page + delta, totalPg);
            rebuild();
        });
    }

    private void buildPlaceTab() {
        int panelW = panelWidth(width);
        int entryX = (width - panelW) / 2;
        int totalPg = totalPages(players.size(), ENTRIES_PER_PAGE);
        playerPage = clampPage(playerPage, totalPg);

        int start = playerPage * ENTRIES_PER_PAGE;
        int end = Math.min(players.size(), start + ENTRIES_PER_PAGE);
        int y = CONTENT_START_Y;

        if (players.isEmpty()) {
            addDrawableChild(ButtonWidget.builder(Text.translatable("gems.screen.bounty.no_players").formatted(Formatting.GRAY), btn -> {
            }).dimensions(entryX, y, panelW, BUTTON_HEIGHT).build()).active = false;
        } else {
            for (int i = start; i < end; i++) {
                BountyBoardPayload.PlayerEntry entry = players.get(i);
                boolean selected = entry.uuid().equals(selectedTarget);
                Text label = Text.literal(entry.name()).formatted(selected ? Formatting.GOLD : Formatting.WHITE);
                int buttonY = y + (i - start) * (BUTTON_HEIGHT + SPACING);
                addDrawableChild(ButtonWidget.builder(label, btn -> selectTarget(entry.uuid()))
                        .dimensions(entryX, buttonY, panelW, BUTTON_HEIGHT)
                        .build());
            }
        }

        int controlsY = CONTENT_START_Y + ENTRIES_PER_PAGE * (BUTTON_HEIGHT + SPACING) + SPACING;
        addCounterRow(entryX, controlsY, Text.translatable("gems.screen.bounty.hearts"), true);
        addCounterRow(entryX, controlsY + BUTTON_HEIGHT + SPACING, Text.translatable("gems.screen.bounty.energy"), false);

        int placeY = controlsY + (BUTTON_HEIGHT + SPACING) * 2;
        ButtonWidget placeButton = ButtonWidget.builder(Text.translatable("gems.screen.bounty.place"), btn -> placeBounty())
                .dimensions(entryX, placeY, panelW, BUTTON_HEIGHT)
                .build();
        placeButton.active = selectedTarget != null && (selectedHearts > 0 || selectedEnergy > 0);
        addDrawableChild(placeButton);

        addNavButtons(players.size(), playerPage, delta -> {
            playerPage = clampPage(playerPage + delta, totalPg);
            rebuild();
        });
    }

    private void addNavButtons(int count, int currentPage, java.util.function.IntConsumer changePage) {
        int centerX = width / 2;
        int navY = navButtonY(height);
        ButtonWidget prev = ButtonWidget.builder(Text.translatable("gems.screen.button.prev"), btn -> changePage.accept(-1))
                .dimensions(centerX - NAV_BUTTON_WIDTH - SPACING, navY, NAV_BUTTON_WIDTH, BUTTON_HEIGHT)
                .build();
        ButtonWidget next = ButtonWidget.builder(Text.translatable("gems.screen.button.next"), btn -> changePage.accept(1))
                .dimensions(centerX + SPACING, navY, NAV_BUTTON_WIDTH, BUTTON_HEIGHT)
                .build();
        prev.active = currentPage > 0;
        next.active = (currentPage + 1) * ENTRIES_PER_PAGE < count;
        addDrawableChild(prev);
        addDrawableChild(next);
    }

    private void addCounterRow(int x, int y, Text label, boolean hearts) {
        int maxTotal = hearts ? maxHeartsTotal() : maxEnergyTotal();
        int minTotal = hearts ? baselineHearts() : baselineEnergy();
        int current = hearts ? selectedHearts : selectedEnergy;

        Text line = Text.literal(label.getString() + ": " + current + " / " + maxTotal)
                .formatted(Formatting.GRAY);
        addDrawableChild(ButtonWidget.builder(line, btn -> {
        }).dimensions(x, y, panelWidth(width), BUTTON_HEIGHT).build()).active = false;

        int btnX = x + panelWidth(width) - (BUTTON_HEIGHT * 2 + SPACING);
        ButtonWidget minus = ButtonWidget.builder(Text.literal("-"), btn -> adjust(hearts, -1))
                .dimensions(btnX, y, BUTTON_HEIGHT, BUTTON_HEIGHT)
                .build();
        ButtonWidget plus = ButtonWidget.builder(Text.literal("+"), btn -> adjust(hearts, 1))
                .dimensions(btnX + BUTTON_HEIGHT + SPACING, y, BUTTON_HEIGHT, BUTTON_HEIGHT)
                .build();
        minus.active = current > minTotal;
        plus.active = current < maxTotal;
        addDrawableChild(minus);
        addDrawableChild(plus);
    }

    private void selectTarget(UUID uuid) {
        selectedTarget = uuid;
        selectedHearts = baselineHearts();
        selectedEnergy = baselineEnergy();
        rebuild();
    }

    private void adjust(boolean hearts, int delta) {
        int min = hearts ? baselineHearts() : baselineEnergy();
        int max = hearts ? maxHeartsTotal() : maxEnergyTotal();
        if (hearts) {
            selectedHearts = Math.max(min, Math.min(max, selectedHearts + delta));
        } else {
            selectedEnergy = Math.max(min, Math.min(max, selectedEnergy + delta));
        }
        rebuild();
    }

    private int baselineHearts() {
        BountyBoardPayload.Entry entry = existingBounty();
        return entry == null ? 0 : entry.hearts();
    }

    private int baselineEnergy() {
        BountyBoardPayload.Entry entry = existingBounty();
        return entry == null ? 0 : entry.energy();
    }

    private int maxHeartsTotal() {
        return baselineHearts() + maxAdditionalHearts;
    }

    private int maxEnergyTotal() {
        return baselineEnergy() + maxAdditionalEnergy;
    }

    private BountyBoardPayload.Entry existingBounty() {
        if (selectedTarget == null) {
            return null;
        }
        for (BountyBoardPayload.Entry entry : bounties) {
            if (entry.targetId().equals(selectedTarget) && entry.placerId().equals(viewerId)) {
                return entry;
            }
        }
        return null;
    }

    private void placeBounty() {
        if (selectedTarget == null) {
            return;
        }
        ClientPlayNetworking.send(new BountyPlacePayload(selectedTarget, selectedHearts, selectedEnergy));
        close();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        renderBase(context);
        if (tab == Tab.ACTIVE && !bounties.isEmpty()) {
            int maxPage = totalPages(bounties.size(), ENTRIES_PER_PAGE);
            String pageText = "Page " + (page + 1) + " / " + maxPage;
            context.drawCenteredTextWithShadow(this.textRenderer, pageText, this.width / 2, SUBTITLE_Y + 22, COLOR_GRAY);
        }
        if (tab == Tab.PLACE && !players.isEmpty()) {
            int maxPage = totalPages(players.size(), ENTRIES_PER_PAGE);
            String pageText = "Page " + (playerPage + 1) + " / " + maxPage;
            context.drawCenteredTextWithShadow(this.textRenderer, pageText, this.width / 2, SUBTITLE_Y + 22, COLOR_GRAY);
        }
    }
}
