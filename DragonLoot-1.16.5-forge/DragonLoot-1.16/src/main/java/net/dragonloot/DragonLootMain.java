package net.dragonloot;

import net.dragonloot.compat.recipes.CompatRecipes;
import net.dragonloot.compat.recipes.RecipeGenerator;
import net.dragonloot.init.BlockInit;
import net.dragonloot.init.ConfigInit;
import net.dragonloot.init.EntityInit;
import net.dragonloot.init.ItemInit;
import net.dragonloot.init.NetworkInit;
import net.dragonloot.init.TagInit;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(DragonLootMain.MOD_ID)
public class DragonLootMain {

    public static final String MOD_ID = "dragonloot";

    public DragonLootMain() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        ConfigInit.register(modBus);
        BlockInit.BLOCKS.register(modBus);
        ItemInit.ITEMS.register(modBus);
        EntityInit.ENTITY_TYPES.register(modBus);

        modBus.addListener(this::commonSetup);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            NetworkInit.init();
            CompatRecipes.loadRecipes();
            RecipeGenerator.addRecipes();
        });
        TagInit.init();
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, path);
    }

    @Deprecated
    public static ResourceLocation ID(String path) {
        return id(path);
    }
}
