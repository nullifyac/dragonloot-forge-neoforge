package net.dragonloot.mixin.client;

import com.mojang.authlib.GameProfile;
import net.dragonloot.init.ItemInit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.ProfilePublicKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@OnlyIn(Dist.CLIENT)
@Mixin(AbstractClientPlayer.class)
public abstract class AbstractClientPlayerEntityMixin extends Player {

    protected AbstractClientPlayerEntityMixin(Level level, BlockPos pos, float yaw, GameProfile profile, ProfilePublicKey profilePublicKey) {
        super(level, pos, yaw, profile, profilePublicKey);
    }

    @Inject(method = "getFieldOfViewModifier", at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/client/player/AbstractClientPlayer;getUseItem()Lnet/minecraft/world/item/ItemStack;"), locals = LocalCapture.CAPTURE_FAILSOFT, cancellable = true)
    private void dragonloot$adjustBowFov(CallbackInfoReturnable<Float> cir, float f) {
        ItemStack stack = this.getUseItem();
        if (this.isUsingItem() && stack.is(ItemInit.DRAGON_BOW_ITEM.get())) {
            int usedTicks = stack.getUseDuration() - this.getUseItemRemainingTicks();
            float draw = (float) usedTicks / 20.0F;
            draw = draw > 1.0F ? 1.0F : draw * draw;
            float adjusted = f * (1.0F - draw * 0.15F);
            float effectScale = Minecraft.getInstance().options.fovEffectScale().get().floatValue();
            cir.setReturnValue(Mth.lerp(effectScale, 1.0F, adjusted));
        }
    }
}