package com.feel.gems.client.screen;

import com.feel.gems.net.BonusSelectionClaimPayload;
import com.feel.gems.net.BonusSelectionScreenPayload;
import com.feel.gems.net.BonusSelectionScreenPayload.BonusEntry;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TabButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Client UI for selecting bonus abilities and passives at energy 10.
 */
public final class BonusSelectionScreen extends Screen {
    private static final int ENTRIES_PER_PAGE = 8;
    private static final int TAB_WIDTH = 100;
    private static final int TAB_HEIGHT = 20;
    
    private final List<BonusEntry> abilities;
    private final List<BonusEntry> passives;
    private final int maxAbilities;
    private final int maxPassives;
    
    private boolean showingAbilities = true;
    private int page = 0;
    
    private ButtonWidget tabAbilities;
    private ButtonWidget tabPassives;
    
    public BonusSelectionScreen(BonusSelectionScreenPayload payload) {
        super(Text.literal("Bonus Selection"));
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
        int topY = 30;
        
        // Tab buttons
        int tabY = topY;
        tabAbilities = addDrawableChild(ButtonWidget.builder(
                        Text.literal("Abilities").formatted(showingAbilities ? Formatting.UNDERLINE : Formatting.RESET),
                        btn -> switchTab(true))
                .dimensions(centerX - TAB_WIDTH - 4, tabY, TAB_WIDTH, TAB_HEIGHT)
                .build());
        tabPassives = addDrawableChild(ButtonWidget.builder(
                        Text.literal("Passives").formatted(!showingAbilities ? Formatting.UNDERLINE : Formatting.RESET),
                        btn -> switchTab(false))
                .dimensions(centerX + 4, tabY, TAB_WIDTH, TAB_HEIGHT)
                .build());
        
        List<BonusEntry> currentList = showingAbilities ? abilities : passives;
        int panelWidth = Math.min(280, this.width - 32);
        int buttonHeight = 20;
        int spacing = 4;
        
        int start = page * ENTRIES_PER_PAGE;
        int end = Math.min(currentList.size(), start + ENTRIES_PER_PAGE);
        int y = topY + TAB_HEIGHT + 26;
        
        if (currentList.isEmpty()) {
            addDrawableChild(ButtonWidget.builder(Text.literal("No bonuses available").formatted(Formatting.GRAY), btn -> {})
                    .dimensions(centerX - (panelWidth / 2), y, panelWidth, buttonHeight)
                    .build()).active = false;
        } else {
            for (int i = start; i < end; i++) {
                BonusEntry entry = currentList.get(i);
                int buttonY = y + (i - start) * (buttonHeight + spacing);
                
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
                final int index = i;
                
                ButtonWidget button = addDrawableChild(ButtonWidget.builder(label, btn -> toggleClaim(finalEntry))
                        .dimensions(centerX - (panelWidth / 2), buttonY, panelWidth, buttonHeight)
                        .build());
                
                // Disable if taken by someone else
                if (!entry.available() && !entry.claimed()) {
                    button.active = false;
                }
            }
        }
        
        // Pagination
        int bottomY = this.height - 64;
        int smallWidth = 70;
        ButtonWidget prev = ButtonWidget.builder(Text.literal("< Prev"), btn -> changePage(-1))
                .dimensions(centerX - smallWidth - 8, bottomY, smallWidth, buttonHeight)
                .build();
        ButtonWidget next = ButtonWidget.builder(Text.literal("Next >"), btn -> changePage(1))
                .dimensions(centerX + 8, bottomY, smallWidth, buttonHeight)
                .build();
        
        int maxPage = Math.max(0, (currentList.size() - 1) / ENTRIES_PER_PAGE);
        prev.active = page > 0;
        next.active = page < maxPage;
        addDrawableChild(prev);
        addDrawableChild(next);
        
        // Close button
        int bottomY2 = bottomY + buttonHeight + 6;
        addDrawableChild(ButtonWidget.builder(Text.literal("Close"), btn -> close())
                .dimensions(centerX - 50, bottomY2, 100, buttonHeight)
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
                            Text.literal("You've reached the maximum of " + max + " bonus " + (isAbility ? "abilities" : "passives") + "."),
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
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, centerX, 12, 0xFFFFFF);
        
        // Claimed count
        int claimedAbilities = countClaims(true);
        int claimedPassives = countClaims(false);
        String statusText = String.format("Abilities: %d/%d | Passives: %d/%d", 
                claimedAbilities, maxAbilities, claimedPassives, maxPassives);
        context.drawCenteredTextWithShadow(this.textRenderer, statusText, centerX, 56, 0xA0A0A0);
        
        // Page info
        List<BonusEntry> currentList = showingAbilities ? abilities : passives;
        if (!currentList.isEmpty()) {
            int maxPage = Math.max(0, (currentList.size() - 1) / ENTRIES_PER_PAGE) + 1;
            String pageText = "Page " + (page + 1) + " / " + maxPage;
            context.drawCenteredTextWithShadow(this.textRenderer, pageText, centerX, this.height - 80, 0x808080);
        }
        
        // Tooltip for hovered entry
        renderTooltip(context, mouseX, mouseY);
    }
    
    private void renderTooltip(DrawContext context, int mouseX, int mouseY) {
        List<BonusEntry> currentList = showingAbilities ? abilities : passives;
        int centerX = this.width / 2;
        int panelWidth = Math.min(280, this.width - 32);
        int buttonHeight = 20;
        int spacing = 4;
        int topY = 30 + TAB_HEIGHT + 26;
        
        int start = page * ENTRIES_PER_PAGE;
        int end = Math.min(currentList.size(), start + ENTRIES_PER_PAGE);
        
        for (int i = start; i < end; i++) {
            int buttonY = topY + (i - start) * (buttonHeight + spacing);
            int buttonX = centerX - (panelWidth / 2);
            
            if (mouseX >= buttonX && mouseX <= buttonX + panelWidth &&
                mouseY >= buttonY && mouseY <= buttonY + buttonHeight) {
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
                    tooltip.add(Text.literal("Click to release").formatted(Formatting.YELLOW));
                } else if (entry.available()) {
                    tooltip.add(Text.literal("Click to claim").formatted(Formatting.GREEN));
                } else {
                    tooltip.add(Text.literal("Claimed by another player").formatted(Formatting.RED));
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
