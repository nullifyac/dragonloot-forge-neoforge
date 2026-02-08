package net.dragonloot.mixin;

import net.dragonloot.compat.AdvancedNetheriteCompat;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Phantom.class)
public abstract class PhantomPacifyMixin extends Mob implements Enemy {

    protected PhantomPacifyMixin(EntityType<? extends Mob> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void dragonloot$pacifyPhantom(CallbackInfo ci) {
        Phantom phantom = (Phantom) (Object) this;
        LivingEntity target = phantom.getTarget();

        if (!(target instanceof Player player)) {
            return;
        }

        if (phantom.getLastHurtByMob() == target) {
            return;
        }

        if (AdvancedNetheriteCompat.isWearingPacifyingArmor(player)) {
            phantom.setTarget(null);
        }
    }
}
