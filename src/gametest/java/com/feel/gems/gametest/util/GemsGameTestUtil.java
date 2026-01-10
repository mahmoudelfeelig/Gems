package com.feel.gems.gametest.util;

import com.feel.gems.assassin.AssassinState;
import com.feel.gems.item.GemItem;
import com.mojang.authlib.GameProfile;
import com.feel.gems.state.GemsPersistentDataHolder;
import java.util.List;
import java.util.UUID;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkSide;
import net.minecraft.server.network.ConnectedClientData;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.test.TestContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.GameMode;
import net.minecraft.world.rule.GameRules;




public final class GemsGameTestUtil {
    private GemsGameTestUtil() {
    }

    public static void forceSurvival(ServerPlayerEntity player) {
        player.changeGameMode(GameMode.SURVIVAL);
        player.setInvulnerable(false);
        var abilities = player.getAbilities();
        abilities.invulnerable = false;
        abilities.creativeMode = false;
        abilities.allowFlying = false;
        abilities.flying = false;
        player.sendAbilitiesUpdate();

        // GameTests run with `gamerule pvp` off by default; player-vs-player damage relies on this being true.
        if (player.getEntityWorld() instanceof ServerWorld world) {
            var server = world.getServer();
            if (server != null) {
                world.getGameRules().setValue(GameRules.PVP, true, server);
            }
        }

        // GameTest players can end up on the same scoreboard team with friendly-fire disabled, which blocks PvP damage.
        var team = player.getScoreboardTeam();
        if (team != null && player.getEntityWorld().getServer() != null) {
            player.getEntityWorld().getServer().getScoreboard().removeScoreHolderFromTeam(player.getNameForScoreboard(), team);
        }
    }

    public static boolean hasItem(ServerPlayerEntity player, Item item) {
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);
            if (stack.isOf(item)) {
                return true;
            }
        }
        return false;
    }

    public static int countItem(ServerPlayerEntity player, Item item) {
        int count = 0;
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);
            if (stack.isOf(item)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    public static int countGlint(ServerPlayerEntity player, Item item) {
        int glint = 0;
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);
            if (stack.isOf(item) && stack.contains(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE)) {
                glint++;
            }
        }
        return glint;
    }

    public static int countGemItems(List<ItemStack> stacks) {
        int gemItems = 0;
        for (ItemStack stack : stacks) {
            if (stack.getItem() instanceof GemItem) {
                gemItems++;
            }
        }
        return gemItems;
    }

    public static int countGemItems(ServerPlayerEntity player) {
        var inventory = player.getInventory();
        int gemItems = 0;
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);
            if (stack.getItem() instanceof GemItem) {
                gemItems++;
            }
        }
        return gemItems;
    }

    public static boolean containsAirMace(ServerPlayerEntity player) {
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.size(); i++) {
            if (isAirMace(inventory.getStack(i))) {
                return true;
            }
        }
        return false;
    }

    public static int countAirMaces(ServerPlayerEntity player) {
        int count = 0;
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.size(); i++) {
            if (isAirMace(inventory.getStack(i))) {
                count++;
            }
        }
        return count;
    }

    public static void resetAssassinState(ServerPlayerEntity player) {
        AssassinState.initIfNeeded(player);
        var data = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        data.putBoolean("assassinIsAssassin", false);
        data.putBoolean("assassinEliminated", false);
        data.putInt("assassinHearts", AssassinState.maxHearts());
    }

    public static ServerPlayerEntity createMockCreativeServerPlayer(TestContext context) {
        // TestContext#createMockCreativeServerPlayerInWorld returns a ServerPlayerEntity subclass whose
        // getGameMode() is hard-overridden to CREATIVE, which makes it impossible to test survival PvP damage.
        // We build an actual ServerPlayerEntity + connect it through PlayerManager so game mode is real and mutable.
        ServerWorld world = context.getWorld();
        var server = world.getServer();
        if (server == null) {
            throw new IllegalStateException("GameTest server is null");
        }

        // Game profiles must have a <=16 char name using [A-Za-z0-9_]; otherwise things like player heads fail to serialize.
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        GameProfile profile = new GameProfile(UUID.randomUUID(), "test" + suffix);
        ConnectedClientData data = ConnectedClientData.createDefault(profile, false);

        ServerPlayerEntity player = new ServerPlayerEntity(server, world, data.gameProfile(), data.syncedOptions());

        ClientConnection connection = new ClientConnection(NetworkSide.SERVERBOUND);
        new EmbeddedChannel(connection);

        server.getPlayerManager().onPlayerConnect(connection, player, data);

        // Newly-connected players are considered "loading" for ~60 ticks and are invulnerable until loaded.
        // GameTests commonly apply damage very early (e.g., tick 5), so fast-forward the loading timer.
        if (player.networkHandler != null) {
            for (int i = 0; i < 80 && !player.networkHandler.canInteractWithGame(); i++) {
                player.networkHandler.tickLoading();
            }
        }
        return player;
    }

    private static boolean isAirMace(ItemStack stack) {
        if (!stack.isOf(Items.MACE)) {
            return false;
        }
        var custom = stack.get(DataComponentTypes.CUSTOM_DATA);
        return custom != null && custom.copyNbt().getBoolean("gemsAirMace", false);
    }
}
