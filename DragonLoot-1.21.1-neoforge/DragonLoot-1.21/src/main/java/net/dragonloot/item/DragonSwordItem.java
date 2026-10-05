package net.dragonloot.item;

import java.util.List;
import net.dragonloot.compat.AdvancedNetheriteCompat;
import net.dragonloot.init.ConfigInit;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;

public class DragonSwordItem extends SwordItem {

    public DragonSwordItem(Tier material, Item.Properties properties) {
        super(material, properties);
    }

    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {
        float attackDamage = ConfigInit.CONFIG.advanced_netherite_gear_perks_enabled ? 6.0F : 3.0F;
        return SwordItem.createAttributes(this.getTier(), attackDamage, -2.4F);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        AdvancedNetheriteCompat.appendSwordPerkTooltips(stack, tooltip);
    }
}
