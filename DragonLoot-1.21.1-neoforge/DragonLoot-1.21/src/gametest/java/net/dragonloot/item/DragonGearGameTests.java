package net.dragonloot.item;

import com.mojang.authlib.GameProfile;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.UUID;
import net.dragonloot.DragonLootMain;
import net.dragonloot.init.ConfigInit;
import net.dragonloot.init.ItemInit;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Native server behavior; gear-expectations.properties optionally supplies custom startup expectations. */
@GameTestHolder(DragonLootMain.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DragonGearGameTests {
    private DragonGearGameTests() {}

    @GameTest(template = "empty")
    public static void registeredGearUsesStartupConfiguration(GameTestHelper helper) {
        Properties expected = expectations();
        ItemStack pickaxe = new ItemStack(ItemInit.DRAGON_PICKAXE_ITEM.get());
        close(helper, pickaxe.getDestroySpeed(Blocks.STONE.defaultBlockState()), expected(expected, "speed", 12), "registered mining speed");
        close(helper, pickaxe.getMaxDamage(), expected(expected, "toolDurability", 2479), "registered tool durability");
        ItemStack sword = new ItemStack(ItemInit.DRAGON_SWORD_ITEM.get());
        close(helper, attribute(sword, EquipmentSlot.MAINHAND, Attributes.ATTACK_DAMAGE), expected(expected, "swordModifier", 8), "registered sword attack modifier");
        ItemStack chest = new ItemStack(ItemInit.DRAGON_CHESTPLATE.get());
        close(helper, chest.getMaxDamage(), expected(expected, "chestDurability", 592), "registered chest durability");
        close(helper, attribute(chest, EquipmentSlot.CHEST, Attributes.ARMOR), expected(expected, "chestArmor", 10), "registered chest protection");
        close(helper, attribute(chest, EquipmentSlot.CHEST, Attributes.ARMOR_TOUGHNESS), expected(expected, "toughness", 3), "registered chest toughness");
        close(helper, ((ArmorItem) ItemInit.DRAGON_HORSE_ARMOR_ITEM.get()).getDefense(), expected(expected, "horseArmor", 18), "registered horse protection");
        close(helper, ItemInit.DRAGON_PICKAXE_ITEM.get().getEnchantmentValue(pickaxe), expected(expected, "toolEnchantability", 20), "registered tool enchantability");
        close(helper, ConfigInit.CONFIG.scale_minimum_drop_amount, expected(expected, "scales", 3), "startup drop setting");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void wingedArmorUsesNativeFlightGates(GameTestHelper helper) {
        GearPlayer player = player(helper);
        ItemStack wings = new ItemStack(ItemInit.UPGRADED_DRAGON_CHESTPLATE.get());
        player.setItemSlot(EquipmentSlot.CHEST, wings);
        player.tick(); // Includes PlayerTickEvent: Caelus populates its flight attribute here.
        player.setOnGround(false);
        check(helper, player.tryToStartFallFlying() && player.isFallFlying(), "native airborne player must start flight");
        player.stopFallFlying();
        player.setOnGround(true);
        check(helper, !player.tryToStartFallFlying(), "grounded player must not start flight");
        player.setOnGround(false);
        player.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 100));
        check(helper, !player.tryToStartFallFlying(), "levitation must block native flight");
        player.removeEffect(MobEffects.LEVITATION);
        wings.setDamageValue(wings.getMaxDamage() - 1);
        player.tick();
        player.setOnGround(false);
        check(helper, !player.tryToStartFallFlying(), "almost-broken winged armor must not fly");
        wings.setDamageValue(0);
        player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ItemInit.DRAGON_CHESTPLATE.get()));
        player.tick();
        player.setOnGround(false);
        check(helper, !player.tryToStartFallFlying(), "ordinary Dragon armor must not grant flight");
        player.setItemSlot(EquipmentSlot.CHEST, wings);
        player.tick();
        BlockPos center = helper.absolutePos(new BlockPos(2, 3, 2));
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                helper.getLevel().setBlockAndUpdate(center.offset(x, 0, z), Blocks.WATER.defaultBlockState());
                helper.getLevel().setBlockAndUpdate(center.offset(x, 1, z), Blocks.WATER.defaultBlockState());
            }
        }
        try {
            player.setPos(Vec3.atBottomCenterOf(center));
            check(helper, player.refreshFluidState() && player.isInWater(), "real water must update player fluid state");
            player.setOnGround(false);
            check(helper, !player.tryToStartFallFlying(), "water must block native flight");
        } finally {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    helper.getLevel().setBlockAndUpdate(center.offset(x, 0, z), Blocks.AIR.defaultBlockState());
                    helper.getLevel().setBlockAndUpdate(center.offset(x, 1, z), Blocks.AIR.defaultBlockState());
                }
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void nativeFlightPreservesNoPassiveArmorWear(GameTestHelper helper) {
        GearPlayer player = player(helper);
        BlockPos column = helper.absolutePos(new BlockPos(2, 0, 2));
        // GameTest origins can lie below surrounding terrain; choose real air above its surface.
        int flightHeight = Math.min(helper.getLevel().getMaxBuildHeight() - 16,
            helper.getLevel().getHeight(Heightmap.Types.MOTION_BLOCKING, column.getX(), column.getZ()) + 80);
        player.setPos(column.getX() + 0.5, flightHeight, column.getZ() + 0.5);
        check(helper, helper.getLevel().noCollision(player), "flight fixture must start in unobstructed air");
        ItemStack wings = new ItemStack(ItemInit.UPGRADED_DRAGON_CHESTPLATE.get());
        player.setItemSlot(EquipmentSlot.CHEST, wings);
        player.tick();
        player.setOnGround(false);
        check(helper, player.tryToStartFallFlying(), "native flight must start for durability test");
        for (int tick = 0; tick < 60; tick++) {
            player.tick();
            check(helper, player.isFallFlying(), "native flight must stay active at real player tick " + tick
                + " (age=" + player.getFallFlyingTicks() + ", position=" + player.position() + ")");
        }
        check(helper, player.getFallFlyingTicks() >= 40, "real player ticks must advance native fall-flight state");
        System.out.println("DRAGONLOOT_NATIVE_FLIGHT age=" + player.getFallFlyingTicks()
            + " position=" + player.position() + " damage=" + wings.getDamageValue());
        check(helper, player.isFallFlying(), "native flight must stay active through sixty real player ticks");
        check(helper, wings.getDamageValue() == 0, "native flight updates must preserve no passive armor wear");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void configReloadRefreshesDropsButKeepsGearStable(GameTestHelper helper) {
        ModConfigSpec.DoubleValue speed = ConfigInit.COMMON_SPEC.getValues().get("dragonloot.tools.dragon_tool_mining_speed");
        ModConfigSpec.IntValue scales = ConfigInit.COMMON_SPEC.getValues().get("dragonloot.scale_minimum_drop_amount");
        double previousSpeed = speed.get();
        int previousScales = scales.get();
        float registeredSpeed = new ItemStack(ItemInit.DRAGON_PICKAXE_ITEM.get()).getDestroySpeed(Blocks.STONE.defaultBlockState());
        try {
            speed.set(previousSpeed + 11);
            scales.set(previousScales + 2);
            ConfigInit.COMMON_SPEC.afterReload();
            ConfigInit.CONFIG.bake();
            close(helper, new ItemStack(ItemInit.DRAGON_PICKAXE_ITEM.get()).getDestroySpeed(Blocks.STONE.defaultBlockState()), registeredSpeed, "reload must preserve registered gear speed");
            close(helper, ConfigInit.CONFIG.dragon_tool_mining_speed, registeredSpeed, "reload must preserve material getter snapshot");
            check(helper, ConfigInit.CONFIG.scale_minimum_drop_amount == previousScales + 2, "runtime drop setting must refresh");
        } finally {
            speed.set(previousSpeed);
            scales.set(previousScales);
            ConfigInit.COMMON_SPEC.afterReload();
            ConfigInit.CONFIG.bake();
        }
        helper.succeed();
    }

    private static Properties expectations() {
        Properties properties = new Properties();
        Path path = Path.of("gear-expectations.properties");
        if (Files.isRegularFile(path)) {
            try (InputStream stream = Files.newInputStream(path)) {
                properties.load(stream);
            } catch (Exception error) {
                throw new IllegalStateException("Cannot read custom gear expectations", error);
            }
        }
        return properties;
    }

    private static double expected(Properties expected, String key, double fallback) {
        return Double.parseDouble(expected.getProperty(key, Double.toString(fallback)));
    }

    private static double attribute(ItemStack stack, EquipmentSlot slot, Holder<Attribute> attribute) {
        double[] amount = { 0 };
        stack.forEachModifier(slot, (type, modifier) -> {
            if (type.equals(attribute)) amount[0] += modifier.amount();
        });
        return amount[0];
    }

    private static GearPlayer player(GameTestHelper helper) {
        GearPlayer player = new GearPlayer(helper.getLevel());
        player.setPos(Vec3.atCenterOf(helper.absolutePos(new BlockPos(2, 3, 2))));
        return player;
    }

    private static void close(GameTestHelper helper, double actual, double expected, String message) {
        check(helper, Math.abs(actual - expected) < 0.0001, message + ": expected " + expected + ", got " + actual);
    }

    private static void check(GameTestHelper helper, boolean condition, String message) {
        if (!condition) helper.fail(message);
    }

    private static final class GearPlayer extends Player {
        GearPlayer(Level level) { super(level, BlockPos.ZERO, 0, new GameProfile(UUID.randomUUID(), "gear-test")); }
        @Override public boolean isSpectator() { return false; }
        @Override public boolean isCreative() { return false; }
        boolean refreshFluidState() { return this.updateInWaterStateAndDoFluidPushing(); }
    }
}
