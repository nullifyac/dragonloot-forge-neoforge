package net.dragonloot.item;

import java.util.EnumMap;
import java.util.Map;
import net.dragonloot.DragonLootMain;
import net.dragonloot.init.ConfigInit;
import net.dragonloot.init.TagInit;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAssets;

public final class DragonArmorMaterial {
    private static final int ADVANCED_NETHERITE_COMPAT_MIN_DURABILITY_MULTIPLIER = 47;
    private static ArmorMaterial instance;

    private DragonArmorMaterial() {
    }

    public static ArmorMaterial getInstance() {
        if (instance == null) {
            instance = create();
        }
        return instance;
    }

    public static ArmorMaterial create() {
        Map<ArmorType, Integer> defense = new EnumMap<>(ArmorType.class);
        defense.put(ArmorType.BOOTS, ConfigInit.CONFIG.dragon_armor_protection_boots);
        defense.put(ArmorType.LEGGINGS, ConfigInit.CONFIG.dragon_armor_protection_leggings);
        defense.put(ArmorType.CHESTPLATE, ConfigInit.CONFIG.dragon_armor_protection_chest);
        defense.put(ArmorType.HELMET, ConfigInit.CONFIG.dragon_armor_protection_helmet);
        defense.put(ArmorType.BODY, ConfigInit.CONFIG.dragon_armor_protection_horse);
        int multiplier = ConfigInit.CONFIG.dragon_armor_durability_multiplier;
        if (ConfigInit.CONFIG.advanced_netherite_gear_perks_enabled) {
            multiplier = Math.max(multiplier, ADVANCED_NETHERITE_COMPAT_MIN_DURABILITY_MULTIPLIER);
        }
        return new ArmorMaterial(
            multiplier, Map.copyOf(defense), ConfigInit.CONFIG.dragon_armor_enchantability,
            SoundEvents.ARMOR_EQUIP_CHAIN, ConfigInit.CONFIG.dragon_armor_toughness,
            ConfigInit.CONFIG.dragon_armor_knockback_resistance, TagInit.REPAIRS_DRAGON_EQUIPMENT,
            ResourceKey.create(EquipmentAssets.ROOT_ID, DragonLootMain.id("dragon"))
        );
    }
}
