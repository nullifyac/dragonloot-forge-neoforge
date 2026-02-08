package net.dragonloot.mixin;

import net.dragonloot.compat.AdvancedNetheriteCompat;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnderMan.class)
public abstract class EnderManPacifyMixin extends Monster implements NeutralMob {

    protected EnderManPacifyMixin(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = {"isBeingStaredBy", "isLookingAtMe"}, at = @At("RETURN"), cancellable = true, require = 0)
    private void dragonloot$pacifyEnderman(Player player, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue() && AdvancedNetheriteCompat.isWearingPacifyingArmor(player)) {
            cir.setReturnValue(false);
        }
    }
}
