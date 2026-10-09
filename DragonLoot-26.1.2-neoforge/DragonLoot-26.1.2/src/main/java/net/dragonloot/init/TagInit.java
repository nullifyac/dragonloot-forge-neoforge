package net.dragonloot.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public class TagInit {
    public static final TagKey<Item> REPAIRS_DRAGON_EQUIPMENT = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("dragonloot", "repairs_dragon_equipment"));
    public static final TagKey<Item> NOT_DESTROYED_BY_EXPLOSION = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("dragonloot", "explosion_resistant"));
    public static final TagKey<Item> AN_PACIFY_ENDERMEN_ARMOR = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("advancednetherite", "pacify_endermen_armor"));
    public static final TagKey<Item> AN_PACIFY_PHANTOMS_ARMOR = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("advancednetherite", "pacify_phantoms_armor"));
    public static final TagKey<Item> AN_PACIFY_PIGLINS_ARMOR = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("advancednetherite", "pacify_piglins_armor"));

    public static void init() {

    }

}
