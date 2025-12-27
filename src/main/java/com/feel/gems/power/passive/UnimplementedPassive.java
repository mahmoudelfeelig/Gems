package com.feel.gems.power.passive;

import com.feel.gems.power.api.GemPassive;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;




public final class UnimplementedPassive implements GemPassive {
    private final Identifier id;
    private final String name;
    private final String description;

    public UnimplementedPassive(Identifier id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
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
    public void apply(ServerPlayerEntity player) {
        // no-op
    }

    @Override
    public void remove(ServerPlayerEntity player) {
        // no-op
    }
}

