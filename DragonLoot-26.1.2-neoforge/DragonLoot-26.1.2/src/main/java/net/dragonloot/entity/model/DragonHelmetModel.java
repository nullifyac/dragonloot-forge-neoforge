package net.dragonloot.entity.model;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;

public final class DragonHelmetModel extends HumanoidModel<HumanoidRenderState> {
    public DragonHelmetModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createLayerDefinition() {
        return createLayerDefinition(false);
    }

    public static LayerDefinition createBabyLayerDefinition() {
        return createLayerDefinition(true);
    }

    private static LayerDefinition createLayerDefinition(boolean baby) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), baby ? PartPose.offset(0, 15, 0) : PartPose.ZERO);
        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        for (String part : new String[]{"body", "right_arm", "left_arm", "right_leg", "left_leg"}) {
            root.addOrReplaceChild(part, CubeListBuilder.create(), PartPose.ZERO);
        }
        // Same horn mesh and scale as the original head layer, now attached to
        // the native armor head so crouching/head rotation and foil stay native.
        PartDefinition helmet = head.addOrReplaceChild("dragon_helmet", CubeListBuilder.create()
            .texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.1F))
            .texOffs(0, 16).addBox(-3.0F, -7.0F, -7.0F, 6.0F, 4.0F, 3.0F),
            PartPose.offset(0.0F, 0.56F, 0.0F).withScale(1.19F));
        helmet.addOrReplaceChild("HornLD", CubeListBuilder.create().texOffs(34, 0)
            .addBox(-1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 4.0F),
            PartPose.offsetAndRotation(-3.0F, -5.0F, 3.0F, 0.0F, -0.3927F, 0.0F));
        helmet.addOrReplaceChild("HornRD", CubeListBuilder.create().texOffs(34, 0)
            .addBox(-2.0F, -1.0F, 0.0F, 2.0F, 2.0F, 4.0F),
            PartPose.offsetAndRotation(4.0F, -5.0F, 3.0F, 0.0F, 0.3927F, 0.0F));
        helmet.addOrReplaceChild("HornLU", CubeListBuilder.create().texOffs(24, 0)
            .addBox(-1.0F, 0.0F, 0.0F, 2.0F, 2.0F, 6.0F).texOffs(24, 0)
            .addBox(4.8F, 0.0F, 0.0F, 2.0F, 2.0F, 6.0F),
            PartPose.offsetAndRotation(-2.9F, -8.0F, 2.0F, 0.7854F, 0.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 32);
    }
}
