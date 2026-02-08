package net.dragonloot.item;

import net.dragonloot.init.ConfigInit;
import net.dragonloot.init.ItemInit;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;

public class DragonArmorMaterial implements ArmorMaterial {

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

    @Override
    public int getDurabilityForType(ArmorItem.Type type) {
        return BASE_DURABILITY[type.getSlot().getIndex()] * ConfigInit.CONFIG.dragon_armor_durability_multiplier;
    }

    @Override
    public int getDefenseForType(ArmorItem.Type type) {
        // Do not cache config-driven values in static fields. The config is loaded/baked after
        // classloading, and caching would permanently lock in the defaults.
        switch (type) {
            case BOOTS:
                return ConfigInit.CONFIG.dragon_armor_protection_boots;
            case LEGGINGS:
                return ConfigInit.CONFIG.dragon_armor_protection_leggings;
            case CHESTPLATE:
                return ConfigInit.CONFIG.dragon_armor_protection_chest;
            case HELMET:
                return ConfigInit.CONFIG.dragon_armor_protection_helmet;
            default:
                return 0;
        }
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

}
