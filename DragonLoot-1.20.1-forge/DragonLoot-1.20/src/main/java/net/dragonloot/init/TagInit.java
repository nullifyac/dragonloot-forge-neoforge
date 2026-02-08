package net.dragonloot.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public class TagInit {
    public static final TagKey<Item> NOT_DESTROYED_BY_EXPLOSION = TagKey.create(Registries.ITEM, new ResourceLocation("dragonloot", "explosion_resistant"));
    public static final TagKey<Item> AN_PACIFY_ENDERMEN_ARMOR = TagKey.create(Registries.ITEM, new ResourceLocation("advancednetherite", "pacify_endermen_armor"));
    public static final TagKey<Item> AN_PACIFY_PHANTOMS_ARMOR = TagKey.create(Registries.ITEM, new ResourceLocation("advancednetherite", "pacify_phantoms_armor"));
    public static final TagKey<Item> AN_PACIFY_PIGLINS_ARMOR = TagKey.create(Registries.ITEM, new ResourceLocation("advancednetherite", "pacify_piglins_armor"));
    public static final TagKey<Item> AN_DROPS_ADDITIONAL_CROPS = TagKey.create(Registries.ITEM, new ResourceLocation("advancednetherite", "drops_additional_crops"));
    public static final TagKey<Item> AN_DROPS_ADDITIONAL_IRON = TagKey.create(Registries.ITEM, new ResourceLocation("advancednetherite", "drops_additional_iron"));
    public static final TagKey<Item> AN_DROPS_ADDITIONAL_GOLD = TagKey.create(Registries.ITEM, new ResourceLocation("advancednetherite", "drops_additional_gold"));
    public static final TagKey<Item> AN_DROPS_ADDITIONAL_EMERALD = TagKey.create(Registries.ITEM, new ResourceLocation("advancednetherite", "drops_additional_emerald"));
    public static final TagKey<Item> AN_DROPS_ADDITIONAL_DIAMOND = TagKey.create(Registries.ITEM, new ResourceLocation("advancednetherite", "drops_additional_diamond"));
    public static final TagKey<Item> AN_DROPS_ADDITIONAL_PHANTOM_LOOT = TagKey.create(Registries.ITEM, new ResourceLocation("advancednetherite", "drops_additional_phantom_loot"));
    public static final TagKey<Item> AN_DROPS_ADDITIONAL_PIGLIN_LOOT = TagKey.create(Registries.ITEM, new ResourceLocation("advancednetherite", "drops_additional_piglin_loot"));
    public static final TagKey<Item> AN_DROPS_ADDITIONAL_ZOMBIFIED_PIGLIN_LOOT = TagKey.create(Registries.ITEM, new ResourceLocation("advancednetherite", "drops_additional_zombified_piglin_loot"));
    public static final TagKey<Item> AN_DROPS_ADDITIONAL_ENDERMAN_LOOT = TagKey.create(Registries.ITEM, new ResourceLocation("advancednetherite", "drops_additional_enderman_loot"));

    public static void init() {

    }

}
