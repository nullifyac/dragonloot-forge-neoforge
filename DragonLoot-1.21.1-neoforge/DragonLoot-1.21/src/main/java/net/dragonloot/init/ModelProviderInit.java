package net.dragonloot.init;

import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Items;

public final class ModelProviderInit {

    private ModelProviderInit() {
    }

    public static void init() {
        ItemProperties.register(ItemInit.DRAGON_BOW_ITEM.get(), ResourceLocation.withDefaultNamespace("pull"), (stack, level, entity, seed) -> {
            if (entity == null) {
                return 0.0F;
            }
            return entity.getUseItem() != stack ? 0.0F : (float) (stack.getUseDuration(entity) - entity.getUseItemRemainingTicks()) / 20.0F;
        });
        ItemProperties.register(ItemInit.DRAGON_BOW_ITEM.get(), ResourceLocation.withDefaultNamespace("pulling"), (stack, level, entity, seed) -> entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1.0F : 0.0F);
        ItemProperties.register(ItemInit.DRAGON_CROSSBOW_ITEM.get(), ResourceLocation.withDefaultNamespace("pull"), (stack, level, entity, seed) -> {
            if (entity == null) {
                return 0.0F;
            }
            return CrossbowItem.isCharged(stack) ? 0.0F : (float) (stack.getUseDuration(entity) - entity.getUseItemRemainingTicks()) / (float) CrossbowItem.getChargeDuration(stack, entity);
        });
        ItemProperties.register(ItemInit.DRAGON_CROSSBOW_ITEM.get(), ResourceLocation.withDefaultNamespace("pulling"), (stack, level, entity, seed) -> entity != null && entity.isUsingItem() && entity.getUseItem() == stack && !CrossbowItem.isCharged(stack) ? 1.0F : 0.0F);
        ItemProperties.register(ItemInit.DRAGON_CROSSBOW_ITEM.get(), ResourceLocation.withDefaultNamespace("charged"), (stack, level, entity, seed) -> entity != null && CrossbowItem.isCharged(stack) ? 1.0F : 0.0F);
        // Note: CrossbowItem.containsChargedProjectile was removed in 1.21, so we skip the firework check for now
        ItemProperties.register(ItemInit.DRAGON_TRIDENT_ITEM.get(), ResourceLocation.withDefaultNamespace("throwing"), (stack, level, entity, seed) -> entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1.0F : 0.0F);
    }
}