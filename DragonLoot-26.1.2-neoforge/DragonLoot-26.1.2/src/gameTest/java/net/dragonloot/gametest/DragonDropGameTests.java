package net.dragonloot.gametest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import net.dragonloot.event.DragonLootNeoForgeEvents;
import net.dragonloot.init.ConfigInit;
import net.dragonloot.init.ItemInit;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

/** Checks the registered death listener through actual native server damage/death hooks. */
public final class DragonDropGameTests {

    private DragonDropGameTests() {
    }

    @GameTest
    public static void nativeDragonDeathDropsConfiguredMinimum(GameTestHelper helper) {
        int expected = expectedMinimum();
        helper.assertTrue(ConfigInit.CONFIG.scale_minimum_drop_amount == expected,
            "Loaded guaranteed scale count disagrees with independent startup expectation " + expected);
        EnderDragon dragon = helper.spawn(EntityType.ENDER_DRAGON, 1, 2, 1);
        dragon.setNoAi(true);
        AABB area = new AABB(dragon.position(), dragon.position()).inflate(3);
        Set<UUID> before = scaleIds(helper, area);
        int previousBonusRolls = ConfigInit.CONFIG.additional_scales_per_player;
        ConfigInit.CONFIG.additional_scales_per_player = 0;
        AtomicInteger nativeDeaths = new AtomicInteger();
        Consumer<LivingDeathEvent> monitor = event -> {
            if (event.getEntity() == dragon) nativeDeaths.incrementAndGet();
        };
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, LivingDeathEvent.class, monitor);
        try {
            dragon.hurtServer(helper.getLevel(),
                dragon.damageSources().playerAttack(helper.makeMockPlayer(GameType.SURVIVAL)), 1_000_000);
            helper.assertTrue(nativeDeaths.get() == 1, "Native lethal Dragon damage must post exactly one death event");
            List<ItemEntity> scales = newScales(helper, area, before);
            helper.assertTrue(scales.stream().mapToInt(drop -> drop.getItem().getCount()).sum() == expected,
                "Native Dragon death must drop exactly the configured minimum when bonus rolls are disabled");
        } finally {
            NeoForge.EVENT_BUS.unregister(monitor);
            ConfigInit.CONFIG.additional_scales_per_player = previousBonusRolls;
            newScales(helper, area, before).forEach(ItemEntity::discard);
            dragon.discard();
        }
        helper.succeed();
    }

    @GameTest
    public static void nativeCancelledDragonDeathCreatesNoScales(GameTestHelper helper) {
        EnderDragon dragon = helper.spawn(EntityType.ENDER_DRAGON, 1, 2, 1);
        dragon.setNoAi(true);
        AABB area = new AABB(dragon.position(), dragon.position()).inflate(3);
        Set<UUID> before = scaleIds(helper, area);
        AtomicInteger cancelledDeaths = new AtomicInteger();
        Consumer<LivingDeathEvent> cancel = event -> {
            if (event.getEntity() == dragon) {
                cancelledDeaths.incrementAndGet();
                event.setCanceled(true);
                // Also exercise our explicit cancellation guard, independent of the
                // event bus's default exclusion of cancelled listeners.
                DragonLootNeoForgeEvents.onLivingDeath(event);
            }
        };
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, LivingDeathEvent.class, cancel);
        try {
            dragon.hurtServer(helper.getLevel(),
                dragon.damageSources().playerAttack(helper.makeMockPlayer(GameType.SURVIVAL)), 1_000_000);
            helper.assertTrue(cancelledDeaths.get() == 1, "Cancellation control did not reach the native Dragon death hook");
            helper.assertTrue(newScales(helper, area, before).isEmpty(), "Cancelled Dragon death must not create scales");
        } finally {
            NeoForge.EVENT_BUS.unregister(cancel);
            newScales(helper, area, before).forEach(ItemEntity::discard);
            dragon.discard();
        }
        helper.succeed();
    }

    @GameTest
    public static void nativeNonDragonDeathCreatesNoScales(GameTestHelper helper) {
        var cow = helper.spawn(EntityType.COW, 1, 2, 1);
        cow.setNoAi(true);
        AABB area = new AABB(cow.position(), cow.position()).inflate(3);
        Set<UUID> before = scaleIds(helper, area);
        try {
            cow.hurtServer(helper.getLevel(), cow.damageSources().generic(), 1_000);
            helper.assertTrue(cow.isDeadOrDying(), "Non-Dragon control must really die through native damage");
            helper.assertTrue(newScales(helper, area, before).isEmpty(), "Ordinary mob death must not create Dragon scales");
        } finally {
            newScales(helper, area, before).forEach(ItemEntity::discard);
            cow.discard();
        }
        helper.succeed();
    }

    private static List<ItemEntity> scales(GameTestHelper helper, AABB area) {
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, area,
            drop -> drop.isAlive() && drop.getItem().is(ItemInit.DRAGON_SCALE_ITEM.get()));
    }

    private static Set<UUID> scaleIds(GameTestHelper helper, AABB area) {
        Set<UUID> ids = new HashSet<>();
        scales(helper, area).forEach(drop -> ids.add(drop.getUUID()));
        return ids;
    }

    private static List<ItemEntity> newScales(GameTestHelper helper, AABB area, Set<UUID> previous) {
        return scales(helper, area).stream().filter(drop -> !previous.contains(drop.getUUID())).toList();
    }

    private static int expectedMinimum() {
        Properties expected = new Properties();
        Path file = Path.of("gear-expectations.properties");
        if (Files.isRegularFile(file)) {
            try (var reader = Files.newBufferedReader(file)) {
                expected.load(reader);
            } catch (IOException exception) {
                throw new IllegalStateException("Cannot read independent gear expectations", exception);
            }
        }
        return Integer.parseInt(expected.getProperty("scales", "3"));
    }
}
