package com.feel.gems.item.legendary;

import com.feel.gems.GemsMod;
import com.feel.gems.config.GemsBalance;
import com.feel.gems.legendary.LegendaryItem;
import com.feel.gems.net.ServerTrophyNecklaceNetworking;
import com.feel.gems.net.TrophyNecklaceScreenPayload;
import com.feel.gems.power.api.GemPassive;
import com.feel.gems.power.runtime.GemPowers;
import com.feel.gems.power.registry.ModPassives;
import com.feel.gems.state.GemsPersistentDataHolder;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Hand;
import net.minecraft.util.ActionResult;
import net.minecraft.world.World;

/**
 * Hunter's Trophy Necklace - steal passives from other players and keep them permanently.
 */
public final class HuntersTrophyNecklaceItem extends Item implements LegendaryItem {
    private static final String KEY_STOLEN_PASSIVES = "trophy_necklace_passives";
    private static final String KEY_LAST_TARGET_NAME = "trophy_necklace_last_target_name";
    private static final String KEY_LAST_OFFERED = "trophy_necklace_last_offered";

    private static int maxStolenPassives() {
        return GemsBalance.v().legendary().trophyNecklaceMaxPassives();
    }

    public HuntersTrophyNecklaceItem(Settings settings) {
        super(settings);
    }

    @Override
    public String legendaryId() {
        return Identifier.of(GemsMod.MOD_ID, "hunters_trophy_necklace").toString();
    }

    /**
     * Called when the holder kills another player.
     * Opens a selection UI to choose passives to steal from the victim.
     */
    public static void onKillPlayer(ServerPlayerEntity killer, ServerPlayerEntity victim) {
        if (!hasNecklace(killer) || victim == null) {
            return;
        }
        openScreen(killer, victim, victim.getName().getString());
    }

    @Override
    public ActionResult use(World world, net.minecraft.entity.player.PlayerEntity user, Hand hand) {
        if (world.isClient() || !(user instanceof ServerPlayerEntity player)) {
            return ActionResult.PASS;
        }
        // Right click a player to steal their passives.
        ServerPlayerEntity target = com.feel.gems.power.util.Targeting.raycastPlayer(player, 15);
        if (target == null || target == player) {
            player.sendMessage(Text.translatable("gems.message.no_player_target"), true);
            return ActionResult.FAIL;
        }
        openScreen(player, target, target.getName().getString());
        return ActionResult.SUCCESS;
    }

    public static boolean hasNecklace(ServerPlayerEntity player) {
        for (ItemStack stack : player.getInventory().getMainStacks()) {
            if (stack.getItem() instanceof HuntersTrophyNecklaceItem) {
                return true;
            }
        }
        return player.getOffHandStack().getItem() instanceof HuntersTrophyNecklaceItem;
    }

    public static Set<Identifier> getStolenPassives(ServerPlayerEntity player) {
        NbtCompound data = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        NbtList list = data.getList(KEY_STOLEN_PASSIVES).orElse(null);
        if (list == null || list.isEmpty()) {
            return Set.of();
        }
        java.util.HashSet<Identifier> out = new java.util.HashSet<>();
        for (int i = 0; i < list.size(); i++) {
            Identifier id = Identifier.tryParse(list.getString(i, ""));
            if (id != null) {
                out.add(id);
            }
        }
        return Set.copyOf(out);
    }

    public static boolean stealPassive(ServerPlayerEntity player, Identifier passiveId) {
        if (player == null || passiveId == null || ModPassives.get(passiveId) == null) {
            return false;
        }
        NbtCompound data = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        Set<Identifier> current = getStolenPassives(player);
        if (current.contains(passiveId)) {
            return false;
        }
        int max = maxStolenPassives();
        if (max > 0 && current.size() >= max) {
            player.sendMessage(Text.translatable("gems.item.trophy_necklace.max", max).formatted(Formatting.RED), true);
            return false;
        }
        NbtList list = new NbtList();
        for (Identifier id : current) {
            list.add(NbtString.of(id.toString()));
        }
        list.add(NbtString.of(passiveId.toString()));
        data.put(KEY_STOLEN_PASSIVES, list);
        player.sendMessage(Text.translatable("gems.item.trophy_necklace.stolen", ModPassives.get(passiveId).name()).formatted(Formatting.GOLD), true);
        return true;
    }

    public static boolean unstealPassive(ServerPlayerEntity player, Identifier passiveId) {
        if (player == null || passiveId == null) {
            return false;
        }
        Set<Identifier> current = getStolenPassives(player);
        if (!current.contains(passiveId)) {
            return false;
        }
        java.util.HashSet<Identifier> next = new java.util.HashSet<>(current);
        next.remove(passiveId);
        NbtCompound data = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        NbtList list = new NbtList();
        for (Identifier id : next) {
            list.add(NbtString.of(id.toString()));
        }
        data.put(KEY_STOLEN_PASSIVES, list);
        GemPassive passive = ModPassives.get(passiveId);
        if (passive != null) {
            passive.remove(player);
        }
        player.sendMessage(Text.translatable("gems.item.trophy_necklace.unstolen", passive != null ? passive.name() : passiveId.getPath()).formatted(Formatting.GRAY), true);
        return true;
    }

    /**
     * Re-open the last Trophy Necklace UI session, if available (used after claim toggles).
     */
    public static void openLastTargetScreen(ServerPlayerEntity player) {
        if (player == null || player.getEntityWorld().getServer() == null) {
            return;
        }
        NbtCompound data = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        String targetName = data.getString(KEY_LAST_TARGET_NAME, "");
        NbtList offeredList = data.getList(KEY_LAST_OFFERED).orElse(null);
        if (offeredList == null || offeredList.isEmpty()) {
            return;
        }
        java.util.HashSet<Identifier> offered = new java.util.HashSet<>();
        for (int i = 0; i < offeredList.size(); i++) {
            Identifier id = Identifier.tryParse(offeredList.getString(i, ""));
            if (id != null) {
                offered.add(id);
            }
        }
        if (offered.isEmpty()) {
            return;
        }
        sendScreenPayload(player, targetName.isBlank() ? "Unknown" : targetName, offered);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, TooltipDisplayComponent displayComponent, Consumer<Text> tooltip, TooltipType type) {
        tooltip.accept(Text.translatable("gems.item.trophy_necklace.tooltip.1").formatted(Formatting.GRAY));
        tooltip.accept(Text.translatable("gems.item.trophy_necklace.tooltip.2").formatted(Formatting.GRAY));
        tooltip.accept(Text.translatable("gems.item.trophy_necklace.tooltip.3").formatted(Formatting.GOLD));
    }

    private static void openScreen(ServerPlayerEntity opener, ServerPlayerEntity target, String targetName) {
        if (opener == null || target == null || opener.getEntityWorld().getServer() == null) {
            return;
        }
        var server = opener.getEntityWorld().getServer();
        java.util.HashSet<Identifier> offered = new java.util.HashSet<>();

        com.feel.gems.state.GemPlayerState.initIfNeeded(target);
        com.feel.gems.core.GemId gem = com.feel.gems.state.GemPlayerState.getActiveGem(target);
        if (gem == com.feel.gems.core.GemId.PRISM && server != null) {
            var prism = com.feel.gems.bonus.PrismSelectionsState.get(server).getSelection(target.getUuid());
            offered.addAll(prism.gemPassives());
        } else {
            offered.addAll(com.feel.gems.core.GemRegistry.definition(gem).passives());
        }

        // Include bonus passives the target currently has (energy 10).
        if (server != null && com.feel.gems.state.GemPlayerState.getEnergy(target) >= 10) {
            offered.addAll(com.feel.gems.bonus.BonusClaimsState.get(server).getPlayerPassives(target.getUuid()));
        }
        offered.removeIf(id -> ModPassives.get(id) == null);
        if (offered.isEmpty()) {
            opener.sendMessage(Text.translatable("gems.item.trophy_necklace.no_passives").formatted(Formatting.RED), true);
            return;
        }
        // Snapshot for the session so the target can switch gems without affecting this selection.
        NbtCompound data = ((GemsPersistentDataHolder) opener).gems$getPersistentData();
        data.putString(KEY_LAST_TARGET_NAME, targetName);
        NbtList list = new NbtList();
        for (Identifier id : offered) {
            list.add(NbtString.of(id.toString()));
        }
        data.put(KEY_LAST_OFFERED, list);

        sendScreenPayload(opener, targetName, offered);
    }

    private static void sendScreenPayload(ServerPlayerEntity opener, String targetName, Set<Identifier> offered) {
        Set<Identifier> stolen = getStolenPassives(opener);
        List<TrophyNecklaceScreenPayload.PassiveEntry> entries = new java.util.ArrayList<>(offered.size());
        for (Identifier id : offered.stream().sorted().toList()) {
            GemPassive passive = ModPassives.get(id);
            if (passive == null) continue;
            entries.add(new TrophyNecklaceScreenPayload.PassiveEntry(
                    id,
                    passive.name(),
                    passive.description(),
                    stolen.contains(id)
            ));
        }
        ServerTrophyNecklaceNetworking.setSession(opener, offered);
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(opener,
                new TrophyNecklaceScreenPayload(targetName, entries, maxStolenPassives()));
    }
}
