package net.dragonloot.init;

import net.dragonloot.config.DragonLootConfig;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public final class ConfigInit {

    private static final String COMMON_CONFIG_NAME = "dragonloot-common.toml";

    public static final DragonLootConfig CONFIG;
    public static final ModConfigSpec COMMON_SPEC;

    static {
        Pair<DragonLootConfig, ModConfigSpec> pair = new ModConfigSpec.Builder().configure(DragonLootConfig::new);
        CONFIG = pair.getLeft();
        COMMON_SPEC = pair.getRight();
    }

    private ConfigInit() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(ConfigInit::onLoad);
        modBus.addListener(ConfigInit::onReload);
        // Item components and armor materials capture these values during registration.
        // STARTUP loads now, before those registries, while keeping the existing file name.
        ModLoadingContext.get().getActiveContainer().registerConfig(ModConfig.Type.STARTUP, COMMON_SPEC, COMMON_CONFIG_NAME);
    }

    private static void onLoad(final ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == COMMON_SPEC) {
            CONFIG.bake();
        }
    }

    private static void onReload(final ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == COMMON_SPEC) {
            CONFIG.bake();
        }
    }
}
