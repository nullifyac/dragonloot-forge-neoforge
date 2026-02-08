package net.dragonloot.init;

import net.dragonloot.DragonLootMain;
import net.dragonloot.item.DragonArmor;
import net.dragonloot.item.DragonArmorMaterial;
import net.dragonloot.item.DragonAxeItem;
import net.dragonloot.item.DragonBowItem;
import net.dragonloot.item.DragonCrossbowItem;
import net.dragonloot.item.DragonHoeItem;
import net.dragonloot.item.DragonPickaxeItem;
import net.dragonloot.item.DragonScaleItem;
import net.dragonloot.item.DragonShovelItem;
import net.dragonloot.item.DragonSwordItem;
import net.dragonloot.item.DragonToolMaterial;
import net.dragonloot.item.DragonTridentItem;
import net.dragonloot.item.render.DragonTridentBewlr;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.IArmorMaterial;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.HorseArmorItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.fml.RegistryObject;

public final class ItemInit {

    private ItemInit() {
    }

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, DragonLootMain.MOD_ID);

    // Use the same translation key pattern as newer versions: itemGroup.<modid>.<tabid>
    public static final ItemGroup DRAGON_ITEM_GROUP = new ItemGroup(DragonLootMain.MOD_ID + "." + DragonLootMain.MOD_ID) {
        @Override
        public ItemStack makeIcon() {
            return new ItemStack(DRAGON_SCALE_ITEM.get());
        }
    };

    public static final RegistryObject<Item> DRAGON_SCALE_ITEM = ITEMS.register("dragon_scale", () -> new DragonScaleItem(new Item.Properties().fireResistant().tab(DRAGON_ITEM_GROUP)));
    public static final RegistryObject<Item> DRAGON_HORSE_ARMOR_ITEM = ITEMS.register("dragon_horse_armor", () -> new HorseArmorItem(18, "dragon", new Item.Properties().stacksTo(1).fireResistant().tab(DRAGON_ITEM_GROUP)));

    public static final IArmorMaterial DRAGON_ARMOR_MATERIAL = DragonArmorMaterial.getInstance();

    public static final RegistryObject<Item> DRAGON_HELMET = ITEMS.register("dragon_helmet", () -> new DragonArmor(DRAGON_ARMOR_MATERIAL, EquipmentSlotType.HEAD, new Item.Properties().fireResistant().tab(DRAGON_ITEM_GROUP)));
    public static final RegistryObject<Item> DRAGON_CHESTPLATE = ITEMS.register("dragon_chestplate", () -> new DragonArmor(DRAGON_ARMOR_MATERIAL, EquipmentSlotType.CHEST, new Item.Properties().fireResistant().tab(DRAGON_ITEM_GROUP)));
    public static final RegistryObject<Item> DRAGON_LEGGINGS = ITEMS.register("dragon_leggings", () -> new DragonArmor(DRAGON_ARMOR_MATERIAL, EquipmentSlotType.LEGS, new Item.Properties().fireResistant().tab(DRAGON_ITEM_GROUP)));
    public static final RegistryObject<Item> DRAGON_BOOTS = ITEMS.register("dragon_boots", () -> new DragonArmor(DRAGON_ARMOR_MATERIAL, EquipmentSlotType.FEET, new Item.Properties().fireResistant().tab(DRAGON_ITEM_GROUP)));
    public static final RegistryObject<Item> UPGRADED_DRAGON_CHESTPLATE = ITEMS.register("upgraded_dragon_chestplate", () -> new DragonArmor(DRAGON_ARMOR_MATERIAL, EquipmentSlotType.CHEST, new Item.Properties().fireResistant().tab(DRAGON_ITEM_GROUP)));

    public static final RegistryObject<Item> DRAGON_PICKAXE_ITEM = ITEMS.register("dragon_pickaxe", () -> new DragonPickaxeItem(DragonToolMaterial.getInstance(), 1, -2.8f, new Item.Properties().fireResistant().tab(DRAGON_ITEM_GROUP)));
    public static final RegistryObject<Item> DRAGON_AXE_ITEM = ITEMS.register("dragon_axe", () -> new DragonAxeItem(DragonToolMaterial.getInstance(), 5, -3.0f, new Item.Properties().fireResistant().tab(DRAGON_ITEM_GROUP)));
    public static final RegistryObject<Item> DRAGON_SHOVEL_ITEM = ITEMS.register("dragon_shovel", () -> new DragonShovelItem(DragonToolMaterial.getInstance(), 1.5f, -3.0f, new Item.Properties().fireResistant().tab(DRAGON_ITEM_GROUP)));
    public static final RegistryObject<Item> DRAGON_HOE_ITEM = ITEMS.register("dragon_hoe", () -> new DragonHoeItem(DragonToolMaterial.getInstance(), -4, -2.0f, new Item.Properties().fireResistant().tab(DRAGON_ITEM_GROUP)));

    public static final RegistryObject<Item> DRAGON_SWORD_ITEM = ITEMS.register("dragon_sword", () -> new DragonSwordItem(DragonToolMaterial.getInstance(), 3, -2.4f, new Item.Properties().fireResistant().tab(DRAGON_ITEM_GROUP)));
    public static final RegistryObject<Item> DRAGON_BOW_ITEM = ITEMS.register("dragon_bow", () -> new DragonBowItem(new Item.Properties().fireResistant().durability(DragonToolMaterial.getInstance().getUses()).tab(DRAGON_ITEM_GROUP)));
    public static final RegistryObject<Item> DRAGON_CROSSBOW_ITEM = ITEMS.register("dragon_crossbow", () -> new DragonCrossbowItem(new Item.Properties().fireResistant().durability(DragonToolMaterial.getInstance().getUses()).tab(DRAGON_ITEM_GROUP)));
    public static final RegistryObject<Item> DRAGON_TRIDENT_ITEM = ITEMS.register("dragon_trident", () -> new DragonTridentItem(new Item.Properties()
        .fireResistant()
        .durability(DragonToolMaterial.getInstance().getUses())
        .tab(DRAGON_ITEM_GROUP)
        .setISTER(() -> DragonTridentBewlr::new)));

    public static final RegistryObject<Item> DRAGON_ANVIL_ITEM = ITEMS.register("dragon_anvil", () -> new BlockItem(BlockInit.DRAGON_ANVIL_BLOCK.get(), new Item.Properties().tab(DRAGON_ITEM_GROUP)));
}
