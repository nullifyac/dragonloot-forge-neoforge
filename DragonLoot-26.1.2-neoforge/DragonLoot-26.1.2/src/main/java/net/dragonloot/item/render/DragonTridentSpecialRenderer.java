package net.dragonloot.item.render;

import net.dragonloot.DragonLootMain;
import net.minecraft.client.renderer.special.NoDataSpecialModelRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Transformation;
import com.mojang.serialization.MapCodec;
import java.util.function.Consumer;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.projectile.TridentModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public class DragonTridentSpecialRenderer implements NoDataSpecialModelRenderer {
    public static final Transformation DEFAULT_TRANSFORMATION = new Transformation(null, null, new Vector3f(1.0F, -1.0F, -1.0F), null);
    private final TridentModel model;

    public DragonTridentSpecialRenderer(TridentModel model) {
        this.model = model;
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, int overlayCoords, boolean hasFoil, int outlineColor) {
        submitNodeCollector.submitModelPart(
            this.model.root(), poseStack, this.model.renderType(DragonLootMain.id("textures/entity/dragon_trident.png")), lightCoords, overlayCoords, null, false, hasFoil, -1, null, outlineColor
        );
    }

    @Override
    public void getExtents(Consumer<Vector3fc> output) {
        PoseStack poseStack = new PoseStack();
        this.model.root().getExtentsForGui(poseStack, output);
    }

        public record Unbaked() implements NoDataSpecialModelRenderer.Unbaked {
        public static final MapCodec<DragonTridentSpecialRenderer.Unbaked> MAP_CODEC = MapCodec.unit(new DragonTridentSpecialRenderer.Unbaked());

        @Override
        public MapCodec<DragonTridentSpecialRenderer.Unbaked> type() {
            return MAP_CODEC;
        }

        public DragonTridentSpecialRenderer bake(SpecialModelRenderer.BakingContext context) {
            return new DragonTridentSpecialRenderer(new TridentModel(context.entityModelSet().bakeLayer(ModelLayers.TRIDENT)));
        }
    }
}
