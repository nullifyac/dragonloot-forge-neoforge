package net.dragonloot.mixin;

import net.dragonloot.init.ItemInit;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {

    protected LivingEntityMixin(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Inject(method = "updateFallFlying", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;setSharedFlag(IZ)V", ordinal = 0), cancellable = true)
    private void dragonloot$keepElytraFlag(CallbackInfo info) {
        LivingEntity living = (LivingEntity) (Object) this;
        boolean flag = this.getSharedFlag(7);
        if (flag && !this.onGround() && !this.isPassenger() && living.getItemBySlot(EquipmentSlot.CHEST).is(ItemInit.UPGRADED_DRAGON_CHESTPLATE.get())) {
            this.setSharedFlag(7, true);
            info.cancel();
        }
    }
}