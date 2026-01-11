package net.dragonloot.entity.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.renderer.RenderType;

public class DragonHelmetModel extends Model {

	private final ModelPart root;
	private final ModelPart hornLowerLeft;
	private final ModelPart hornLowerRight;
	private final ModelPart hornUpper;

	public DragonHelmetModel(ModelPart root) {
		super(RenderType::entityCutoutNoCull);
		this.root = root.getChild("root");
		this.hornUpper = this.root.getChild("HornLU");
		this.hornLowerRight = this.root.getChild("HornRD");
		this.hornLowerLeft = this.root.getChild("HornLD");
	}

	public static LayerDefinition createLayerDefinition() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		PartDefinition base = root.addOrReplaceChild("root", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.1F)).texOffs(0, 16).addBox(-3.0F, -7.0F, -7.0F, 6.0F, 4.0F, 3.0F), PartPose.offset(0.0F, 24.0F, 0.0F));
		base.addOrReplaceChild("HornLD", CubeListBuilder.create().texOffs(34, 0).addBox(-1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 4.0F), PartPose.offset(-3.0F, -5.0F, 3.0F));
		base.addOrReplaceChild("HornRD", CubeListBuilder.create().texOffs(34, 0).addBox(-2.0F, -1.0F, 0.0F, 2.0F, 2.0F, 4.0F), PartPose.offset(4.0F, -5.0F, 3.0F));
		base.addOrReplaceChild("HornLU", CubeListBuilder.create().texOffs(24, 0).addBox(-1.0F, 0.0F, 0.0F, 2.0F, 2.0F, 6.0F).texOffs(24, 0).addBox(4.8F, 0.0F, 0.0F, 2.0F, 2.0F, 6.0F), PartPose.offset(-2.9F, -8.0F, 2.0F));
		return LayerDefinition.create(mesh, 64, 32);
	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int packedColor) {
		this.hornUpper.xRot = 0.7854F;
		this.hornLowerRight.yRot = 0.3927F;
		this.hornLowerLeft.yRot = -0.3927F;
		this.root.render(poseStack, buffer, packedLight, packedOverlay, packedColor);
	}
}
