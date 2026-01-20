package com.feel.gems.item.legendary;

import com.feel.gems.GemsMod;
import com.feel.gems.legendary.LegendaryItem;
import com.feel.gems.state.GemsPersistentDataHolder;
import com.feel.gems.util.GemsNbt;
import com.feel.gems.util.GemsTime;
import com.feel.gems.util.GemsTooltipFormat;
import com.feel.gems.power.util.Targeting;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.minecraft.util.math.Vec3d;




public final class HunterSightBowItem extends BowItem implements LegendaryItem {
    private static final String KEY_LAST_TARGET = "legendaryHuntersSightTarget";
    private static final String KEY_LAST_SEEN = "legendaryHuntersSightLastSeen";
    private static final double ASSIST_RANGE = 50.0D;

    public HunterSightBowItem(Settings settings) {
        super(settings.enchantable(15));
    }

    @Override
    public String legendaryId() {
        return Identifier.of(GemsMod.MOD_ID, "hunters_sight_bow").toString();
    }

    @Override
    public boolean onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
        if (!world.isClient() && user instanceof net.minecraft.server.network.ServerPlayerEntity player) {
            LivingEntity target = getAssistTarget(player);
            if (target != null) {
                float oldYaw = player.getYaw();
                float oldPitch = player.getPitch();
                float oldHead = player.getHeadYaw();
                int useTicks = getMaxUseTime(stack, user) - remainingUseTicks;
                aimAt(player, target, useTicks);
                boolean fired = super.onStoppedUsing(stack, world, user, remainingUseTicks);
                player.setYaw(oldYaw);
                player.setPitch(oldPitch);
                player.setHeadYaw(oldHead);
                return fired;
            }
        }
        return super.onStoppedUsing(stack, world, user, remainingUseTicks);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, TooltipDisplayComponent displayComponent, Consumer<Text> tooltip, TooltipType type) {
        GemsTooltipFormat.appendDescription(tooltip, Text.translatable("item.gems.hunters_sight_bow.desc"));
    }

    public static void recordHit(net.minecraft.server.network.ServerPlayerEntity attacker, LivingEntity target) {
        if (attacker == null || target == null || target == attacker) {
            return;
        }
        var data = ((GemsPersistentDataHolder) attacker).gems$getPersistentData();
        UUID uuid = target.getUuid();
        GemsNbt.putUuid(data, KEY_LAST_TARGET, uuid);
        data.putLong(KEY_LAST_SEEN, GemsTime.now(attacker));
    }

    public static LivingEntity getAssistTarget(net.minecraft.server.network.ServerPlayerEntity player) {
        if (player == null) {
            return null;
        }
        var data = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        UUID uuid = GemsNbt.getUuid(data, KEY_LAST_TARGET);
        if (uuid != null) {
            Entity entity = player.getEntityWorld().getEntity(uuid);
            if (entity instanceof LivingEntity living && living.isAlive()) {
                if (player.squaredDistanceTo(living) <= ASSIST_RANGE * ASSIST_RANGE && player.canSee(living)) {
                    return living;
                }
            }
        }

        return Targeting.raycastLiving(player, ASSIST_RANGE);
    }

    private static void aimAt(LivingEntity user, LivingEntity target, int useTicks) {
        Vec3d from = user.getEyePos();
        Vec3d targetPos = target.getEyePos();
        Vec3d relativeVel = target.getVelocity().subtract(user.getVelocity());
        double speed = Math.max(0.1D, arrowSpeed(useTicks));
        double distance = from.distanceTo(targetPos);
        double time = Math.min(3.0D, distance / speed);
        Vec3d predicted = targetPos.add(relativeVel.multiply(time));
        Vec3d to = predicted.subtract(from);
        float yaw = (float) (Math.toDegrees(Math.atan2(to.z, to.x)) - 90.0D);
        float pitch = (float) (-Math.toDegrees(Math.atan2(to.y, Math.sqrt(to.x * to.x + to.z * to.z))));
        user.setYaw(yaw);
        user.setPitch(pitch);
        user.setHeadYaw(yaw);
    }

    private static double arrowSpeed(int useTicks) {
        float pull = Math.max(0.0F, useTicks) / 20.0F;
        pull = (pull * pull + pull * 2.0F) / 3.0F;
        if (pull > 1.0F) {
            pull = 1.0F;
        }
        return pull * 3.0F;
    }
}
