package com.feel.gems.client.screen;

import static com.feel.gems.client.screen.GemsScreenConstants.*;

import com.feel.gems.net.TrackerCompassScreenPayload;
import com.feel.gems.net.TrackerCompassSelectPayload;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class TrackerCompassScreen extends GemsScreenBase {
    private final List<TrackerCompassScreenPayload.Entry> entries;
    private int page = 0;

    public TrackerCompassScreen(TrackerCompassScreenPayload payload) {
        super(Text.translatable("gems.screen.tracker_compass.title"));
        this.entries = new ArrayList<>(payload.entries());
        this.entries.sort(Comparator.comparing(entry -> entry.name().toLowerCase(java.util.Locale.ROOT)));
    }

    @Override
    protected void init() {
        super.init();
        rebuild();
    }

    private void rebuild() {
        clearChildren();

        int panelW = panelWidth(width);
        int centerX = width / 2;
        int entryX = centerX - panelW / 2;

        int totalPg = totalPages(entries.size(), ENTRIES_PER_PAGE);
        page = clampPage(page, totalPg);

        int start = page * ENTRIES_PER_PAGE;
        int end = Math.min(entries.size(), start + ENTRIES_PER_PAGE);
        int y = CONTENT_START_Y;

        if (entries.isEmpty()) {
            addDrawableChild(ButtonWidget.builder(Text.translatable("gems.screen.tracker_compass.no_players").formatted(Formatting.GRAY), btn -> {
            }).dimensions(entryX, y, panelW, BUTTON_HEIGHT).build()).active = false;
        } else {
            for (int i = start; i < end; i++) {
                TrackerCompassScreenPayload.Entry entry = entries.get(i);
                Text label = Text.literal(entry.name()).formatted(entry.online() ? Formatting.GREEN : Formatting.GRAY);
                int buttonY = y + (i - start) * (BUTTON_HEIGHT + SPACING);
                addDrawableChild(ButtonWidget.builder(label, btn -> select(entry.uuid()))
                        .dimensions(entryX, buttonY, panelW, BUTTON_HEIGHT)
                        .build());
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
        next.active = (page + 1) * ENTRIES_PER_PAGE < entries.size();
        addDrawableChild(prev);
        addDrawableChild(next);

        int closeY = closeButtonY(height);
        addDrawableChild(ButtonWidget.builder(Text.translatable("gems.screen.button.clear"), btn -> clearTarget())
                .dimensions(centerX - NAV_BUTTON_WIDTH - SPACING, closeY, NAV_BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("gems.screen.button.close"), btn -> close())
                .dimensions(centerX + SPACING, closeY, NAV_BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
    }

    private void changePage(int delta) {
        int maxPage = Math.max(0, (entries.size() - 1) / ENTRIES_PER_PAGE);
        page = Math.max(0, Math.min(maxPage, page + delta));
        rebuild();
    }

    private void select(UUID uuid) {
        ClientPlayNetworking.send(new TrackerCompassSelectPayload(Optional.of(uuid)));
        close();
    }

    private void clearTarget() {
        ClientPlayNetworking.send(new TrackerCompassSelectPayload(Optional.empty()));
        close();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        renderBase(context);
        if (!entries.isEmpty()) {
            int maxPage = totalPages(entries.size(), ENTRIES_PER_PAGE);
            String pageText = "Page " + (page + 1) + " / " + maxPage;
            context.drawCenteredTextWithShadow(this.textRenderer, pageText, this.width / 2, SUBTITLE_Y, COLOR_GRAY);
        }
    }
}
