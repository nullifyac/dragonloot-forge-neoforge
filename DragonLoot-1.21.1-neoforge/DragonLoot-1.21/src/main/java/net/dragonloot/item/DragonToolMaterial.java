package net.dragonloot.item;

import net.dragonloot.init.ConfigInit;
import net.dragonloot.init.ItemInit;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;

public class DragonToolMaterial implements Tier {

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

    public float getAttackDamageBonus() {
        return ConfigInit.CONFIG.dragon_item_base_damage;
    }

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

    @Override
    public net.minecraft.tags.TagKey<net.minecraft.world.level.block.Block> getIncorrectBlocksForDrops() {
        return net.minecraft.tags.BlockTags.INCORRECT_FOR_NETHERITE_TOOL;
    }

    // For LevelZ compat
    public String toString() {
        return "DRAGON";
    }

}
