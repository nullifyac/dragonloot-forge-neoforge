package net.dragonloot.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.ToolMaterial;

public class DragonShovelItem extends ShovelItem {
    public DragonShovelItem(ToolMaterial material, Item.Properties properties) {
        super(material, 1.5F, -3.0F, properties);
    }
}
