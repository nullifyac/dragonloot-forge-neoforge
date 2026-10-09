package net.dragonloot.entity.model;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.world.entity.EquipmentSlot;

/** Native baby armor proportions with UVs that reuse the established 64x32 Dragon textures. */
public final class DragonBabyArmorModel extends HumanoidModel<HumanoidRenderState> {
    private final HumanoidModel<?> animationModel;

    public DragonBabyArmorModel(net.minecraft.client.model.geom.ModelPart root) {
        this(root, null);
    }

    public DragonBabyArmorModel(net.minecraft.client.model.geom.ModelPart root, HumanoidModel<?> animationModel) {
        super(root);
        this.animationModel = animationModel;
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void setupAnim(HumanoidRenderState state) {
        if (this.animationModel == null) {
            super.setupAnim(state);
        } else {
            this.resetPose();
            // Keep native mob-specific armor animation, including raised zombie
            // arms, while rendering the Dragon texture on the remapped UV mesh.
            ((HumanoidModel) this.animationModel).setupAnim(state);
            net.neoforged.neoforge.client.ClientHooks.copyModelProperties(this.animationModel, this);
        }
    }

    public static LayerDefinition createLayerDefinition(EquipmentSlot slot) {
        var mesh = new MeshDefinition();
        var root = mesh.getRoot();
        var head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0, 15, 0));
        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        CubeDeformation deformation = new CubeDeformation(slot == EquipmentSlot.LEGS ? 0.5F : 1);
        boolean torso = slot == EquipmentSlot.CHEST || slot == EquipmentSlot.LEGS;
        root.addOrReplaceChild("body", torso ? CubeListBuilder.create().texOffs(16, 16)
                .addBox(-3, -3, -1.5F, 6, 5, 3, deformation) : CubeListBuilder.create(), PartPose.offset(0, 18, 0));
        root.addOrReplaceChild("right_arm", slot == EquipmentSlot.CHEST ? CubeListBuilder.create().texOffs(40, 16)
                .addBox(-1, 0, -1.53F, 2, 5, 3, deformation) : CubeListBuilder.create(), PartPose.offset(-3.5F, 15.5F, 0));
        root.addOrReplaceChild("left_arm", slot == EquipmentSlot.CHEST ? CubeListBuilder.create().texOffs(40, 16).mirror()
                .addBox(-1, 0, -1.53F, 2, 5, 3, deformation) : CubeListBuilder.create(), PartPose.offset(3.5F, 15.5F, 0));
        boolean legs = slot == EquipmentSlot.LEGS;
        var leftLeg = root.addOrReplaceChild("left_leg", legs ? CubeListBuilder.create().texOffs(0, 16).mirror()
                .addBox(-2, -0.2F, -2, 3, 4, 3, deformation.extend(-0.1F)) : CubeListBuilder.create(), PartPose.offset(1.5F, 20, 0.5F));
        var rightLeg = root.addOrReplaceChild("right_leg", legs ? CubeListBuilder.create().texOffs(0, 16)
                .addBox(-1, -0.2F, -2, 3, 4, 3, deformation.extend(-0.1F)) : CubeListBuilder.create(), PartPose.offset(-1.5F, 20, 0.5F));
        leftLeg.addOrReplaceChild("left_foot", slot == EquipmentSlot.FEET ? CubeListBuilder.create().texOffs(0, 24).mirror()
                .addBox(-2, 2.9F, -2, 3, 1, 3, deformation) : CubeListBuilder.create(), PartPose.ZERO);
        rightLeg.addOrReplaceChild("right_foot", slot == EquipmentSlot.FEET ? CubeListBuilder.create().texOffs(0, 24)
                .addBox(-1, 2.9F, -2, 3, 1, 3, deformation) : CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("waist", legs ? CubeListBuilder.create().texOffs(16, 22)
                .addBox(-3, -1.2F, -1.49F, 5.9F, 2, 2.9F, deformation.extend(-0.1F)) : CubeListBuilder.create(), PartPose.offset(0, 19, 0));
        return LayerDefinition.create(mesh, 64, 32);
    }
}
