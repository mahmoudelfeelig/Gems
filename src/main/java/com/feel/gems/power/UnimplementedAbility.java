package com.feel.gems.power;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class UnimplementedAbility implements GemAbility {
    private final Identifier id;
    private final String name;
    private final String description;
    private final int cooldownTicks;

    public UnimplementedAbility(Identifier id, String name, String description, int cooldownTicks) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.cooldownTicks = cooldownTicks;
    }

    @Override
    public Identifier id() {
        return id;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String description() {
        return description;
    }

    @Override
    public int cooldownTicks() {
        return cooldownTicks;
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        player.sendMessage(Text.literal(name + " is not implemented yet."), true);
        return false;
    }
}
