package net.dragonloot.item.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.TridentModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.ItemStack;

public class DragonTridentBewlr extends BlockEntityWithoutLevelRenderer {

    private TridentModel model;

    public DragonTridentBewlr() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        this.model = null;
    }

    private TridentModel getOrCreateModel() {
        if (this.model == null) {
            Minecraft minecraft = Minecraft.getInstance();
            EntityModelSet modelSet = minecraft != null ? minecraft.getEntityModels() : null;
            if (modelSet == null) {
                return null;
            }
            this.model = new TridentModel(modelSet.bakeLayer(ModelLayers.TRIDENT));
        }
        return this.model;
    }

    @Override
    public void renderByItem(ItemStack stack, ItemTransforms.TransformType transformType, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        TridentModel model = this.getOrCreateModel();
        if (model == null) {
            return;
        }
        DragonTridentItemRenderer.render(model, stack, transformType, poseStack, buffer, packedLight, packedOverlay);
    }
}
