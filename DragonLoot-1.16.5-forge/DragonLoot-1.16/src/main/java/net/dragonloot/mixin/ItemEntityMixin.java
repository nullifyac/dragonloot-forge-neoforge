package net.dragonloot.mixin;

import net.dragonloot.init.TagInit;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.util.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin {

    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void dragonloot$preventExplosionDestruction(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        ItemEntity self = (ItemEntity) (Object) this;
        if (TagInit.NOT_DESTROYED_BY_EXPLOSION.contains(self.getItem().getItem()) && source.isExplosion()) {
            cir.setReturnValue(false);
        }
    }
}
