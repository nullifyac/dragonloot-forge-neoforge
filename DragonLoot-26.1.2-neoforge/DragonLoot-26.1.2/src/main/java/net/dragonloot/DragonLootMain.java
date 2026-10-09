package net.dragonloot;

import net.dragonloot.event.DragonLootNeoForgeEvents;
import net.dragonloot.init.BlockInit;
import net.dragonloot.init.ConfigInit;
import net.dragonloot.init.EntityInit;
import net.dragonloot.init.ItemInit;
import net.dragonloot.init.NetworkInit;
import net.dragonloot.init.TagInit;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.DispenserBlock;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;

@Mod(DragonLootMain.MOD_ID)
public class DragonLootMain {

    public static final String MOD_ID = "dragonloot";

    public DragonLootMain(IEventBus modBus, ModContainer modContainer) {
        ConfigInit.register(modBus, modContainer);
        BlockInit.BLOCKS.register(modBus);
        ItemInit.ITEMS.register(modBus);
        ItemInit.CREATIVE_TABS.register(modBus);
        EntityInit.ENTITY_TYPES.register(modBus);
        NetworkInit.register(modBus);

        modBus.addListener(this::commonSetup);
        NeoForge.EVENT_BUS.addListener(DragonLootNeoForgeEvents::onLivingDeath);

        if (FMLEnvironment.getDist() == Dist.CLIENT) {
            DragonLootClient.registerClientListeners(modBus);
        }
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            DispenserBlock.registerProjectileBehavior(ItemInit.DRAGON_TRIDENT_ITEM.get());
        });
        TagInit.init();
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    @Deprecated(forRemoval = true)
    public static Identifier ID(String path) {
        return id(path);
    }
}
