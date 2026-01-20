package com.feel.gems.gametest.util;

import com.feel.gems.assassin.AssassinState;
import com.feel.gems.core.GemId;
import com.feel.gems.item.GemItem;
import com.feel.gems.power.runtime.GemPowers;
import com.feel.gems.state.GemPlayerState;
import com.mojang.authlib.GameProfile;
import com.feel.gems.state.GemsPersistentDataHolder;
import java.util.List;
import java.util.UUID;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.block.Blocks;
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
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.minecraft.world.rule.GameRules;
import java.util.function.BooleanSupplier;




public final class GemsGameTestUtil {
    private static final int COMPLETE_DELAY_TICKS = Integer.getInteger("gems.gametest.complete-delay", 10);

    private GemsGameTestUtil() {
    }

    public static void complete(TestContext context) {
        completeAfter(context, COMPLETE_DELAY_TICKS);
    }

    public static void completeAfter(TestContext context, long delayTicks) {
        if (delayTicks <= 0L) {
            context.complete();
            return;
        }
        context.runAtTick(delayTicks, context::complete);
    }

    public static void assertEventually(TestContext context, long startTick, long timeoutTick, long stepTicks, BooleanSupplier condition, String failMessage) {
        long step = Math.max(1L, stepTicks);
        context.runAtTick(startTick, () -> pollCondition(context, startTick, timeoutTick, step, condition, failMessage));
    }

    private static void pollCondition(TestContext context, long currentTick, long timeoutTick, long stepTicks, BooleanSupplier condition, String failMessage) {
        if (condition.getAsBoolean()) {
            context.complete();
            return;
        }
        if (currentTick >= timeoutTick) {
            context.throwGameTestException(failMessage);
            return;
        }
        context.runAtTick(currentTick + stepTicks, () -> pollCondition(context, currentTick + stepTicks, timeoutTick, stepTicks, condition, failMessage));
    }

    public static void assertStaysTrue(TestContext context, long startTick, long endTick, long stepTicks, BooleanSupplier condition, String failMessage) {
        long step = Math.max(1L, stepTicks);
        context.runAtTick(startTick, () -> pollStaysTrue(context, startTick, endTick, step, condition, failMessage));
    }

    private static void pollStaysTrue(TestContext context, long currentTick, long endTick, long stepTicks, BooleanSupplier condition, String failMessage) {
        if (!condition.getAsBoolean()) {
            context.throwGameTestException(failMessage);
            return;
        }
        if (currentTick >= endTick) {
            context.complete();
            return;
        }
        context.runAtTick(currentTick + stepTicks, () -> pollStaysTrue(context, currentTick + stepTicks, endTick, stepTicks, condition, failMessage));
    }

    public static void placeStoneFloor(TestContext context, int radius) {
        placeStoneFloor(context, 1, radius);
    }

    public static void placeStoneFloor(TestContext context, int y, int radius) {
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                context.setBlockState(x, y, z, Blocks.STONE.getDefaultState());
            }
        }
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

    public static void resetPlayerForTest(ServerPlayerEntity player) {
        player.clearStatusEffects();
        player.setFireTicks(0);
        player.extinguish();
        player.setVelocity(Vec3d.ZERO);
        player.setAbsorptionAmount(0.0F);

        var inst = player.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.MAX_HEALTH);
        if (inst != null) {
            inst.setBaseValue(20.0D);
        }
        player.setHealth(player.getMaxHealth());

        var hunger = player.getHungerManager();
        hunger.setFoodLevel(20);
        hunger.setSaturationLevel(5.0F);
        hunger.addExhaustion(-10.0F);

        player.getInventory().clear();
        player.experienceLevel = 0;
        player.totalExperience = 0;
        player.experienceProgress = 0.0F;
        player.stopUsingItem();

        if (player.getEntityWorld() instanceof ServerWorld world && world.getServer() != null) {
            com.feel.gems.bonus.BonusClaimsState.get(world.getServer()).releaseAllClaims(player.getUuid());
        }
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

        // Ensure a clean per-player state between GameTests, even if the run directory persisted playerdata.
        ((GemsPersistentDataHolder) player).gems$setPersistentData(new net.minecraft.nbt.NbtCompound());
        resetPlayerForTest(player);

        // GameTests should be deterministic. The production init flow assigns a random gem at first join,
        // which makes tests flaky (e.g., the "enemy" rolling VOID and becoming immune to effects).
        GemPlayerState.resetToNew(player, GemId.ASTRA);
        GemPowers.sync(player);
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
