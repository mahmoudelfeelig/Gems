package com.feel.gems.augment;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.core.GemDefinition;
import com.feel.gems.core.GemId;
import com.feel.gems.core.GemRegistry;
import com.feel.gems.legendary.LegendaryItem;
import com.feel.gems.state.GemsPersistentDataHolder;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

public final class AugmentRuntime {
    private static final String KEY_GEM_AUGMENTS = "gemAugments";
    private static final String KEY_LEGENDARY_AUGMENTS = "legendaryAugments";
    private static final String KEY_AUGMENT_ID = "id";
    private static final String KEY_AUGMENT_RARITY = "rarity";
    private static final String KEY_AUGMENT_MAG = "mag";

    private static final Identifier MOD_LEGENDARY_DAMAGE = Identifier.of("gems", "legendary_damage");
    private static final Identifier MOD_LEGENDARY_ATTACK_SPEED = Identifier.of("gems", "legendary_attack_speed");
    private static final Identifier MOD_LEGENDARY_MOVE_SPEED = Identifier.of("gems", "legendary_move_speed");
    private static final Identifier MOD_LEGENDARY_ARMOR = Identifier.of("gems", "legendary_armor");

    private AugmentRuntime() {
    }

    public static AugmentInstance getInstance(ItemStack stack) {
        NbtComponent custom = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (custom == null) {
            return null;
        }
        NbtCompound nbt = custom.copyNbt();
        String id = nbt.getString(KEY_AUGMENT_ID, "");
        if (id.isEmpty()) {
            return null;
        }
        String rarityStr = nbt.getString(KEY_AUGMENT_RARITY, "");
        float mag = nbt.getFloat(KEY_AUGMENT_MAG, 0.0F);
        AugmentRarity rarity;
        try {
            rarity = AugmentRarity.valueOf(rarityStr);
        } catch (IllegalArgumentException e) {
            rarity = AugmentRarity.COMMON;
        }
        return new AugmentInstance(id, rarity, mag);
    }

    public static void assignInstance(ItemStack stack, AugmentInstance instance) {
        NbtComponent.set(DataComponentTypes.CUSTOM_DATA, stack, nbt -> {
            nbt.putString(KEY_AUGMENT_ID, instance.augmentId());
            nbt.putString(KEY_AUGMENT_RARITY, instance.rarity().name());
            nbt.putFloat(KEY_AUGMENT_MAG, instance.magnitude());
        });
    }

    public static AugmentInstance rollInstance(String augmentId) {
        AugmentDefinition def = AugmentRegistry.get(augmentId);
        if (def == null) {
            return null;
        }
        AugmentRarity rarity = rollRarity();
        float mag = rollMagnitude(rarity);
        return new AugmentInstance(augmentId, rarity, mag);
    }

    public static boolean applyGemAugment(ServerPlayerEntity player, GemId gemId, AugmentInstance instance) {
        if (gemId == null) {
            return false;
        }
        AugmentDefinition def = AugmentRegistry.get(instance.augmentId());
        if (def == null || def.target() != AugmentTarget.GEM) {
            return false;
        }

        List<AugmentInstance> current = getGemAugments(player, gemId);
        int maxSlots = GemsBalance.v().augments().gemMaxSlots();
        if (current.size() >= maxSlots) {
            player.sendMessage(Text.translatable("gems.augment.no_slots").formatted(Formatting.RED), true);
            return false;
        }
        if (hasConflict(current, def)) {
            player.sendMessage(Text.translatable("gems.augment.conflict").formatted(Formatting.RED), true);
            return false;
        }

        current.add(instance);
        saveGemAugments(player, gemId, current);
        player.sendMessage(Text.translatable("gems.augment.applied_gem").formatted(Formatting.GREEN), true);
        return true;
    }

    public static boolean applyLegendaryAugment(ServerPlayerEntity player, ItemStack target, AugmentInstance instance) {
        if (!(target.getItem() instanceof LegendaryItem)) {
            return false;
        }
        AugmentDefinition def = AugmentRegistry.get(instance.augmentId());
        if (def == null || def.target() != AugmentTarget.LEGENDARY) {
            return false;
        }
        List<AugmentInstance> current = getLegendaryAugments(target);
        int maxSlots = GemsBalance.v().augments().legendaryMaxSlots();
        if (current.size() >= maxSlots) {
            player.sendMessage(Text.translatable("gems.augment.no_slots").formatted(Formatting.RED), true);
            return false;
        }
        if (hasConflict(current, def)) {
            player.sendMessage(Text.translatable("gems.augment.conflict").formatted(Formatting.RED), true);
            return false;
        }
        current.add(instance);
        saveLegendaryAugments(target, current);
        player.sendMessage(Text.translatable("gems.augment.applied_legendary").formatted(Formatting.GOLD), true);
        return true;
    }

    public static float cooldownMultiplier(ServerPlayerEntity player, GemId gemId) {
        float reduction = 0.0f;
        for (AugmentInstance instance : getGemAugments(player, gemId)) {
            AugmentDefinition def = AugmentRegistry.get(instance.augmentId());
            if (def == null) continue;
            for (AugmentModifier mod : def.modifiers()) {
                if (mod.type() == AugmentModifierType.COOLDOWN_MULTIPLIER) {
                    reduction += mod.baseMagnitude() * instance.magnitude();
                }
            }
        }
        reduction = Math.min(reduction, 0.6f); // cap 60% reduction
        return Math.max(0.1f, 1.0f - reduction);
    }

    public static int passiveAmplifierBonus(ServerPlayerEntity player, Identifier passiveId) {
        Optional<GemId> gem = findGemForPassive(passiveId);
        if (gem.isEmpty()) {
            return 0;
        }
        int bonus = 0;
        for (AugmentInstance instance : getGemAugments(player, gem.get())) {
            AugmentDefinition def = AugmentRegistry.get(instance.augmentId());
            if (def == null) continue;
            for (AugmentModifier mod : def.modifiers()) {
                if (mod.type() == AugmentModifierType.PASSIVE_AMP_BONUS) {
                    bonus += Math.round(mod.baseMagnitude() * instance.magnitude());
                }
            }
        }
        return Math.max(0, bonus);
    }

    public static void applyLegendaryModifiers(ServerPlayerEntity player) {
        float damage = 0.0f;
        float attackSpeed = 0.0f;
        float moveSpeed = 0.0f;
        float armor = 0.0f;

        for (ItemStack stack : getLegendaryStacks(player)) {
            for (AugmentInstance instance : getLegendaryAugments(stack)) {
                AugmentDefinition def = AugmentRegistry.get(instance.augmentId());
                if (def == null) continue;
                for (AugmentModifier mod : def.modifiers()) {
                    float value = mod.baseMagnitude() * instance.magnitude();
                    switch (mod.type()) {
                        case LEGENDARY_ATTACK_DAMAGE -> damage += value;
                        case LEGENDARY_ATTACK_SPEED -> attackSpeed += value;
                        case LEGENDARY_MOVE_SPEED -> moveSpeed += value;
                        case LEGENDARY_ARMOR -> armor += value;
                        default -> {
                        }
                    }
                }
            }
        }

        applyAttribute(player, EntityAttributes.ATTACK_DAMAGE, MOD_LEGENDARY_DAMAGE, damage);
        applyAttribute(player, EntityAttributes.ATTACK_SPEED, MOD_LEGENDARY_ATTACK_SPEED, attackSpeed);
        applyAttribute(player, EntityAttributes.MOVEMENT_SPEED, MOD_LEGENDARY_MOVE_SPEED, moveSpeed);
        applyAttribute(player, EntityAttributes.ARMOR, MOD_LEGENDARY_ARMOR, armor);
    }

    private static void applyAttribute(ServerPlayerEntity player, RegistryEntry<net.minecraft.entity.attribute.EntityAttribute> attr, Identifier id, float value) {
        EntityAttributeInstance inst = player.getAttributeInstance(attr);
        if (inst == null) {
            return;
        }
        inst.removeModifier(id);
        if (value > 0.0001f) {
            inst.addTemporaryModifier(new EntityAttributeModifier(id, (double) value, EntityAttributeModifier.Operation.ADD_VALUE));
        }
    }

    private static List<ItemStack> getLegendaryStacks(ServerPlayerEntity player) {
        List<ItemStack> stacks = new ArrayList<>();
        if (player.getMainHandStack().getItem() instanceof LegendaryItem) {
            stacks.add(player.getMainHandStack());
        }
        if (player.getOffHandStack().getItem() instanceof LegendaryItem) {
            stacks.add(player.getOffHandStack());
        }
        ItemStack head = player.getEquippedStack(net.minecraft.entity.EquipmentSlot.HEAD);
        if (head.getItem() instanceof LegendaryItem) {
            stacks.add(head);
        }
        ItemStack chest = player.getEquippedStack(net.minecraft.entity.EquipmentSlot.CHEST);
        if (chest.getItem() instanceof LegendaryItem) {
            stacks.add(chest);
        }
        ItemStack legs = player.getEquippedStack(net.minecraft.entity.EquipmentSlot.LEGS);
        if (legs.getItem() instanceof LegendaryItem) {
            stacks.add(legs);
        }
        ItemStack feet = player.getEquippedStack(net.minecraft.entity.EquipmentSlot.FEET);
        if (feet.getItem() instanceof LegendaryItem) {
            stacks.add(feet);
        }
        return stacks;
    }

    private static boolean hasConflict(List<AugmentInstance> current, AugmentDefinition def) {
        Set<String> conflicts = def.conflicts();
        if (conflicts == null || conflicts.isEmpty()) {
            return false;
        }
        for (AugmentInstance inst : current) {
            if (conflicts.contains(inst.augmentId())) {
                return true;
            }
        }
        return false;
    }

    private static Optional<GemId> findGemForPassive(Identifier passiveId) {
        for (GemId gem : GemId.values()) {
            try {
                GemDefinition def = GemRegistry.definition(gem);
                if (def.passives().contains(passiveId)) {
                    return Optional.of(gem);
                }
            } catch (IllegalArgumentException ignored) {
            }
        }
        return Optional.empty();
    }

    private static NbtCompound persistentRoot(ServerPlayerEntity player) {
        return ((GemsPersistentDataHolder) player).gems$getPersistentData();
    }

    public static List<AugmentInstance> getGemAugments(ServerPlayerEntity player, GemId gemId) {
        NbtCompound root = persistentRoot(player);
        NbtCompound gemData = root.getCompound(KEY_GEM_AUGMENTS).orElse(new NbtCompound());
        String key = gemId.name().toLowerCase();
        NbtList list = gemData.getList(key).orElse(new NbtList());
        return readList(list);
    }

    public static boolean removeGemAugment(ServerPlayerEntity player, GemId gemId, int index) {
        if (player == null || gemId == null) {
            return false;
        }
        List<AugmentInstance> current = getGemAugments(player, gemId);
        if (index < 0 || index >= current.size()) {
            return false;
        }
        current.remove(index);
        saveGemAugments(player, gemId, current);
        return true;
    }

    public static void clearGemAugments(ServerPlayerEntity player, GemId gemId) {
        if (player == null || gemId == null) {
            return;
        }
        saveGemAugments(player, gemId, List.of());
    }

    private static void saveGemAugments(ServerPlayerEntity player, GemId gemId, List<AugmentInstance> list) {
        NbtCompound root = persistentRoot(player);
        NbtCompound gemData = root.getCompound(KEY_GEM_AUGMENTS).orElse(new NbtCompound());
        gemData.put(gemId.name().toLowerCase(), writeList(list));
        root.put(KEY_GEM_AUGMENTS, gemData);
    }

    public static List<AugmentInstance> getLegendaryAugments(ItemStack stack) {
        NbtComponent custom = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (custom == null) {
            return List.of();
        }
        NbtCompound nbt = custom.copyNbt();
        NbtList list = nbt.getList(KEY_LEGENDARY_AUGMENTS).orElse(new NbtList());
        return readList(list);
    }

    private static void saveLegendaryAugments(ItemStack stack, List<AugmentInstance> list) {
        NbtComponent.set(DataComponentTypes.CUSTOM_DATA, stack, nbt -> nbt.put(KEY_LEGENDARY_AUGMENTS, writeList(list)));
    }

    private static List<AugmentInstance> readList(NbtList list) {
        List<AugmentInstance> out = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            NbtCompound entry = list.getCompound(i).orElse(null);
            if (entry == null) {
                continue;
            }
            String id = entry.getString(KEY_AUGMENT_ID, "");
            String rarityStr = entry.getString(KEY_AUGMENT_RARITY, "");
            float mag = entry.getFloat(KEY_AUGMENT_MAG, 0.0F);
            AugmentRarity rarity;
            try {
                rarity = AugmentRarity.valueOf(rarityStr);
            } catch (IllegalArgumentException e) {
                rarity = AugmentRarity.COMMON;
            }
            if (!id.isEmpty()) {
                out.add(new AugmentInstance(id, rarity, mag));
            }
        }
        return out;
    }

    private static NbtList writeList(List<AugmentInstance> list) {
        NbtList out = new NbtList();
        for (AugmentInstance inst : list) {
            NbtCompound entry = new NbtCompound();
            entry.putString(KEY_AUGMENT_ID, inst.augmentId());
            entry.putString(KEY_AUGMENT_RARITY, inst.rarity().name());
            entry.putFloat(KEY_AUGMENT_MAG, inst.magnitude());
            out.add(entry);
        }
        return out;
    }

    private static AugmentRarity rollRarity() {
        int common = Math.max(0, GemsBalance.v().augments().rarityCommonWeight());
        int rare = Math.max(0, GemsBalance.v().augments().rarityRareWeight());
        int epic = Math.max(0, GemsBalance.v().augments().rarityEpicWeight());
        int total = common + rare + epic;
        if (total <= 0) {
            return AugmentRarity.COMMON;
        }
        int roll = (int) (Math.random() * total);
        if (roll < common) {
            return AugmentRarity.COMMON;
        }
        roll -= common;
        if (roll < rare) {
            return AugmentRarity.RARE;
        }
        return AugmentRarity.EPIC;
    }

    private static float rollMagnitude(AugmentRarity rarity) {
        var cfg = GemsBalance.v().augments();
        return switch (rarity) {
            case COMMON -> randomRange(cfg.commonMagnitudeMin(), cfg.commonMagnitudeMax());
            case RARE -> randomRange(cfg.rareMagnitudeMin(), cfg.rareMagnitudeMax());
            case EPIC -> randomRange(cfg.epicMagnitudeMin(), cfg.epicMagnitudeMax());
        };
    }

    private static float randomRange(float min, float max) {
        if (max < min) {
            float tmp = min;
            min = max;
            max = tmp;
        }
        float range = Math.max(0.0f, max - min);
        return min + (float) Math.random() * range;
    }
}
