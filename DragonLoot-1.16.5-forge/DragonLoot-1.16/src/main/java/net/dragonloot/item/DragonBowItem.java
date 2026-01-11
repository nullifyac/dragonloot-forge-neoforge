package net.dragonloot.item;

import net.minecraft.entity.projectile.AbstractArrowEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.Item;

public class DragonBowItem extends BowItem {

    public DragonBowItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public AbstractArrowEntity customArrow(AbstractArrowEntity arrow) {
        arrow.setBaseDamage(arrow.getBaseDamage() * 1.25D + 1.0D);
        return arrow;
    }
}
