package net.dragonloot;

import net.dragonloot.init.ItemInit;
import net.dragonloot.init.ModelProviderInit;
import net.dragonloot.init.RenderInit;
import net.dragonloot.item.render.DragonTridentBewlr;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

public final class DragonLootClient {

    private static DragonTridentBewlr tridentRenderer;

    private DragonLootClient() {
    }

    public static void registerClientListeners(IEventBus modBus) {
        modBus.addListener(DragonLootClient::onClientSetup);
        modBus.addListener(DragonLootClient::registerClientExtensions);
        modBus.addListener(DragonLootClient::registerReloadListeners);
        modBus.addListener(RenderInit::registerRenderers);
        modBus.addListener(RenderInit::registerLayerDefinitions);
    }

    public static void onClientSetup(final FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ModelProviderInit.init();
        });
    }

    private static void registerRenderers(final EntityRenderersEvent.RegisterRenderers event) {
        RenderInit.registerRenderers(event);
    }

    private static void registerLayerDefinitions(final EntityRenderersEvent.RegisterLayerDefinitions event) {
        RenderInit.registerLayerDefinitions(event);
    }

    public static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerItem(new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return getTridentRenderer();
            }
        }, ItemInit.DRAGON_TRIDENT_ITEM.get());
    }

    private static DragonTridentBewlr getTridentRenderer() {
        if (tridentRenderer == null) {
            tridentRenderer = new DragonTridentBewlr();
        }
        return tridentRenderer;
    }

    private static void registerReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(getTridentRenderer());
    }
}
