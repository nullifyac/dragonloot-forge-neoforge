package net.dragonloot.item;

import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.Item;

public class DragonBowItem extends BowItem {

    public DragonBowItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public AbstractArrow customArrow(AbstractArrow arrow) {
        arrow.setBaseDamage(arrow.getBaseDamage() * 1.25D + 1.0D);
        return arrow;
    }
}
