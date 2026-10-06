package net.dragonloot.item;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ElytraItem;
import net.minecraft.world.item.ItemStack;

public class DragonWingedArmorItem extends DragonArmorItem {

    public DragonWingedArmorItem(Holder<ArmorMaterial> material, ArmorItem.Type type, Properties properties) {
        super(material, type, properties);
    }

    @Override
    public boolean canElytraFly(ItemStack stack, LivingEntity entity) {
        return ElytraItem.isFlyEnabled(stack);
    }

    @Override
    public boolean elytraFlightTick(ItemStack stack, LivingEntity entity, int flightTicks) {
        // Preserve the winged armor's existing lack of passive flight wear.
        // Let the normal flight update apply environmental and other mods' checks.
        return this.canElytraFly(stack, entity);
    }
}
