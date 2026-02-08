package net.dragonloot.mixin;

import net.dragonloot.compat.AdvancedNetheriteCompat;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.monster.piglin.PiglinTasks;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PiglinTasks.class)
public abstract class PiglinPacifyMixin {

    @Inject(method = "isWearingGold", at = @At("RETURN"), cancellable = true, require = 0)
    private static void dragonloot$pacifyPiglin(LivingEntity entity, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) {
            return;
        }

        if (!(entity instanceof PlayerEntity)) {
            return;
        }

        PlayerEntity player = (PlayerEntity) entity;
        if (AdvancedNetheriteCompat.isWearingPacifyingArmor(player)) {
            cir.setReturnValue(true);
        }
    }
}

