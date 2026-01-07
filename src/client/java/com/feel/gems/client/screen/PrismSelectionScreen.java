package com.feel.gems.client.screen;

import com.feel.gems.net.PrismSelectionClaimPayload;
import com.feel.gems.net.PrismSelectionScreenPayload;
import com.feel.gems.net.PrismSelectionScreenPayload.PowerEntry;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * Client UI for Prism gem ability/passive selection at energy 10.
 */
public final class PrismSelectionScreen extends Screen {
    private static final int ENTRIES_PER_PAGE = 8;
    private static final int TAB_WIDTH = 90;
    private static final int TAB_HEIGHT = 20;

    private final List<PowerEntry> gemAbilities;
    private final List<PowerEntry> bonusAbilities;
    private final List<PowerEntry> gemPassives;
    private final List<PowerEntry> bonusPassives;
    private final List<Identifier> selectedGemAbilities;
    private final List<Identifier> selectedBonusAbilities;
    private final List<Identifier> selectedGemPassives;
    private final List<Identifier> selectedBonusPassives;
    private final int maxGemAbilities;
    private final int maxBonusAbilities;
    private final int maxGemPassives;
    private final int maxBonusPassives;

    private int currentTab = 0; // 0=gem abilities, 1=bonus abilities, 2=gem passives, 3=bonus passives
    private int page = 0;

    private ButtonWidget[] tabButtons;

    public PrismSelectionScreen(PrismSelectionScreenPayload payload) {
        super(Text.translatable("gems.prism.screen.title"));
        this.gemAbilities = payload.gemAbilities();
        this.bonusAbilities = payload.bonusAbilities();
        this.gemPassives = payload.gemPassives();
        this.bonusPassives = payload.bonusPassives();
        this.selectedGemAbilities = new ArrayList<>(payload.selectedGemAbilities());
        this.selectedBonusAbilities = new ArrayList<>(payload.selectedBonusAbilities());
        this.selectedGemPassives = new ArrayList<>(payload.selectedGemPassives());
        this.selectedBonusPassives = new ArrayList<>(payload.selectedBonusPassives());
        this.maxGemAbilities = payload.maxGemAbilities();
        this.maxBonusAbilities = payload.maxBonusAbilities();
        this.maxGemPassives = payload.maxGemPassives();
        this.maxBonusPassives = payload.maxBonusPassives();
    }

    @Override
    protected void init() {
        rebuild();
    }

    private void rebuild() {
        clearChildren();

        int centerX = width / 2;
        int startY = 30;

        // Tab buttons
        tabButtons = new ButtonWidget[4];
        int tabX = centerX - (TAB_WIDTH * 4 + 12) / 2;

        String[] tabLabels = {"Gem Abilities", "Bonus Abilities", "Gem Passives", "Bonus Passives"};
        for (int i = 0; i < 4; i++) {
            final int tabIdx = i;
            String label = tabLabels[i];
            if (i == 0) label += " (" + selectedGemAbilities.size() + "/" + maxGemAbilities + ")";
            else if (i == 1) label += " (" + selectedBonusAbilities.size() + "/" + maxBonusAbilities + ")";
            else if (i == 2) label += " (" + selectedGemPassives.size() + "/" + maxGemPassives + ")";
            else label += " (" + selectedBonusPassives.size() + "/" + maxBonusPassives + ")";

            tabButtons[i] = ButtonWidget.builder(Text.literal(label), b -> switchTab(tabIdx))
                    .dimensions(tabX + i * (TAB_WIDTH + 4), startY, TAB_WIDTH, TAB_HEIGHT)
                    .build();
            addDrawableChild(tabButtons[i]);
        }

        // Update tab button appearance
        for (int i = 0; i < 4; i++) {
            tabButtons[i].active = (i != currentTab);
        }

        // Get current list
        List<PowerEntry> entries = getCurrentEntries();
        List<Identifier> selected = getCurrentSelected();

        int totalPages = Math.max(1, (entries.size() + ENTRIES_PER_PAGE - 1) / ENTRIES_PER_PAGE);
        if (page >= totalPages) page = totalPages - 1;
        if (page < 0) page = 0;

        int entryY = startY + TAB_HEIGHT + 20;
        int entryWidth = 300;
        int entryHeight = 24;
        int entryX = centerX - entryWidth / 2;

        int start = page * ENTRIES_PER_PAGE;
        int end = Math.min(start + ENTRIES_PER_PAGE, entries.size());

        for (int i = start; i < end; i++) {
            PowerEntry entry = entries.get(i);
            boolean isSelected = selected.contains(entry.id());

            String buttonText = (isSelected ? "\u2714 " : "") + entry.name() + " (" + entry.sourceName() + ")";

            final Identifier entryId = entry.id();
            ButtonWidget entryButton = ButtonWidget.builder(
                    Text.literal(buttonText).formatted(isSelected ? Formatting.GREEN : Formatting.WHITE),
                    b -> toggleEntry(entryId)
            ).dimensions(entryX, entryY, entryWidth, entryHeight).build();

            addDrawableChild(entryButton);
            entryY += entryHeight + 4;
        }

        // Pagination
        int pageY = height - 40;
        if (page > 0) {
            addDrawableChild(ButtonWidget.builder(Text.literal("<< Prev"), b -> changePage(-1))
                    .dimensions(centerX - 110, pageY, 100, 20)
                    .build());
        }
        if (page < totalPages - 1) {
            addDrawableChild(ButtonWidget.builder(Text.literal("Next >>"), b -> changePage(1))
                    .dimensions(centerX + 10, pageY, 100, 20)
                    .build());
        }

        // Close button
        addDrawableChild(ButtonWidget.builder(Text.literal("Close"), b -> close())
                .dimensions(centerX - 50, height - 25, 100, 20)
                .build());
    }

    private List<PowerEntry> getCurrentEntries() {
        return switch (currentTab) {
            case 0 -> gemAbilities;
            case 1 -> bonusAbilities;
            case 2 -> gemPassives;
            case 3 -> bonusPassives;
            default -> List.of();
        };
    }

    private List<Identifier> getCurrentSelected() {
        return switch (currentTab) {
            case 0 -> selectedGemAbilities;
            case 1 -> selectedBonusAbilities;
            case 2 -> selectedGemPassives;
            case 3 -> selectedBonusPassives;
            default -> List.of();
        };
    }

    private int getMaxForCurrentTab() {
        return switch (currentTab) {
            case 0 -> maxGemAbilities;
            case 1 -> maxBonusAbilities;
            case 2 -> maxGemPassives;
            case 3 -> maxBonusPassives;
            default -> 0;
        };
    }

    private void switchTab(int tab) {
        currentTab = tab;
        page = 0;
        rebuild();
    }

    private void changePage(int delta) {
        page += delta;
        rebuild();
    }

    private void toggleEntry(Identifier entryId) {
        List<Identifier> selected = getCurrentSelected();
        boolean isSelected = selected.contains(entryId);
        boolean isAbility = currentTab < 2;
        boolean isBonus = currentTab == 1 || currentTab == 3;

        if (isSelected) {
            // Release
            selected.remove(entryId);
            ClientPlayNetworking.send(new PrismSelectionClaimPayload(entryId, isAbility, isBonus, false));
        } else {
            // Claim - check limit
            if (selected.size() >= getMaxForCurrentTab()) {
                return;
            }
            selected.add(entryId);
            ClientPlayNetworking.send(new PrismSelectionClaimPayload(entryId, isAbility, isBonus, true));
        }
        rebuild();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        // Title
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 10, 0xFFFFFF);

        // Page indicator
        List<PowerEntry> entries = getCurrentEntries();
        int totalPages = Math.max(1, (entries.size() + ENTRIES_PER_PAGE - 1) / ENTRIES_PER_PAGE);
        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal("Page " + (page + 1) + "/" + totalPages),
                width / 2, height - 55, 0xAAAAAA);

        // Hover tooltip
        int centerX = width / 2;
        int entryWidth = 300;
        int entryX = centerX - entryWidth / 2;
        int startY = 30 + TAB_HEIGHT + 20;
        int entryHeight = 24;

        int start = page * ENTRIES_PER_PAGE;
        int end = Math.min(start + ENTRIES_PER_PAGE, entries.size());

        for (int i = start; i < end; i++) {
            int y = startY + (i - start) * (entryHeight + 4);
            if (mouseX >= entryX && mouseX < entryX + entryWidth && mouseY >= y && mouseY < y + entryHeight) {
                PowerEntry entry = entries.get(i);
                List<Text> tooltip = new ArrayList<>();
                tooltip.add(Text.literal(entry.name()).formatted(Formatting.YELLOW));
                tooltip.add(Text.literal(entry.description()).formatted(Formatting.GRAY));
                tooltip.add(Text.literal("Source: " + entry.sourceName()).formatted(Formatting.AQUA));
                context.drawTooltip(textRenderer, tooltip, mouseX, mouseY);
                break;
            }
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
