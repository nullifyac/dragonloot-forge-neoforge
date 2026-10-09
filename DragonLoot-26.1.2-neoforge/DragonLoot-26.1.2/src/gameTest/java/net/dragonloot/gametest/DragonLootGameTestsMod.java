package net.dragonloot.gametest;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestServer;
import net.minecraft.gametest.framework.GameTestTicker;
import java.util.function.Consumer;
import java.lang.reflect.InvocationTargetException;

/** Loaded only by the dedicated regression-test and runtime-smoke run configurations. */
@Mod(DragonLootGameTestsMod.MOD_ID)
public final class DragonLootGameTestsMod {

    public static final String MOD_ID = "dragonloot_game_tests";

    public DragonLootGameTestsMod(IEventBus eventBus) {
        DeferredRegister<Consumer<GameTestHelper>> functions = DeferredRegister.create(Registries.TEST_FUNCTION, MOD_ID);
        Class<?>[] suites = {DragonGearGameTests.class, DragonRecipeGameTests.class, DragonTridentGameTests.class,
                DragonDropGameTests.class, DragonCompatibilityGameTests.class};
        for (Class<?> suite : suites) {
            for (var method : suite.getDeclaredMethods()) {
                if (!method.isAnnotationPresent(GameTest.class)) continue;
                functions.register(method.getName().toLowerCase(java.util.Locale.ROOT), () -> helper -> {
                    try {
                        method.invoke(null, helper);
                    } catch (InvocationTargetException exception) {
                        if (exception.getCause() instanceof RuntimeException runtime) throw runtime;
                        throw new RuntimeException(exception.getCause());
                    } catch (ReflectiveOperationException exception) {
                        throw new RuntimeException(exception);
                    }
                });
            }
        }
        functions.register(eventBus);
        // Native test_instance/test_environment resources register the metadata
        // in both development runs and the installed production loader. NeoForge
        // only fires RegisterGameTestsEvent in development environments.
        if (FMLEnvironment.isProduction() && Boolean.getBoolean("dragonloot.test.packagedTick")) {
            // The installed loader also disables the native GameTest ticker.
            // Advance that same ticker solely for the opt-in fixture server;
            // ordinary dedicated servers and development ticks remain native.
            NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post event) -> {
                if (event.getServer() instanceof GameTestServer server && server.tickRateManager().runsNormally()) {
                    GameTestTicker.SINGLETON.tick();
                }
            });
            System.out.println("DRAGONLOOT_PACKAGED_GAMETEST_SCOPE production=true nativeTickerFixture=true cleanDist=false normalDedicatedBootCovered=false");
        }
    }
}
