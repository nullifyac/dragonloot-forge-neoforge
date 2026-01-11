package net.dragonloot.init;

import net.dragonloot.DragonLootMain;
import net.dragonloot.entity.model.DragonElytraEntityModel;
import net.dragonloot.entity.render.DragonTridentEntityRenderer;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

public final class RenderInit {

    private RenderInit() {
    }

    public static final ModelLayerLocation DRAGON_ELYTRA_LAYER = new ModelLayerLocation(DragonLootMain.id("dragon_elytra_render_layer"), "dragon_elytra_render_layer");

    public static void registerRenderers(final EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(EntityInit.DRAGONTRIDENT_ENTITY.get(), DragonTridentEntityRenderer::new);
    }

    public static void registerLayerDefinitions(final EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(DRAGON_ELYTRA_LAYER, DragonElytraEntityModel::createLayerDefinition);
    }
}