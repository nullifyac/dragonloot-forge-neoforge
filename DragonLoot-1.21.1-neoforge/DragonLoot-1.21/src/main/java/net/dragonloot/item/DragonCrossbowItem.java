package net.dragonloot.item;

import net.dragonloot.init.ItemInit;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class DragonCrossbowItem extends CrossbowItem {

    public DragonCrossbowItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public boolean useOnRelease(ItemStack stack) {
        return stack.is(ItemInit.DRAGON_CROSSBOW_ITEM.get());
    }
}
