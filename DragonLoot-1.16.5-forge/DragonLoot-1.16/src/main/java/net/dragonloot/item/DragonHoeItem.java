package net.dragonloot.item;

import java.util.List;
import net.dragonloot.compat.AdvancedNetheriteCompat;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.HoeItem;
import net.minecraft.item.IItemTier;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;

public class DragonHoeItem extends HoeItem {

    public DragonHoeItem(IItemTier material, int attackDamage, float attackSpeed, Item.Properties properties) {
        super(material, attackDamage, attackSpeed, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, World worldIn, List<ITextComponent> tooltip, ITooltipFlag flagIn) {
        super.appendHoverText(stack, worldIn, tooltip, flagIn);
        AdvancedNetheriteCompat.appendHoePerkTooltips(stack, tooltip);
    }
}
