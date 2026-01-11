package net.dragonloot.init;

import net.minecraft.item.ItemModelsProperties;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.Items;
import net.minecraft.util.ResourceLocation;

public final class ModelProviderInit {

    private ModelProviderInit() {
    }

    public static void init() {
        ItemModelsProperties.register(ItemInit.DRAGON_BOW_ITEM.get(), new ResourceLocation("pull"), (stack, level, entity) -> {
            if (entity == null) {
                return 0.0F;
            }
            return entity.getUseItem() != stack ? 0.0F : (float) (stack.getUseDuration() - entity.getUseItemRemainingTicks()) / 20.0F;
        });
        ItemModelsProperties.register(ItemInit.DRAGON_BOW_ITEM.get(), new ResourceLocation("pulling"), (stack, level, entity) -> entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1.0F : 0.0F);
        ItemModelsProperties.register(ItemInit.DRAGON_CROSSBOW_ITEM.get(), new ResourceLocation("pull"), (stack, level, entity) -> {
            if (entity == null) {
                return 0.0F;
            }
            return CrossbowItem.isCharged(stack) ? 0.0F : (float) (stack.getUseDuration() - entity.getUseItemRemainingTicks()) / (float) CrossbowItem.getChargeDuration(stack);
        });
        ItemModelsProperties.register(ItemInit.DRAGON_CROSSBOW_ITEM.get(), new ResourceLocation("pulling"), (stack, level, entity) -> entity != null && entity.isUsingItem() && entity.getUseItem() == stack && !CrossbowItem.isCharged(stack) ? 1.0F : 0.0F);
        ItemModelsProperties.register(ItemInit.DRAGON_CROSSBOW_ITEM.get(), new ResourceLocation("charged"), (stack, level, entity) -> entity != null && CrossbowItem.isCharged(stack) ? 1.0F : 0.0F);
        ItemModelsProperties.register(ItemInit.DRAGON_CROSSBOW_ITEM.get(), new ResourceLocation("firework"), (stack, level, entity) -> entity != null && CrossbowItem.isCharged(stack) && CrossbowItem.containsChargedProjectile(stack, Items.FIREWORK_ROCKET) ? 1.0F : 0.0F);
        ItemModelsProperties.register(ItemInit.DRAGON_TRIDENT_ITEM.get(), new ResourceLocation("throwing"), (stack, level, entity) -> entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1.0F : 0.0F);
    }
}
