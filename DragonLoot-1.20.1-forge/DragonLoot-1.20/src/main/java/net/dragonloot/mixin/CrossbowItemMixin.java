package net.dragonloot.mixin;

import net.dragonloot.init.ItemInit;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(CrossbowItem.class)
public class CrossbowItemMixin {

    @Inject(method = "getArrow", at = @At("RETURN"), locals = LocalCapture.CAPTURE_FAILSOFT, cancellable = true)
    private static void dragonloot$scaleArrow(Level level, LivingEntity entity, ItemStack crossbow, ItemStack arrowStack, CallbackInfoReturnable<AbstractArrow> cir, ArrowItem arrowItem, AbstractArrow arrow) {
        if (crossbow.is(ItemInit.DRAGON_CROSSBOW_ITEM.get())) {
            arrow.setBaseDamage(arrow.getBaseDamage() * 1.25F + 1.0F);
            cir.setReturnValue(arrow);
        }
    }

    @Inject(method = "getShootingPower", at = @At("HEAD"), cancellable = true)
    private static void dragonloot$adjustSpeed(ItemStack stack, CallbackInfoReturnable<Float> cir) {
        if (stack.is(ItemInit.DRAGON_CROSSBOW_ITEM.get())) {
            cir.setReturnValue(CrossbowItem.containsChargedProjectile(stack, Items.FIREWORK_ROCKET) ? 1.6F : 3.15F);
        }
    }
}