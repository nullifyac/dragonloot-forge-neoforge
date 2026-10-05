package net.dragonloot.item;

import net.dragonloot.init.ConfigInit;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.component.ItemAttributeModifiers;

public class DragonAxeItem extends AxeItem {

    public DragonAxeItem(Tier material, Item.Properties properties) {
        super(material, properties);
    }

    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {
        float attackDamage = ConfigInit.CONFIG.advanced_netherite_gear_perks_enabled ? 8.0F : 5.0F;
        return AxeItem.createAttributes(this.getTier(), attackDamage, -3.0F);
    }
}
