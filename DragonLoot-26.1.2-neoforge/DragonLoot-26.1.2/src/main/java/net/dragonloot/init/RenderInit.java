package net.dragonloot.init;

import net.dragonloot.DragonLootClient;
import net.dragonloot.DragonLootMain;
import net.dragonloot.entity.model.DragonElytraEntityModel;
import net.dragonloot.entity.model.DragonHelmetModel;
import net.dragonloot.entity.model.DragonBabyArmorModel;
import net.dragonloot.entity.render.DragonTridentEntityRenderer;
import net.dragonloot.entity.render.DragonWingsLayer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.EquipmentSlot;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

public final class RenderInit {
    public static final ModelLayerLocation DRAGON_ELYTRA_LAYER = new ModelLayerLocation(DragonLootMain.id("dragon_elytra"), "main");
    public static final ModelLayerLocation DRAGON_BABY_ELYTRA_LAYER = new ModelLayerLocation(DragonLootMain.id("dragon_elytra"), "baby");
    public static final ModelLayerLocation DRAGON_HELMET_LAYER = new ModelLayerLocation(DragonLootMain.id("dragon_helmet"), "main");
    public static final ModelLayerLocation DRAGON_BABY_HELMET_LAYER = new ModelLayerLocation(DragonLootMain.id("dragon_helmet"), "baby");
    public static final ModelLayerLocation DRAGON_BABY_CHEST_LAYER = new ModelLayerLocation(DragonLootMain.id("dragon_armor"), "baby_chest");
    public static final ModelLayerLocation DRAGON_BABY_LEGS_LAYER = new ModelLayerLocation(DragonLootMain.id("dragon_armor"), "baby_legs");
    public static final ModelLayerLocation DRAGON_BABY_FEET_LAYER = new ModelLayerLocation(DragonLootMain.id("dragon_armor"), "baby_feet");

    private RenderInit() {}

    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(EntityInit.DRAGONTRIDENT_ENTITY.get(), DragonTridentEntityRenderer::new);
    }

    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(DRAGON_ELYTRA_LAYER, DragonElytraEntityModel::createLayerDefinition);
        event.registerLayerDefinition(DRAGON_BABY_ELYTRA_LAYER, DragonElytraEntityModel::createBabyLayerDefinition);
        event.registerLayerDefinition(DRAGON_HELMET_LAYER, DragonHelmetModel::createLayerDefinition);
        event.registerLayerDefinition(DRAGON_BABY_HELMET_LAYER, DragonHelmetModel::createBabyLayerDefinition);
        event.registerLayerDefinition(DRAGON_BABY_CHEST_LAYER, () -> DragonBabyArmorModel.createLayerDefinition(EquipmentSlot.CHEST));
        event.registerLayerDefinition(DRAGON_BABY_LEGS_LAYER, () -> DragonBabyArmorModel.createLayerDefinition(EquipmentSlot.LEGS));
        event.registerLayerDefinition(DRAGON_BABY_FEET_LAYER, () -> DragonBabyArmorModel.createLayerDefinition(EquipmentSlot.FEET));
    }

    public static void addLayers(EntityRenderersEvent.AddLayers event) {
        // Rebuilt on each renderer reload; no model retains an old baked layer.
        DragonLootClient.setEquipmentModels(
            new DragonHelmetModel(event.getEntityModels().bakeLayer(DRAGON_HELMET_LAYER)),
            new DragonHelmetModel(event.getEntityModels().bakeLayer(DRAGON_BABY_HELMET_LAYER)),
            new DragonBabyArmorModel(event.getEntityModels().bakeLayer(DRAGON_BABY_CHEST_LAYER)),
            new DragonBabyArmorModel(event.getEntityModels().bakeLayer(DRAGON_BABY_LEGS_LAYER)),
            new DragonBabyArmorModel(event.getEntityModels().bakeLayer(DRAGON_BABY_FEET_LAYER)));
        for (var skin : event.getSkins()) {
            var player = event.getPlayerRenderer(skin);
            if (player != null) player.addLayer(new DragonWingsLayer<>(player, event.getEntityModels()));
            var mannequin = event.getMannequinRenderer(skin);
            if (mannequin != null) mannequin.addLayer(new DragonWingsLayer<>(mannequin, event.getEntityModels()));
        }
        for (var type : event.getEntityTypes()) {
            var renderer = event.getRenderer(type);
            if (renderer instanceof LivingEntityRenderer<?, ?, ?> living && living.getModel() instanceof HumanoidModel<?>) {
                addHumanoidWings(living, event.getEntityModels());
            }
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void addHumanoidWings(LivingEntityRenderer renderer, EntityModelSet modelSet) {
        renderer.addLayer(new DragonWingsLayer(renderer, modelSet));
    }
}
