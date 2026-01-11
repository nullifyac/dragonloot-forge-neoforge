package net.dragonloot.mixin;

import net.dragonloot.init.ItemInit;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerEntityMixin extends LivingEntity {

    protected PlayerEntityMixin(EntityType<? extends LivingEntity> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "tryToStartFallFlying", at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/world/entity/player/Player;getItemBySlot(Lnet/minecraft/world/entity/EquipmentSlot;)Lnet/minecraft/world/item/ItemStack;"), cancellable = true)
    private void dragonloot$forceFallFlying(CallbackInfoReturnable<Boolean> cir) {
        if (this.getItemBySlot(EquipmentSlot.CHEST).is(ItemInit.UPGRADED_DRAGON_CHESTPLATE.get())) {
            this.startFallFlying();
            cir.setReturnValue(true);
        }
    }

    @Shadow
    public abstract void startFallFlying();
}
