package net.dragonloot.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Unit;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;

public class DragonWingedArmorItem extends DragonArmorItem {
    public DragonWingedArmorItem(ArmorMaterial material, ArmorType type, Properties properties) {
        super(material, type, properties.component(DataComponents.GLIDER, Unit.INSTANCE));
    }
}
