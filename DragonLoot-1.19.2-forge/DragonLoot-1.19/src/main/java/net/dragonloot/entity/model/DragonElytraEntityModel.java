package net.dragonloot.entity.model;

import com.google.common.collect.ImmutableList;
import net.minecraft.client.model.AgeableListModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public class DragonElytraEntityModel<T extends LivingEntity> extends AgeableListModel<T> {

    private final ModelPart rightWing;
    private final ModelPart leftWing;

    public DragonElytraEntityModel(ModelPart root) {
        this.leftWing = root.getChild("left_wing");
        this.rightWing = root.getChild("right_wing");
    }

    public static LayerDefinition createLayerDefinition() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        CubeDeformation dilation = new CubeDeformation(1.0F);
        root.addOrReplaceChild("left_wing", CubeListBuilder.create().texOffs(22, 0).addBox(-10.0F, 0.0F, 0.0F, 20.0F, 21.0F, 2.0F, dilation), PartPose.offsetAndRotation(5.0F, 0.0F, 0.0F, 0.2617994F, 0.0F, -0.2617994F));
        root.addOrReplaceChild("right_wing", CubeListBuilder.create().texOffs(22, 0).mirror().addBox(-10.0F, 0.0F, 0.0F, 20.0F, 21.0F, 2.0F, dilation), PartPose.offsetAndRotation(-5.0F, 0.0F, 0.0F, 0.2617994F, 0.0F, 0.2617994F));
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    protected Iterable<ModelPart> headParts() {
        return ImmutableList.of();
    }

    @Override
    protected Iterable<ModelPart> bodyParts() {
        return ImmutableList.of(this.leftWing, this.rightWing);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        float pitch = 0.2617994F;
        float roll = -0.2617994F;
        float yOffset = 0.0F;
        float yaw = 0.0F;
        if (entity.isFallFlying()) {
            float flightAmount = 1.0F;
            Vec3 motion = entity.getDeltaMovement();
            if (motion.y < 0.0D) {
                Vec3 motionNorm = motion.normalize();
                flightAmount = 1.0F - (float) Math.pow(-motionNorm.y, 1.5D);
            }

            pitch = flightAmount * 0.34906584F + (1.0F - flightAmount) * pitch;
            roll = flightAmount * -1.5707964F + (1.0F - flightAmount) * roll;
        } else if (entity.isCrouching()) {
            pitch = 0.6981317F;
            roll = -0.7853982F;
            yOffset = 3.0F;
            yaw = 0.08726646F;
        }

        this.leftWing.y = yOffset;
        this.leftWing.xRot = pitch;
        this.leftWing.yRot = yaw;
        this.leftWing.zRot = roll;

        this.rightWing.yRot = -this.leftWing.yRot;
        this.rightWing.y = this.leftWing.y;
        this.rightWing.xRot = this.leftWing.xRot;
        this.rightWing.zRot = -this.leftWing.zRot;
    }
}
