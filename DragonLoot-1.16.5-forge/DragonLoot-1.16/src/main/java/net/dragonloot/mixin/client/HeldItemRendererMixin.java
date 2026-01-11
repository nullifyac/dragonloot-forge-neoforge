package net.dragonloot.mixin.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.dragonloot.init.ItemInit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.AbstractClientPlayerEntity;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.FirstPersonRenderer;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.HandSide;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@OnlyIn(Dist.CLIENT)
@Mixin(FirstPersonRenderer.class)
public class HeldItemRendererMixin {

    @Shadow
    private Minecraft minecraft;

    @Inject(
        method = "renderArmWithItem(Lnet/minecraft/client/entity/player/AbstractClientPlayerEntity;FFLnet/minecraft/util/Hand;FLnet/minecraft/item/ItemStack;FLcom/mojang/blaze3d/matrix/MatrixStack;Lnet/minecraft/client/renderer/IRenderTypeBuffer;I)V",
        at = @At(value = "HEAD"),
        cancellable = true,
        require = 0
    )
    private void dragonloot$renderDragonCrossbow(AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand, float swingProgress, ItemStack stack, float equipProgress, MatrixStack poseStack, IRenderTypeBuffer buffer, int packedLight, CallbackInfo info) {
        if (stack.getItem() == ItemInit.DRAGON_CROSSBOW_ITEM.get()) {
            poseStack.pushPose();
            boolean mainHand = hand == Hand.MAIN_HAND;
            HandSide arm = mainHand ? player.getMainArm() : player.getMainArm().getOpposite();
            boolean charged = CrossbowItem.isCharged(stack);
            boolean rightArm = arm == HandSide.RIGHT;
            int direction = rightArm ? 1 : -1;
            float x;
            float y;
            float w;
            float v;
            if (player.isUsingItem() && player.getUseItemRemainingTicks() > 0 && player.getUsedItemHand() == hand) {
                this.applyItemArmTransform(poseStack, arm, equipProgress);
                poseStack.translate((double) ((float) direction * -0.4785682F), -0.0943870022892952D, 0.05731530860066414D);
                poseStack.mulPose(Vector3f.XP.rotationDegrees(-11.935F));
                poseStack.mulPose(Vector3f.YP.rotationDegrees((float) direction * 65.3F));
                poseStack.mulPose(Vector3f.ZP.rotationDegrees((float) direction * -9.785F));
                v = (float) stack.getUseDuration() - ((float) this.minecraft.player.getUseItemRemainingTicks() - tickDelta + 1.0F);
                w = v / (float) CrossbowItem.getChargeDuration(stack);

                if (w > 1.0F) {
                    w = 1.0F;
                }

                if (w > 0.1F) {
                    x = MathHelper.sin((v - 0.1F) * 1.3F);
                    y = w - 0.1F;
                    float k = x * y;
                    poseStack.translate((double) (k * 0.0F), (double) (k * 0.004F), (double) (k * 0.0F));
                }

                poseStack.translate((double) (w * 0.0F), (double) (w * 0.0F), (double) (w * 0.04F));
                poseStack.scale(1.0F, 1.0F, 1.0F + w * 0.2F);
                poseStack.mulPose(Vector3f.YN.rotationDegrees((float) direction * 45.0F));
            } else {
                v = -0.4F * MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
                w = 0.2F * MathHelper.sin(MathHelper.sqrt(swingProgress) * ((float) Math.PI * 2.0F));
                x = -0.2F * MathHelper.sin(swingProgress * (float) Math.PI);
                poseStack.translate((double) ((float) direction * v), (double) w, (double) x);
                this.applyItemArmTransform(poseStack, arm, equipProgress);
                this.applyItemArmAttackTransform(poseStack, arm, swingProgress);
                if (charged && swingProgress < 0.001F) {
                    poseStack.translate((double) ((float) direction * -0.641864F), 0.0D, 0.0D);
                    poseStack.mulPose(Vector3f.YP.rotationDegrees((float) direction * 10.0F));
                }
            }

            this.renderItem(player, stack, rightArm ? ItemCameraTransforms.TransformType.FIRST_PERSON_RIGHT_HAND : ItemCameraTransforms.TransformType.FIRST_PERSON_LEFT_HAND, !rightArm, poseStack, buffer, packedLight);
            poseStack.popPose();
            info.cancel();
        }
    }

    @Shadow
    public void renderItem(LivingEntity entity, ItemStack stack, ItemCameraTransforms.TransformType transformType, boolean leftHanded, MatrixStack poseStack, IRenderTypeBuffer buffer, int packedLight) {
    }

    @Shadow
    private void applyItemArmTransform(MatrixStack poseStack, HandSide arm, float equipProgress) {
    }

    @Shadow
    private void applyItemArmAttackTransform(MatrixStack poseStack, HandSide arm, float swingProgress) {
    }
}
