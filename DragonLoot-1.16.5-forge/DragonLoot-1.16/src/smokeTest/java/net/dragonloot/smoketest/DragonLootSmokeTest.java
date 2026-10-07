package net.dragonloot.smoketest;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import net.dragonloot.entity.DragonTridentEntity;
import net.dragonloot.init.ConfigInit;
import net.dragonloot.init.EntityInit;
import net.dragonloot.init.ItemInit;
import net.minecraft.block.Blocks;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.passive.PigEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.AbstractArrowEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.HorseArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.server.FMLServerStartedEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Real server API assertions, loaded only by the runSmokeTest development run. */
@Mod("dragonloot_smoketest")
public final class DragonLootSmokeTest {
    private static final Logger LOGGER = LogManager.getLogger();
    private final JsonArray cases = new JsonArray();
    private final Properties expected = new Properties();
    private int failures;
    private ServerWorld world;
    private HarnessPlayer player;
    private BlockPos origin;

    public DragonLootSmokeTest() {
        MinecraftForge.EVENT_BUS.addListener(this::onServerStarted);
    }

    private void onServerStarted(FMLServerStartedEvent event) {
        MinecraftServer server = event.getServer();
        try {
            if ("network-flight".equals(System.getProperty("dragonloot.smoke.case"))) {
                world = server.getLevel(World.OVERWORLD);
                run("network-flight", () -> NetworkFlightSmokeTests.run(world));
                return;
            }
            Path expectationFile = Paths.get("smoke-expectations.properties");
            if (!Files.isRegularFile(expectationFile)) {
                throw new IllegalStateException("Prepare smoke-expectations.properties before running this harness");
            }
            try (InputStream stream = Files.newInputStream(expectationFile)) {
                expected.load(stream);
            }
            world = server.getLevel(World.OVERWORLD);
            origin = world.getSharedSpawnPos().above(80);
            world.getChunk(origin);
            player = new HarnessPlayer(world, "DragonSmokeOwner");
            player.setPos(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
            player.setOnGround(false);
            player.abilities.instabuild = false;
            world.addNewPlayer(player);
            if (System.getProperty("dragonloot.compat.profile") != null) {
                run("optional-compatibility-mod-set", this::checkCompatibilityMods);
            }
            run("startup-config-and-registered-items", this::checkRegisteredItems);
            run("trident-constructors", this::checkConstructors);
            run("throw-collision-save-loyalty-owner-pickup", this::checkThrowAndReturn);
            run("almost-broken-release-guard", this::checkDurabilityGuard);
            run("native-winged-flight-gates-and-wear", this::checkFlight);
            run("reload-runtime-values-with-frozen-gear", this::checkReload);
        } catch (Throwable failure) {
            recordFailure("harness-initialization", failure);
        } finally {
            JsonObject report = new JsonObject();
            report.addProperty("minecraft", "1.16.5");
            report.addProperty("forge", "36.2.39");
            report.addProperty("case", System.getProperty("dragonloot.smoke.case", "baseline"));
            report.addProperty("compatibilityProfile", System.getProperty("dragonloot.compat.profile", "none"));
            report.addProperty("passed", failures == 0);
            report.addProperty("failures", failures);
            report.addProperty("scope", "Real dedicated-server APIs; graphical client checks recorded separately");
            report.add("tests", cases);
            try {
                Files.write(Paths.get("smoke-report.json"),
                    new GsonBuilder().setPrettyPrinting().create().toJson(report).getBytes(StandardCharsets.UTF_8));
            } catch (Exception error) {
                LOGGER.error("Cannot write smoke report", error);
            }
            LOGGER.info("DRAGONLOOT_SMOKE_RESULT: {} failures in {} cases", failures, cases.size());
            server.halt(false);
        }
    }

    private void checkCompatibilityMods() {
        String expectedMods = System.getProperty("dragonloot.compat.expectedMods", "");
        assertTrue(!expectedMods.isEmpty(), "compatibility profile must specify the expected mod/version set");
        for (String entry : expectedMods.split(",")) {
            int separator = entry.indexOf('=');
            assertTrue(separator > 0, "expected mod entries must have id=version format");
            String id = entry.substring(0, separator).trim();
            String version = entry.substring(separator + 1).trim();
            String actual = ModList.get().getModContainerById(id)
                .orElseThrow(() -> new AssertionError("Expected compatibility mod is not loaded: " + id))
                .getModInfo().getVersion().toString();
            assertTrue(actual.equals(version), "Compatibility mod " + id + ": expected " + version + ", got " + actual);
            LOGGER.info("DRAGONLOOT_COMPAT_LOADED: {}={}", id, actual);
        }
    }

    private void checkRegisteredItems() {
        assertTrue(Files.isRegularFile(Paths.get("config", "dragonloot-common.toml")),
            "Forge COMMON config must exist before the server starts");
        ItemStack pickaxe = new ItemStack(ItemInit.DRAGON_PICKAXE_ITEM.get());
        close(pickaxe.getDestroySpeed(Blocks.STONE.defaultBlockState()), number("speed", 12),
            "actual registered pickaxe mining speed");
        equal(pickaxe.getMaxDamage(), integer("toolDurability", 2479), "registered tool durability");
        ItemStack sword = new ItemStack(ItemInit.DRAGON_SWORD_ITEM.get());
        double attackModifier = sword.getAttributeModifiers(EquipmentSlotType.MAINHAND)
            .get(Attributes.ATTACK_DAMAGE).stream().mapToDouble(AttributeModifier::getAmount).sum();
        close(attackModifier, number("swordModifier", 8), "registered sword attack modifier");
        ItemStack chest = new ItemStack(ItemInit.DRAGON_CHESTPLATE.get());
        equal(chest.getMaxDamage(), integer("chestDurability", 1295), "registered chest durability");
        close(chest.getAttributeModifiers(EquipmentSlotType.CHEST).get(Attributes.ARMOR)
            .stream().mapToDouble(AttributeModifier::getAmount).sum(), number("chestArmor", 10),
            "registered chest armor modifier");
        close(chest.getAttributeModifiers(EquipmentSlotType.CHEST).get(Attributes.ARMOR_TOUGHNESS)
            .stream().mapToDouble(AttributeModifier::getAmount).sum(), number("toughness", 3),
            "registered chest toughness modifier");
        equal(((HorseArmorItem) ItemInit.DRAGON_HORSE_ARMOR_ITEM.get()).getProtection(),
            integer("horseArmor", 18), "registered horse protection");
        equal(ItemInit.DRAGON_PICKAXE_ITEM.get().getEnchantmentValue(),
            integer("toolEnchantability", 20), "registered tool enchantability");
        equal(ConfigInit.CONFIG.scale_minimum_drop_amount, integer("scales", 3), "startup runtime drop setting");
    }

    private void checkConstructors() {
        DragonTridentEntity fromRegistry = EntityInit.DRAGONTRIDENT_ENTITY.get().create(world);
        assertTrue(fromRegistry != null, "registry constructor returns an entity");
        DragonTridentEntity atPosition = new DragonTridentEntity(world, player.getX(), player.getY(), player.getZ());
        CompoundNBT saved = new CompoundNBT();
        atPosition.addAdditionalSaveData(saved);
        assertTrue(ItemStack.of(saved.getCompound("Dragon_Trident")).getItem() == ItemInit.DRAGON_TRIDENT_ITEM.get(),
            "position constructor holds a Dragon trident");
        ItemStack stack = enchantedTrident();
        DragonTridentEntity fromOwner = new DragonTridentEntity(world, player, stack);
        assertTrue(fromOwner.getOwner() == player && fromOwner.isEnchanted(), "owner constructor retains owner/glint");
        fromOwner.addAdditionalSaveData(saved);
        equal(ItemStack.of(saved.getCompound("Dragon_Trident")).getDamageValue(), 7, "constructor item damage copy");
        stack.setDamageValue(15);
        fromOwner.addAdditionalSaveData(saved);
        equal(ItemStack.of(saved.getCompound("Dragon_Trident")).getDamageValue(), 7, "constructor stack isolation");
    }

    private void checkThrowAndReturn() {
        player.inventory.clearContent();
        player.setItemInHand(Hand.MAIN_HAND, enchantedTrident());
        player.startUsingItem(Hand.MAIN_HAND);
        player.xRot = 0;
        player.yRot = 0;
        ItemStack held = player.getMainHandItem();
        int wear = held.getDamageValue();
        ItemInit.DRAGON_TRIDENT_ITEM.get().releaseUsing(held, world, player,
            ItemInit.DRAGON_TRIDENT_ITEM.get().getUseDuration(held) - 10);
        List<DragonTridentEntity> thrown = world.getEntitiesOfClass(DragonTridentEntity.class,
            player.getBoundingBox().inflate(10), entity -> entity.getOwner() == player && !entity.removed);
        equal(thrown.size(), 1, "releaseUsing spawns exactly one real Dragon projectile");
        DragonTridentEntity projectile = thrown.get(0);
        assertTrue(player.getMainHandItem().isEmpty(), "survival throw removes the held item");
        PigEntity pig = EntityType.PIG.create(world);
        assertTrue(pig != null, "create collision target");
        pig.setNoAi(true);
        pig.setNoGravity(true);
        pig.setPos(player.getX(), projectile.getY() - 0.25, player.getZ() + 3);
        world.addFreshEntity(pig);
        float before = pig.getHealth();
        for (int tick = 0; tick < 8 && pig.getHealth() == before; tick++) {
            projectile.tick();
        }
        assertTrue(pig.getHealth() < before, "actual arrow collision calls damage on the target");
        close(before - pig.getHealth(), 8, "unenhanced Dragon projectile damage");
        CompoundNBT saved = new CompoundNBT();
        projectile.save(saved);
        assertTrue(saved.getBoolean("DealtDamage"), "save records the completed hit");
        assertTrue(saved.hasUUID("Owner") && saved.getUUID("Owner").equals(player.getUUID()),
            "save records the actual owner UUID");
        ItemStack savedStack = ItemStack.of(saved.getCompound("Dragon_Trident"));
        equal(savedStack.getDamageValue(), wear + 1, "throw consumes exactly one durability");
        equal(EnchantmentHelper.getLoyalty(savedStack), 3, "save keeps Loyalty");
        assertTrue(savedStack.hasCustomHoverName(), "save keeps custom item data");
        world.removeEntity(projectile, false);
        world.removeEntity(pig, false);

        DragonTridentEntity restored = EntityInit.DRAGONTRIDENT_ENTITY.get().create(world);
        assertTrue(restored != null, "registry constructor for restore");
        restored.load(saved);
        assertTrue(restored.getOwner() == player, "entity NBT reload resolves the saved owner through ServerWorld");
        world.addFreshEntity(restored);
        assertTrue(restored.isEnchanted(), "entity NBT reload restores synchronized enchanted appearance");
        Vector3d towardOwner = player.getEyePosition(1).subtract(restored.position());
        Vector3d previousVelocity = restored.getDeltaMovement();
        restored.tick();
        assertTrue(restored.isNoPhysics() && restored.returnTimer > 0, "saved hit + Loyalty starts return");
        assertTrue(restored.getDeltaMovement().subtract(previousVelocity).dot(towardOwner) > 0,
            "Loyalty accelerates the returning projectile toward its owner");
        HarnessPlayer stranger = new HarnessPlayer(world, "DragonSmokeOther");
        restored.shakeTime = 0;
        restored.playerTouch(stranger);
        assertTrue(!restored.removed && stranger.inventory.isEmpty(), "non-owner cannot collect returning trident");
        restored.playerTouch(player);
        assertTrue(restored.removed, "legacy owner pickup removes returned projectile");
        ItemStack returned = player.inventory.items.stream()
            .filter(item -> item.getItem() == ItemInit.DRAGON_TRIDENT_ITEM.get()).findFirst().orElse(ItemStack.EMPTY);
        assertTrue(!returned.isEmpty(), "returned trident enters real player inventory");
        equal(returned.getDamageValue(), wear + 1, "returned item retains consumed durability");
        equal(EnchantmentHelper.getLoyalty(returned), 3, "returned item retains Loyalty");
        player.stopUsingItem();
    }

    private void checkDurabilityGuard() {
        player.inventory.clearContent();
        ItemStack nearlyBroken = new ItemStack(ItemInit.DRAGON_TRIDENT_ITEM.get());
        nearlyBroken.setDamageValue(nearlyBroken.getMaxDamage() - 1);
        player.setItemInHand(Hand.MAIN_HAND, nearlyBroken);
        player.startUsingItem(Hand.MAIN_HAND);
        ItemInit.DRAGON_TRIDENT_ITEM.get().releaseUsing(nearlyBroken, world, player,
            ItemInit.DRAGON_TRIDENT_ITEM.get().getUseDuration(nearlyBroken) - 10);
        equal(nearlyBroken.getDamageValue(), nearlyBroken.getMaxDamage() - 1, "release guard keeps item durability");
        assertTrue(world.getEntitiesOfClass(DragonTridentEntity.class, player.getBoundingBox().inflate(10),
            entity -> entity.getOwner() == player && !entity.removed).isEmpty(), "release guard spawns no projectile");
        player.stopUsingItem();
    }

    private void checkFlight() {
        FlightPlayer flightPlayer = new FlightPlayer(world);
        flightPlayer.setPos(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
        flightPlayer.refreshFluidState();
        ItemStack wings = new ItemStack(ItemInit.UPGRADED_DRAGON_CHESTPLATE.get());
        flightPlayer.setItemSlot(EquipmentSlotType.CHEST, wings);
        flightPlayer.setOnGround(false);
        assertTrue(flightPlayer.tryToStartFallFlying() && flightPlayer.isFallFlying(), "native flightPlayer gate starts winged flight");
        flightPlayer.stopFallFlying();
        flightPlayer.setOnGround(true);
        assertTrue(!flightPlayer.tryToStartFallFlying(), "grounded flightPlayer cannot start winged flight");
        flightPlayer.setOnGround(false);
        flightPlayer.addEffect(new EffectInstance(Effects.LEVITATION, 100));
        assertTrue(!flightPlayer.tryToStartFallFlying(), "levitation blocks winged flight through vanilla gate");
        flightPlayer.removeEffect(Effects.LEVITATION);
        wings.setDamageValue(wings.getMaxDamage() - 1);
        assertTrue(!flightPlayer.tryToStartFallFlying(), "almost-broken winged armor cannot fly");
        wings.setDamageValue(0);
        flightPlayer.setItemSlot(EquipmentSlotType.CHEST, new ItemStack(ItemInit.DRAGON_CHESTPLATE.get()));
        assertTrue(!flightPlayer.tryToStartFallFlying(), "ordinary Dragon armor has no flight hook");
        flightPlayer.setItemSlot(EquipmentSlotType.CHEST, wings);
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                world.setBlockAndUpdate(origin.offset(x, 0, z), Blocks.WATER.defaultBlockState());
                world.setBlockAndUpdate(origin.offset(x, 1, z), Blocks.WATER.defaultBlockState());
            }
        }
        assertTrue(flightPlayer.refreshFluidState() && flightPlayer.isInWater(), "real water updates flightPlayer fluid state");
        assertTrue(!flightPlayer.tryToStartFallFlying(), "water blocks native winged flight");
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                world.setBlockAndUpdate(origin.offset(x, 0, z), Blocks.AIR.defaultBlockState());
                world.setBlockAndUpdate(origin.offset(x, 1, z), Blocks.AIR.defaultBlockState());
            }
        }
        flightPlayer.setPos(flightPlayer.getX(), flightPlayer.getY() + 3, flightPlayer.getZ());
        flightPlayer.refreshFluidState();
        flightPlayer.setOnGround(false);
        assertTrue(flightPlayer.tryToStartFallFlying(), "flight resumes after leaving water");
        int damage = wings.getDamageValue();
        for (int tick = 1; tick <= 60; tick++) {
            flightPlayer.tick();
        }
        assertTrue(flightPlayer.isFallFlying(), "native flight stays active through sixty real player ticks");
        equal(wings.getDamageValue(), damage, "native flight updates preserve no passive durability wear");
        flightPlayer.stopFallFlying();
    }

    private void checkReload() {
        ForgeConfigSpec.DoubleValue speed = ConfigInit.COMMON_SPEC.getValues().get("dragonloot.tools.dragon_tool_mining_speed");
        ForgeConfigSpec.IntValue scales = ConfigInit.COMMON_SPEC.getValues().get("dragonloot.scale_minimum_drop_amount");
        double oldSpeed = speed.get();
        int oldScales = scales.get();
        try {
            speed.set(oldSpeed + 11);
            scales.set(oldScales + 2);
            ConfigInit.COMMON_SPEC.afterReload();
            ConfigInit.CONFIG.bake();
            close(new ItemStack(ItemInit.DRAGON_PICKAXE_ITEM.get()).getDestroySpeed(Blocks.STONE.defaultBlockState()),
                number("speed", 12), "reload cannot change registered gear mining speed");
            equal(ConfigInit.CONFIG.scale_minimum_drop_amount, oldScales + 2, "runtime scale value refreshes");
        } finally {
            speed.set(oldSpeed);
            scales.set(oldScales);
            ConfigInit.COMMON_SPEC.afterReload();
            ConfigInit.CONFIG.bake();
        }
    }

    private ItemStack enchantedTrident() {
        ItemStack stack = new ItemStack(ItemInit.DRAGON_TRIDENT_ITEM.get());
        stack.enchant(Enchantments.LOYALTY, 3);
        stack.setDamageValue(7);
        stack.setHoverName(new StringTextComponent("Smoke test trident"));
        return stack;
    }

    private double number(String key, double fallback) {
        return Double.parseDouble(expected.getProperty(key, Double.toString(fallback)));
    }

    private int integer(String key, int fallback) {
        return Integer.parseInt(expected.getProperty(key, Integer.toString(fallback)));
    }

    private void run(String name, Runnable assertion) {
        try {
            assertion.run();
            JsonObject result = new JsonObject();
            result.addProperty("name", name);
            result.addProperty("passed", true);
            cases.add(result);
            LOGGER.info("DRAGONLOOT_SMOKE_PASS: {}", name);
        } catch (Throwable failure) {
            recordFailure(name, failure);
        } finally {
            if (world != null && player != null) {
                world.getEntitiesOfClass(DragonTridentEntity.class, player.getBoundingBox().inflate(64),
                    entity -> entity.getOwner() == player && !entity.removed)
                    .forEach(entity -> world.removeEntity(entity, false));
            }
        }
    }

    private void recordFailure(String name, Throwable failure) {
        failures++;
        JsonObject result = new JsonObject();
        result.addProperty("name", name);
        result.addProperty("passed", false);
        result.addProperty("error", failure.toString());
        cases.add(result);
        LOGGER.error("DRAGONLOOT_SMOKE_FAIL: " + name, failure);
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void equal(int actual, int expected, String message) {
        if (actual != expected) throw new AssertionError(message + ": expected " + expected + ", got " + actual);
    }

    private static void close(double actual, double expected, String message) {
        if (Math.abs(actual - expected) > 0.0001) {
            throw new AssertionError(message + ": expected " + expected + ", got " + actual);
        }
    }

    private static final class FlightPlayer extends PlayerEntity {
        FlightPlayer(World level) {
            super(level, BlockPos.ZERO, 0, new GameProfile(UUID.randomUUID(), "DragonSmokeFlight"));
        }

        @Override public boolean isSpectator() { return false; }
        @Override public boolean isCreative() { return false; }
        boolean refreshFluidState() { return this.updateInWaterStateAndDoFluidPushing(); }
    }

    private static final class HarnessPlayer extends FakePlayer {
        HarnessPlayer(ServerWorld level, String name) {
            super(level, new GameProfile(UUID.randomUUID(), name));
        }

        boolean refreshFluidState() {
            return this.updateInWaterStateAndDoFluidPushing();
        }
    }
}
