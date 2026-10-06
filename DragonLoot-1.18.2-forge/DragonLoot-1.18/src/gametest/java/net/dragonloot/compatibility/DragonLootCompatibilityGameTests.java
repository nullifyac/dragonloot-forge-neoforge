package net.dragonloot.compatibility;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.dragonloot.DragonLootMain;
import net.dragonloot.init.ItemInit;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.gametest.GameTestHolder;

/** Optional runtime integration checks; no external mod is required to compile. */
@GameTestHolder(DragonLootMain.MOD_ID)
public final class DragonLootCompatibilityGameTests {
    private DragonLootCompatibilityGameTests() {
    }

    @GameTestGenerator
    public static Collection<TestFunction> integrationTests() {
        if (System.getProperty("dragonloot.compat.profile", "").isEmpty()) {
            return List.of();
        }
        List<TestFunction> tests = new ArrayList<>();
        tests.add(new TestFunction("dragonloot", "dragonloot.compatibility.loaded_mods", "dragonloot:empty",
            100, 0, true, DragonLootCompatibilityGameTests::loadedModsMatchPins));
        if (ModList.get().isLoaded("bettercombat")) {
            tests.add(new TestFunction("dragonloot", "dragonloot.compatibility.better_combat_presets", "dragonloot:empty",
                100, 0, true, DragonLootCompatibilityGameTests::betterCombatResolvesWeaponPresets));
        }
        if (ModList.get().isLoaded("enigmaticlegacy")) {
            tests.add(new TestFunction("dragonloot", "dragonloot.compatibility.enigmatic_curios_flight", "dragonloot:empty",
                100, 0, true, helper -> externalWingsAllowNativeFlight(helper, "enigmaticlegacy", "enigmatic_elytra")));
        }
        if (ModList.get().isLoaded("icarus")) {
            tests.add(new TestFunction("dragonloot", "dragonloot.compatibility.icarus_curios_flight", "dragonloot:empty",
                100, 0, true, helper -> externalWingsAllowNativeFlight(helper, "icarus", "white_feathered_wings")));
        }
        return tests;
    }

    private static void loadedModsMatchPins(GameTestHelper helper) {
        Map<String, String> actual = new HashMap<>();
        ModList.get().getMods().forEach(mod -> {
            actual.put(mod.getModId(), mod.getVersion().toString());
            System.out.println("DRAGONLOOT_COMPAT_LOADED " + mod.getModId() + "=" + mod.getVersion());
        });
        String expected = System.getProperty("dragonloot.compat.expectedMods", "");
        check(helper, !expected.isEmpty(), "Integration must declare its pinned required mods");
        for (String entry : expected.split(",")) {
            String[] pair = entry.split("=", 2);
            check(helper, pair.length == 2, "Malformed integration mod pin: " + entry);
            check(helper, pair[1].equals(actual.get(pair[0])),
                "Expected " + entry + ", loaded " + actual.get(pair[0]));
        }
        helper.succeed();
    }

    private static void betterCombatResolvesWeaponPresets(GameTestHelper helper) {
        // Read the registry populated by the mod's server hooks and datapack reload.
        helper.runAtTickTime(5, () -> {
            try {
                Class<?> registry = Class.forName("net.bettercombat.logic.WeaponRegistry");
                Method lookup = registry.getMethod("getAttributes", ItemStack.class);
                checkWeapon(helper, lookup, ItemInit.DRAGON_SWORD_ITEM.get().getDefaultInstance(), "sword");
                checkWeapon(helper, lookup, ItemInit.DRAGON_AXE_ITEM.get().getDefaultInstance(), "axe");
                checkWeapon(helper, lookup, ItemInit.DRAGON_TRIDENT_ITEM.get().getDefaultInstance(), "trident");
                helper.succeed();
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException("Cannot inspect the loaded Better Combat API", exception);
            }
        });
    }

    private static void checkWeapon(GameTestHelper helper, Method lookup, ItemStack stack, String category)
            throws ReflectiveOperationException {
        Object attributes = lookup.invoke(null, stack);
        check(helper, attributes != null, "Better Combat must resolve dragon " + category + " attributes");
        String actualCategory = (String) attributes.getClass().getMethod("category").invoke(attributes);
        check(helper, category.equals(actualCategory), "Dragon " + category + " must resolve its intended preset category");
        Object attacks = attributes.getClass().getMethod("attacks").invoke(attributes);
        check(helper, attacks != null && Array.getLength(attacks) > 0, "Dragon " + category + " must have an attack combo");
        double range = ((Number) attributes.getClass().getMethod("attackRange").invoke(attributes)).doubleValue();
        check(helper, Double.isFinite(range) && range > 0, "Dragon " + category + " must have a valid attack range");
        System.out.println("DRAGONLOOT_BETTER_COMBAT " + category + " category=" + actualCategory
            + " attacks=" + Array.getLength(attacks) + " range=" + range);
    }


    private static void externalWingsAllowNativeFlight(GameTestHelper helper, String namespace, String path) {
        try {
            List<String> failures = new ArrayList<>();
            exerciseExternalWings(helper, namespace, path, "empty", ItemStack.EMPTY, failures);
            exerciseExternalWings(helper, namespace, path, "vanilla",
                net.minecraft.world.item.Items.IRON_CHESTPLATE.getDefaultInstance(), failures);
            exerciseExternalWings(helper, namespace, path, "dragon",
                ItemInit.DRAGON_CHESTPLATE.get().getDefaultInstance(), failures);
            check(helper, failures.isEmpty(), String.join("; ", failures));
            helper.succeed();
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Cannot exercise the loaded Curios flight item", exception);
        }
    }

    private static void exerciseExternalWings(GameTestHelper helper, String namespace, String path,
            String control, ItemStack armor, List<String> failures) throws ReflectiveOperationException {
            WingPlayer player = new WingPlayer(helper.getLevel());
            net.minecraft.core.BlockPos column = helper.absolutePos(new net.minecraft.core.BlockPos(2, 0, 2));
            int altitude = Math.min(helper.getLevel().getMaxBuildHeight() - 16,
                helper.getLevel().getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,
                    column.getX(), column.getZ()) + 80);
            player.setPos(column.getX() + 0.5, altitude, column.getZ() + 0.5);
            player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, armor);
            player.tick();
            player.setOnGround(false);
            check(helper, !player.tryToStartFallFlying(), "Ordinary Dragon armor alone must not grant flight");
            ItemStack wings = lookupExternalItem(namespace, path);
            check(helper, !wings.isEmpty(), "The external flight item must actually be registered: " + namespace + ":" + path);
            Object inventory = curiosInventory(player);
            Class<?> inventoryApi = Class.forName("top.theillusivec4.curios.api.type.capability.ICuriosItemHandler");
            Optional<?> back = (Optional<?>) inventoryApi.getMethod("getStacksHandler", String.class).invoke(inventory, "back");
            check(helper, back.isPresent(), "The real Curios back slot must be registered");
            Class<?> slotApi = Class.forName("top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler");
            Object stacks = slotApi.getMethod("getStacks").invoke(back.orElseThrow());
            Class<?> stackApi = Class.forName("top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler");
            Method setStack = stackApi.getMethod("setStackInSlot", int.class, ItemStack.class);
            Method getStack = stackApi.getMethod("getStackInSlot", int.class);
            Class<?> caelusApi = Class.forName("top.theillusivec4.caelus.api.CaelusApi");
            Object instance = caelusApi.getMethod("getInstance").invoke(null);
            net.minecraft.world.entity.ai.attributes.Attribute flightAttribute =
                (net.minecraft.world.entity.ai.attributes.Attribute) caelusApi.getMethod("getFlightAttribute").invoke(instance);
            double previousGrant = player.getAttributeValue(flightAttribute);
            ItemStack previous = ((ItemStack) getStack.invoke(stacks, 0)).copy();
            try {
                setStack.invoke(stacks, 0, wings);
                for (int tick = 0; tick < 3; tick++) {
                    player.tick();
                }
                player.setOnGround(false);
                double grant = player.getAttributeValue(flightAttribute);
                boolean started = requestCaelusFlight(player);
                System.out.println("DRAGONLOOT_CURIO_CONTROL item=" + namespace + ":" + path
                    + " chest=" + control + " grant=" + grant + " started=" + started
                    + " equipped=" + getStack.invoke(stacks, 0));
                if (grant < 1 || !started) {
                    failures.add(namespace + " " + control + " chest: grant=" + grant + ", started=" + started);
                    return;
                }
                for (int tick = 0; tick < 40; tick++) {
                    player.tick();
                    check(helper, player.isFallFlying(), namespace + " Curios flight must remain active at tick " + tick);
                }
                check(helper, player.getFallFlyingTicks() >= 30, namespace + " must advance actual native flight ticks");
                check(helper, armor.getDamageValue() == 0, "External flight must not damage ordinary Dragon chest armor");
                System.out.println("DRAGONLOOT_EXTERNAL_FLIGHT item=" + namespace + ":" + path
                    + " chest=" + control + " age=" + player.getFallFlyingTicks() + " armorDamage=" + armor.getDamageValue());
                Optional<?> liveBack = (Optional<?>) inventoryApi.getMethod("getStacksHandler", String.class).invoke(inventory, "back");
                Object currentStacks = slotApi.getMethod("getStacks").invoke(liveBack.orElseThrow());
                System.out.println("DRAGONLOOT_CURIO_HANDLER_CHANGED item=" + namespace + ":" + path
                    + " chest=" + control + " changed=" + (currentStacks != stacks));
                stacks = currentStacks;
                setStack.invoke(stacks, 0, previous);
                for (int tick = 0; tick < 3; tick++) { player.tick(); }
                System.out.println("DRAGONLOOT_CURIO_REMOVAL_STATE item=" + namespace + ":" + path
                    + " chest=" + control + " equipped=" + getStack.invoke(stacks, 0)
                    + " grant=" + player.getAttributeValue(flightAttribute)
                    + " modifiers=" + player.getAttribute(flightAttribute).getModifiers());
                check(helper, Math.abs(player.getAttributeValue(flightAttribute) - previousGrant) < 0.0001,
                    namespace + " must remove its real Caelus grant after unequipping wings: " + control);
                check(helper, !player.isFallFlying(),
                    namespace + " flight must stop after unequipping wings: " + control);
                System.out.println("DRAGONLOOT_EXTERNAL_FLIGHT_REMOVED item=" + namespace + ":" + path
                    + " chest=" + control + " grant=" + player.getAttributeValue(flightAttribute));
            } finally {
                Optional<?> liveBack = (Optional<?>) inventoryApi.getMethod("getStacksHandler", String.class).invoke(inventory, "back");
                Object currentStacks = slotApi.getMethod("getStacks").invoke(liveBack.orElseThrow());
                System.out.println("DRAGONLOOT_CURIO_HANDLER_CHANGED item=" + namespace + ":" + path
                    + " chest=" + control + " changed=" + (currentStacks != stacks));
                stacks = currentStacks;
                setStack.invoke(stacks, 0, previous);
            }
    }

    private static Object curiosInventory(net.minecraft.world.entity.LivingEntity player) throws ReflectiveOperationException {
        Class<?> api = Class.forName("top.theillusivec4.curios.api.CuriosApi");
        Object curiosHelper = api.getMethod("getCuriosHelper").invoke(null);
        Class<?> helperApi = Class.forName("top.theillusivec4.curios.api.type.util.ICuriosHelper");
        Object optional = helperApi.getMethod("getCuriosHandler", net.minecraft.world.entity.LivingEntity.class)
            .invoke(curiosHelper, player);
        Optional<?> resolved = optional instanceof Optional<?> ? (Optional<?>) optional
            : (Optional<?>) optional.getClass().getMethod("resolve").invoke(optional);
        return resolved.orElseThrow(() -> new IllegalStateException("The player must have a real Curios capability"));
    }

    private static ItemStack lookupExternalItem(String namespace, String path) {
        return java.util.Objects.requireNonNull(net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(new net.minecraft.resources.ResourceLocation(namespace, path))).getDefaultInstance();
    }

    private static boolean requestCaelusFlight(WingPlayer player) throws ReflectiveOperationException {
        // Exercise the actual mod's server handler with its real provider attribute.
        // The synthetic connection supplies the sender; no flight flags or modifiers are assigned here.
        net.minecraft.network.Connection connection = new net.minecraft.network.Connection(
            net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        connection.setListener(player.connection);
        java.lang.reflect.Constructor<net.minecraftforge.network.NetworkEvent.Context> constructor =
            net.minecraftforge.network.NetworkEvent.Context.class.getDeclaredConstructor(
                net.minecraft.network.Connection.class, net.minecraftforge.network.NetworkDirection.class, int.class);
        constructor.setAccessible(true);
        net.minecraftforge.network.NetworkEvent.Context context = constructor.newInstance(connection,
            net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER, 0);
        Class<?> packet = Class.forName("top.theillusivec4.caelus.common.network.CPacketFlight");
        packet.getMethod("handle", packet, java.util.function.Supplier.class)
            .invoke(null, packet.getConstructor().newInstance(), (java.util.function.Supplier<?>) () -> context);
        return player.isFallFlying();
    }

    private static final class WingPlayer extends net.minecraftforge.common.util.FakePlayer {
        WingPlayer(net.minecraft.server.level.ServerLevel level) {
            super(level, new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "compat-wings"));
        }
        // FakePlayer.tick is intentionally empty; doTick runs the real inherited Player tick.
        @Override public void tick() { super.doTick(); }
    }

    private static void check(GameTestHelper helper, boolean condition, String message) {
        if (!condition) {
            helper.fail(message);
        }
    }
}
