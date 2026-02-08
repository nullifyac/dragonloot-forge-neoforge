package net.dragonloot.item;

import java.util.List;
import net.dragonloot.compat.AdvancedNetheriteCompat;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;

public class DragonPickaxeItem extends PickaxeItem {

    public DragonPickaxeItem(Tier material, Item.Properties properties) {
        super(material, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        AdvancedNetheriteCompat.appendPickaxePerkTooltips(stack, tooltip);
    }
}
