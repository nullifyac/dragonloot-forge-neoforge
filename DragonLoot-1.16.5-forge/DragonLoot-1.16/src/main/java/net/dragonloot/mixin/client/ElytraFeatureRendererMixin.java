package net.dragonloot.mixin.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.dragonloot.entity.model.DragonElytraEntityModel;
import net.dragonloot.init.ItemInit;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.IEntityRenderer;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.entity.layers.ElytraLayer;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.client.renderer.entity.model.EntityModel;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.entity.LivingEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@OnlyIn(Dist.CLIENT)
@Mixin(ElytraLayer.class)
public abstract class ElytraFeatureRendererMixin<T extends LivingEntity, M extends EntityModel<T>> extends LayerRenderer<T, M> {
    private static final ResourceLocation DRAGON_ELYTRA_TEXTURE = new ResourceLocation("dragonloot", "textures/entity/dragon_elytra.png");
    private final DragonElytraEntityModel<T> dragonElytraModel = new DragonElytraEntityModel<>();

    protected ElytraFeatureRendererMixin(IEntityRenderer<T, M> parent) {
        super(parent);
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void dragonloot$renderUpgradedElytra(MatrixStack poseStack, IRenderTypeBuffer buffer, int packedLight, T entity, float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo info) {
        ItemStack chest = entity.getItemBySlot(EquipmentSlotType.CHEST);
        if (chest.getItem() == ItemInit.UPGRADED_DRAGON_CHESTPLATE.get()) {
            poseStack.pushPose();
            poseStack.translate(0.0D, 0.0D, 0.02D);
            this.getParentModel().copyPropertiesTo(this.dragonElytraModel);
            this.dragonElytraModel.setupAnim(entity, limbSwing, limbSwingAmount, partialTick, netHeadYaw, headPitch);
            IVertexBuilder consumer = ItemRenderer.getArmorFoilBuffer(buffer, RenderType.armorCutoutNoCull(DRAGON_ELYTRA_TEXTURE), false, chest.hasFoil());
            this.dragonElytraModel.renderToBuffer(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
            poseStack.popPose();
            info.cancel();
        }
    }
}
