package net.dragonloot.compat.recipes;

import net.dragonloot.DragonLootMain;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.ModList;

public class CompatRecipes {

    private static final ResourceLocation NETHERITE_UPGRADE_TEMPLATE = new ResourceLocation("minecraft:netherite_upgrade_smithing_template");

    public static void loadRecipes() {

        if (ModList.get().isLoaded("netherite_plus")) {
            RecipeGenerator.SMITHING_RECIPES.put("dragon_horse_armor", new RecipeMaterial(new ResourceLocation("netherite_plus", "netherite_horse_armor"), DragonLootMain.id("dragon_scale"), "item", "item", DragonLootMain.id("dragon_horse_armor"), NETHERITE_UPGRADE_TEMPLATE));
            RecipeGenerator.SMITHING_RECIPES.put("dragon_trident", new RecipeMaterial(new ResourceLocation("netherite_plus", "netherite_trident"), DragonLootMain.id("dragon_scale"), "item", "item", DragonLootMain.id("dragon_trident"), NETHERITE_UPGRADE_TEMPLATE));
            RecipeGenerator.SMITHING_RECIPES.put("dragon_bow", new RecipeMaterial(new ResourceLocation("netherite_plus", "netherite_bow"), DragonLootMain.id("dragon_scale"), "item", "item", DragonLootMain.id("dragon_bow"), NETHERITE_UPGRADE_TEMPLATE));
            RecipeGenerator.SMITHING_RECIPES.put("dragon_crossbow", new RecipeMaterial(new ResourceLocation("netherite_plus", "netherite_crossbow"), DragonLootMain.id("dragon_scale"), "item", "item", DragonLootMain.id("dragon_crossbow"), NETHERITE_UPGRADE_TEMPLATE));
            RecipeGenerator.SMITHING_RECIPES.put("upgraded_dragon_chestplate", new RecipeMaterial(new ResourceLocation("dragonloot", "dragon_chestplate"), new ResourceLocation("netherite_plus", "netherite_elytra"), "item", "item", DragonLootMain.id("upgraded_dragon_chestplate"), NETHERITE_UPGRADE_TEMPLATE));
        } else {
            RecipeGenerator.SMITHING_RECIPES.put("dragon_horse_armor", new RecipeMaterial(new ResourceLocation("minecraft", "diamond_horse_armor"), DragonLootMain.id("dragon_scale"), "item", "item", DragonLootMain.id("dragon_horse_armor"), NETHERITE_UPGRADE_TEMPLATE));
            RecipeGenerator.SMITHING_RECIPES.put("dragon_trident", new RecipeMaterial(new ResourceLocation("minecraft", "trident"), DragonLootMain.id("dragon_scale"), "item", "item", DragonLootMain.id("dragon_trident"), NETHERITE_UPGRADE_TEMPLATE));
            RecipeGenerator.SMITHING_RECIPES.put("dragon_bow", new RecipeMaterial(new ResourceLocation("minecraft", "bow"), DragonLootMain.id("dragon_scale"), "item", "item", DragonLootMain.id("dragon_bow"), NETHERITE_UPGRADE_TEMPLATE));
            RecipeGenerator.SMITHING_RECIPES.put("dragon_crossbow", new RecipeMaterial(new ResourceLocation("minecraft", "crossbow"), DragonLootMain.id("dragon_scale"), "item", "item", DragonLootMain.id("dragon_crossbow"), NETHERITE_UPGRADE_TEMPLATE));
            RecipeGenerator.SMITHING_RECIPES.put("upgraded_dragon_chestplate", new RecipeMaterial(new ResourceLocation("dragonloot", "dragon_chestplate"), new ResourceLocation("minecraft", "elytra"), "item", "item", DragonLootMain.id("upgraded_dragon_chestplate"), NETHERITE_UPGRADE_TEMPLATE));

        }

    }
}

// {"type":"minecraft:smithing","base":{"item":"dragonloot:dragon_chestplate"},"addition":{"item":"minecraft:elytra"},"result":{"item":"dragonloot:upgraded_dragon_chestplate"}}
