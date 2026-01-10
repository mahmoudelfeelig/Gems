package com.feel.gems.mixin;

import com.feel.gems.power.ability.trickster.TricksterMindGamesRuntime;
import com.feel.gems.power.ability.trickster.TricksterPuppetRuntime;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.world.ServerWorld;
import java.util.Set;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;




@Mixin(MobEntity.class)
public abstract class MobEntityTricksterEffectsMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void gems$tickTricksterEffects(CallbackInfo ci) {
        MobEntity self = (MobEntity) (Object) this;
        if (!(self.getEntityWorld() instanceof ServerWorld world)) {
            return;
        }
        // Avoid per-tick work on every mob: only do anything if the mob has relevant tags.
        Set<String> tags = self.getCommandTags();
        if (tags.isEmpty()) {
            return;
        }
        boolean puppeted = tags.contains("gems_puppeted");
        boolean confused = tags.contains("gems_confused");
        if (!puppeted && !confused) {
            return;
        }
        // Puppeting needs to feel responsive; mind games can be a bit lower frequency.
        if (puppeted) {
            TricksterPuppetRuntime.tickPuppetedMob(self, world);
        }
        if (confused && world.getTime() % 5 == 0) {
            TricksterMindGamesRuntime.tickConfusedMob(self, world);
        }
    }
}
