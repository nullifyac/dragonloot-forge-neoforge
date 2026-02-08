package net.dragonloot.mixin;

import net.dragonloot.compat.AdvancedNetheriteCompat;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.monster.PhantomEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PhantomEntity.class)
public abstract class PhantomPacifyMixin extends MobEntity {

    protected PhantomPacifyMixin(EntityType<? extends MobEntity> entityType, World level) {
        super(entityType, level);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void dragonloot$pacifyPhantom(CallbackInfo ci) {
        PhantomEntity phantom = (PhantomEntity) (Object) this;
        LivingEntity target = phantom.getTarget();

        if (!(target instanceof PlayerEntity)) {
            return;
        }

        if (phantom.getLastHurtByMob() == target) {
            return;
        }

        PlayerEntity player = (PlayerEntity) target;
        if (AdvancedNetheriteCompat.isWearingPacifyingArmor(player)) {
            phantom.setTarget(null);
        }
    }
}
