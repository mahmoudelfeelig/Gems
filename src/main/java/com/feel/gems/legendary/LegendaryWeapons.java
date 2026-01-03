package com.feel.gems.legendary;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.item.ModItems;
import com.feel.gems.util.GemsNbt;
import java.util.UUID;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;




public final class LegendaryWeapons {
    private static final String KEY_BLOOD_OATH_KILLS = "legendaryBloodOathKills";

    private LegendaryWeapons() {
    }

    public static void onPlayerKill(ServerPlayerEntity killer, ServerPlayerEntity victim) {
        if (!killer.getMainHandStack().isOf(ModItems.BLOOD_OATH_BLADE)) {
            return;
        }
        ItemStackHelper.applyUniqueKill(killer, victim);
    }

    private static final class ItemStackHelper {
        static void applyUniqueKill(ServerPlayerEntity killer, ServerPlayerEntity victim) {
            var stack = killer.getMainHandStack();
            NbtComponent data = stack.get(DataComponentTypes.CUSTOM_DATA);
            NbtCompound nbt = data != null ? data.copyNbt() : new NbtCompound();
            NbtList list = nbt.getListOrEmpty(KEY_BLOOD_OATH_KILLS);
            UUID victimId = victim.getUuid();
            if (containsUuid(list, victimId)) {
                return;
            }
            list.add(GemsNbt.fromUuid(victimId));
            nbt.put(KEY_BLOOD_OATH_KILLS, list);
            NbtComponent.set(DataComponentTypes.CUSTOM_DATA, stack, out -> out.copyFrom(nbt));

            RegistryEntry<Enchantment> sharpness = resolveSharpness(killer);
            if (sharpness == null) {
                return;
            }
            int current = EnchantmentHelper.getLevel(sharpness, stack);
            int cap = GemsBalance.v().legendary().bloodOathSharpnessCap();
            int next = Math.min(cap, current + 1);
            if (next > current) {
                EnchantmentHelper.apply(stack, builder -> builder.set(sharpness, next));
            }
        }

        private static boolean containsUuid(NbtList list, UUID uuid) {
            for (int i = 0; i < list.size(); i++) {
                if (uuid.equals(GemsNbt.toUuid(list.get(i)))) {
                    return true;
                }
            }
            return false;
        }

        private static RegistryEntry<Enchantment> resolveSharpness(ServerPlayerEntity killer) {
            return killer.getEntityWorld().getRegistryManager().getOptionalEntry(Enchantments.SHARPNESS).orElse(null);
        }
    }
}
