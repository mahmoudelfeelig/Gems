package com.feel.gems.gametest.augment;

import com.feel.gems.augment.AugmentDefinition;
import com.feel.gems.augment.AugmentInstance;
import com.feel.gems.augment.AugmentRegistry;
import com.feel.gems.augment.AugmentRarity;
import com.feel.gems.augment.AugmentRuntime;
import com.feel.gems.augment.AugmentTarget;
import com.feel.gems.core.GemId;
import com.feel.gems.core.GemRegistry;
import com.feel.gems.gametest.util.GemsGameTestUtil;
import com.feel.gems.item.ModItems;
import com.feel.gems.state.GemPlayerState;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.TestContext;
import net.minecraft.util.Identifier;

public final class AugmentGameTests {
    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void gemAugmentsApplyAndAffectStats(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.FIRE);
        ItemStack gemStack = new ItemStack(ModItems.gemItem(GemId.FIRE));

        for (AugmentDefinition def : AugmentRegistry.all()) {
            if (def.target() != AugmentTarget.GEM) {
                continue;
            }
            AugmentInstance inst = new AugmentInstance(def.id(), AugmentRarity.COMMON, 1.0f);
            if (!AugmentRuntime.applyGemAugment(player, gemStack, inst)) {
                context.throwGameTestException("Failed to apply gem augment: " + def.id());
                return;
            }
        }

        float cooldownMultiplier = AugmentRuntime.cooldownMultiplier(player, GemId.FIRE);
        if (cooldownMultiplier >= 1.0f) {
            context.throwGameTestException("Expected cooldown multiplier to be < 1 after augments");
            return;
        }

        List<Identifier> passives = GemRegistry.definition(GemId.FIRE).passives();
        if (!passives.isEmpty()) {
            int bonus = AugmentRuntime.passiveAmplifierBonus(player, passives.get(0));
            if (bonus <= 0) {
                context.throwGameTestException("Expected passive amplifier bonus from resonance augment");
                return;
            }
        }

        GemsGameTestUtil.complete(context);
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void legendaryInscriptionsApplyAndModifyAttributes(TestContext context) {
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);

        for (AugmentDefinition def : AugmentRegistry.all()) {
            if (def.target() != AugmentTarget.LEGENDARY) {
                continue;
            }
            ItemStack legendary = new ItemStack(ModItems.DUELISTS_RAPIER);
            AugmentInstance inst = new AugmentInstance(def.id(), AugmentRarity.COMMON, 1.0f);
            if (!AugmentRuntime.applyLegendaryAugment(player, legendary, inst)) {
                context.throwGameTestException("Failed to apply legendary inscription: " + def.id());
                return;
            }
            if (AugmentRuntime.getLegendaryAugments(legendary).isEmpty()) {
                context.throwGameTestException("Legendary inscription did not persist: " + def.id());
                return;
            }
        }

        ItemStack edge = new ItemStack(ModItems.DUELISTS_RAPIER);
        ItemStack swift = new ItemStack(ModItems.HUNTERS_TROPHY_NECKLACE);
        ItemStack ward = new ItemStack(ModItems.SUPREME_HELMET);
        AugmentRuntime.applyLegendaryAugment(player, edge, new AugmentInstance("edge", AugmentRarity.COMMON, 1.0f));
        AugmentRuntime.applyLegendaryAugment(player, swift, new AugmentInstance("swift", AugmentRarity.COMMON, 1.0f));
        AugmentRuntime.applyLegendaryAugment(player, ward, new AugmentInstance("ward", AugmentRarity.COMMON, 1.0f));

        player.equipStack(net.minecraft.entity.EquipmentSlot.MAINHAND, edge);
        player.equipStack(net.minecraft.entity.EquipmentSlot.OFFHAND, swift);
        player.equipStack(net.minecraft.entity.EquipmentSlot.HEAD, ward);

        EntityAttributeInstance damage = player.getAttributeInstance(EntityAttributes.ATTACK_DAMAGE);
        EntityAttributeInstance armor = player.getAttributeInstance(EntityAttributes.ARMOR);
        if (damage == null || armor == null) {
            context.throwGameTestException("Missing expected player attributes");
            return;
        }
        double baseDamage = damage.getBaseValue();
        double baseArmor = armor.getBaseValue();

        AugmentRuntime.applyLegendaryModifiers(player);

        if (damage.getValue() <= baseDamage) {
            context.throwGameTestException("Expected legendary edge inscription to increase attack damage");
            return;
        }
        if (armor.getValue() <= baseArmor) {
            context.throwGameTestException("Expected legendary ward inscription to increase armor");
            return;
        }

        GemsGameTestUtil.complete(context);
    }
}
