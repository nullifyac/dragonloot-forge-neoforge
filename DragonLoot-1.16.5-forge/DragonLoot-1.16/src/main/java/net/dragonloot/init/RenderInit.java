package net.dragonloot.init;

import net.dragonloot.entity.render.DragonTridentEntityRenderer;
import net.minecraftforge.fml.client.registry.RenderingRegistry;

public final class RenderInit {

    private RenderInit() {
    }

    public static void registerRenderers() {
        RenderingRegistry.registerEntityRenderingHandler(EntityInit.DRAGONTRIDENT_ENTITY.get(), DragonTridentEntityRenderer::new);
    }
}
