package com.feel.gems.item.legendary;

import com.feel.gems.GemsMod;
import com.feel.gems.config.GemsBalance;
import com.feel.gems.legendary.LegendaryItem;
import java.util.List;
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
        if (cooldown > 0) {
            player.getItemCooldownManager().set(this, cooldown);
        }
        Vec3d eye = player.getCameraPosVec(1.0F);
        Vec3d dir = player.getRotationVec(1.0F).normalize();
        Vec3d base = eye.add(dir.multiply(2.0D));
        int fuse = GemsBalance.v().legendary().demolitionFuseTicks();
        for (int i = 0; i < 3; i++) {
            double ox = (world.getRandom().nextDouble() - 0.5D) * 0.4D;
            double oy = (world.getRandom().nextDouble() - 0.5D) * 0.2D;
            double oz = (world.getRandom().nextDouble() - 0.5D) * 0.4D;
            TntEntity tnt = new TntEntity(world, base.x + ox, base.y + oy, base.z + oz, player);
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
