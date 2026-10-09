package net.dragonloot.init;

import java.util.function.Supplier;
import net.dragonloot.DragonLootMain;
import net.dragonloot.item.DragonArmorItem;
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
import net.dragonloot.item.DragonWingedArmorItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ItemInit {
    private ItemInit() {
    }

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(DragonLootMain.MOD_ID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, DragonLootMain.MOD_ID);
    public static final Supplier<ArmorMaterial> DRAGON_ARMOR_MATERIAL = DragonArmorMaterial::getInstance;

    public static final DeferredItem<DragonScaleItem> DRAGON_SCALE_ITEM = ITEMS.registerItem("dragon_scale", DragonScaleItem::new, properties -> properties.fireResistant());
    public static final DeferredItem<Item> DRAGON_HORSE_ARMOR_ITEM = ITEMS.registerItem("dragon_horse_armor", Item::new,
        properties -> properties.horseArmor(DRAGON_ARMOR_MATERIAL.get()).fireResistant());

    public static final DeferredItem<DragonArmorItem> DRAGON_HELMET = ITEMS.registerItem("dragon_helmet",
        properties -> new DragonArmorItem(DRAGON_ARMOR_MATERIAL.get(), ArmorType.HELMET, properties.fireResistant()));
    public static final DeferredItem<DragonArmorItem> DRAGON_CHESTPLATE = ITEMS.registerItem("dragon_chestplate",
        properties -> new DragonArmorItem(DRAGON_ARMOR_MATERIAL.get(), ArmorType.CHESTPLATE, properties.fireResistant()));
    public static final DeferredItem<DragonArmorItem> DRAGON_LEGGINGS = ITEMS.registerItem("dragon_leggings",
        properties -> new DragonArmorItem(DRAGON_ARMOR_MATERIAL.get(), ArmorType.LEGGINGS, properties.fireResistant()));
    public static final DeferredItem<DragonArmorItem> DRAGON_BOOTS = ITEMS.registerItem("dragon_boots",
        properties -> new DragonArmorItem(DRAGON_ARMOR_MATERIAL.get(), ArmorType.BOOTS, properties.fireResistant()));
    public static final DeferredItem<DragonWingedArmorItem> UPGRADED_DRAGON_CHESTPLATE = ITEMS.registerItem("upgraded_dragon_chestplate",
        properties -> new DragonWingedArmorItem(DRAGON_ARMOR_MATERIAL.get(), ArmorType.CHESTPLATE, properties.fireResistant()));

    public static final DeferredItem<DragonPickaxeItem> DRAGON_PICKAXE_ITEM = ITEMS.registerItem("dragon_pickaxe",
        properties -> new DragonPickaxeItem(DragonToolMaterial.getInstance(), properties.fireResistant()));
    public static final DeferredItem<DragonAxeItem> DRAGON_AXE_ITEM = ITEMS.registerItem("dragon_axe",
        properties -> new DragonAxeItem(DragonToolMaterial.getInstance(), properties.fireResistant()));
    public static final DeferredItem<DragonShovelItem> DRAGON_SHOVEL_ITEM = ITEMS.registerItem("dragon_shovel",
        properties -> new DragonShovelItem(DragonToolMaterial.getInstance(), properties.fireResistant()));
    public static final DeferredItem<DragonHoeItem> DRAGON_HOE_ITEM = ITEMS.registerItem("dragon_hoe",
        properties -> new DragonHoeItem(DragonToolMaterial.getInstance(), properties.fireResistant()));
    public static final DeferredItem<DragonSwordItem> DRAGON_SWORD_ITEM = ITEMS.registerItem("dragon_sword",
        properties -> new DragonSwordItem(DragonToolMaterial.getInstance(), properties.fireResistant()));
    public static final DeferredItem<DragonBowItem> DRAGON_BOW_ITEM = ITEMS.registerItem("dragon_bow", DragonBowItem::new, ItemInit::weaponProperties);
    public static final DeferredItem<DragonCrossbowItem> DRAGON_CROSSBOW_ITEM = ITEMS.registerItem("dragon_crossbow", DragonCrossbowItem::new,
        () -> weaponProperties().component(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.EMPTY));
    public static final DeferredItem<DragonTridentItem> DRAGON_TRIDENT_ITEM = ITEMS.registerItem("dragon_trident", DragonTridentItem::new,
        () -> weaponProperties().component(DataComponents.TOOL, TridentItem.createToolProperties()));
    public static final DeferredItem<BlockItem> DRAGON_ANVIL_ITEM = ITEMS.registerItem("dragon_anvil",
        properties -> new BlockItem(BlockInit.DRAGON_ANVIL_BLOCK.get(), properties));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> DRAGON_ITEM_GROUP = CREATIVE_TABS.register("dragonloot", () -> CreativeModeTab.builder()
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
        }).build());

    private static Item.Properties weaponProperties() {
        ToolMaterial material = DragonToolMaterial.getInstance();
        return new Item.Properties().fireResistant().durability(material.durability())
            .enchantable(material.enchantmentValue()).repairable(material.repairItems());
    }
}
