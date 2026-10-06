package net.dragonloot.init;

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
import net.minecraft.world.item.AnimalArmorItem;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ItemInit {

    private ItemInit() {
    }

    // Keep Dragon armor durability competitive when Advanced Netherite compat/perks are enabled.
    // Advanced Netherite's top tier uses a 47 durability multiplier.
    private static final int ADVANCED_NETHERITE_COMPAT_MIN_ARMOR_DURABILITY_MULTIPLIER = 47;

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, DragonLootMain.MOD_ID);
    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS = DeferredRegister.create(Registries.ARMOR_MATERIAL, DragonLootMain.MOD_ID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, DragonLootMain.MOD_ID);

    public static final DeferredHolder<Item, Item> DRAGON_SCALE_ITEM = ITEMS.register("dragon_scale", () -> new DragonScaleItem(new Item.Properties().fireResistant()));

    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> DRAGON_ARMOR_MATERIAL = ARMOR_MATERIALS.register("dragon", DragonArmorMaterial::create);

    public static final DeferredHolder<Item, Item> DRAGON_HORSE_ARMOR_ITEM = ITEMS.register("dragon_horse_armor", () -> new AnimalArmorItem(DRAGON_ARMOR_MATERIAL, AnimalArmorItem.BodyType.EQUESTRIAN, false, new Item.Properties().stacksTo(1).fireResistant()));

    public static final DeferredHolder<Item, Item> DRAGON_HELMET = ITEMS.register("dragon_helmet", () -> new DragonArmorItem(DRAGON_ARMOR_MATERIAL, ArmorItem.Type.HELMET, armorProperties(ArmorItem.Type.HELMET)));
    public static final DeferredHolder<Item, Item> DRAGON_CHESTPLATE = ITEMS.register("dragon_chestplate", () -> new DragonArmorItem(DRAGON_ARMOR_MATERIAL, ArmorItem.Type.CHESTPLATE, armorProperties(ArmorItem.Type.CHESTPLATE)));
    public static final DeferredHolder<Item, Item> DRAGON_LEGGINGS = ITEMS.register("dragon_leggings", () -> new DragonArmorItem(DRAGON_ARMOR_MATERIAL, ArmorItem.Type.LEGGINGS, armorProperties(ArmorItem.Type.LEGGINGS)));
    public static final DeferredHolder<Item, Item> DRAGON_BOOTS = ITEMS.register("dragon_boots", () -> new DragonArmorItem(DRAGON_ARMOR_MATERIAL, ArmorItem.Type.BOOTS, armorProperties(ArmorItem.Type.BOOTS)));
    public static final DeferredHolder<Item, Item> UPGRADED_DRAGON_CHESTPLATE = ITEMS.register("upgraded_dragon_chestplate", () -> new DragonWingedArmorItem(DRAGON_ARMOR_MATERIAL, ArmorItem.Type.CHESTPLATE, armorProperties(ArmorItem.Type.CHESTPLATE).fireResistant()));

    public static final DeferredHolder<Item, Item> DRAGON_PICKAXE_ITEM = ITEMS.register("dragon_pickaxe", () -> {
        Tier tier = DragonToolMaterial.getInstance();
        return new DragonPickaxeItem(tier, toolProperties().attributes(PickaxeItem.createAttributes(tier, 1.0F, -2.8F)));
    });
    public static final DeferredHolder<Item, Item> DRAGON_AXE_ITEM = ITEMS.register("dragon_axe", () -> {
        Tier tier = DragonToolMaterial.getInstance();
        return new DragonAxeItem(tier, toolProperties());
    });
    public static final DeferredHolder<Item, Item> DRAGON_SHOVEL_ITEM = ITEMS.register("dragon_shovel", () -> {
        Tier tier = DragonToolMaterial.getInstance();
        return new DragonShovelItem(tier, toolProperties().attributes(ShovelItem.createAttributes(tier, 1.5F, -3.0F)));
    });
    public static final DeferredHolder<Item, Item> DRAGON_HOE_ITEM = ITEMS.register("dragon_hoe", () -> {
        Tier tier = DragonToolMaterial.getInstance();
        return new DragonHoeItem(tier, toolProperties().attributes(HoeItem.createAttributes(tier, -4.0F, 0.0F)));
    });

    public static final DeferredHolder<Item, Item> DRAGON_SWORD_ITEM = ITEMS.register("dragon_sword", () -> {
        Tier tier = DragonToolMaterial.getInstance();
        return new DragonSwordItem(tier, toolProperties());
    });
    public static final DeferredHolder<Item, Item> DRAGON_BOW_ITEM = ITEMS.register("dragon_bow", () -> new DragonBowItem(new Item.Properties().fireResistant().durability(DragonToolMaterial.getInstance().getUses())));
    public static final DeferredHolder<Item, Item> DRAGON_CROSSBOW_ITEM = ITEMS.register("dragon_crossbow", () -> new DragonCrossbowItem(new Item.Properties()
        .fireResistant()
        .durability(DragonToolMaterial.getInstance().getUses())
        .component(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.EMPTY)));
    public static final DeferredHolder<Item, Item> DRAGON_TRIDENT_ITEM = ITEMS.register("dragon_trident", () -> new DragonTridentItem(new Item.Properties()
        .fireResistant()
        .durability(DragonToolMaterial.getInstance().getUses())
        .component(DataComponents.TOOL, TridentItem.createToolProperties())));

    public static final DeferredHolder<Item, Item> DRAGON_ANVIL_ITEM = ITEMS.register("dragon_anvil", () -> new BlockItem(BlockInit.DRAGON_ANVIL_BLOCK.get(), new Item.Properties()));

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
        })
        .build());

    private static Item.Properties armorProperties(ArmorItem.Type type) {
        return new Item.Properties().fireResistant().durability(type.getDurability(effectiveDragonArmorDurabilityMultiplier()));
    }

    private static Item.Properties toolProperties() {
        return new Item.Properties().fireResistant();
    }

    private static int effectiveDragonArmorDurabilityMultiplier() {
        int multiplier = ConfigInit.CONFIG.dragon_armor_durability_multiplier;
        if (ConfigInit.CONFIG.advanced_netherite_gear_perks_enabled) {
            multiplier = Math.max(multiplier, ADVANCED_NETHERITE_COMPAT_MIN_ARMOR_DURABILITY_MULTIPLIER);
        }
        return multiplier;
    }
}
