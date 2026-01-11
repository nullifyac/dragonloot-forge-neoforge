package net.dragonloot.mixin;

import net.dragonloot.init.ItemInit;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin extends LivingEntity {

    protected PlayerEntityMixin(EntityType<? extends LivingEntity> entityType, World level) {
        super(entityType, level);
    }

    @Inject(method = "tryToStartFallFlying", at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/entity/player/PlayerEntity;getItemBySlot(Lnet/minecraft/inventory/EquipmentSlotType;)Lnet/minecraft/item/ItemStack;"), cancellable = true)
    private void dragonloot$forceFallFlying(CallbackInfoReturnable<Boolean> cir) {
        if (this.getItemBySlot(EquipmentSlotType.CHEST).getItem() == ItemInit.UPGRADED_DRAGON_CHESTPLATE.get()) {
            this.startFallFlying();
            cir.setReturnValue(true);
        }
    }

    @Shadow
    public abstract void startFallFlying();
}
