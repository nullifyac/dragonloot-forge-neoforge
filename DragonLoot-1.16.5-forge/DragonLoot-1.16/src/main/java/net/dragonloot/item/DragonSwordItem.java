package net.dragonloot.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import java.util.List;
import java.util.Map;
import net.dragonloot.compat.AdvancedNetheriteCompat;
import net.dragonloot.init.ConfigInit;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.ai.attributes.Attribute;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.IItemTier;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;

public class DragonSwordItem extends SwordItem {

    private static final double ADVANCED_NETHERITE_COMPAT_ATTACK_DAMAGE_BONUS = 3.0D;

    public DragonSwordItem(IItemTier material, int attackDamage, float attackSpeed, Item.Properties properties) {
        super(material, attackDamage, attackSpeed, properties);
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlotType slot, ItemStack stack) {
        Multimap<Attribute, AttributeModifier> baseModifiers = this.getDefaultAttributeModifiers(slot);
        if (slot != EquipmentSlotType.MAINHAND || !ConfigInit.CONFIG.advanced_netherite_gear_perks_enabled) {
            return baseModifiers;
        }

        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
        for (Map.Entry<Attribute, AttributeModifier> entry : baseModifiers.entries()) {
            Attribute attribute = entry.getKey();
            AttributeModifier modifier = entry.getValue();
            if (attribute.equals(Attributes.ATTACK_DAMAGE) && modifier.getOperation() == AttributeModifier.Operation.ADDITION) {
                builder.put(attribute, new AttributeModifier(modifier.getId(), modifier.getName(), modifier.getAmount() + ADVANCED_NETHERITE_COMPAT_ATTACK_DAMAGE_BONUS, modifier.getOperation()));
            } else {
                builder.put(attribute, modifier);
            }
        }
        return builder.build();
    }

    @Override
    public void appendHoverText(ItemStack stack, World worldIn, List<ITextComponent> tooltip, ITooltipFlag flagIn) {
        super.appendHoverText(stack, worldIn, tooltip, flagIn);
        AdvancedNetheriteCompat.appendSwordPerkTooltips(stack, tooltip);
    }
}
