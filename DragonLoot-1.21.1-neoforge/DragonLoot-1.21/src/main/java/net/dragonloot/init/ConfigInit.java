package net.dragonloot.init;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.mojang.logging.LogUtils;
import java.nio.file.Files;
import java.nio.file.Path;
import net.dragonloot.config.DragonLootConfig;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;

public final class ConfigInit {

    private static final Logger LOGGER = LogUtils.getLogger();
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
        ensureCommonConfigFile();
        ModLoadingContext.get().getActiveContainer().registerConfig(ModConfig.Type.COMMON, COMMON_SPEC);
        modBus.addListener(ConfigInit::onLoad);
        modBus.addListener(ConfigInit::onReload);
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

    private static void ensureCommonConfigFile() {
        Path configDir = FMLPaths.CONFIGDIR.get();
        Path configPath = configDir.resolve(COMMON_CONFIG_NAME);
        try {
            Files.createDirectories(configDir);
            try (CommentedFileConfig fileConfig = CommentedFileConfig.builder(configPath)
                    .sync()
                    .preserveInsertionOrder()
                    .build()) {
                fileConfig.load();
                if (!COMMON_SPEC.isCorrect(fileConfig)) {
                    COMMON_SPEC.correct(fileConfig);
                    fileConfig.save();
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to pre-seed config {}", configPath, e);
        }
    }
}
