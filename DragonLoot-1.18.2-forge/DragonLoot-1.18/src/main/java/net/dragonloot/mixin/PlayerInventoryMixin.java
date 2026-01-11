package net.dragonloot.mixin;

import net.dragonloot.init.ItemInit;
import net.minecraft.core.NonNullList;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Inventory.class)
public abstract class PlayerInventoryMixin {

    @Shadow
    @Final
    public Player player;

    @Shadow
    @Final
    public NonNullList<ItemStack> armor;

    @Inject(method = "hurtArmor", at = @At("HEAD"), cancellable = true)
    private void dragonloot$swapBrokenDragonChestplate(DamageSource source, float amount, int[] slots, CallbackInfo info) {
        ItemStack stack = this.armor.get(2);
        if (amount > 0.0F && stack.is(ItemInit.UPGRADED_DRAGON_CHESTPLATE.get()) && stack.getDamageValue() == stack.getMaxDamage() - 1) {
            stack.shrink(1);
            ItemStack elytra = new ItemStack(Items.ELYTRA);
            elytra.setDamageValue(Items.ELYTRA.getMaxDamage());
            this.armor.set(2, elytra);
            if (!this.player.isSilent()) {
                this.player.level.playSound(null, this.player.getX(), this.player.getY(), this.player.getZ(), SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 0.8F, 0.8F + this.player.level.getRandom().nextFloat() * 0.4F);
            }
            info.cancel();
        }
    }
}
