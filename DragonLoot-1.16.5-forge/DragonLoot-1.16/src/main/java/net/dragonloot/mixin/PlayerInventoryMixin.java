package net.dragonloot.mixin;

import net.dragonloot.init.ItemInit;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.DamageSource;
import net.minecraft.util.NonNullList;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerInventory.class)
public abstract class PlayerInventoryMixin {

    @Shadow
    @Final
    public PlayerEntity player;

    @Shadow
    @Final
    public NonNullList<ItemStack> armor;

    @Inject(method = "hurtArmor", at = @At("HEAD"), cancellable = true)
    private void dragonloot$swapBrokenDragonChestplate(DamageSource source, float amount, CallbackInfo info) {
        ItemStack stack = this.armor.get(2);
        if (amount > 0.0F && stack.getItem() == ItemInit.UPGRADED_DRAGON_CHESTPLATE.get() && stack.getDamageValue() == stack.getMaxDamage() - 1) {
            stack.shrink(1);
            ItemStack elytra = new ItemStack(Items.ELYTRA);
            elytra.setDamageValue(Items.ELYTRA.getMaxDamage());
            this.armor.set(2, elytra);
            if (!this.player.isSilent()) {
                this.player.level.playSound(null, this.player.getX(), this.player.getY(), this.player.getZ(), SoundEvents.ITEM_BREAK, SoundCategory.PLAYERS, 0.8F, 0.8F + this.player.level.random.nextFloat() * 0.4F);
            }
            info.cancel();
        }
    }
}
