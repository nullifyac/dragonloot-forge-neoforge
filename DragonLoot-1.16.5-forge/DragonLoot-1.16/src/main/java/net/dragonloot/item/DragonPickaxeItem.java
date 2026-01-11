package net.dragonloot.item;

import net.minecraft.item.IItemTier;
import net.minecraft.item.Item;
import net.minecraft.item.PickaxeItem;

public class DragonPickaxeItem extends PickaxeItem {

    public DragonPickaxeItem(IItemTier material, int attackDamage, float attackSpeed, Item.Properties properties) {
        super(material, attackDamage, attackSpeed, properties);
    }

}
