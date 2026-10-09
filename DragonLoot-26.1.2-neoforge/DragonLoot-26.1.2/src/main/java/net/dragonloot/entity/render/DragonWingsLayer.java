package net.dragonloot.entity.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.dragonloot.DragonLootMain;
import net.dragonloot.entity.model.DragonElytraEntityModel;
import net.dragonloot.init.ItemInit;
import net.dragonloot.init.RenderInit;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

public final class DragonWingsLayer<S extends HumanoidRenderState, M extends EntityModel<? super S>> extends RenderLayer<S, M> {
    public static final Identifier TEXTURE = DragonLootMain.id("textures/entity/dragon_elytra.png");
    private final DragonElytraEntityModel model;
    private final DragonElytraEntityModel babyModel;

    public DragonWingsLayer(RenderLayerParent<S, M> renderer, EntityModelSet modelSet) {
        super(renderer);
        this.model = new DragonElytraEntityModel(modelSet.bakeLayer(RenderInit.DRAGON_ELYTRA_LAYER));
        this.babyModel = new DragonElytraEntityModel(modelSet.bakeLayer(RenderInit.DRAGON_BABY_ELYTRA_LAYER));
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, S state, float yRot, float xRot) {
        if (!state.chestEquipment.is(ItemInit.UPGRADED_DRAGON_CHESTPLATE.get())) {
            return;
        }
        DragonElytraEntityModel selectedModel = state.isBaby ? this.babyModel : this.model;
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.0F, 0.02F);
        collector.order(0).submitModel(selectedModel, state, poseStack, RenderTypes.armorCutoutNoCull(TEXTURE),
            light, OverlayTexture.NO_OVERLAY, -1, null, state.outlineColor, null);
        if (state.chestEquipment.hasFoil()) {
            collector.order(1).submitModel(selectedModel, state, poseStack, RenderTypes.armorEntityGlint(),
                light, OverlayTexture.NO_OVERLAY, -1, null, state.outlineColor, null);
        }
        poseStack.popPose();
    }
}
