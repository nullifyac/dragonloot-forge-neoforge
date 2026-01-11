package net.dragonloot.mixin;

import net.dragonloot.init.ItemInit;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {

    protected LivingEntityMixin(EntityType<?> type, World level) {
        super(type, level);
    }

    @Inject(method = "updateFallFlying", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;setSharedFlag(IZ)V", ordinal = 0), cancellable = true)
    private void dragonloot$keepElytraFlag(CallbackInfo info) {
        LivingEntity living = (LivingEntity) (Object) this;
        boolean flag = this.getSharedFlag(7);
        if (flag && !this.isOnGround() && !this.isPassenger() && living.getItemBySlot(EquipmentSlotType.CHEST).getItem() == ItemInit.UPGRADED_DRAGON_CHESTPLATE.get()) {
            this.setSharedFlag(7, true);
            info.cancel();
        }
    }
}
