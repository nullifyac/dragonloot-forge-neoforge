package net.dragonloot.item;

import net.dragonloot.init.ConfigInit;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;

public class DragonAxeItem extends AxeItem {
    public DragonAxeItem(ToolMaterial material, Item.Properties properties) {
        super(material, ConfigInit.CONFIG.advanced_netherite_gear_perks_enabled ? 8.0F : 5.0F, -3.0F, properties);
    }
}
