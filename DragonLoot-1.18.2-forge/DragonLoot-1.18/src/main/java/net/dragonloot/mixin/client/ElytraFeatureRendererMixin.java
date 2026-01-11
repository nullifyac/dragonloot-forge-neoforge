package net.dragonloot.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.dragonloot.entity.model.DragonElytraEntityModel;
import net.dragonloot.init.ItemInit;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.ElytraLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.model.EntityModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@OnlyIn(Dist.CLIENT)
@Mixin(ElytraLayer.class)
public abstract class ElytraFeatureRendererMixin<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {
    private static final ResourceLocation DRAGON_ELYTRA_TEXTURE = new ResourceLocation("dragonloot", "textures/entity/dragon_elytra.png");
    private final DragonElytraEntityModel<T> dragonElytraModel = new DragonElytraEntityModel<>(DragonElytraEntityModel.createLayerDefinition().bakeRoot());

    protected ElytraFeatureRendererMixin(RenderLayerParent<T, M> parent) {
        super(parent);
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void dragonloot$renderUpgradedElytra(PoseStack poseStack, MultiBufferSource buffer, int packedLight, T entity, float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo info) {
        ItemStack chest = entity.getItemBySlot(EquipmentSlot.CHEST);
        if (chest.is(ItemInit.UPGRADED_DRAGON_CHESTPLATE.get())) {
            poseStack.pushPose();
            poseStack.translate(0.0D, 0.0D, 0.02D);
            this.getParentModel().copyPropertiesTo(this.dragonElytraModel);
            this.dragonElytraModel.setupAnim(entity, limbSwing, limbSwingAmount, partialTick, netHeadYaw, headPitch);
            VertexConsumer consumer = ItemRenderer.getArmorFoilBuffer(buffer, RenderType.armorCutoutNoCull(DRAGON_ELYTRA_TEXTURE), false, chest.hasFoil());
            this.dragonElytraModel.renderToBuffer(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
            poseStack.popPose();
            info.cancel();
        }
    }
}
