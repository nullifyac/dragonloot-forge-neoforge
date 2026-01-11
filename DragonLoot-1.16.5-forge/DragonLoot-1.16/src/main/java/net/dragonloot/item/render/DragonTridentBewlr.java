package net.dragonloot.item.render;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.entity.model.TridentModel;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.client.renderer.tileentity.ItemStackTileEntityRenderer;
import net.minecraft.item.ItemStack;

public class DragonTridentBewlr extends ItemStackTileEntityRenderer {

    private final TridentModel model = new TridentModel();

    @Override
    public void renderByItem(ItemStack stack, ItemCameraTransforms.TransformType transformType, MatrixStack poseStack, IRenderTypeBuffer buffer, int packedLight, int packedOverlay) {
        DragonTridentItemRenderer.render(this.model, stack, transformType, poseStack, buffer, packedLight, packedOverlay);
    }
}
