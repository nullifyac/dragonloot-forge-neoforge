package net.dragonloot.init;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import java.nio.file.Files;
import java.nio.file.Path;
import net.dragonloot.config.DragonLootConfig;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class ConfigInit {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final String COMMON_CONFIG_NAME = "dragonloot-common.toml";

    public static final DragonLootConfig CONFIG;
    public static final ForgeConfigSpec COMMON_SPEC;

    static {
        Pair<DragonLootConfig, ForgeConfigSpec> pair = new ForgeConfigSpec.Builder().configure(DragonLootConfig::new);
        CONFIG = pair.getLeft();
        COMMON_SPEC = pair.getRight();
    }

    private ConfigInit() {
    }

    public static void register(IEventBus modBus) {
        ensureCommonConfigFile();
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, COMMON_SPEC);
        modBus.addListener(ConfigInit::onLoad);
        modBus.addListener(ConfigInit::onReload);
    }

    private static void onLoad(final ModConfig.Loading event) {
        if (event.getConfig().getSpec() == COMMON_SPEC) {
            CONFIG.bake();
        }
    }

    private static void onReload(final ModConfig.Reloading event) {
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
