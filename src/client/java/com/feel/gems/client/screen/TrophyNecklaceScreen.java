package com.feel.gems.client.screen;

import com.feel.gems.net.TrophyNecklaceClaimPayload;
import com.feel.gems.net.TrophyNecklaceScreenPayload;
import com.feel.gems.net.TrophyNecklaceScreenPayload.PassiveEntry;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

public final class TrophyNecklaceScreen extends Screen {
    private static final int ENTRIES_PER_PAGE = 8;

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
        int startY = 40;

        int stolenCount = 0;
        for (PassiveEntry entry : passives) {
            if (entry.stolen()) stolenCount++;
        }

        int entryY = startY;
        int entryWidth = 300;
        int entryHeight = 24;
        int entryX = centerX - entryWidth / 2;

        int totalPages = Math.max(1, (passives.size() + ENTRIES_PER_PAGE - 1) / ENTRIES_PER_PAGE);
        if (page >= totalPages) page = totalPages - 1;
        if (page < 0) page = 0;

        int start = page * ENTRIES_PER_PAGE;
        int end = Math.min(start + ENTRIES_PER_PAGE, passives.size());

        for (int i = start; i < end; i++) {
            PassiveEntry entry = passives.get(i);
            boolean stolen = entry.stolen();

            String prefix = stolen ? "\u2714 " : "";
            Text label = Text.literal(prefix + entry.name()).formatted(stolen ? Formatting.GREEN : Formatting.WHITE);

            Identifier id = entry.id();
            ButtonWidget btn = ButtonWidget.builder(label, b -> toggle(id, stolen))
                    .dimensions(entryX, entryY, entryWidth, entryHeight)
                    .build();
            if (!stolen && stolenCount >= maxStolenPassives) {
                btn.active = false;
            }
            addDrawableChild(btn);
            entryY += entryHeight + 4;
        }

        // Pagination
        int pageY = height - 50;
        if (page > 0) {
            addDrawableChild(ButtonWidget.builder(Text.translatable("gems.screen.button.prev"), b -> changePage(-1))
                    .dimensions(centerX - 110, pageY, 100, 20)
                    .build());
        }
        if (page < totalPages - 1) {
            addDrawableChild(ButtonWidget.builder(Text.translatable("gems.screen.button.next"), b -> changePage(1))
                    .dimensions(centerX + 10, pageY, 100, 20)
                    .build());
        }

        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), b -> close())
                .dimensions(centerX - 50, height - 25, 100, 20)
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
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 10, 0xFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer,
                Text.translatable("gems.screen.trophy_necklace.subtitle", targetName, maxStolenPassives),
                width / 2, 24, 0xAAAAAA);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}

