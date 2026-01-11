package net.dragonloot.mixin;

import net.dragonloot.init.ItemInit;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.AbstractArrowEntity;
import net.minecraft.item.ArrowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(CrossbowItem.class)
public class CrossbowItemMixin {

    @Inject(method = "getArrow", at = @At("RETURN"), locals = LocalCapture.CAPTURE_FAILSOFT, cancellable = true)
    private static void dragonloot$scaleArrow(World level, LivingEntity entity, ItemStack crossbow, ItemStack arrowStack, CallbackInfoReturnable<AbstractArrowEntity> cir, ArrowItem arrowItem, AbstractArrowEntity arrow) {
        if (crossbow.getItem() == ItemInit.DRAGON_CROSSBOW_ITEM.get()) {
            arrow.setBaseDamage(arrow.getBaseDamage() * 1.25F + 1.0F);
            cir.setReturnValue(arrow);
        }
    }

    @Inject(method = "getShootingPower", at = @At("HEAD"), cancellable = true)
    private static void dragonloot$adjustSpeed(ItemStack stack, CallbackInfoReturnable<Float> cir) {
        if (stack.getItem() == ItemInit.DRAGON_CROSSBOW_ITEM.get()) {
            cir.setReturnValue(CrossbowItem.containsChargedProjectile(stack, Items.FIREWORK_ROCKET) ? 1.6F : 3.15F);
        }
    }
}
