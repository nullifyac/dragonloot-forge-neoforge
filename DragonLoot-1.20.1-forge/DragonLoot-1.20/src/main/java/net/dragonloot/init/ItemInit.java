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
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.HorseArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ItemInit {

    private ItemInit() {
    }

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, DragonLootMain.MOD_ID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, DragonLootMain.MOD_ID);

    public static final RegistryObject<Item> DRAGON_SCALE_ITEM = ITEMS.register("dragon_scale", () -> new DragonScaleItem(new Item.Properties().fireResistant()));
    public static final RegistryObject<Item> DRAGON_HORSE_ARMOR_ITEM = ITEMS.register("dragon_horse_armor", () -> new HorseArmorItem(18, "dragon", new Item.Properties().stacksTo(1).fireResistant()));

    public static final ArmorMaterial DRAGON_ARMOR_MATERIAL = DragonArmorMaterial.getInstance();

    public static final RegistryObject<Item> DRAGON_HELMET = ITEMS.register("dragon_helmet", () -> new DragonArmor(DRAGON_ARMOR_MATERIAL, ArmorItem.Type.HELMET, new Item.Properties().fireResistant()));
    public static final RegistryObject<Item> DRAGON_CHESTPLATE = ITEMS.register("dragon_chestplate", () -> new DragonArmor(DRAGON_ARMOR_MATERIAL, ArmorItem.Type.CHESTPLATE, new Item.Properties().fireResistant()));
    public static final RegistryObject<Item> DRAGON_LEGGINGS = ITEMS.register("dragon_leggings", () -> new DragonArmor(DRAGON_ARMOR_MATERIAL, ArmorItem.Type.LEGGINGS, new Item.Properties().fireResistant()));
    public static final RegistryObject<Item> DRAGON_BOOTS = ITEMS.register("dragon_boots", () -> new DragonArmor(DRAGON_ARMOR_MATERIAL, ArmorItem.Type.BOOTS, new Item.Properties().fireResistant()));
    public static final RegistryObject<Item> UPGRADED_DRAGON_CHESTPLATE = ITEMS.register("upgraded_dragon_chestplate", () -> new DragonArmor(DRAGON_ARMOR_MATERIAL, ArmorItem.Type.CHESTPLATE, new Item.Properties().fireResistant()));

    public static final RegistryObject<Item> DRAGON_PICKAXE_ITEM = ITEMS.register("dragon_pickaxe", () -> new DragonPickaxeItem(DragonToolMaterial.getInstance(), 1, -2.8f, new Item.Properties().fireResistant()));
    public static final RegistryObject<Item> DRAGON_AXE_ITEM = ITEMS.register("dragon_axe", () -> new DragonAxeItem(DragonToolMaterial.getInstance(), 5, -3.0f, new Item.Properties().fireResistant()));
    public static final RegistryObject<Item> DRAGON_SHOVEL_ITEM = ITEMS.register("dragon_shovel", () -> new DragonShovelItem(DragonToolMaterial.getInstance(), 1.5f, -3.0f, new Item.Properties().fireResistant()));
    public static final RegistryObject<Item> DRAGON_HOE_ITEM = ITEMS.register("dragon_hoe", () -> new DragonHoeItem(DragonToolMaterial.getInstance(), -4, -2.0f, new Item.Properties().fireResistant()));

    public static final RegistryObject<Item> DRAGON_SWORD_ITEM = ITEMS.register("dragon_sword", () -> new DragonSwordItem(DragonToolMaterial.getInstance(), 3, -2.4f, new Item.Properties().fireResistant()));
    public static final RegistryObject<Item> DRAGON_BOW_ITEM = ITEMS.register("dragon_bow", () -> new DragonBowItem(new Item.Properties().fireResistant().durability(DragonToolMaterial.getInstance().getUses())));
    public static final RegistryObject<Item> DRAGON_CROSSBOW_ITEM = ITEMS.register("dragon_crossbow", () -> new DragonCrossbowItem(new Item.Properties().fireResistant().durability(DragonToolMaterial.getInstance().getUses())));
    public static final RegistryObject<Item> DRAGON_TRIDENT_ITEM = ITEMS.register("dragon_trident", () -> new DragonTridentItem(new Item.Properties().fireResistant().durability(DragonToolMaterial.getInstance().getUses())));

    public static final RegistryObject<Item> DRAGON_ANVIL_ITEM = ITEMS.register("dragon_anvil", () -> new BlockItem(BlockInit.DRAGON_ANVIL_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<CreativeModeTab> DRAGON_ITEM_GROUP = CREATIVE_TABS.register("dragonloot", () -> CreativeModeTab.builder()
        .icon(() -> new ItemStack(DRAGON_SCALE_ITEM.get()))
        .title(Component.translatable("itemGroup.dragonloot.dragonloot"))
        .displayItems((parameters, output) -> {
            output.accept(DRAGON_SCALE_ITEM.get());
            output.accept(DRAGON_HORSE_ARMOR_ITEM.get());
            output.accept(DRAGON_HELMET.get());
            output.accept(DRAGON_CHESTPLATE.get());
            output.accept(DRAGON_LEGGINGS.get());
            output.accept(DRAGON_BOOTS.get());
            output.accept(UPGRADED_DRAGON_CHESTPLATE.get());
            output.accept(DRAGON_PICKAXE_ITEM.get());
            output.accept(DRAGON_AXE_ITEM.get());
            output.accept(DRAGON_SHOVEL_ITEM.get());
            output.accept(DRAGON_HOE_ITEM.get());
            output.accept(DRAGON_SWORD_ITEM.get());
            output.accept(DRAGON_BOW_ITEM.get());
            output.accept(DRAGON_CROSSBOW_ITEM.get());
            output.accept(DRAGON_TRIDENT_ITEM.get());
            output.accept(DRAGON_ANVIL_ITEM.get());
        })
        .build());
}
