package net.dragonloot.item;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;

public class DragonWingedArmorItem extends DragonArmor {

    public DragonWingedArmorItem(ArmorMaterial material, EquipmentSlot slot, Properties properties) {
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
