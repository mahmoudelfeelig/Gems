package com.feel.gems.client.modmenu;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.nio.file.Path;

public final class GemsConfigScreen extends Screen {
    private final Screen parent;

    public GemsConfigScreen(Screen parent) {
        super(Text.literal("Gems Config"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int y = 48;

        addDrawableChild(ButtonWidget.builder(Text.literal("Copy balance.json path"), button -> {
            Path path = balancePath();
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null) {
                client.keyboard.setClipboard(path.toString());
            }
        }).dimensions(centerX - 110, y, 220, 20).build());

        y += 24;
        addDrawableChild(ButtonWidget.builder(Text.literal("Run /gems reloadBalance"), button -> {
            ClientCommandSender.sendCommand("gems reloadBalance");
        }).dimensions(centerX - 110, y, 220, 20).build());

        y += 24;
        addDrawableChild(ButtonWidget.builder(Text.literal("Run /gems dumpBalance"), button -> {
            ClientCommandSender.sendCommand("gems dumpBalance");
        }).dimensions(centerX - 110, y, 220, 20).build());

        y += 32;
        addDrawableChild(ButtonWidget.builder(Text.literal("Done"), button -> close()).dimensions(centerX - 110, y, 220, 20).build());
    }

    private static Path balancePath() {
        return FabricLoader.getInstance().getConfigDir().resolve("gems").resolve("balance.json");
    }

    @Override
    public void close() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null) {
            client.setScreen(parent);
        }
    }

    @Override
    public void render(net.minecraft.client.gui.DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        int centerX = this.width / 2;
        context.drawCenteredTextWithShadow(textRenderer, this.title, centerX, 18, 0xFFFFFF);

        String path = balancePath().toString();
        context.drawCenteredTextWithShadow(textRenderer, Text.literal(path), centerX, 34, 0xA0A0A0);

        MinecraftClient client = MinecraftClient.getInstance();
        boolean singleplayer = client != null && client.getServer() != null;
        Text hint = singleplayer
                ? Text.literal("Singleplayer: edit the file above, then reload.")
                : Text.literal("Multiplayer: server admins must edit server config, then reload.");
        context.drawCenteredTextWithShadow(textRenderer, hint, centerX, this.height - 28, 0xA0A0A0);
    }
}
