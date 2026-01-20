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
import com.feel.gems.util.GemsNbt;
import com.feel.gems.util.GemsTooltipFormat;
import java.util.List;
import java.util.Set;
import java.util.UUID;
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
    private static final String KEY_STOLEN_FROM = "trophy_necklace_stolen_from";
    private static final String KEY_LOST_PASSIVES = "trophy_necklace_lost_passives";
    private static final String KEY_LAST_TARGET_NAME = "trophy_necklace_last_target_name";
    private static final String KEY_LAST_TARGET_UUID = "trophy_necklace_last_target_uuid";
    private static final String KEY_LAST_OFFERED = "trophy_necklace_last_offered";
    private static final String KEY_LAST_KILL_TARGET_UUID = "trophy_necklace_last_kill_uuid";
    private static final String KEY_LAST_KILL_USED = "trophy_necklace_last_kill_used";

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
        markKillSession(killer, victim);
        openScreen(killer, victim, victim.getName().getString());
    }

    public static void openScreenForTarget(ServerPlayerEntity opener, ServerPlayerEntity target) {
        if (opener == null || target == null || opener.getEntityWorld().getServer() == null) {
            return;
        }
        openScreen(opener, target, target.getName().getString());
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
        return stealPassiveFrom(player, passiveId, null, true);
    }

    public static boolean stealPassiveFrom(ServerPlayerEntity player, Identifier passiveId, UUID sourceUuid) {
        return stealPassiveFrom(player, passiveId, sourceUuid, true);
    }

    private static boolean stealPassiveFrom(ServerPlayerEntity player, Identifier passiveId, UUID sourceUuid, boolean enforceLimit) {
        if (player == null || passiveId == null || ModPassives.get(passiveId) == null) {
            return false;
        }
        if (sourceUuid != null && !canClaimFromKill(player, sourceUuid)) {
            player.sendMessage(Text.translatable("gems.item.trophy_necklace.need_kill").formatted(Formatting.RED), true);
            return false;
        }
        NbtCompound data = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        Set<Identifier> current = getStolenPassives(player);
        if (current.contains(passiveId)) {
            return false;
        }
        int max = maxStolenPassives();
        if (enforceLimit && max > 0 && current.size() >= max) {
            player.sendMessage(Text.translatable("gems.item.trophy_necklace.max", max).formatted(Formatting.RED), true);
            return false;
        }
        NbtList list = new NbtList();
        for (Identifier id : current) {
            list.add(NbtString.of(id.toString()));
        }
        list.add(NbtString.of(passiveId.toString()));
        data.put(KEY_STOLEN_PASSIVES, list);
        if (sourceUuid != null) {
            NbtCompound stolenFrom = data.getCompound(KEY_STOLEN_FROM).orElse(new NbtCompound());
            stolenFrom.put(passiveId.toString(), GemsNbt.fromUuid(sourceUuid));
            data.put(KEY_STOLEN_FROM, stolenFrom);
            ServerPlayerEntity victim = player.getEntityWorld().getServer() != null
                    ? player.getEntityWorld().getServer().getPlayerManager().getPlayer(sourceUuid)
                    : null;
            if (victim != null) {
                markLostPassive(victim, passiveId, player.getUuid());
                GemPowers.sync(victim);
            }
            markKillUsed(player);
        }
        com.feel.gems.net.StolenStateSync.send(player);
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
        UUID source = findSourceForPassive(data, passiveId);
        NbtList list = new NbtList();
        for (Identifier id : next) {
            list.add(NbtString.of(id.toString()));
        }
        data.put(KEY_STOLEN_PASSIVES, list);
        // Preserve stolen-from mapping so victims can recover on kill even if the passive is unselected.
        GemPassive passive = ModPassives.get(passiveId);
        if (passive != null) {
            passive.remove(player);
        }
        if (source != null && player.getEntityWorld().getServer() != null) {
            ServerPlayerEntity victim = player.getEntityWorld().getServer().getPlayerManager().getPlayer(source);
            if (victim != null) {
                clearLostPassive(victim, passiveId);
                GemPowers.sync(victim);
            }
        }
        com.feel.gems.net.StolenStateSync.send(player);
        player.sendMessage(Text.translatable("gems.item.trophy_necklace.unstolen", passive != null ? passive.name() : passiveId.getPath()).formatted(Formatting.GRAY), true);
        return true;
    }

    /**
     * Re-open the last Trophy Necklace UI session, if available (used after claim toggles).
     */
    public static boolean openLastTargetScreen(ServerPlayerEntity player) {
        if (player == null || player.getEntityWorld().getServer() == null) {
            return false;
        }
        NbtCompound data = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        String targetName = data.getString(KEY_LAST_TARGET_NAME, "");
        NbtList offeredList = data.getList(KEY_LAST_OFFERED).orElse(null);
        if (offeredList == null || offeredList.isEmpty()) {
            return false;
        }
        java.util.HashSet<Identifier> offered = new java.util.HashSet<>();
        for (int i = 0; i < offeredList.size(); i++) {
            Identifier id = Identifier.tryParse(offeredList.getString(i, ""));
            if (id != null) {
                offered.add(id);
            }
        }
        if (offered.isEmpty()) {
            return false;
        }
        sendScreenPayload(player, targetName.isBlank() ? "Unknown" : targetName, offered);
        return true;
    }

    public static UUID getLastTargetUuid(ServerPlayerEntity player) {
        if (player == null) {
            return null;
        }
        NbtCompound data = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        return GemsNbt.getUuid(data, KEY_LAST_TARGET_UUID);
    }

    public static boolean wasLastOffered(ServerPlayerEntity player, Identifier passiveId) {
        if (player == null || passiveId == null) {
            return false;
        }
        NbtCompound data = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        NbtList offeredList = data.getList(KEY_LAST_OFFERED).orElse(null);
        if (offeredList == null || offeredList.isEmpty()) {
            return false;
        }
        String raw = passiveId.toString();
        for (int i = 0; i < offeredList.size(); i++) {
            if (raw.equals(offeredList.getString(i, ""))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, TooltipDisplayComponent displayComponent, Consumer<Text> tooltip, TooltipType type) {
        GemsTooltipFormat.appendDescription(
                tooltip,
                Text.translatable("gems.item.trophy_necklace.tooltip.1"),
                Text.translatable("gems.item.trophy_necklace.tooltip.2"),
                Text.translatable("gems.item.trophy_necklace.tooltip.3")
        );
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
        // Don't offer passives already stolen from this target.
        offered.removeAll(stolenFromTarget(((GemsPersistentDataHolder) opener).gems$getPersistentData(), target.getUuid()));
        offered.removeIf(id -> ModPassives.get(id) == null);
        if (offered.isEmpty()) {
            opener.sendMessage(Text.translatable("gems.item.trophy_necklace.no_passives").formatted(Formatting.RED), true);
            return;
        }
        // Snapshot for the session so the target can switch gems without affecting this selection.
        NbtCompound data = ((GemsPersistentDataHolder) opener).gems$getPersistentData();
        data.putString(KEY_LAST_TARGET_NAME, targetName);
        GemsNbt.putUuid(data, KEY_LAST_TARGET_UUID, target.getUuid());
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

    public static void restoreStolenOnKill(ServerPlayerEntity killer, ServerPlayerEntity thief) {
        if (killer == null || thief == null) {
            return;
        }
        NbtCompound thiefData = ((GemsPersistentDataHolder) thief).gems$getPersistentData();
        NbtCompound stolenFrom = thiefData.getCompound(KEY_STOLEN_FROM).orElse(null);
        if (stolenFrom == null || stolenFrom.getKeys().isEmpty()) {
            return;
        }

        java.util.List<Identifier> recovered = new java.util.ArrayList<>();
        for (String key : java.util.List.copyOf(stolenFrom.getKeys())) {
            java.util.UUID source = GemsNbt.toUuid(stolenFrom.get(key));
            if (source == null || !source.equals(killer.getUuid())) {
                continue;
            }
            Identifier id = Identifier.tryParse(key);
            if (id != null) {
                recovered.add(id);
            }
            stolenFrom.remove(key);
        }

        if (stolenFrom.isEmpty()) {
            thiefData.remove(KEY_STOLEN_FROM);
        } else {
            thiefData.put(KEY_STOLEN_FROM, stolenFrom);
        }

        if (recovered.isEmpty()) {
            return;
        }

        for (Identifier id : recovered) {
            removeStolenPassiveSilent(thiefData, id);
            clearLostPassive(killer, id);
        }

        GemPowers.sync(killer);
        GemPowers.sync(thief);
        com.feel.gems.net.StolenStateSync.send(thief);
    }

    private static void removeStolenSource(NbtCompound data, Identifier passiveId) {
        NbtCompound stolenFrom = data.getCompound(KEY_STOLEN_FROM).orElse(null);
        if (stolenFrom == null) {
            return;
        }
        stolenFrom.remove(passiveId.toString());
        if (stolenFrom.isEmpty()) {
            data.remove(KEY_STOLEN_FROM);
        } else {
            data.put(KEY_STOLEN_FROM, stolenFrom);
        }
    }

    private static void removeStolenPassiveSilent(NbtCompound data, Identifier passiveId) {
        NbtList list = data.getList(KEY_STOLEN_PASSIVES).orElse(null);
        if (list == null || list.isEmpty()) {
            return;
        }
        String raw = passiveId.toString();
        NbtList next = new NbtList();
        for (int i = 0; i < list.size(); i++) {
            String entry = list.getString(i, "");
            if (!raw.equals(entry)) {
                next.add(NbtString.of(entry));
            }
        }
        if (next.isEmpty()) {
            data.remove(KEY_STOLEN_PASSIVES);
        } else {
            data.put(KEY_STOLEN_PASSIVES, next);
        }
        removeStolenSource(data, passiveId);
    }

    private static void addStolenPassiveForced(ServerPlayerEntity player, Identifier passiveId) {
        if (player == null || passiveId == null || ModPassives.get(passiveId) == null) {
            return;
        }
        NbtCompound data = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        Set<Identifier> current = getStolenPassives(player);
        if (current.contains(passiveId)) {
            return;
        }
        NbtList list = new NbtList();
        for (Identifier id : current) {
            list.add(NbtString.of(id.toString()));
        }
        list.add(NbtString.of(passiveId.toString()));
        data.put(KEY_STOLEN_PASSIVES, list);
        NbtCompound stolenFrom = data.getCompound(KEY_STOLEN_FROM).orElse(new NbtCompound());
        stolenFrom.put(passiveId.toString(), GemsNbt.fromUuid(player.getUuid()));
        data.put(KEY_STOLEN_FROM, stolenFrom);
    }

    public static boolean isPassiveStolenFrom(ServerPlayerEntity player, Identifier passiveId) {
        if (player == null || passiveId == null) {
            return false;
        }
        NbtCompound data = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        NbtCompound lost = data.getCompound(KEY_LOST_PASSIVES).orElse(null);
        return lost != null && lost.contains(passiveId.toString());
    }

    private static void markKillSession(ServerPlayerEntity killer, ServerPlayerEntity victim) {
        if (killer == null || victim == null) {
            return;
        }
        NbtCompound data = ((GemsPersistentDataHolder) killer).gems$getPersistentData();
        GemsNbt.putUuid(data, KEY_LAST_KILL_TARGET_UUID, victim.getUuid());
        data.putBoolean(KEY_LAST_KILL_USED, false);
    }

    private static void markKillUsed(ServerPlayerEntity player) {
        NbtCompound data = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        data.putBoolean(KEY_LAST_KILL_USED, true);
    }

    private static boolean canClaimFromKill(ServerPlayerEntity player, UUID targetUuid) {
        if (player == null || targetUuid == null) {
            return false;
        }
        NbtCompound data = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        UUID lastKill = GemsNbt.getUuid(data, KEY_LAST_KILL_TARGET_UUID);
        if (lastKill == null || !lastKill.equals(targetUuid)) {
            return false;
        }
        return !data.getBoolean(KEY_LAST_KILL_USED).orElse(false);
    }

    private static void markLostPassive(ServerPlayerEntity victim, Identifier passiveId, UUID thiefUuid) {
        if (victim == null || passiveId == null || thiefUuid == null) {
            return;
        }
        NbtCompound data = ((GemsPersistentDataHolder) victim).gems$getPersistentData();
        NbtCompound lost = data.getCompound(KEY_LOST_PASSIVES).orElse(new NbtCompound());
        lost.put(passiveId.toString(), GemsNbt.fromUuid(thiefUuid));
        data.put(KEY_LOST_PASSIVES, lost);
    }

    private static void clearLostPassive(ServerPlayerEntity victim, Identifier passiveId) {
        if (victim == null || passiveId == null) {
            return;
        }
        NbtCompound data = ((GemsPersistentDataHolder) victim).gems$getPersistentData();
        NbtCompound lost = data.getCompound(KEY_LOST_PASSIVES).orElse(null);
        if (lost == null) {
            return;
        }
        lost.remove(passiveId.toString());
        if (lost.isEmpty()) {
            data.remove(KEY_LOST_PASSIVES);
        } else {
            data.put(KEY_LOST_PASSIVES, lost);
        }
    }

    private static Identifier findStolenFromSource(NbtCompound data, UUID sourceUuid) {
        if (data == null || sourceUuid == null) {
            return null;
        }
        NbtCompound stolenFrom = data.getCompound(KEY_STOLEN_FROM).orElse(null);
        if (stolenFrom == null || stolenFrom.getKeys().isEmpty()) {
            return null;
        }
        for (String key : stolenFrom.getKeys()) {
            UUID source = GemsNbt.toUuid(stolenFrom.get(key));
            if (source != null && source.equals(sourceUuid)) {
                return Identifier.tryParse(key);
            }
        }
        return null;
    }

    private static Set<Identifier> stolenFromTarget(NbtCompound data, UUID targetUuid) {
        if (data == null || targetUuid == null) {
            return Set.of();
        }
        NbtCompound stolenFrom = data.getCompound(KEY_STOLEN_FROM).orElse(null);
        if (stolenFrom == null || stolenFrom.getKeys().isEmpty()) {
            return Set.of();
        }
        java.util.HashSet<Identifier> out = new java.util.HashSet<>();
        for (String key : stolenFrom.getKeys()) {
            UUID source = GemsNbt.toUuid(stolenFrom.get(key));
            if (source != null && source.equals(targetUuid)) {
                Identifier id = Identifier.tryParse(key);
                if (id != null) {
                    out.add(id);
                }
            }
        }
        return out;
    }

    private static UUID findSourceForPassive(NbtCompound data, Identifier passiveId) {
        if (data == null || passiveId == null) {
            return null;
        }
        NbtCompound stolenFrom = data.getCompound(KEY_STOLEN_FROM).orElse(null);
        if (stolenFrom == null) {
            return null;
        }
        return GemsNbt.toUuid(stolenFrom.get(passiveId.toString()));
    }
}
