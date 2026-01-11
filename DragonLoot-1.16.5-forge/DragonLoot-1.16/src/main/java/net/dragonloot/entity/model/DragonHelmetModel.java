package net.dragonloot.entity.model;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.model.Model;
import net.minecraft.client.renderer.model.ModelRenderer;

public class DragonHelmetModel extends Model {

	private final ModelRenderer root;
	private final ModelRenderer hornLowerLeft;
	private final ModelRenderer hornLowerRight;
	private final ModelRenderer hornUpper;

	public DragonHelmetModel() {
		super(RenderType::entityCutoutNoCull);
		this.texWidth = 64;
		this.texHeight = 32;

		this.root = new ModelRenderer(this);
		this.root.setPos(0.0F, 24.0F, 0.0F);
		this.root.texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, 0.1F);
		this.root.texOffs(0, 16).addBox(-3.0F, -7.0F, -7.0F, 6.0F, 4.0F, 3.0F, 0.0F);

		this.hornLowerLeft = new ModelRenderer(this);
		this.hornLowerLeft.setPos(-3.0F, -5.0F, 3.0F);
		this.hornLowerLeft.texOffs(34, 0).addBox(-1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 4.0F, 0.0F);
		this.root.addChild(this.hornLowerLeft);

		this.hornLowerRight = new ModelRenderer(this);
		this.hornLowerRight.setPos(4.0F, -5.0F, 3.0F);
		this.hornLowerRight.texOffs(34, 0).addBox(-2.0F, -1.0F, 0.0F, 2.0F, 2.0F, 4.0F, 0.0F);
		this.root.addChild(this.hornLowerRight);

		this.hornUpper = new ModelRenderer(this);
		this.hornUpper.setPos(-2.9F, -8.0F, 2.0F);
		this.hornUpper.texOffs(24, 0).addBox(-1.0F, 0.0F, 0.0F, 2.0F, 2.0F, 6.0F, 0.0F);
		this.hornUpper.texOffs(24, 0).addBox(4.8F, 0.0F, 0.0F, 2.0F, 2.0F, 6.0F, 0.0F);
		this.root.addChild(this.hornUpper);
	}

	@Override
	public void renderToBuffer(MatrixStack poseStack, IVertexBuilder buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
		this.hornUpper.xRot = 0.7854F;
		this.hornLowerRight.yRot = 0.3927F;
		this.hornLowerLeft.yRot = -0.3927F;
		this.root.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
	}
}
