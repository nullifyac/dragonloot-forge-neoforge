package net.dragonloot.item;

import net.dragonloot.init.ConfigInit;
import net.dragonloot.init.ItemInit;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.IArmorMaterial;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.SoundEvents;

public class DragonArmorMaterial implements IArmorMaterial {

    private DragonArmorMaterial() {
    }

    private static DragonArmorMaterial INSTANCE = null;

    public static DragonArmorMaterial getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new DragonArmorMaterial();
        }
        return INSTANCE;
    }

    private static final int[] BASE_DURABILITY = new int[] { 28, 32, 35, 26 };
    private static final int[] PROTECTION_AMOUNTS = new int[] { ConfigInit.CONFIG.dragon_armor_protection_boots, ConfigInit.CONFIG.dragon_armor_protection_leggings, ConfigInit.CONFIG.dragon_armor_protection_chest, ConfigInit.CONFIG.dragon_armor_protection_helmet };

    @Override
    public int getDurabilityForSlot(EquipmentSlotType slot) {
        return BASE_DURABILITY[getIndex(slot)] * ConfigInit.CONFIG.dragon_armor_durability_multiplier;
    }

    @Override
    public int getDefenseForSlot(EquipmentSlotType slot) {
        return PROTECTION_AMOUNTS[getIndex(slot)];
    }

    @Override
    public int getEnchantmentValue() {
        return ConfigInit.CONFIG.dragon_armor_enchantability;
    }

    @Override
    public SoundEvent getEquipSound() {
        return SoundEvents.ARMOR_EQUIP_CHAIN;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.of(ItemInit.DRAGON_SCALE_ITEM.get());
    }

    @Override
    public String getName() {
        return "dragon";
    }

    @Override
    public float getToughness() {
        return ConfigInit.CONFIG.dragon_armor_toughness;
    }

    @Override
    public float getKnockbackResistance() {
        return ConfigInit.CONFIG.dragon_armor_knockback_resistance;
    }

    private static int getIndex(EquipmentSlotType slot) {
        switch (slot) {
            case FEET:
                return 0;
            case LEGS:
                return 1;
            case CHEST:
                return 2;
            case HEAD:
                return 3;
            default:
                return 0;
        }
    }

}
