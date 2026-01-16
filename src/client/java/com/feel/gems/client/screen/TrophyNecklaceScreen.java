package com.feel.gems.client.screen;

import static com.feel.gems.client.screen.GemsScreenConstants.*;

import com.feel.gems.net.TrophyNecklaceClaimPayload;
import com.feel.gems.net.TrophyNecklaceScreenPayload;
import com.feel.gems.net.TrophyNecklaceScreenPayload.PassiveEntry;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

public final class TrophyNecklaceScreen extends GemsScreenBase {
    private final String targetName;
    private final List<PassiveEntry> passives;
    private final int maxStolenPassives;

    private int page = 0;

    public TrophyNecklaceScreen(TrophyNecklaceScreenPayload payload) {
        super(Text.translatable("gems.screen.trophy_necklace.title"));
        this.targetName = payload.targetName();
        this.passives = new ArrayList<>(payload.passives());
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

        int stolenCount = 0;
        for (PassiveEntry entry : passives) {
            if (entry.stolen()) stolenCount++;
        }

        int totalPages = totalPages(passives.size(), ENTRIES_PER_PAGE);
        page = clampPage(page, totalPages);

        int start = page * ENTRIES_PER_PAGE;
        int end = Math.min(start + ENTRIES_PER_PAGE, passives.size());

        int entryY = CONTENT_START_Y;
        for (int i = start; i < end; i++) {
            PassiveEntry entry = passives.get(i);
            boolean stolen = entry.stolen();

            String prefix = stolen ? "\u2714 " : "";
            Text label = Text.literal(prefix + entry.name()).formatted(stolen ? Formatting.GREEN : Formatting.WHITE);

            Identifier id = entry.id();
            ButtonWidget btn = ButtonWidget.builder(label, b -> toggle(id, stolen))
                    .dimensions(entryX, entryY, panelW, ENTRY_HEIGHT)
                    .build();
            if (!stolen && stolenCount >= maxStolenPassives) {
                btn.active = false;
            }
            addDrawableChild(btn);
            entryY += ENTRY_HEIGHT + SPACING;
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

    private void toggle(Identifier passiveId, boolean currentlyStolen) {
        ClientPlayNetworking.send(new TrophyNecklaceClaimPayload(passiveId, !currentlyStolen));
        // UI will be refreshed by the server sending a new payload.
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        renderBase(context);
        context.drawCenteredTextWithShadow(textRenderer,
                Text.translatable("gems.screen.trophy_necklace.subtitle", targetName, maxStolenPassives),
                width / 2, SUBTITLE_Y, COLOR_GRAY);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}

