package net.dragonloot.gametest;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.util.List;
import net.dragonloot.DragonLootMain;
import net.dragonloot.init.ItemInit;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.fml.ModList;

/** Uses the server's decoded recipes, including the currently enabled world datapacks. */
public final class DragonRecipeGameTests {

    private static MinecraftServer preparedServer;
    private static final List<String> PROGRESSION = List.of(
        "axe", "boots", "chestplate", "helmet", "hoe", "leggings", "pickaxe", "shovel", "sword"
    );
    private static final List<Upgrade> OTHER_UPGRADES = List.of(
        new Upgrade("dragon_anvil", "minecraft:anvil", "dragonloot:dragon_scale"),
        new Upgrade("dragon_horse_armor", "minecraft:diamond_horse_armor", "dragonloot:dragon_scale"),
        new Upgrade("dragon_trident", "minecraft:trident", "dragonloot:dragon_scale"),
        new Upgrade("dragon_bow", "minecraft:bow", "dragonloot:dragon_scale"),
        new Upgrade("dragon_crossbow", "minecraft:crossbow", "dragonloot:dragon_scale"),
        new Upgrade("upgraded_dragon_chestplate", "dragonloot:dragon_chestplate", "minecraft:elytra")
    );

    private DragonRecipeGameTests() {
    }

    @GameTest
    public static void nativeEnchantmentTagsSupportDragonEquipment(GameTestHelper helper) {
        var enchantments = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        List<Item> armor = List.of(ItemInit.DRAGON_HELMET.get(), ItemInit.DRAGON_CHESTPLATE.get(),
            ItemInit.DRAGON_LEGGINGS.get(), ItemInit.DRAGON_BOOTS.get(), ItemInit.UPGRADED_DRAGON_CHESTPLATE.get());
        List<Item> tools = List.of(ItemInit.DRAGON_PICKAXE_ITEM.get(), ItemInit.DRAGON_AXE_ITEM.get(),
            ItemInit.DRAGON_SHOVEL_ITEM.get(), ItemInit.DRAGON_HOE_ITEM.get());
        for (Item item : armor) {
            ItemStack stack = new ItemStack(item);
            helper.assertTrue(stack.is(ItemTags.ARMOR_ENCHANTABLE)
                && stack.supportsEnchantment(enchantments.getOrThrow(Enchantments.PROTECTION)),
                "Dragon armor must support native Protection");
        }
        for (Item item : tools) {
            helper.assertTrue(new ItemStack(item).supportsEnchantment(enchantments.getOrThrow(Enchantments.EFFICIENCY)),
                "Dragon tools must support native Efficiency");
        }
        helper.assertTrue(new ItemStack(ItemInit.DRAGON_SWORD_ITEM.get())
            .supportsEnchantment(enchantments.getOrThrow(Enchantments.SHARPNESS)), "Dragon sword must support native Sharpness");
        helper.assertTrue(new ItemStack(ItemInit.DRAGON_BOW_ITEM.get())
            .supportsEnchantment(enchantments.getOrThrow(Enchantments.POWER)), "Dragon bow must support native Power");
        helper.assertTrue(new ItemStack(ItemInit.DRAGON_CROSSBOW_ITEM.get())
            .supportsEnchantment(enchantments.getOrThrow(Enchantments.QUICK_CHARGE)), "Dragon crossbow must support native Quick Charge");
        helper.assertTrue(new ItemStack(ItemInit.DRAGON_TRIDENT_ITEM.get())
            .supportsEnchantment(enchantments.getOrThrow(Enchantments.LOYALTY)), "Dragon trident must support native Loyalty");
        var equipment = new java.util.ArrayList<Item>(armor);
        equipment.addAll(tools);
        equipment.addAll(List.of(ItemInit.DRAGON_SWORD_ITEM.get(), ItemInit.DRAGON_BOW_ITEM.get(),
            ItemInit.DRAGON_CROSSBOW_ITEM.get(), ItemInit.DRAGON_TRIDENT_ITEM.get()));
        for (Item item : equipment) {
            helper.assertTrue(new ItemStack(item).supportsEnchantment(enchantments.getOrThrow(Enchantments.UNBREAKING))
                && new ItemStack(item).supportsEnchantment(enchantments.getOrThrow(Enchantments.MENDING)),
                "Every durable Dragon player item must support native Unbreaking and Mending");
        }
        helper.assertTrue(!new ItemStack(ItemInit.DRAGON_SCALE_ITEM.get()).is(ItemTags.DURABILITY_ENCHANTABLE),
            "Dragon scales must not be classified as durable equipment");
        helper.succeed();
    }

    @GameTest
    public static void nativeSmithingRecipesUseSelectedProgression(GameTestHelper helper) {
        prepareRecipeProfile(helper);
        for (Upgrade upgrade : upgrades(helper)) {
            SmithingRecipe recipe = recipe(helper, upgrade);
            ItemStack base = stack(helper, upgrade.base());
            ItemStack addition = stack(helper, upgrade.addition());
            SmithingRecipeInput input = input(base, addition);
            helper.assertTrue(recipe.matches(input, helper.getLevel()), upgrade.id() + " rejected its native ingredients");
            ItemStack result = recipe.assemble(input);
            helper.assertTrue(result.is(item(helper, "dragonloot:" + upgrade.id())) && result.getCount() == 1,
                upgrade.id() + " must assemble exactly one registered Dragon item");
            helper.assertTrue(base.getCount() == 1 && addition.getCount() == 1 && input.template().getCount() == 1,
                upgrade.id() + " assemble must leave input consumption to the native smithing menu");
            helper.assertTrue(!recipe.matches(input(new ItemStack(Items.STONE), addition), helper.getLevel()),
                upgrade.id() + " accepted the wrong base");
            helper.assertTrue(!recipe.matches(input(base, new ItemStack(Items.DIRT)), helper.getLevel()),
                upgrade.id() + " accepted the wrong addition");
            helper.assertTrue(!recipe.matches(new SmithingRecipeInput(new ItemStack(Items.PAPER), base, addition), helper.getLevel()),
                upgrade.id() + " accepted the wrong template");
            helper.assertTrue(!recipe.matches(new SmithingRecipeInput(ItemStack.EMPTY, base, addition), helper.getLevel()),
                upgrade.id() + " accepted a missing template");

            if (advancedProfile(helper) && PROGRESSION.contains(upgrade.id().substring("dragon_".length()))) {
                String type = upgrade.id().substring("dragon_".length());
                helper.assertTrue(!recipe.matches(input(stack(helper, "minecraft:netherite_" + type), addition), helper.getLevel()),
                    upgrade.id() + " ignored the datapack's Advanced Netherite base override");
            }
        }
        helper.succeed();
    }

    @GameTest
    public static void nativeSmithingPreservesComponentsAndDragonDefaults(GameTestHelper helper) {
        prepareRecipeProfile(helper);
        var unbreaking = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.UNBREAKING);
        for (Upgrade upgrade : upgrades(helper)) {
            ItemStack base = stack(helper, upgrade.base());
            Component customName = Component.literal("Dragon upgrade: " + upgrade.id());
            base.set(DataComponents.CUSTOM_NAME, customName);
            CompoundTag savedData = new CompoundTag();
            savedData.putString("dragonloot_recipe_test", upgrade.id());
            base.set(DataComponents.CUSTOM_DATA, CustomData.of(savedData));
            ItemEnchantments.Mutable enchantments = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
            enchantments.set(unbreaking, 3);
            base.set(DataComponents.ENCHANTMENTS, enchantments.toImmutable());
            if (base.isDamageableItem()) {
                base.setDamageValue(23);
            }
            ItemStack original = base.copy();
            ItemStack result = recipe(helper, upgrade).assemble(input(base, stack(helper, upgrade.addition())));
            ItemStack dragonDefaults = stack(helper, "dragonloot:" + upgrade.id());
            helper.assertTrue(result.is(dragonDefaults.getItem()) && result.getCount() == 1,
                upgrade.id() + " returned an empty or unexpected item when upgrading customized equipment");
            helper.assertTrue(customName.equals(result.get(DataComponents.CUSTOM_NAME)),
                upgrade.id() + " lost the custom name");
            helper.assertTrue(base.get(DataComponents.CUSTOM_DATA).equals(result.get(DataComponents.CUSTOM_DATA)),
                upgrade.id() + " lost saved custom data");
            helper.assertTrue(base.get(DataComponents.ENCHANTMENTS).equals(result.get(DataComponents.ENCHANTMENTS)),
                upgrade.id() + " lost enchantments");
            helper.assertTrue(result.getMaxDamage() == dragonDefaults.getMaxDamage(),
                upgrade.id() + " inherited base durability instead of Dragon equipment defaults");
            helper.assertTrue(result.getDamageValue() == original.getDamageValue(),
                upgrade.id() + " changed existing durability damage");
            helper.assertTrue(ItemStack.isSameItemSameComponents(base, original) && base.getCount() == original.getCount(),
                upgrade.id() + " mutated the base item during native assembly");
        }
        helper.succeed();
    }

    private static List<Upgrade> upgrades(GameTestHelper helper) {
        String basePrefix = advancedProfile(helper) ? "advancednetherite:netherite_diamond_" : "minecraft:netherite_";
        var upgrades = new java.util.ArrayList<Upgrade>();
        for (String type : PROGRESSION) {
            upgrades.add(new Upgrade("dragon_" + type, basePrefix + type, "dragonloot:dragon_scale"));
        }
        upgrades.addAll(OTHER_UPGRADES);
        return upgrades;
    }

    private static void prepareRecipeProfile(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        if (preparedServer == server) return;
        logPackProof(server, "initial-native-selection");
        if (advancedProfile(helper)) {
            String fileId = System.getProperty("dragonloot.test.recipePackId", "file/DLxAN_26.1.2-1.zip");
            Pack pack = server.getPackRepository().getPack(fileId);
            helper.assertTrue(pack != null, "Missing real Advanced Netherite recipe datapack " + fileId);
            helper.assertTrue(pack.getCompatibility().isCompatible(), "Recipe datapack is not natively compatible");
            var selected = new java.util.ArrayList<>(server.getPackRepository().getSelectedPacks().stream().map(Pack::getId).toList());
            helper.assertTrue(selected.remove(fileId), "Real recipe datapack must already be enabled");
            selected.add(fileId);
            // Native GameTestServer starts with discovery order, not user-selected
            // world priority. Use the real reload API with this pack last; no recipe
            // map is modified by the fixture or by the production mod.
            var reload = server.reloadResources(selected);
            server.managedBlock(reload::isDone);
            reload.join();
            logPackProof(server, "native-reload-pack-last");
            var winner = server.getResourceManager().getResource(DragonLootMain.id("recipe/dragon_sword.json")).orElseThrow();
            helper.assertTrue(winner.sourcePackId().equals(fileId),
                "Enabled recipe datapack must actually supply the highest-priority sword recipe");
        }
        preparedServer = server;
    }

    private static void logPackProof(MinecraftServer server, String stage) {
        JsonObject proof = new JsonObject();
        proof.addProperty("stage", stage);
        JsonArray selected = new JsonArray();
        server.getPackRepository().getSelectedPacks().forEach(pack -> selected.add(pack.getId()));
        proof.add("selectedLowToHigh", selected);
        JsonArray available = new JsonArray();
        server.getPackRepository().getAvailablePacks().forEach(pack -> {
            JsonObject row = new JsonObject();
            row.addProperty("id", pack.getId());
            row.addProperty("compatibility", pack.getCompatibility().name());
            row.addProperty("required", pack.isRequired());
            available.add(row);
        });
        proof.add("available", available);
        Identifier recipeId = DragonLootMain.id("recipe/dragon_sword.json");
        JsonArray resources = new JsonArray();
        for (var resource : server.getResourceManager().getResourceStack(recipeId)) {
            JsonObject row = new JsonObject();
            row.addProperty("sourcePackId", resource.sourcePackId());
            try (var reader = resource.openAsReader()) {
                row.add("contents", JsonParser.parseReader(reader));
            } catch (IOException exception) {
                throw new IllegalStateException("Cannot read live recipe resource provenance", exception);
            }
            resources.add(row);
        }
        proof.add("swordResourceStackLowToHigh", resources);
        server.getResourceManager().getResource(recipeId).ifPresent(resource -> proof.addProperty("winningSwordSource", resource.sourcePackId()));
        System.out.println("DRAGONLOOT_RECIPE_PACK_PROOF " + proof);
    }

    private static boolean advancedProfile(GameTestHelper helper) {
        String profile = System.getProperty("dragonloot.test.recipeProfile", "vanilla");
        helper.assertTrue(profile.equals("vanilla") || profile.equals("advancednetherite"),
            "Unknown recipe profile: " + profile);
        if (profile.equals("advancednetherite")) {
            helper.assertTrue(ModList.get().isLoaded("advancednetherite"),
                "Advanced Netherite recipe profile requires its real mod");
            return true;
        }
        return false;
    }

    private static SmithingRecipe recipe(GameTestHelper helper, Upgrade upgrade) {
        var key = ResourceKey.<Recipe<?>>create(Registries.RECIPE, DragonLootMain.id(upgrade.id()));
        Recipe<?> recipe = helper.getLevel().getServer().getRecipeManager().byKey(key).orElseThrow().value();
        helper.assertTrue(recipe instanceof SmithingRecipe, upgrade.id() + " is not a native smithing recipe");
        return (SmithingRecipe) recipe;
    }

    private static Item item(GameTestHelper helper, String id) {
        Item item = BuiltInRegistries.ITEM.getValue(Identifier.parse(id));
        helper.assertTrue(item != null && item != Items.AIR, "Missing required registered recipe item " + id);
        return item;
    }

    private static ItemStack stack(GameTestHelper helper, String id) {
        return new ItemStack(item(helper, id));
    }

    private static SmithingRecipeInput input(ItemStack base, ItemStack addition) {
        return new SmithingRecipeInput(new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE), base, addition);
    }

    private record Upgrade(String id, String base, String addition) {
    }
}
