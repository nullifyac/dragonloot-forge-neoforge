package net.dragonloot.entity.model;

import com.google.common.collect.ImmutableList;
import net.minecraft.client.renderer.entity.model.AgeableModel;
import net.minecraft.client.renderer.model.ModelRenderer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.vector.Vector3d;

public class DragonElytraEntityModel<T extends LivingEntity> extends AgeableModel<T> {

    private final ModelRenderer rightWing;
    private final ModelRenderer leftWing;

    public DragonElytraEntityModel() {
        this.texWidth = 64;
        this.texHeight = 32;
        this.leftWing = new ModelRenderer(this, 22, 0);
        this.leftWing.addBox(-10.0F, 0.0F, 0.0F, 20.0F, 21.0F, 2.0F, 1.0F);
        this.leftWing.setPos(5.0F, 0.0F, 0.0F);
        this.rightWing = new ModelRenderer(this, 22, 0);
        this.rightWing.mirror = true;
        this.rightWing.addBox(-10.0F, 0.0F, 0.0F, 20.0F, 21.0F, 2.0F, 1.0F);
        this.rightWing.setPos(-5.0F, 0.0F, 0.0F);
    }

    @Override
    protected Iterable<ModelRenderer> headParts() {
        return ImmutableList.of();
    }

    @Override
    protected Iterable<ModelRenderer> bodyParts() {
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
            Vector3d motion = entity.getDeltaMovement();
            if (motion.y < 0.0D) {
                Vector3d motionNorm = motion.normalize();
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
