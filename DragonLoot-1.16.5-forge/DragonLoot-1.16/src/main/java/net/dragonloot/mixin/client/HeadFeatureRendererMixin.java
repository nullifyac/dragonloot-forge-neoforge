package net.dragonloot.mixin.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.dragonloot.DragonLootMain;
import net.dragonloot.entity.model.DragonHelmetModel;
import net.dragonloot.init.ItemInit;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.entity.layers.HeadLayer;
import net.minecraft.client.renderer.entity.model.IHasHead;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.entity.LivingEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HeadLayer.class)
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
    private final DragonHelmetModel dragonHelmetModel = new DragonHelmetModel();

    @SuppressWarnings("rawtypes")
    @Inject(method = "render(Lcom/mojang/blaze3d/matrix/MatrixStack;Lnet/minecraft/client/renderer/IRenderTypeBuffer;ILnet/minecraft/entity/LivingEntity;FFFFFF)V", at = @At("HEAD"), cancellable = true)
    private void render(MatrixStack poseStack, IRenderTypeBuffer buffer, int packedLight, LivingEntity entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo info) {
        ItemStack stack = entity.getItemBySlot(EquipmentSlotType.HEAD);
        if (!stack.isEmpty() && stack.getItem() == ItemInit.DRAGON_HELMET.get()) {
            poseStack.pushPose();
            poseStack.scale(this.scaleX, this.scaleY, this.scaleZ);
            ((IHasHead) ((HeadLayer<?, ?>) (Object) this).getParentModel()).getHead().translateAndRotate(poseStack);
            poseStack.translate(0.0D, -1.75D, 0.0D);
            poseStack.scale(1.19F, 1.19F, 1.19F);
            IVertexBuilder consumer = ItemRenderer.getArmorFoilBuffer(buffer, this.dragonHelmetModel.renderType(HELMET_TEXTURE), false, stack.hasFoil());
            this.dragonHelmetModel.renderToBuffer(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
            poseStack.popPose();
            info.cancel();
        }
    }
}
