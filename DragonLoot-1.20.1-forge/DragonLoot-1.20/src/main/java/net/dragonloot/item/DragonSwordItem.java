package net.dragonloot.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import java.util.List;
import java.util.Map;
import net.dragonloot.compat.AdvancedNetheriteCompat;
import net.dragonloot.init.ConfigInit;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public class DragonSwordItem extends SwordItem {

    private static final double ADVANCED_NETHERITE_COMPAT_ATTACK_DAMAGE_BONUS = 3.0D;

    public DragonSwordItem(Tier material, int attackDamage, float attackSpeed, Item.Properties properties) {
        super(material, attackDamage, attackSpeed, properties);
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        Multimap<Attribute, AttributeModifier> baseModifiers = this.getDefaultAttributeModifiers(slot);
        if (slot != EquipmentSlot.MAINHAND || !ConfigInit.CONFIG.advanced_netherite_gear_perks_enabled) {
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
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        AdvancedNetheriteCompat.appendSwordPerkTooltips(stack, tooltip);
    }
}