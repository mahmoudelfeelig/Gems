package com.feel.gems.item.legendary;

import com.feel.gems.GemsMod;
import com.feel.gems.config.GemsBalance;
import com.feel.gems.legendary.LegendaryItem;
import com.feel.gems.power.util.Targeting;
import java.util.List;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.Item.TooltipContext;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;




public final class DemolitionBladeItem extends SwordItem implements LegendaryItem {
    public DemolitionBladeItem(ToolMaterial material, Settings settings) {
        super(material, settings);
    }

    @Override
    public String legendaryId() {
        return Identifier.of(GemsMod.MOD_ID, "demolition_blade").toString();
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (world.isClient) {
            return TypedActionResult.success(stack);
        }
        if (!(user instanceof ServerPlayerEntity player)) {
            return TypedActionResult.pass(stack);
        }
        if (player.getItemCooldownManager().isCoolingDown(this)) {
            return TypedActionResult.success(stack);
        }
        int cooldown = GemsBalance.v().legendary().demolitionCooldownTicks();
        int cooldownScale = GemsBalance.v().legendary().demolitionCooldownScalePercent();
        if (cooldown > 0 && cooldownScale != 100) {
            cooldown = Math.max(0, Math.round(cooldown * (cooldownScale / 100.0F)));
        }
        if (cooldown > 0) {
            player.getItemCooldownManager().set(this, cooldown);
        }
        int range = GemsBalance.v().legendary().demolitionRangeBlocks();
        LivingEntity target = Targeting.raycastLiving(player, range);
        Vec3d spawnPos = null;
        if (target != null) {
            spawnPos = target.getPos().add(0.0D, 0.2D, 0.0D);
        } else {
            HitResult hit = player.raycast(range, 1.0F, false);
            if (hit instanceof BlockHitResult blockHit) {
                Vec3d pos = blockHit.getPos();
                spawnPos = new Vec3d(pos.x, pos.y + 0.1D, pos.z);
            }
        }
        if (spawnPos == null) {
            player.sendMessage(Text.literal("No target for demolition charge."), true);
            return TypedActionResult.success(stack);
        }
        int fuse = Math.max(1, GemsBalance.v().legendary().demolitionFuseTicks());
        int tntCount = GemsBalance.v().legendary().demolitionTntCount();
        for (int i = 0; i < tntCount; i++) {
            TntEntity tnt = new TntEntity(world, spawnPos.x, spawnPos.y, spawnPos.z, player);
            tnt.setFuse(fuse);
            world.spawnEntity(tnt);
        }
        return TypedActionResult.success(stack);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.translatable("item.gems.demolition_blade.desc"));
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return true;
    }
}
