package net.dragonloot.item;

import net.dragonloot.init.ConfigInit;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;

public class DragonSwordItem extends Item {
    public DragonSwordItem(ToolMaterial material, Properties properties) {
        super(properties.sword(material, ConfigInit.CONFIG.advanced_netherite_gear_perks_enabled ? 6.0F : 3.0F, -2.4F));
    }
}
