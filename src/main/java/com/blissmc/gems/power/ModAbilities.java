package com.blissmc.gems.power;

import net.minecraft.util.Identifier;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class ModAbilities {
    private static final Map<Identifier, GemAbility> ABILITIES = new HashMap<>();

    static {
        // Astra
        register(new DimensionalDriftAbility());
        register(new DimensionalVoidAbility());
        register(new AstralDaggersAbility());
        register(new UnboundedAbility());
        register(new AstralProjectionAbility());
        register(new SpookAbility());
        register(new TagAbility());

        // Fire
        register(new CosyCampfireAbility());
        register(new CrispAbility());
        register(new FireballAbility());
        register(new MeteorShowerAbility());

        // Flux
        register(new FluxBeamAbility());
        register(new StaticBurstAbility());

        // Life
        register(new VitalityVortexAbility());
        register(new HealthDrainAbility());
        register(new LifeCircleAbility());
        register(new HeartLockAbility());

        // Puff
        register(new DoubleJumpAbility());
        register(new DashAbility());
        register(new BreezyBashAbility());
        register(new GroupBreezyBashAbility());

        // Speed
        register(new BlurAbility());
        register(new SpeedStormAbility());
        register(new TerminalVelocityAbility());

        // Strength
        register(new NullifyAbility());
        register(new FrailerAbility());
        register(new BountyHuntingAbility());
        register(new ChadStrengthAbility());

        // Wealth
        register(new PocketsAbility());
        register(new UnfortunateAbility());
        register(new ItemLockAbility());
        register(new AmplificationAbility());
        register(new RichRushAbility());
    }

    private ModAbilities() {
    }

    public static GemAbility get(Identifier id) {
        return ABILITIES.get(id);
    }

    public static Map<Identifier, GemAbility> all() {
        return Collections.unmodifiableMap(ABILITIES);
    }

    public static void override(GemAbility ability) {
        ABILITIES.put(ability.id(), ability);
    }

    private static void register(GemAbility ability) {
        ABILITIES.put(ability.id(), ability);
    }
}
