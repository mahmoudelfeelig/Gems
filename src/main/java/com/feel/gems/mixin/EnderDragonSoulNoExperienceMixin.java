package com.feel.gems.mixin;

import com.feel.gems.power.gem.astra.SoulSummons;
import com.feel.gems.power.gem.summoner.SummonerSummons;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;




@Mixin(EnderDragonEntity.class)
public abstract class EnderDragonSoulNoExperienceMixin {
    @Redirect(
            method = "updatePostDeath",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/ExperienceOrbEntity;spawn(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/util/math/Vec3d;I)V"
            ),
            require = 0
    )
    private void gems$soulNoDragonXp(ServerWorld world, Vec3d pos, int amount) {
        EnderDragonEntity self = (EnderDragonEntity) (Object) this;
        if (SoulSummons.isSoul(self) || SummonerSummons.isSummon(self)) {
            return;
        }
        ExperienceOrbEntity.spawn(world, pos, amount);
    }
}

