package net.dragonloot.gametest;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import net.dragonloot.init.ConfigInit;
import net.dragonloot.init.ItemInit;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.fml.ModList;

/** Optional runtime checks use public APIs reflectively, keeping production dependencies optional. */
public final class DragonCompatibilityGameTests {

    private static final Gson ATTRIBUTE_JSON = new GsonBuilder().serializeNulls().create();

    private static final Map<String, String> EXPECTED_MODS = Map.of(
        "advancednetherite", "2.4.1",
        "bettercombat", "3.2.2",
        "player_animation_library", "1.2.8+mc.26.1",
        "cloth_config", "26.1.154"
    );

    private DragonCompatibilityGameTests() {
    }

    @GameTest
    public static void optionalBetterCombatResolvesNativeWeaponPresets(GameTestHelper helper) throws ReflectiveOperationException {
        if (!integrationProfile(helper)) {
            // Baseline asserts that these integrations are absent; it makes no
            // functional Better Combat coverage claim.
            helper.succeed();
            return;
        }
        Class<?> registry = Class.forName("net.bettercombat.logic.WeaponRegistry");
        Method getAttributes = registry.getMethod("getAttributes", ItemStack.class);
        for (Weapon weapon : List.of(
            new Weapon(new ItemStack(ItemInit.DRAGON_SWORD_ITEM.get()), new ItemStack(Items.NETHERITE_SWORD), "sword", 3),
            new Weapon(new ItemStack(ItemInit.DRAGON_AXE_ITEM.get()), new ItemStack(Items.NETHERITE_AXE), "axe", 2),
            new Weapon(new ItemStack(ItemInit.DRAGON_TRIDENT_ITEM.get()), new ItemStack(Items.TRIDENT), "trident", 1)
        )) {
            Object dragonAttributes = getAttributes.invoke(null, weapon.dragon());
            Object vanillaAttributes = getAttributes.invoke(null, weapon.vanilla());
            helper.assertTrue(dragonAttributes != null && vanillaAttributes != null,
                "Better Combat's actual loaded registry must resolve " + weapon.category());
            Class<?> attributes = dragonAttributes.getClass();
            helper.assertTrue(weapon.category().equals(attributes.getMethod("category").invoke(dragonAttributes)),
                "Dragon weapon resolved the wrong Better Combat category");
            Object attacks = attributes.getMethod("attacks").invoke(dragonAttributes);
            helper.assertTrue(attacks != null && Array.getLength(attacks) == weapon.attackCount(),
                "Dragon " + weapon.category() + " must inherit the native preset's attack sequence");
            // Better Combat's WeaponAttributes.equals compares its attacks array
            // by reference and omits several fields. Its registry sync serializes
            // the complete attributes with Gson; compare that complete structure,
            // including ordered attacks, conditions, sounds and trail particles.
            JsonElement dragonJson = ATTRIBUTE_JSON.toJsonTree(dragonAttributes);
            JsonElement vanillaJson = ATTRIBUTE_JSON.toJsonTree(vanillaAttributes);
            JsonObject proof = new JsonObject();
            proof.addProperty("category", weapon.category());
            proof.add("dragon", dragonJson);
            proof.add("vanilla", vanillaJson);
            proof.addProperty("structurallyEqual", dragonJson.equals(vanillaJson));
            System.out.println("DRAGONLOOT_BETTER_COMBAT_PRESET_PROOF " + proof);
            helper.assertTrue(dragonJson.equals(vanillaJson),
                "Dragon " + weapon.category() + " must inherit the complete native Better Combat preset");
            for (int index = 0; index < Array.getLength(attacks); index++) {
                Object attack = Array.get(attacks, index);
                Object animation = attack.getClass().getMethod("animation").invoke(attack);
                helper.assertTrue(animation instanceof String name && !name.isBlank(),
                    "Inherited Better Combat attacks must include an actual animation");
            }
        }
        helper.succeed();
    }

    @GameTest
    public static void optionalAdvancedNetheriteRetainsExplicitDragonGearSettings(GameTestHelper helper) {
        integrationProfile(helper);
        String expectedFlag = System.getProperty("dragonloot.test.gearPerksExpected", "false");
        helper.assertTrue(expectedFlag.equals("true") || expectedFlag.equals("false"),
            "Gear perk expectation must be an explicit boolean");
        boolean enabled = Boolean.parseBoolean(expectedFlag);
        helper.assertTrue(ConfigInit.CONFIG.advanced_netherite_gear_perks_enabled == enabled,
            "Loading Advanced Netherite must not implicitly enable Dragon-owned gear perks");
        Properties expected = expectations();
        ItemStack pickaxe = new ItemStack(ItemInit.DRAGON_PICKAXE_ITEM.get());
        ItemStack chest = new ItemStack(ItemInit.DRAGON_CHESTPLATE.get());
        close(helper, pickaxe.getMaxDamage(), expected(expected, "toolDurability", enabled ? 3149 : 2479),
            "Explicit startup Dragon tool durability");
        close(helper, chest.getMaxDamage(), expected(expected, "chestDurability", enabled ? 752 : 592),
            "Explicit startup Dragon armor durability");
        close(helper, amount(new ItemStack(ItemInit.DRAGON_SWORD_ITEM.get()), EquipmentSlot.MAINHAND, Attributes.ATTACK_DAMAGE),
            expected(expected, "swordModifier", enabled ? 11 : 8), "Explicit startup Dragon sword damage");
        close(helper, amount(new ItemStack(ItemInit.DRAGON_AXE_ITEM.get()), EquipmentSlot.MAINHAND, Attributes.ATTACK_DAMAGE),
            expected(expected, "axeModifier", enabled ? 13 : 10), "Explicit startup Dragon axe damage");
        close(helper, pickaxe.getDestroySpeed(net.minecraft.world.level.block.Blocks.STONE.defaultBlockState()),
            expected(expected, "speed", 12), "Dragon mining speed remains configured independently");
        close(helper, amount(chest, EquipmentSlot.CHEST, Attributes.ARMOR_TOUGHNESS),
            expected(expected, "toughness", 3), "Dragon toughness remains configured independently");
        helper.succeed();
    }

    private static boolean integrationProfile(GameTestHelper helper) {
        String profile = System.getProperty("dragonloot.test.compatProfile", "none");
        helper.assertTrue(profile.equals("none") || profile.equals("all") || profile.equals("advancednetherite"),
            "Unknown compatibility profile " + profile);
        for (var mod : EXPECTED_MODS.entrySet()) {
            var container = ModList.get().getModContainerById(mod.getKey());
            boolean expectedLoaded = profile.equals("all") || (profile.equals("advancednetherite") && mod.getKey().equals("advancednetherite"));
            if (!expectedLoaded) {
                helper.assertTrue(container.isEmpty(), "Clean baseline unexpectedly loaded " + mod.getKey());
            } else {
                helper.assertTrue(container.isPresent(), "Missing required integration fixture mod " + mod.getKey());
                String actual = container.orElseThrow().getModInfo().getVersion().toString();
                helper.assertTrue(actual.equals(mod.getValue()), "Unexpected integration fixture version: "
                    + mod.getKey() + " expected " + mod.getValue() + ", loaded " + actual);
            }
        }
        return profile.equals("all");
    }

    private static Properties expectations() {
        Properties expected = new Properties();
        Path file = Path.of("gear-expectations.properties");
        if (Files.isRegularFile(file)) {
            try (var reader = Files.newBufferedReader(file)) {
                expected.load(reader);
            } catch (IOException exception) {
                throw new IllegalStateException("Cannot read independent gear expectations", exception);
            }
        }
        return expected;
    }

    private static double expected(Properties expected, String key, double fallback) {
        return Double.parseDouble(expected.getProperty(key, Double.toString(fallback)));
    }

    private static double amount(ItemStack stack, EquipmentSlot slot, Holder<Attribute> type) {
        double[] amount = {0};
        stack.forEachModifier(slot, (attribute, modifier) -> {
            if (attribute.equals(type)) amount[0] += modifier.amount();
        });
        return amount[0];
    }

    private static void close(GameTestHelper helper, double actual, double expected, String name) {
        helper.assertTrue(Math.abs(actual - expected) < 0.0001,
            name + ": expected " + expected + ", got " + actual);
    }

    private record Weapon(ItemStack dragon, ItemStack vanilla, String category, int attackCount) {
    }
}
