package net.dragonloot.item;

import net.dragonloot.init.ConfigInit;
import net.dragonloot.init.ItemInit;
import net.minecraft.item.IItemTier;
import net.minecraft.item.crafting.Ingredient;

public class DragonToolMaterial implements IItemTier {

    private DragonToolMaterial() {
    }

    private static DragonToolMaterial INSTANCE = null;

    public static DragonToolMaterial getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new DragonToolMaterial();
        }
        return INSTANCE;
    }

    @Override
    public int getUses() {
        return 67 * ConfigInit.CONFIG.dragon_item_durability_multiplier;
    }

    @Override
    public float getSpeed() {
        return 12.0F;
    }

    @Override
    public float getAttackDamageBonus() {
        return ConfigInit.CONFIG.dragon_item_base_damage;
    }

    @Override
    public int getLevel() {
        return 5;
    }

    @Override
    public int getEnchantmentValue() {
        return ConfigInit.CONFIG.dragon_tool_enchantability;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.of(ItemInit.DRAGON_SCALE_ITEM.get());
    }

    // For LevelZ compat
    @Override
    public String toString() {
        return "DRAGON";
    }

}
