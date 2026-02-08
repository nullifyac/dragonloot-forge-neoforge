package net.dragonloot.mixin;

import net.dragonloot.compat.AdvancedNetheriteCompat;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PiglinAi.class)
public abstract class PiglinPacifyMixin {

    @Inject(method = "isWearingGold", at = @At("RETURN"), cancellable = true, require = 0)
    private static void dragonloot$pacifyPiglin(LivingEntity entity, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) {
            return;
        }

        if (!(entity instanceof Player player)) {
            return;
        }

        if (AdvancedNetheriteCompat.isWearingPacifyingArmor(player)) {
            cir.setReturnValue(true);
        }
    }
}
