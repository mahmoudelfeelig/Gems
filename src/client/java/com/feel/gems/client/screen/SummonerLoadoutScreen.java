package com.feel.gems.client.screen;

import com.feel.gems.net.SummonerLoadoutSavePayload;
import com.feel.gems.net.SummonerLoadoutScreenPayload;
import com.feel.gems.power.SummonerLoadouts;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.entity.EntityType;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Client UI for choosing Summoner minions per slot.
 */
public final class SummonerLoadoutScreen extends Screen {
    private static final int MAX_ROWS = 3;

    private final int maxPoints;
    private final int maxActive;
    private final Map<String, Integer> costs;
    private final List<List<SummonerLoadouts.Entry>> initial;

    private final List<EntityOption> options;
    private final SlotEditor[] slots = new SlotEditor[5];

    private ButtonWidget saveButton;
    private ButtonWidget resetButton;
    private ButtonWidget cancelButton;
    private int totalCost;

    public SummonerLoadoutScreen(SummonerLoadoutScreenPayload payload) {
        super(Text.literal("Summoner Loadout"));
        this.maxPoints = payload.maxPoints();
        this.maxActive = payload.maxActiveSummons();
        this.costs = payload.costs() == null ? Map.of() : payload.costs();
        this.initial = List.of(
            safeEntries(payload.slot1()),
            safeEntries(payload.slot2()),
            safeEntries(payload.slot3()),
            safeEntries(payload.slot4()),
            safeEntries(payload.slot5())
        );
        this.options = buildOptions(this.costs);
    }

    @Override
    protected void init() {
        super.init();
        clearChildren();

        for (int i = 0; i < slots.length; i++) {
            slots[i] = new SlotEditor("Slot " + (i + 1), options);
            slots[i].applyEntries(initial.get(i));
        }

        layoutSlots();

        int buttonWidth = 110;
        int buttonHeight = 20; // Ensure button height is consistent
        int centerX = this.width / 2;
        int bottomY = this.height - 40;

        saveButton = addDrawableChild(ButtonWidget.builder(Text.literal("Save"), btn -> save()).dimensions(centerX - buttonWidth - 8, bottomY, buttonWidth, buttonHeight).build());
        resetButton = addDrawableChild(ButtonWidget.builder(Text.literal("Reset"), btn -> reset()).dimensions(centerX - (buttonWidth / 2), bottomY, buttonWidth, buttonHeight).build());
        cancelButton = addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"), btn -> close()).dimensions(centerX + 8 + (buttonWidth / 2), bottomY, buttonWidth, buttonHeight).build());

        updateCost(); // Update cost after buttons are initialized
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);

        int panelLeft = (this.width / 2) - 270;
        int panelRight = (this.width / 2) + 270;
        int panelTop = 32;
        int panelBottom = this.height - 60;
        context.fill(panelLeft, panelTop, panelRight, panelBottom, 0xAA101010);
        context.fill(panelLeft, panelTop, panelRight, panelTop + 1, 0x44FFFFFF);
        context.fill(panelLeft, panelBottom - 1, panelRight, panelBottom, 0x44111111);

        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 12, 0xFFFFFF);
        context.drawCenteredTextWithShadow(
                this.textRenderer,
                Text.literal("Pick minions for each Summon ability. Budget and active caps apply."),
                this.width / 2,
                22,
                0xA0A0A0
        );

        for (SlotEditor slot : slots) {
            if (slot != null) {
                slot.renderLabels(context, this.textRenderer);
            }
        }

        super.render(context, mouseX, mouseY, delta);

        int color = totalCost > maxPoints ? 0xFF5555 : 0x80FF80;
        String budget = "Budget: " + totalCost + " / " + maxPoints + " pts";
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(budget), this.width / 2, this.height - 68, color);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Active summon cap: " + maxActive), this.width / 2, this.height - 56, 0xA0A0A0);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(null);
        }
    }

    private void layoutSlots() {
        int columnWidth = 250;
        int gapX = 30;
        int gapY = 24;
        int startY = 52;
        int centerX = this.width / 2; // Center X position for layout
        int leftX = centerX - columnWidth - (gapX / 2);
        int rightX = centerX + (gapX / 2);

        for (int i = 0; i < slots.length; i++) {
            SlotEditor slot = slots[i];
            int col = i % 2;
            int row = i / 2;
            int x = col == 0 ? leftX : rightX;
            int y = startY + row * ((MAX_ROWS * 28) + gapY);
            slot.position(x, y);
            slot.addWidgets(widget -> this.addDrawableChild(widget)); // Use lambda for clarity
        }
    }

    private void updateCost() {
        int total = 0;
        for (SlotEditor slot : slots) {
            if (slot == null) {
                continue;
            }
            for (SummonerLoadouts.Entry entry : slot.entries()) {
                Integer cost = costs.get(entry.entityId());
                if (cost != null && cost > 0) {
                    total += cost * entry.count();
                }
            }
        }
        this.totalCost = total;
        if (saveButton != null) {
            boolean over = total > maxPoints;
            saveButton.active = !over;
            saveButton.setMessage(Text.literal(over ? "Save (over budget)" : "Save"));
        }
    }

    private void save() {
        ClientPlayNetworking.send(new SummonerLoadoutSavePayload(
                slots[0].entries(),
                slots[1].entries(),
                slots[2].entries(),
                slots[3].entries(),
                slots[4].entries()
        ));
        close();
    }

    private void reset() {
        for (int i = 0; i < slots.length; i++) {
            slots[i].applyEntries(initial.get(i));
        }
        updateCost();
    }

    private List<EntityOption> buildOptions(Map<String, Integer> costMap) {
        List<EntityOption> opts = new ArrayList<>();
        opts.add(EntityOption.none());
        for (Map.Entry<String, Integer> entry : costMap.entrySet()) {
            Identifier id = Identifier.tryParse(entry.getKey());
            if (id == null) {
                continue;
            }
            EntityType<?> type = Registries.ENTITY_TYPE.getOrEmpty(id).orElse(null);
            if (type == null) {
                continue;
            }
            Text name = type.getName();
            opts.add(new EntityOption(id.toString(), name, entry.getValue()));
        }
        opts.sort(Comparator.comparingInt(EntityOption::cost).thenComparing(o -> o.name.getString()));
        return opts;
    }

    private static List<SummonerLoadouts.Entry> safeEntries(List<SummonerLoadouts.Entry> entries) {
        return entries == null ? List.of() : entries;
    }

    private final class SlotEditor {
        private final String title;
        private final List<EntityOption> options;
        private final Row[] rows = new Row[MAX_ROWS];
        private ButtonWidget addButton;
        private int baseX;
        private int baseY;

        private SlotEditor(String title, List<EntityOption> options) {
            this.title = title;
            this.options = options;
            for (int i = 0; i < MAX_ROWS; i++) {
                rows[i] = new Row(options);
            }
        }

        void position(int x, int y) {
            this.baseX = x;
            this.baseY = y;
            relayout();
        }

        void addWidgets(java.util.function.Consumer<net.minecraft.client.gui.widget.ClickableWidget> adder) {
            for (Row row : rows) {
                adder.accept(row.mobButton);
                adder.accept(row.countButton);
                adder.accept(row.removeButton);
            }
            if (addButton == null) {
                addButton = ButtonWidget.builder(Text.literal("Add row"), btn -> addRow()).dimensions(baseX, baseY + (MAX_ROWS * 28) + 6, 200, 20).build();
            }
            adder.accept(addButton);
            refreshVisibility();
        }

        void renderLabels(DrawContext context, net.minecraft.client.font.TextRenderer font) {
            context.drawTextWithShadow(font, title, baseX, baseY - 12, 0xFFFFFF);
        }

        void applyEntries(List<SummonerLoadouts.Entry> entries) {
            int idx = 0;
            if (entries != null) {
                for (SummonerLoadouts.Entry entry : entries) {
                    if (idx >= MAX_ROWS) {
                        break;
                    }
                    rows[idx].set(entry);
                    idx++;
                }
            }
            for (; idx < MAX_ROWS; idx++) {
                rows[idx].clear();
            }
            refreshVisibility();
        }

        List<SummonerLoadouts.Entry> entries() {
            List<SummonerLoadouts.Entry> list = new ArrayList<>();
            for (Row row : rows) {
                if (!row.visible()) {
                    continue;
                }
                SummonerLoadouts.Entry e = row.asEntry();
                if (e != null) {
                    list.add(e);
                }
            }
            return list;
        }

        private void addRow() {
            for (Row row : rows) {
                if (!row.visible()) {
                    row.show(); // Show the first hidden row
                    refreshVisibility();
                    SummonerLoadoutScreen.this.updateCost();
                    return;
                }
            }
        }

        private void removeRow(Row row) {
            row.clear();
            refreshVisibility();
            SummonerLoadoutScreen.this.updateCost();
        }

        private void refreshVisibility() {
            int visibleCount = 0;
            for (Row row : rows) {
                if (row.visible()) {
                    visibleCount++; // Count visible rows
                }
            }
            for (Row row : rows) {
                row.setVisible(row == rows[0] || row.visible());
            }
            if (addButton != null) {
                addButton.active = visibleCount < MAX_ROWS;
                addButton.visible = visibleCount < MAX_ROWS;
            }
            SummonerLoadoutScreen.this.updateCost();
        }

        private void relayout() {
            int rowHeight = 28;
            for (int i = 0; i < rows.length; i++) {
                int y = baseY + 4 + i * rowHeight;
                Row row = rows[i];
                row.setPosition(baseX, y);
                row.setRemoveHandler(() -> removeRow(row));
            }
            if (addButton != null) {
                addButton.setPosition(baseX, baseY + (MAX_ROWS * rowHeight) + 6);
            }
        }

        private final class Row {
            private final CyclingButtonWidget<EntityOption> mobButton;
            private final CyclingButtonWidget<Integer> countButton;
            private final ButtonWidget removeButton;
            private Runnable removeAction = () -> {};

            Row(List<EntityOption> options) {
                this.mobButton = CyclingButtonWidget.builder(EntityOption::label)
                        .values(options)
                        .initially(options.get(0))
                        .build(0, 0, 150, 20, Text.literal("Mob"), (btn, value) -> {
                            refreshVisibility();
                            SummonerLoadoutScreen.this.updateCost();
                        });
                this.countButton = CyclingButtonWidget.<Integer>builder(i -> Text.literal("Count: " + i))
                        .values(java.util.List.of(1, 2, 3, 4, 5, 6, 7, 8))
                        .initially(1)
                            .build(0, 0, 70, 20, Text.literal("Count"), (btn, value) -> {
                                SummonerLoadoutScreen.this.updateCost(); // Update cost on count change
                            });
                this.removeButton = ButtonWidget.builder(Text.literal("X"), btn -> removeAction.run()).dimensions(0, 0, 22, 20).build();
            }

            void setPosition(int x, int y) {
                mobButton.setPosition(x, y);
                countButton.setPosition(x + 158, y);
                removeButton.setPosition(x + 158 + 76 + 4, y);
            }

            void setRemoveHandler(Runnable runnable) {
                this.removeAction = runnable;
            }

            void set(SummonerLoadouts.Entry entry) {
                EntityOption opt = options.stream().filter(o -> o.id != null && o.id.equals(entry.entityId())).findFirst().orElse(options.get(0));
                mobButton.setValue(opt);
                countButton.setValue(Math.max(1, entry.count()));
                show();
            }

            void clear() {
                mobButton.setValue(options.get(0));
                countButton.setValue(1);
                mobButton.visible = false;
                countButton.visible = false;
                removeButton.visible = false;
            }

            void show() {
                mobButton.visible = true;
                countButton.visible = true;
                removeButton.visible = true;
            }

            boolean visible() {
                return mobButton.visible;
            }

            void setVisible(boolean visible) {
                mobButton.visible = visible;
                countButton.visible = visible;
                removeButton.visible = visible;
            }

            SummonerLoadouts.Entry asEntry() {
                EntityOption opt = mobButton.getValue();
                if (opt == null || opt.isNone()) {
                    return null;
                }
                int count = countButton.getValue();
                if (count <= 0) {
                    return null;
                }
                return new SummonerLoadouts.Entry(opt.id, count);
            }
        }
    }

    private record EntityOption(String id, Text name, int cost) {
        static EntityOption none() {
            return new EntityOption(null, Text.literal("None"), 0);
        }

        boolean isNone() {
            return id == null;
        }

        Text label() {
            if (isNone()) {
                return Text.literal("None");
            }
            return Text.literal(name.getString() + " (" + cost + " pts)");
        }
    }
}