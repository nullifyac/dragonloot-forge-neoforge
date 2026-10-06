package net.dragonloot.item;

import net.minecraft.entity.LivingEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.IArmorMaterial;
import net.minecraft.item.ItemStack;

public class DragonWingedArmorItem extends DragonArmor {

    public DragonWingedArmorItem(IArmorMaterial material, EquipmentSlotType slot, Properties properties) {
        super(material, slot, properties);
    }

    @Override
    public boolean canElytraFly(ItemStack stack, LivingEntity entity) {
        return stack.getDamageValue() < stack.getMaxDamage() - 1;
    }

    @Override
    public boolean elytraFlightTick(ItemStack stack, LivingEntity entity, int flightTicks) {
        // Preserve the winged armor's existing lack of passive flight wear.
        // Let the normal flight update apply environmental and other mods' checks.
        return this.canElytraFly(stack, entity);
    }
}
