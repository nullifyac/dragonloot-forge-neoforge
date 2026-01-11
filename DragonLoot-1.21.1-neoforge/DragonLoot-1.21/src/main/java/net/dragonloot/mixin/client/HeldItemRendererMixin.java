package net.dragonloot.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.dragonloot.init.ItemInit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@OnlyIn(Dist.CLIENT)
@Mixin(ItemInHandRenderer.class)
public class HeldItemRendererMixin {

    @Shadow
    private Minecraft minecraft;

    @Inject(
        method = "renderArmWithItem(Lnet/minecraft/client/player/AbstractClientPlayer;FFLnet/minecraft/world/InteractionHand;FLnet/minecraft/world/item/ItemStack;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
        at = @At(value = "HEAD"),
        cancellable = true,
        require = 0
    )
    private void dragonloot$renderDragonCrossbow(AbstractClientPlayer player, float tickDelta, float pitch, InteractionHand hand, float swingProgress, ItemStack stack, float equipProgress, PoseStack poseStack, MultiBufferSource buffer, int packedLight, CallbackInfo info) {
        if (stack.getItem() == ItemInit.DRAGON_CROSSBOW_ITEM.get()) {
            poseStack.pushPose();
            boolean mainHand = hand == InteractionHand.MAIN_HAND;
            HumanoidArm arm = mainHand ? player.getMainArm() : player.getMainArm().getOpposite();
            boolean charged = CrossbowItem.isCharged(stack);
            boolean rightArm = arm == HumanoidArm.RIGHT;
            int direction = rightArm ? 1 : -1;
            float x;
            float y;
            float w;
            float v;
            if (player.isUsingItem() && player.getUseItemRemainingTicks() > 0 && player.getUsedItemHand() == hand) {
                this.applyItemArmTransform(poseStack, arm, equipProgress);
                poseStack.translate((double) ((float) direction * -0.4785682F), -0.0943870022892952D, 0.05731530860066414D);
                poseStack.mulPose(Axis.XP.rotationDegrees(-11.935F));
                poseStack.mulPose(Axis.YP.rotationDegrees((float) direction * 65.3F));
                poseStack.mulPose(Axis.ZP.rotationDegrees((float) direction * -9.785F));
                v = (float) stack.getUseDuration(player) - ((float) this.minecraft.player.getUseItemRemainingTicks() - tickDelta + 1.0F);
                w = v / (float) CrossbowItem.getChargeDuration(stack, player);

                if (w > 1.0F) {
                    w = 1.0F;
                }

                if (w > 0.1F) {
                    x = Mth.sin((v - 0.1F) * 1.3F);
                    y = w - 0.1F;
                    float k = x * y;
                    poseStack.translate((double) (k * 0.0F), (double) (k * 0.004F), (double) (k * 0.0F));
                }

                poseStack.translate((double) (w * 0.0F), (double) (w * 0.0F), (double) (w * 0.04F));
                poseStack.scale(1.0F, 1.0F, 1.0F + w * 0.2F);
                poseStack.mulPose(Axis.YN.rotationDegrees((float) direction * 45.0F));
            } else {
                v = -0.4F * Mth.sin(Mth.sqrt(swingProgress) * (float) Math.PI);
                w = 0.2F * Mth.sin(Mth.sqrt(swingProgress) * ((float) Math.PI * 2.0F));
                x = -0.2F * Mth.sin(swingProgress * (float) Math.PI);
                poseStack.translate((double) ((float) direction * v), (double) w, (double) x);
                this.applyItemArmTransform(poseStack, arm, equipProgress);
                this.applyItemArmAttackTransform(poseStack, arm, swingProgress);
                if (charged && swingProgress < 0.001F) {
                    poseStack.translate((double) ((float) direction * -0.641864F), 0.0D, 0.0D);
                    poseStack.mulPose(Axis.YP.rotationDegrees((float) direction * 10.0F));
                }
            }

            this.renderItem(player, stack, rightArm ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND, !rightArm, poseStack, buffer, packedLight);
            poseStack.popPose();
            info.cancel();
        }
    }

    @Shadow
    public void renderItem(LivingEntity entity, ItemStack stack, ItemDisplayContext context, boolean leftHanded, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
    }

    @Shadow
    private void applyItemArmTransform(PoseStack poseStack, HumanoidArm arm, float equipProgress) {
    }

    @Shadow
    private void applyItemArmAttackTransform(PoseStack poseStack, HumanoidArm arm, float swingProgress) {
    }
}
