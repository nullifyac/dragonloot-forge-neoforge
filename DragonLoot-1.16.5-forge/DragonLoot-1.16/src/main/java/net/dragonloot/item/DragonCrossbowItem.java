package net.dragonloot.item;

import net.dragonloot.init.ItemInit;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class DragonCrossbowItem extends CrossbowItem {

    public DragonCrossbowItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public boolean useOnRelease(ItemStack stack) {
        return stack.getItem() == ItemInit.DRAGON_CROSSBOW_ITEM.get();
    }
}
