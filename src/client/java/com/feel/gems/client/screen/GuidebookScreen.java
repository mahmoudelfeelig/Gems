package com.feel.gems.client.screen;

import static com.feel.gems.client.screen.GemsScreenConstants.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.resource.Resource;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * In-game guidebook for player-facing docs.
 */
public final class GuidebookScreen extends GemsScreenBase {
    private static final int LEFT_WIDTH = 140;
    private static final int ENTRY_HEIGHT = 20;
    private static final Pattern LINK_PATTERN = Pattern.compile("\\[(.+?)\\]\\((.+?)\\)");

    private final List<DocEntry> entries = List.of(
            new DocEntry("gameplay", "Gameplay Overview", "gameplay.md"),
            new DocEntry("index", "Guidebook Index", "readme.md"),
            new DocEntry("progression", "Progression", "progression.md"),
            new DocEntry("gems", "Gems", "gems.md"),
            new DocEntry("bonus_pool", "Bonus Pool", "bonus_pool.md"),
            new DocEntry("mastery_rivalry", "Mastery & Rivalry", "mastery_and_rivalry.md"),
            new DocEntry("synergies", "Synergies", "synergies.md"),
            new DocEntry("legendary", "Legendary Items", "legendary_items.md"),
            new DocEntry("trust", "Trust & Ownership", "trust_and_ownership.md"),
            new DocEntry("controls", "Controls & HUD", "controls_and_hud.md"),
            new DocEntry("assassin", "Assassin Endgame", "assassin_endgame.md"),
            new DocEntry("recipes", "Recipes", "recipes.md"),
            new DocEntry("test_dummy", "Test Dummy", "test_dummy.md")
    );

    private final Map<String, GuideDoc> docCache = new HashMap<>();

    private int selectedIndex = 0;
    private int page = 0;
    private List<OrderedText> wrappedLines = List.of();

    private ButtonWidget prevPage;
    private ButtonWidget nextPage;

    public GuidebookScreen() {
        super(Text.translatable("gems.screen.guidebook.title"));
    }

    public static void open(MinecraftClient client) {
        if (client != null) {
            client.setScreen(new GuidebookScreen());
        }
    }

    @Override
    protected void init() {
        super.init();
        clearChildren();
        buildDocButtons();
        buildNavButtons();
        addCloseButton();
        select(0);
    }

    private void buildDocButtons() {
        int x = 16;
        int y = 32;
        for (int i = 0; i < entries.size(); i++) {
            int idx = i;
            DocEntry entry = entries.get(i);
            GuideDoc doc = loadDoc(entry);
            ButtonWidget btn = ButtonWidget.builder(Text.literal(doc.title()), b -> select(idx))
                    .dimensions(x, y + (i * (ENTRY_HEIGHT + 2)), LEFT_WIDTH, ENTRY_HEIGHT)
                    .build();
            addDrawableChild(btn);
        }
    }

    private void buildNavButtons() {
        int navY = navButtonY(height);
        prevPage = addDrawableChild(ButtonWidget.builder(Text.translatable("gems.screen.button.prev"), b -> changePage(-1))
                .dimensions(width / 2 + 10, navY, NAV_BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
        nextPage = addDrawableChild(ButtonWidget.builder(Text.translatable("gems.screen.button.next"), b -> changePage(1))
                .dimensions(width / 2 + 10 + NAV_BUTTON_WIDTH + 6, navY, NAV_BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
    }

    private void select(int index) {
        selectedIndex = Math.max(0, Math.min(entries.size() - 1, index));
        page = 0;
        wrapBody();
        updateNavButtons();
    }

    private void changePage(int delta) {
        int total = totalPages(wrappedLines.size(), linesPerPage());
        page = clampPage(page + delta, total);
        updateNavButtons();
    }

    private void updateNavButtons() {
        int total = totalPages(wrappedLines.size(), linesPerPage());
        prevPage.active = page > 0;
        nextPage.active = page + 1 < total;
    }

    private void wrapBody() {
        DocEntry entry = entries.get(selectedIndex);
        GuideDoc doc = loadDoc(entry);
        int contentWidth = width - LEFT_WIDTH - 60;
        wrappedLines = this.textRenderer.wrapLines(Text.literal(doc.body()), Math.max(120, contentWidth));
    }

    private int linesPerPage() {
        int available = height - 80;
        return Math.max(6, available / this.textRenderer.fontHeight);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        renderBase(context);

        if (wrappedLines.isEmpty()) {
            wrapBody();
            updateNavButtons();
        }

        int left = LEFT_WIDTH + 32;
        int top = 32;
        int right = width - 24;
        int bottom = height - 50;
        drawPanel(context, left, top - 8, right, bottom);

        DocEntry entry = entries.get(selectedIndex);
        GuideDoc doc = loadDoc(entry);
        context.drawTextWithShadow(this.textRenderer, Text.literal(doc.title()), left + 10, top, COLOR_WHITE);

        int linesPerPage = linesPerPage();
        int start = page * linesPerPage;
        int end = Math.min(wrappedLines.size(), start + linesPerPage);
        int y = top + 18;
        for (int i = start; i < end; i++) {
            context.drawTextWithShadow(this.textRenderer, wrappedLines.get(i), left + 10, y, COLOR_GRAY);
            y += this.textRenderer.fontHeight;
        }
    }

    private GuideDoc loadDoc(DocEntry entry) {
        GuideDoc cached = docCache.get(entry.id());
        if (cached != null) {
            return cached;
        }
        GuideDoc loaded = readDoc(entry);
        docCache.put(entry.id(), loaded);
        return loaded;
    }

    private GuideDoc readDoc(DocEntry entry) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getResourceManager() == null) {
            return new GuideDoc(entry.defaultTitle(), "Guidebook data unavailable.");
        }
        Identifier id = Identifier.of("gems", "guidebook/" + entry.resourcePath());
        try {
            Resource resource = client.getResourceManager().getResourceOrThrow(id);
            try (InputStream stream = resource.getInputStream();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                StringBuilder raw = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    raw.append(line).append("\n");
                }
                return parseDoc(entry.defaultTitle(), raw.toString());
            }
        } catch (IOException e) {
            return new GuideDoc(entry.defaultTitle(), "Missing guidebook entry: " + entry.resourcePath());
        }
    }

    private GuideDoc parseDoc(String fallbackTitle, String raw) {
        String normalized = raw.replace("\r\n", "\n").replace("\r", "\n");
        String[] lines = normalized.split("\n", -1);
        String title = null;
        StringBuilder body = new StringBuilder();
        for (String line : lines) {
            String trimmed = line.trim();
            if (title == null && trimmed.startsWith("#")) {
                title = trimmed.replaceFirst("^#+\\s*", "").trim();
                if (!title.isEmpty()) {
                    continue;
                }
            }
            if (trimmed.startsWith("#")) {
                line = trimmed.replaceFirst("^#+\\s*", "");
            }
            if (line.startsWith("- ")) {
                line = "• " + line.substring(2);
            }
            line = LINK_PATTERN.matcher(line).replaceAll("$1");
            line = line.replace("**", "")
                    .replace("__", "")
                    .replace("*", "")
                    .replace("_", "")
                    .replace("`", "");
            body.append(line).append("\n");
        }
        String bodyText = body.toString().trim();
        if (bodyText.isEmpty()) {
            bodyText = "(No content)";
        }
        return new GuideDoc(title != null && !title.isEmpty() ? title : fallbackTitle, bodyText);
    }

    private record DocEntry(String id, String defaultTitle, String resourcePath) {
    }

    private record GuideDoc(String title, String body) {
    }
}