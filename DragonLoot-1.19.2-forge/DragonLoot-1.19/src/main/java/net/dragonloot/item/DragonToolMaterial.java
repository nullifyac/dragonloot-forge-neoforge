package net.dragonloot.item;

import net.dragonloot.init.ConfigInit;
import net.dragonloot.init.ItemInit;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;

public class DragonToolMaterial implements Tier {

    private DragonToolMaterial() {
    }

    // Preserve the existing opt-in durability floor. Other balance settings are explicit
    // Dragon Loot config values; this does not read Advanced Netherite's configuration.
    private static final int ADVANCED_NETHERITE_COMPAT_MIN_DURABILITY_MULTIPLIER = 47;

    private static DragonToolMaterial INSTANCE = null;

    public static DragonToolMaterial getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new DragonToolMaterial();
        }
        return INSTANCE;
    }

    @Override
    public int getUses() {
        int multiplier = ConfigInit.CONFIG.dragon_item_durability_multiplier;
        if (ConfigInit.CONFIG.advanced_netherite_gear_perks_enabled) {
            multiplier = Math.max(multiplier, ADVANCED_NETHERITE_COMPAT_MIN_DURABILITY_MULTIPLIER);
        }
        return 67 * multiplier;
    }

    @Override
    public float getSpeed() {
        return ConfigInit.CONFIG.dragon_tool_mining_speed;
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
