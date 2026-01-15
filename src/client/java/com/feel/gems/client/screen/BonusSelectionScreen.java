package com.feel.gems.client.screen;

import static com.feel.gems.client.screen.GemsScreenConstants.*;

import com.feel.gems.net.BonusSelectionClaimPayload;
import com.feel.gems.net.BonusSelectionScreenPayload;
import com.feel.gems.net.BonusSelectionScreenPayload.BonusEntry;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Client UI for selecting bonus abilities and passives at energy 10.
 */
public final class BonusSelectionScreen extends Screen {
    private final List<BonusEntry> abilities;
    private final List<BonusEntry> passives;
    private final int maxAbilities;
    private final int maxPassives;
    
    private boolean showingAbilities = true;
    private int page = 0;
    
    private ButtonWidget tabAbilities;
    private ButtonWidget tabPassives;
    
    public BonusSelectionScreen(BonusSelectionScreenPayload payload) {
        super(Text.translatable("gems.screen.bonus_selection.title"));
        this.abilities = new ArrayList<>(payload.abilities());
        this.passives = new ArrayList<>(payload.passives());
        this.maxAbilities = payload.maxAbilities();
        this.maxPassives = payload.maxPassives();
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
        
        // Tab buttons
        int tabY = 30;
        tabAbilities = addDrawableChild(ButtonWidget.builder(
                        Text.translatable("gems.screen.bonus_selection.tab_abilities").formatted(showingAbilities ? Formatting.UNDERLINE : Formatting.RESET),
                        btn -> switchTab(true))
                .dimensions(centerX - TAB_WIDTH - TAB_GAP / 2, tabY, TAB_WIDTH, TAB_HEIGHT)
                .build());
        tabPassives = addDrawableChild(ButtonWidget.builder(
                        Text.translatable("gems.screen.bonus_selection.tab_passives").formatted(!showingAbilities ? Formatting.UNDERLINE : Formatting.RESET),
                        btn -> switchTab(false))
                .dimensions(centerX + TAB_GAP / 2, tabY, TAB_WIDTH, TAB_HEIGHT)
                .build());
        
        List<BonusEntry> currentList = showingAbilities ? abilities : passives;
        
        int totalPg = totalPages(currentList.size(), ENTRIES_PER_PAGE);
        page = clampPage(page, totalPg);
        
        int start = page * ENTRIES_PER_PAGE;
        int end = Math.min(currentList.size(), start + ENTRIES_PER_PAGE);
        int y = tabY + TAB_HEIGHT + 26;
        
        if (currentList.isEmpty()) {
            addDrawableChild(ButtonWidget.builder(Text.translatable("gems.screen.bonus_selection.no_bonuses").formatted(Formatting.GRAY), btn -> {})
                    .dimensions(entryX, y, panelW, BUTTON_HEIGHT)
                    .build()).active = false;
        } else {
            for (int i = start; i < end; i++) {
                BonusEntry entry = currentList.get(i);
                int buttonY = y + (i - start) * (BUTTON_HEIGHT + SPACING);
                
                Formatting formatting;
                String suffix;
                if (entry.claimed()) {
                    formatting = Formatting.GREEN;
                    suffix = " [Claimed]";
                } else if (entry.available()) {
                    formatting = Formatting.WHITE;
                    suffix = "";
                } else {
                    formatting = Formatting.DARK_GRAY;
                    suffix = " [Taken]";
                }
                
                Text label = Text.literal(entry.name() + suffix).formatted(formatting);
                final BonusEntry finalEntry = entry;
                
                ButtonWidget button = addDrawableChild(ButtonWidget.builder(label, btn -> toggleClaim(finalEntry))
                        .dimensions(entryX, buttonY, panelW, BUTTON_HEIGHT)
                        .build());
                
                // Disable if taken by someone else
                if (!entry.available() && !entry.claimed()) {
                    button.active = false;
                }
            }
        }
        
        // Pagination
        int navY = navButtonY(height);
        ButtonWidget prev = ButtonWidget.builder(Text.translatable("gems.screen.button.prev"), btn -> changePage(-1))
                .dimensions(centerX - NAV_BUTTON_WIDTH - SPACING, navY, NAV_BUTTON_WIDTH, BUTTON_HEIGHT)
                .build();
        ButtonWidget next = ButtonWidget.builder(Text.translatable("gems.screen.button.next"), btn -> changePage(1))
                .dimensions(centerX + SPACING, navY, NAV_BUTTON_WIDTH, BUTTON_HEIGHT)
                .build();
        
        int maxPage = Math.max(0, (currentList.size() - 1) / ENTRIES_PER_PAGE);
        prev.active = page > 0;
        next.active = page < maxPage;
        addDrawableChild(prev);
        addDrawableChild(next);
        
        // Close button
        addDrawableChild(ButtonWidget.builder(Text.translatable("gems.screen.button.close"), btn -> close())
                .dimensions(centerX - CLOSE_BUTTON_WIDTH / 2, closeButtonY(height), CLOSE_BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
    }
    
    private void switchTab(boolean toAbilities) {
        if (showingAbilities != toAbilities) {
            showingAbilities = toAbilities;
            page = 0;
            rebuild();
        }
    }
    
    private void changePage(int delta) {
        List<BonusEntry> currentList = showingAbilities ? abilities : passives;
        int maxPage = Math.max(0, (currentList.size() - 1) / ENTRIES_PER_PAGE);
        page = Math.max(0, Math.min(maxPage, page + delta));
        rebuild();
    }
    
    private void toggleClaim(BonusEntry entry) {
        boolean isAbility = showingAbilities;
        boolean currentlyClaimed = entry.claimed();
        
        // Check if trying to claim but at limit
        if (!currentlyClaimed) {
            int currentClaims = countClaims(isAbility);
            int max = isAbility ? maxAbilities : maxPassives;
            if (currentClaims >= max) {
                if (this.client != null && this.client.player != null) {
                    this.client.player.sendMessage(
                            Text.translatable(isAbility ? "gems.screen.bonus_selection.max_abilities" : "gems.screen.bonus_selection.max_passives", max),
                            true
                    );
                }
                return;
            }
        }
        
        ClientPlayNetworking.send(new BonusSelectionClaimPayload(
                entry.id(),
                isAbility,
                !currentlyClaimed
        ));
    }
    
    private int countClaims(boolean abilities) {
        List<BonusEntry> list = abilities ? this.abilities : this.passives;
        int count = 0;
        for (BonusEntry entry : list) {
            if (entry.claimed()) count++;
        }
        return count;
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        
        int centerX = this.width / 2;
        
        // Title
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, centerX, TITLE_Y, COLOR_WHITE);
        
        // Claimed count
        int claimedAbilities = countClaims(true);
        int claimedPassives = countClaims(false);
        String statusText = String.format("Abilities: %d/%d | Passives: %d/%d", 
                claimedAbilities, maxAbilities, claimedPassives, maxPassives);
        context.drawCenteredTextWithShadow(this.textRenderer, statusText, centerX, 56, COLOR_GRAY);
        
        // Page info
        List<BonusEntry> currentList = showingAbilities ? abilities : passives;
        if (!currentList.isEmpty()) {
            int maxPage = totalPages(currentList.size(), ENTRIES_PER_PAGE);
            String pageText = "Page " + (page + 1) + " / " + maxPage;
            context.drawCenteredTextWithShadow(this.textRenderer, pageText, centerX, this.height - 80, COLOR_DARK_GRAY);
        }
        
        // Tooltip for hovered entry
        renderTooltip(context, mouseX, mouseY);
    }
    
    private void renderTooltip(DrawContext context, int mouseX, int mouseY) {
        List<BonusEntry> currentList = showingAbilities ? abilities : passives;
        int centerX = this.width / 2;
        int panelW = panelWidth(width);
        int topY = 30 + TAB_HEIGHT + 26;
        
        int start = page * ENTRIES_PER_PAGE;
        int end = Math.min(currentList.size(), start + ENTRIES_PER_PAGE);
        
        for (int i = start; i < end; i++) {
            int buttonY = topY + (i - start) * (BUTTON_HEIGHT + SPACING);
            int buttonX = centerX - panelW / 2;
            
            if (mouseX >= buttonX && mouseX <= buttonX + panelW &&
                mouseY >= buttonY && mouseY <= buttonY + BUTTON_HEIGHT) {
                BonusEntry entry = currentList.get(i);
                List<Text> tooltip = new ArrayList<>();
                tooltip.add(Text.literal(entry.name()).formatted(Formatting.WHITE, Formatting.BOLD));
                
                // Wrap description
                String desc = entry.description();
                if (desc != null && !desc.isEmpty()) {
                    List<String> lines = wrapText(desc, 40);
                    for (String line : lines) {
                        tooltip.add(Text.literal(line).formatted(Formatting.GRAY));
                    }
                }
                
                if (entry.claimed()) {
                    tooltip.add(Text.translatable("gems.screen.bonus_selection.click_release").formatted(Formatting.YELLOW));
                } else if (entry.available()) {
                    tooltip.add(Text.translatable("gems.screen.bonus_selection.click_claim").formatted(Formatting.GREEN));
                } else {
                    tooltip.add(Text.translatable("gems.screen.bonus_selection.claimed_by_other").formatted(Formatting.RED));
                }
                
                context.drawTooltip(this.textRenderer, tooltip, mouseX, mouseY);
                break;
            }
        }
    }
    
    private List<String> wrapText(String text, int maxLineLength) {
        List<String> lines = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();
        
        for (String word : words) {
            if (currentLine.length() + word.length() + 1 > maxLineLength) {
                if (currentLine.length() > 0) {
                    lines.add(currentLine.toString().trim());
                    currentLine = new StringBuilder();
                }
            }
            currentLine.append(word).append(" ");
        }
        
        if (currentLine.length() > 0) {
            lines.add(currentLine.toString().trim());
        }
        
        return lines;
    }
    
    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }
}
