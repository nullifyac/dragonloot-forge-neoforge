package net.dragonloot;

import net.dragonloot.compat.recipes.CompatRecipes;
import net.dragonloot.compat.recipes.RecipeGenerator;
import net.dragonloot.init.BlockInit;
import net.dragonloot.init.ConfigInit;
import net.dragonloot.init.EntityInit;
import net.dragonloot.init.ItemInit;
import net.dragonloot.init.NetworkInit;
import net.dragonloot.init.TagInit;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;

@Mod(DragonLootMain.MOD_ID)
public class DragonLootMain {

    public static final String MOD_ID = "dragonloot";

    public DragonLootMain() {
        IEventBus modBus = ModLoadingContext.get().getActiveContainer().getEventBus();

        ConfigInit.register(modBus);
        BlockInit.BLOCKS.register(modBus);
        ItemInit.ITEMS.register(modBus);
        ItemInit.ARMOR_MATERIALS.register(modBus);
        ItemInit.CREATIVE_TABS.register(modBus);
        EntityInit.ENTITY_TYPES.register(modBus);
        NetworkInit.register(modBus);

        modBus.addListener(this::commonSetup);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            DragonLootClient.registerClientListeners(modBus);
        }
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            CompatRecipes.loadRecipes();
            RecipeGenerator.addRecipes();
        });
        TagInit.init();
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    @Deprecated(forRemoval = true)
    public static ResourceLocation ID(String path) {
        return id(path);
    }
}
