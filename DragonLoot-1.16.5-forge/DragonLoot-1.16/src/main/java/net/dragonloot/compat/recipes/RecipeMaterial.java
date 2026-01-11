package net.dragonloot.compat.recipes;

import net.minecraft.util.ResourceLocation;

public class RecipeMaterial {

    public ResourceLocation baseItem;
    public ResourceLocation additionItem;
    public String baseType;
    public String additionType;
    public ResourceLocation output;
    public ResourceLocation template;

    public RecipeMaterial(ResourceLocation baseItem, ResourceLocation additionItem, String baseType, String additionType, ResourceLocation output, ResourceLocation template) {
        this.baseItem = baseItem;
        this.additionItem = additionItem;
        this.baseType = baseType;
        this.additionType = additionType;
        this.output = output;
        this.template = template;
    }

}
