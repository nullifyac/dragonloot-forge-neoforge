package net.dragonloot.item;

import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;

public class DragonHoeItem extends HoeItem {
    public DragonHoeItem(ToolMaterial material, Item.Properties properties) {
        super(material, -4.0F, 0.0F, properties);
    }
}
