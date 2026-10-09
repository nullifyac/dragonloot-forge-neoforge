package net.dragonloot.gametest;

import com.mojang.authlib.GameProfile;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.UUID;
import java.util.HashMap;
import net.dragonloot.init.ConfigInit;
import net.dragonloot.init.ItemInit;
import net.minecraft.core.BlockPos;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.dragonloot.init.BlockInit;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.ModConfigSpec;



/** Native server behavior; gear-expectations.properties supplies independent custom-startup expectations. */
public final class DragonGearGameTests {
    private DragonGearGameTests() {}

    @GameTest(template = "empty")
    public static void nativeInventoryAcceptsArmorAndShiftEquipsWings(GameTestHelper helper) {
        GearPlayer player = player(helper);
        ItemStack wings = new ItemStack(ItemInit.UPGRADED_DRAGON_CHESTPLATE.get());
        player.getInventory().setItem(9, wings);
        check(helper, player.inventoryMenu.getSlot(6).mayPlace(wings), "native chest slot must accept winged armor");
        check(helper, !player.inventoryMenu.getSlot(5).mayPlace(wings), "native helmet slot must reject winged chest armor");
        player.inventoryMenu.clicked(9, 0, ContainerInput.QUICK_MOVE, player);
        check(helper, player.getInventory().getItem(9).isEmpty(), "shift-equip must consume the original inventory slot");
        check(helper, player.getItemBySlot(EquipmentSlot.CHEST).is(ItemInit.UPGRADED_DRAGON_CHESTPLATE.get()), "native shift-equip must select the chest slot");
        player.inventoryMenu.clicked(6, 0, ContainerInput.PICKUP, player);
        check(helper, player.getItemBySlot(EquipmentSlot.CHEST).isEmpty() && player.inventoryMenu.getCarried().is(ItemInit.UPGRADED_DRAGON_CHESTPLATE.get()), "native click must unequip the chestplate without loss");
        player.discard();
        helper.succeed();
    }

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
        close(helper, attribute(new ItemStack(ItemInit.DRAGON_HORSE_ARMOR_ITEM.get()), EquipmentSlot.BODY, Attributes.ARMOR), expected(expected, "horseArmor", 18), "registered horse protection");
        close(helper, pickaxe.get(DataComponents.ENCHANTABLE).value(), expected(expected, "toolEnchantability", 20), "registered tool enchantability");
        close(helper, ConfigInit.CONFIG.scale_minimum_drop_amount, expected(expected, "scales", 3), "startup drop setting");
        ItemStack scale = new ItemStack(ItemInit.DRAGON_SCALE_ITEM.get());
        check(helper, pickaxe.isValidRepairItem(scale) && chest.isValidRepairItem(scale), "native repair components must resolve Dragon scales");
        check(helper, !pickaxe.isValidRepairItem(new ItemStack(Items.DIAMOND)), "native Dragon repair tag must reject unrelated materials");
        close(helper, new ItemStack(ItemInit.DRAGON_AXE_ITEM.get()).getDestroySpeed(Blocks.OAK_LOG.defaultBlockState()), expected(expected, "speed", 12), "native axe mining speed");
        close(helper, new ItemStack(ItemInit.DRAGON_SHOVEL_ITEM.get()).getDestroySpeed(Blocks.DIRT.defaultBlockState()), expected(expected, "speed", 12), "native shovel mining speed");
        close(helper, new ItemStack(ItemInit.DRAGON_HOE_ITEM.get()).getDestroySpeed(Blocks.OAK_LEAVES.defaultBlockState()), expected(expected, "speed", 12), "native hoe mining speed");
        check(helper, new ItemStack(ItemInit.UPGRADED_DRAGON_CHESTPLATE.get()).has(DataComponents.GLIDER)
            && !chest.has(DataComponents.GLIDER), "only upgraded armor must expose the native glider component");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void wingedArmorUsesNativeFlightGates(GameTestHelper helper) {
        GearPlayer player = player(helper);
        ItemStack wings = new ItemStack(ItemInit.UPGRADED_DRAGON_CHESTPLATE.get());
        BlockPos column = helper.absolutePos(new BlockPos(2, 0, 2));
        int height = Math.min(helper.getLevel().getMaxY() - 16,
            helper.getLevel().getHeight(Heightmap.Types.MOTION_BLOCKING, column.getX(), column.getZ()) + 40);
        BlockPos center = new BlockPos(column.getX(), height, column.getZ());
        HashMap<BlockPos, BlockState> previousBlocks = new HashMap<>();
        try {
            player.setPos(Vec3.atBottomCenterOf(center));
            player.setItemSlot(EquipmentSlot.CHEST, wings);
            player.tick();
            check(helper, !player.onGround() && helper.getLevel().noCollision(player), "airborne gate fixture must be in actual unobstructed air");
            check(helper, player.tryToStartFallFlying() && player.isFallFlying(), "native airborne player must start flight");
            player.stopFallFlying();

            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    BlockPos floor = center.offset(x, -1, z);
                    previousBlocks.put(floor, helper.getLevel().getBlockState(floor));
                    helper.getLevel().setBlockAndUpdate(floor, Blocks.STONE.defaultBlockState());
                }
            }
            player.setPos(Vec3.atBottomCenterOf(center));
            player.setDeltaMovement(new Vec3(0, -0.2, 0));
            player.tick();
            check(helper, player.onGround(), "real downward collision with a solid floor must establish grounded state");
            check(helper, !player.tryToStartFallFlying(), "grounded player must not start flight");

            player.setPos(Vec3.atBottomCenterOf(center.above(6)));
            player.setDeltaMovement(Vec3.ZERO);
            player.tick();
            check(helper, !player.onGround(), "native movement must establish airborne state after leaving floor");
            player.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 100));
            check(helper, !player.tryToStartFallFlying(), "levitation must block native flight");
            player.removeEffect(MobEffects.LEVITATION);
            wings.setDamageValue(wings.getMaxDamage() - 1);
            player.tick();
            check(helper, !player.tryToStartFallFlying(), "almost-broken winged armor must not fly");
            wings.setDamageValue(0);
            player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ItemInit.DRAGON_CHESTPLATE.get()));
            player.tick();
            check(helper, !player.tryToStartFallFlying(), "ordinary Dragon armor must not grant flight");
            player.setItemSlot(EquipmentSlot.CHEST, wings);

            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    for (int y = 0; y <= 1; y++) {
                        BlockPos water = center.offset(x, y, z);
                        previousBlocks.put(water, helper.getLevel().getBlockState(water));
                        helper.getLevel().setBlockAndUpdate(water, Blocks.WATER.defaultBlockState());
                    }
                }
            }
            player.setPos(Vec3.atBottomCenterOf(center));
            player.setDeltaMovement(Vec3.ZERO);
            check(helper, player.refreshFluidState() && player.isInWater(), "real water must update native player fluid interaction");
            check(helper, !player.tryToStartFallFlying(), "water must block native flight");
        } finally {
            previousBlocks.forEach((pos, state) -> helper.getLevel().setBlockAndUpdate(pos, state));
            player.discard();
        }
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void nativeFlightPreservesNoPassiveArmorWear(GameTestHelper helper) {
        GearPlayer player = player(helper);
        BlockPos column = helper.absolutePos(new BlockPos(2, 0, 2));
        // GameTest origins can lie below surrounding terrain; choose real air above its surface.
        int flightHeight = Math.min(helper.getLevel().getMaxY() - 16,
            helper.getLevel().getHeight(Heightmap.Types.MOTION_BLOCKING, column.getX(), column.getZ()) + 80);
        player.setPos(column.getX() + 0.5, flightHeight, column.getZ() + 0.5);
        check(helper, helper.getLevel().noCollision(player), "flight fixture must start in unobstructed air");
        ItemStack wings = new ItemStack(ItemInit.UPGRADED_DRAGON_CHESTPLATE.get());
        player.setItemSlot(EquipmentSlot.CHEST, wings);
        player.tick();
        check(helper, !player.onGround(), "native airborne placement must remain off the ground");
        check(helper, player.tryToStartFallFlying(), "native flight must start for durability test");
        GearPlayer vanillaPlayer = player(helper);
        vanillaPlayer.setPos(player.position().add(2, 0, 0));
        ItemStack vanillaWings = new ItemStack(Items.ELYTRA);
        vanillaPlayer.setItemSlot(EquipmentSlot.CHEST, vanillaWings);
        vanillaPlayer.tick();
        check(helper, !vanillaPlayer.onGround() && helper.getLevel().noCollision(vanillaPlayer), "vanilla Elytra control must start in unobstructed air");
        check(helper, vanillaPlayer.tryToStartFallFlying(), "native vanilla Elytra control must start flight");
        int[] ticks = {0};
        helper.onEachTick(() -> {
            if (ticks[0] >= 60) return;
            int tick = ticks[0]++;
            player.tick();
            vanillaPlayer.tick();
            check(helper, player.isFallFlying(), "native flight must stay active at real player tick " + tick
                + " (age=" + player.getFallFlyingTicks() + ", position=" + player.position() + ")");
            check(helper, vanillaPlayer.isFallFlying(), "vanilla Elytra control must remain airborne");
        });
        helper.runAfterDelay(61, () -> {
            check(helper, ticks[0] == 60 && player.getFallFlyingTicks() >= 40, "sixty real server/player ticks must advance native fall-flight state");
            check(helper, wings.getDamageValue() == 0, "native flight updates must preserve no passive armor wear");
            check(helper, vanillaWings.getDamageValue() >= 2, "vanilla Elytra must retain native passive flight wear");
            wings.hurtAndBreak(1, player, EquipmentSlot.CHEST);
            check(helper, wings.getDamageValue() == 1, "winged armor must retain ordinary durability damage while flying");
            System.out.println("DRAGONLOOT_NATIVE_FLIGHT age=" + player.getFallFlyingTicks()
                + " position=" + player.position() + " dragonDamage=" + wings.getDamageValue() + " vanillaDamage=" + vanillaWings.getDamageValue());
            player.discard();
            vanillaPlayer.discard();
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void dragonAnvilConsumesInputsAndChargesCappedCost(GameTestHelper helper) {
        boolean previous = ConfigInit.CONFIG.dragon_anvil_no_cap;
        GearPlayer player = player(helper);
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        var priorState = helper.getLevel().getBlockState(pos);
        try {
            ConfigInit.CONFIG.dragon_anvil_no_cap = true;
            var dragonState = BlockInit.DRAGON_ANVIL_BLOCK.get().defaultBlockState();
            helper.getLevel().setBlockAndUpdate(pos, dragonState);
            player.setPos(Vec3.atCenterOf(pos.above()));
            player.experienceLevel = 100;
            AnvilMenu menu = new AnvilMenu(123, player.getInventory(), ContainerLevelAccess.create(helper.getLevel(), pos));
            player.containerMenu = menu;
            check(helper, menu.stillValid(player), "Dragon anvil must be accepted by the actual native menu");
            ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
            sword.setDamageValue(300);
            sword.set(DataComponents.REPAIR_COST, 50);
            menu.getSlot(0).set(sword);
            menu.getSlot(1).set(new ItemStack(Items.DIAMOND, 2));
            check(helper, menu.getCost() == 30 && !menu.getSlot(2).getItem().isEmpty(), "native expensive repair must produce a capped Dragon anvil result");
            menu.clicked(2, 0, ContainerInput.PICKUP, player);
            check(helper, player.experienceLevel == 70, "native take must charge exactly thirty levels");
            check(helper, menu.getSlot(0).getItem().isEmpty(), "native take must consume its original sword");
            check(helper, menu.getSlot(1).getItem().is(Items.DIAMOND) && menu.getSlot(1).getItem().getCount() == 1, "native take must consume exactly one repair material");
            check(helper, menu.getCarried().is(Items.DIAMOND_SWORD) && menu.getCarried().getDamageValue() == 0, "native take must yield the repaired sword");
            check(helper, helper.getLevel().getBlockState(pos).equals(dragonState), "native take must preserve Dragon anvil block");
            check(helper, net.minecraft.world.level.block.AnvilBlock.damage(dragonState).equals(dragonState), "native anvil damage must preserve Dragon state");
            check(helper, net.minecraft.world.level.block.AnvilBlock.damage(Blocks.ANVIL.defaultBlockState()).is(Blocks.CHIPPED_ANVIL), "ordinary anvils must retain native damage progression");
        } finally {
            ConfigInit.CONFIG.dragon_anvil_no_cap = previous;
            helper.getLevel().setBlockAndUpdate(pos, priorState);
            player.discard();
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void dragonAnvilCapOptionAndOrdinaryAnvilsStayNative(GameTestHelper helper) {
        boolean previous = ConfigInit.CONFIG.dragon_anvil_no_cap;
        GearPlayer player = player(helper);
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        var priorState = helper.getLevel().getBlockState(pos);
        try {
            player.experienceLevel = 100;
            player.setPos(Vec3.atCenterOf(pos.above()));
            for (boolean dragon : new boolean[] {true, false}) {
                ConfigInit.CONFIG.dragon_anvil_no_cap = !dragon;
                helper.getLevel().setBlockAndUpdate(pos, dragon ? BlockInit.DRAGON_ANVIL_BLOCK.get().defaultBlockState() : Blocks.ANVIL.defaultBlockState());
                AnvilMenu menu = new AnvilMenu(124, player.getInventory(), ContainerLevelAccess.create(helper.getLevel(), pos));
                player.containerMenu = menu;
                check(helper, menu.stillValid(player), "native anvil control must be valid");
                ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
                sword.setDamageValue(300);
                sword.set(DataComponents.REPAIR_COST, 50);
                menu.getSlot(0).set(sword);
                menu.getSlot(1).set(new ItemStack(Items.DIAMOND));
                check(helper, menu.getCost() >= 40 && menu.getSlot(2).getItem().isEmpty(), "disabled Dragon cap/ordinary anvil must preserve native too-expensive rejection");
                menu.clicked(2, 0, ContainerInput.PICKUP, player);
                check(helper, player.experienceLevel == 100 && menu.getSlot(0).getItem() == sword && menu.getSlot(1).getItem().getCount() == 1, "rejected native take must preserve XP and both inputs");
            }
        } finally {
            ConfigInit.CONFIG.dragon_anvil_no_cap = previous;
            helper.getLevel().setBlockAndUpdate(pos, priorState);
            player.discard();
        }
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
        GearPlayer(Level level) { super(level, new GameProfile(UUID.randomUUID(), "gear-test")); }
        @Override public GameType gameMode() { return GameType.SURVIVAL; }
        @Override public boolean isClientAuthoritative() { return false; }
        boolean refreshFluidState() { return this.updateFluidInteraction(); }
    }
}
