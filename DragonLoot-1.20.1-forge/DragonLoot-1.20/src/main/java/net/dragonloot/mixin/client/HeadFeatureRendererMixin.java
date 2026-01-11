package net.dragonloot.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.dragonloot.DragonLootMain;
import net.dragonloot.entity.model.DragonHelmetModel;
import net.dragonloot.init.ItemInit;
import net.minecraft.client.model.HeadedModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CustomHeadLayer.class)
public class HeadFeatureRendererMixin {
    @Shadow
    @Final
    private float scaleX;
    @Shadow
    @Final
    private float scaleY;
    @Shadow
    @Final
    private float scaleZ;

    private static final ResourceLocation HELMET_TEXTURE = DragonLootMain.id("textures/entity/dragon_helmet_3d.png");
    private final DragonHelmetModel dragonHelmetModel = new DragonHelmetModel(DragonHelmetModel.createLayerDefinition().bakeRoot());

    @SuppressWarnings("rawtypes")
    @Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V", at = @At("HEAD"), cancellable = true)
    private void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, LivingEntity entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo info) {
        ItemStack stack = entity.getItemBySlot(EquipmentSlot.HEAD);
        if (!stack.isEmpty() && stack.is(ItemInit.DRAGON_HELMET.get())) {
            poseStack.pushPose();
            poseStack.scale(this.scaleX, this.scaleY, this.scaleZ);
            ((HeadedModel) ((CustomHeadLayer<?, ?>) (Object) this).getParentModel()).getHead().translateAndRotate(poseStack);
            poseStack.translate(0.0D, -1.75D, 0.0D);
            poseStack.scale(1.19F, 1.19F, 1.19F);
            VertexConsumer consumer = ItemRenderer.getArmorFoilBuffer(buffer, this.dragonHelmetModel.renderType(HELMET_TEXTURE), false, stack.hasFoil());
            this.dragonHelmetModel.renderToBuffer(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
            poseStack.popPose();
            info.cancel();
        }
    }
}
