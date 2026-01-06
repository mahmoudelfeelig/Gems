package com.feel.gems.client.screen;

import com.feel.gems.net.TrackerCompassScreenPayload;
import com.feel.gems.net.TrackerCompassSelectPayload;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class TrackerCompassScreen extends Screen {
    private static final int ENTRIES_PER_PAGE = 8;
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

        int panelWidth = Math.min(240, this.width - 32);
        int centerX = this.width / 2;
        int topY = 30;
        int buttonHeight = 20;
        int spacing = 4;

        int start = page * ENTRIES_PER_PAGE;
        int end = Math.min(entries.size(), start + ENTRIES_PER_PAGE);
        int y = topY + 18;

        if (entries.isEmpty()) {
            addDrawableChild(ButtonWidget.builder(Text.translatable("gems.screen.tracker_compass.no_players").formatted(Formatting.GRAY), btn -> {
            }).dimensions(centerX - (panelWidth / 2), y, panelWidth, buttonHeight).build()).active = false;
        } else {
            for (int i = start; i < end; i++) {
                TrackerCompassScreenPayload.Entry entry = entries.get(i);
                Text label = Text.literal(entry.name()).formatted(entry.online() ? Formatting.GREEN : Formatting.GRAY);
                int buttonY = y + (i - start) * (buttonHeight + spacing);
                addDrawableChild(ButtonWidget.builder(label, btn -> select(entry.uuid()))
                        .dimensions(centerX - (panelWidth / 2), buttonY, panelWidth, buttonHeight)
                        .build());
            }
        }

        int bottomY = this.height - 44;
        int smallWidth = 70;
        ButtonWidget prev = ButtonWidget.builder(Text.translatable("gems.screen.button.prev"), btn -> changePage(-1))
                .dimensions(centerX - smallWidth - 8, bottomY, smallWidth, buttonHeight)
                .build();
        ButtonWidget next = ButtonWidget.builder(Text.translatable("gems.screen.button.next"), btn -> changePage(1))
                .dimensions(centerX + 8, bottomY, smallWidth, buttonHeight)
                .build();
        prev.active = page > 0;
        next.active = (page + 1) * ENTRIES_PER_PAGE < entries.size();
        addDrawableChild(prev);
        addDrawableChild(next);

        int bottomY2 = bottomY + buttonHeight + 6;
        addDrawableChild(ButtonWidget.builder(Text.translatable("gems.screen.button.clear"), btn -> clearTarget())
                .dimensions(centerX - smallWidth - 8, bottomY2, smallWidth, buttonHeight)
                .build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("gems.screen.button.cancel"), btn -> close())
                .dimensions(centerX + 8, bottomY2, smallWidth, buttonHeight)
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
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 12, 0xFFFFFF);
        if (!entries.isEmpty()) {
            int maxPage = Math.max(0, (entries.size() - 1) / ENTRIES_PER_PAGE) + 1;
            String pageText = "Page " + (page + 1) + " / " + maxPage;
            context.drawCenteredTextWithShadow(this.textRenderer, pageText, this.width / 2, 20, 0xA0A0A0);
        }
    }
}
