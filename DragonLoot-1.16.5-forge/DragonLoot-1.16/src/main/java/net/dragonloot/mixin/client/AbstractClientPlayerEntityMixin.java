package net.dragonloot.mixin.client;

import com.mojang.authlib.GameProfile;
import net.dragonloot.init.ItemInit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.AbstractClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@OnlyIn(Dist.CLIENT)
@Mixin(AbstractClientPlayerEntity.class)
public abstract class AbstractClientPlayerEntityMixin extends PlayerEntity {

    protected AbstractClientPlayerEntityMixin(World level, BlockPos pos, float yaw, GameProfile profile) {
        super(level, pos, yaw, profile);
    }

    @Inject(method = "getFieldOfViewModifier", at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/client/entity/player/AbstractClientPlayerEntity;getUseItem()Lnet/minecraft/item/ItemStack;"), locals = LocalCapture.CAPTURE_FAILSOFT, cancellable = true)
    private void dragonloot$adjustBowFov(CallbackInfoReturnable<Float> cir, float f) {
        ItemStack stack = this.getUseItem();
        if (this.isUsingItem() && stack.getItem() == ItemInit.DRAGON_BOW_ITEM.get()) {
            int usedTicks = stack.getUseDuration() - this.getUseItemRemainingTicks();
            float draw = (float) usedTicks / 20.0F;
            draw = draw > 1.0F ? 1.0F : draw * draw;
            float adjusted = f * (1.0F - draw * 0.15F);
            float effectScale = (float) Minecraft.getInstance().options.fovEffectScale;
            cir.setReturnValue(MathHelper.lerp(effectScale, 1.0F, adjusted));
        }
    }
}
