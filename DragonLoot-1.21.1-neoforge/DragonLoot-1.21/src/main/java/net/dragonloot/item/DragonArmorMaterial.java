package net.dragonloot.item;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import net.dragonloot.DragonLootMain;
import net.dragonloot.init.ConfigInit;
import net.dragonloot.init.ItemInit;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;

public final class DragonArmorMaterial {

    private DragonArmorMaterial() {
    }

    public static ArmorMaterial create() {
        Map<ArmorItem.Type, Integer> defenseValues = new EnumMap<>(ArmorItem.Type.class);
        defenseValues.put(ArmorItem.Type.BOOTS, ConfigInit.CONFIG.dragon_armor_protection_boots);
        defenseValues.put(ArmorItem.Type.LEGGINGS, ConfigInit.CONFIG.dragon_armor_protection_leggings);
        defenseValues.put(ArmorItem.Type.CHESTPLATE, ConfigInit.CONFIG.dragon_armor_protection_chest);
        defenseValues.put(ArmorItem.Type.HELMET, ConfigInit.CONFIG.dragon_armor_protection_helmet);

        return new ArmorMaterial(
            defenseValues,
            ConfigInit.CONFIG.dragon_armor_enchantability,
            SoundEvents.ARMOR_EQUIP_CHAIN,
            () -> Ingredient.of(ItemInit.DRAGON_SCALE_ITEM.get()),
            List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(DragonLootMain.MOD_ID, "dragon"))),
            ConfigInit.CONFIG.dragon_armor_toughness,
            ConfigInit.CONFIG.dragon_armor_knockback_resistance
        );
    }
}