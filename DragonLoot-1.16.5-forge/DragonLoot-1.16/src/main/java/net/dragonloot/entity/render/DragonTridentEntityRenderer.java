package net.dragonloot.entity.render;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.dragonloot.DragonLootMain;
import net.dragonloot.entity.DragonTridentEntity;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.entity.model.TridentModel;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3f;

public class DragonTridentEntityRenderer extends EntityRenderer<DragonTridentEntity> {
    public static final ResourceLocation TEXTURE = DragonLootMain.id("textures/entity/dragon_trident.png");
    private final TridentModel model = new TridentModel();

    public DragonTridentEntityRenderer(EntityRendererManager renderManager) {
        super(renderManager);
    }

    @Override
    public void render(DragonTridentEntity entity, float entityYaw, float partialTick, MatrixStack poseStack, IRenderTypeBuffer buffer, int packedLight) {
        poseStack.pushPose();
        poseStack.mulPose(Vector3f.YP.rotationDegrees(MathHelper.lerp(partialTick, entity.yRotO, entity.yRot) - 90.0F));
        poseStack.mulPose(Vector3f.ZP.rotationDegrees(MathHelper.lerp(partialTick, entity.xRotO, entity.xRot) + 90.0F));
        IVertexBuilder vertexConsumer = ItemRenderer.getFoilBufferDirect(buffer, this.model.renderType(this.getTextureLocation(entity)), false, entity.isEnchanted());
        this.model.renderToBuffer(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(DragonTridentEntity entity) {
        return TEXTURE;
    }
}
