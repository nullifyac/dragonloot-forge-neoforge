package net.dragonloot.init;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import java.nio.file.Files;
import java.nio.file.Path;
import net.dragonloot.config.DragonLootConfig;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.loading.FMLConfig;
import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.commons.lang3.tuple.Pair;

public final class ConfigInit {

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
        modBus.addListener(ConfigInit::onLoad);
        modBus.addListener(ConfigInit::onReload);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, COMMON_SPEC);
        // Forge's COMMON config loads after item registration. Read one startup snapshot now
        // so item constructors capture configured attributes, mining speed and durability.
        preloadAndBakeCommonConfig();
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

    private static void preloadAndBakeCommonConfig() {
        Path configPath = FMLPaths.CONFIGDIR.get().resolve(COMMON_CONFIG_NAME);
        try {
            Files.createDirectories(configPath.getParent());
            // Match Forge's normal first-run behavior for modpack-provided default configs.
            Path defaultConfigPath = FMLPaths.GAMEDIR.get().resolve(FMLConfig.defaultConfigPath()).resolve(COMMON_CONFIG_NAME);
            if (!Files.exists(configPath) && Files.exists(defaultConfigPath)) {
                Files.copy(defaultConfigPath, configPath);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Cannot prepare Dragon Loot config " + configPath, e);
        }
        try (CommentedFileConfig fileConfig = CommentedFileConfig.builder(configPath)
                .sync()
                .preserveInsertionOrder()
                .build()) {
            fileConfig.load();
            if (!COMMON_SPEC.isCorrect(fileConfig)) {
                COMMON_SPEC.correct(fileConfig);
                fileConfig.save();
            }
            // The Forge-managed file config replaces this in-memory copy during normal loading.
            // Do not leave the spec pointing at the temporary file config after it is closed.
            COMMON_SPEC.setConfig(CommentedConfig.copy(fileConfig));
            CONFIG.bake();
        } catch (Exception e) {
            throw new IllegalStateException("Cannot load Dragon Loot config before item registration: " + configPath, e);
        }
    }
}
