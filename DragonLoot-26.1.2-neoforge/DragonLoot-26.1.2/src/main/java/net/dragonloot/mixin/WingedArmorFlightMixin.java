package net.dragonloot.mixin;

import com.llamalad7.mixinextras.injector.WrapWithCondition;
import net.dragonloot.init.ItemInit;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Preserves the winged chestplate's existing lack of passive gliding wear. */
@Mixin(LivingEntity.class)
public abstract class WingedArmorFlightMixin {
    @WrapWithCondition(method = "updateFallFlying", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;hurtAndBreak(ILnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;)V"))
    private boolean dragonloot$passiveGlidingWear(ItemStack stack, int amount,
                                                LivingEntity wearer, EquipmentSlot slot) {
        return !stack.is(ItemInit.UPGRADED_DRAGON_CHESTPLATE.get());
    }
}
