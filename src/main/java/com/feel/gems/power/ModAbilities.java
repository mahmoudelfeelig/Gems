package com.feel.gems.power;

import net.minecraft.util.Identifier;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class ModAbilities {
    private static final Map<Identifier, GemAbility> ABILITIES = new HashMap<>();

    static {
        // Astra
        register(new ShadowAnchorAbility());
        register(new DimensionalVoidAbility());
        register(new AstralDaggersAbility());
        register(new UnboundedAbility());
        register(new AstralCameraAbility());
        register(new SpookAbility());
        register(new TagAbility());

        // Fire
        register(new CosyCampfireAbility());
        register(new HeatHazeZoneAbility());
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
        register(new ArcShotAbility());
        register(new SpeedStormAbility());
        register(new TerminalVelocityAbility());
        register(new SpeedSlipstreamAbility());
        register(new SpeedAfterimageAbility());

        // Strength
        register(new NullifyAbility());
        register(new FrailerAbility());
        register(new BountyHuntingAbility());
        register(new ChadStrengthAbility());

        // Wealth
        register(new PocketsAbility());
        register(new FumbleAbility());
        register(new HotbarLockAbility());
        register(new AmplificationAbility());
        register(new RichRushAbility());

        // Terror
        register(new TerrorTradeAbility());
        register(new PanicRingAbility());

        // Summoner
        register(new SummonSlotAbility(1));
        register(new SummonSlotAbility(2));
        register(new SummonSlotAbility(3));
        register(new SummonSlotAbility(4));
        register(new SummonSlotAbility(5));
        register(new SummonRecallAbility());

        // Space
        register(new SpaceOrbitalLaserAbility());
        register(new SpaceGravityFieldAbility());
        register(new SpaceBlackHoleAbility());
        register(new SpaceWhiteHoleAbility());

        // Reaper
        register(new ReaperGraveSteedAbility());
        register(new ReaperWitheringStrikesAbility());
        register(new ReaperDeathOathAbility());
        register(new ReaperScytheSweepAbility());
        register(new ReaperBloodChargeAbility());
        register(new ReaperShadeCloneAbility());

        // Pillager
        register(new PillagerFangsAbility());
        register(new PillagerRavageAbility());
        register(new PillagerVindicatorBreakAbility());
        register(new PillagerVolleyAbility());

        // Spy/Mimic
        register(new SpyMimicFormAbility());
        register(new SpyEchoAbility());
        register(new SpyStealAbility());
        register(new SpySmokeBombAbility());
        register(new SpyStolenCastAbility());

        // Beacon
        register(new BeaconAuraAbility(BeaconAuraRuntime.AuraType.SPEED));
        register(new BeaconAuraAbility(BeaconAuraRuntime.AuraType.HASTE));
        register(new BeaconAuraAbility(BeaconAuraRuntime.AuraType.RESISTANCE));
        register(new BeaconAuraAbility(BeaconAuraRuntime.AuraType.JUMP));
        register(new BeaconAuraAbility(BeaconAuraRuntime.AuraType.STRENGTH));
        register(new BeaconAuraAbility(BeaconAuraRuntime.AuraType.REGEN));

        // Air
        register(new AirWindJumpAbility());
        register(new AirGaleSlamAbility());
        register(new AirUpdraftZoneAbility());
        register(new AirDashAbility());
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
