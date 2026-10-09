package net.dragonloot.item;

import net.dragonloot.init.ConfigInit;
import net.dragonloot.init.TagInit;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ToolMaterial;

public final class DragonToolMaterial {
    private static final int ADVANCED_NETHERITE_COMPAT_MIN_DURABILITY_MULTIPLIER = 47;
    private static ToolMaterial instance;

    private DragonToolMaterial() {
    }

    public static ToolMaterial getInstance() {
        if (instance == null) {
            int multiplier = ConfigInit.CONFIG.dragon_item_durability_multiplier;
            if (ConfigInit.CONFIG.advanced_netherite_gear_perks_enabled) {
                multiplier = Math.max(multiplier, ADVANCED_NETHERITE_COMPAT_MIN_DURABILITY_MULTIPLIER);
            }
            instance = new ToolMaterial(
                BlockTags.INCORRECT_FOR_NETHERITE_TOOL,
                67 * multiplier,
                ConfigInit.CONFIG.dragon_tool_mining_speed,
                ConfigInit.CONFIG.dragon_item_base_damage,
                ConfigInit.CONFIG.dragon_tool_enchantability,
                TagInit.REPAIRS_DRAGON_EQUIPMENT
            );
        }
        return instance;
    }
}
