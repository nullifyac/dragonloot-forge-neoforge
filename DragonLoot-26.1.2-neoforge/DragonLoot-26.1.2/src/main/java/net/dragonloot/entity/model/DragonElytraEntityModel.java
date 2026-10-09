package net.dragonloot.entity.model;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;

public final class DragonElytraEntityModel extends EntityModel<HumanoidRenderState> {
    private final ModelPart leftWing;
    private final ModelPart rightWing;

    public DragonElytraEntityModel(ModelPart root) {
        super(root);
        this.leftWing = root.getChild("left_wing");
        this.rightWing = root.getChild("right_wing");
    }

    public static LayerDefinition createLayerDefinition() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        CubeDeformation dilation = new CubeDeformation(1.0F);
        root.addOrReplaceChild("left_wing", CubeListBuilder.create().texOffs(22, 0)
            .addBox(-10.0F, 0.0F, 0.0F, 20.0F, 21.0F, 2.0F, dilation),
            PartPose.offsetAndRotation(5.0F, 0.0F, 0.0F, 0.2617994F, 0.0F, -0.2617994F));
        root.addOrReplaceChild("right_wing", CubeListBuilder.create().texOffs(22, 0).mirror()
            .addBox(-10.0F, 0.0F, 0.0F, 20.0F, 21.0F, 2.0F, dilation),
            PartPose.offsetAndRotation(-5.0F, 0.0F, 0.0F, 0.2617994F, 0.0F, 0.2617994F));
        return LayerDefinition.create(mesh, 64, 32);
    }

    public static LayerDefinition createBabyLayerDefinition() {
        // Match native baby equipment positioning without changing the Dragon UVs.
        return createLayerDefinition().apply(net.minecraft.client.model.object.equipment.ElytraModel.BABY_TRANSFORMER);
    }

    @Override
    public void setupAnim(HumanoidRenderState state) {
        super.setupAnim(state);
        this.leftWing.y = state.isCrouching ? 3.0F : 0.0F;
        this.leftWing.xRot = state.elytraRotX;
        this.leftWing.yRot = state.elytraRotY;
        this.leftWing.zRot = state.elytraRotZ;
        this.rightWing.y = this.leftWing.y;
        this.rightWing.xRot = this.leftWing.xRot;
        this.rightWing.yRot = -this.leftWing.yRot;
        this.rightWing.zRot = -this.leftWing.zRot;
    }
}
