package com.feel.gems.mixin;

import com.feel.gems.legendary.HypnoControl;
import com.feel.gems.power.gem.astra.SoulSummons;
import com.feel.gems.power.gem.summoner.SummonerSummons;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.SlimeEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;




@Mixin(SlimeEntity.class)
public abstract class SlimeEntitySummonNoSplitMixin {
    @Redirect(
            method = "remove",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;spawnEntity(Lnet/minecraft/entity/Entity;)Z")
    )
    private boolean gems$skipSummonSplit(World world, Entity entity) {
        SlimeEntity self = (SlimeEntity) (Object) this;
        if (SummonerSummons.isSummon(self) || SoulSummons.isSoul(self) || HypnoControl.isHypno(self)) {
            return false;
        }
        return world.spawnEntity(entity);
    }
}
