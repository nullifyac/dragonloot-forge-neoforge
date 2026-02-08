package net.dragonloot.mixin;

import net.dragonloot.compat.AdvancedNetheriteCompat;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.monster.EndermanEntity;
import net.minecraft.entity.monster.MonsterEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EndermanEntity.class)
public abstract class EnderManPacifyMixin extends MonsterEntity {

    protected EnderManPacifyMixin(EntityType<? extends MonsterEntity> entityType, World level) {
        super(entityType, level);
    }

    @Inject(method = {"isBeingStaredBy", "isLookingAtMe"}, at = @At("RETURN"), cancellable = true)
    private void dragonloot$pacifyEnderman(PlayerEntity player, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue() && AdvancedNetheriteCompat.isWearingPacifyingArmor(player)) {
            cir.setReturnValue(false);
        }
    }
}
