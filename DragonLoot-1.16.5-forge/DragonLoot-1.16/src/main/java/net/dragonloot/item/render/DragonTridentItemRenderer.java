package net.dragonloot.item.render;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.dragonloot.DragonLootMain;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.entity.model.TridentModel;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

public final class DragonTridentItemRenderer {

    private static final ResourceLocation TEXTURE = DragonLootMain.id("textures/entity/dragon_trident.png");

    private DragonTridentItemRenderer() {
    }

    public static void render(TridentModel model, ItemStack stack, ItemCameraTransforms.TransformType transformType, MatrixStack poseStack, IRenderTypeBuffer buffer, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        poseStack.scale(1.0F, -1.0F, -1.0F);
        IVertexBuilder consumer = ItemRenderer.getFoilBufferDirect(buffer, model.renderType(TEXTURE), false, stack.hasFoil());
        model.renderToBuffer(poseStack, consumer, packedLight, packedOverlay, 1.0F, 1.0F, 1.0F, 1.0F);
        poseStack.popPose();
    }
}
