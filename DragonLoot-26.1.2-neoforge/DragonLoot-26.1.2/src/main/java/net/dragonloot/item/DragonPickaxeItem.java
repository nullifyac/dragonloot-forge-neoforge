package net.dragonloot.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;

public class DragonPickaxeItem extends Item {
    public DragonPickaxeItem(ToolMaterial material, Properties properties) {
        super(properties.pickaxe(material, 1.0F, -2.8F));
    }
}
