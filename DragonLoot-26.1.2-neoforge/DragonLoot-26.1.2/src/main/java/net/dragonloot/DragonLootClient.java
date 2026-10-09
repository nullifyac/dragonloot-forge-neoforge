package net.dragonloot;

import net.dragonloot.entity.model.DragonHelmetModel;
import net.dragonloot.entity.model.DragonBabyArmorModel;
import net.dragonloot.init.ItemInit;
import net.dragonloot.init.RenderInit;
import net.dragonloot.item.render.DragonTridentSpecialRenderer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.neoforged.neoforge.client.event.RegisterSpecialModelRendererEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.common.NeoForge;

public final class DragonLootClient {
    private static DragonHelmetModel helmetModel;
    private static DragonHelmetModel babyHelmetModel;
    private static DragonBabyArmorModel babyChestModel;
    private static DragonBabyArmorModel babyLegsModel;
    private static DragonBabyArmorModel babyFeetModel;
    private static final java.util.Map<Model<?>, DragonBabyArmorModel> babyChestModels = new java.util.IdentityHashMap<>();
    private static final java.util.Map<Model<?>, DragonBabyArmorModel> babyLegsModels = new java.util.IdentityHashMap<>();
    private static final java.util.Map<Model<?>, DragonBabyArmorModel> babyFeetModels = new java.util.IdentityHashMap<>();

    private DragonLootClient() {}

    public static void registerClientListeners(IEventBus modBus) {
        modBus.addListener(DragonLootClient::registerClientExtensions);
        modBus.addListener(DragonLootClient::registerSpecialModels);
        modBus.addListener(RenderInit::registerRenderers);
        modBus.addListener(RenderInit::registerLayerDefinitions);
        modBus.addListener(RenderInit::addLayers);
        NeoForge.EVENT_BUS.addListener(DragonLootClient::adjustBowFov);
    }

    public static void setEquipmentModels(DragonHelmetModel adult, DragonHelmetModel baby,
        DragonBabyArmorModel chest, DragonBabyArmorModel legs, DragonBabyArmorModel feet) {
        helmetModel = adult;
        babyHelmetModel = baby;
        babyChestModel = chest;
        babyLegsModel = legs;
        babyFeetModel = feet;
        babyChestModels.clear();
        babyLegsModels.clear();
        babyFeetModels.clear();
    }

    private static void registerSpecialModels(RegisterSpecialModelRendererEvent event) {
        event.register(DragonLootMain.id("dragon_trident"), DragonTridentSpecialRenderer.Unbaked.MAP_CODEC);
    }

    private static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerItem(new IClientItemExtensions() {
            @Override
            public HumanoidModel.ArmPose getArmPose(LivingEntity entity, InteractionHand hand, ItemStack stack) {
                return !entity.swinging && CrossbowItem.isCharged(stack) ? HumanoidModel.ArmPose.CROSSBOW_HOLD : null;
            }
        }, ItemInit.DRAGON_CROSSBOW_ITEM.get());
        event.registerItem(new IClientItemExtensions() {
            @Override
            public Model<?> getHumanoidArmorModel(ItemStack stack, EquipmentClientInfo.LayerType type, Model original) {
                var model = type == EquipmentClientInfo.LayerType.HUMANOID_BABY ? babyHelmetModel : helmetModel;
                return model == null ? original : model;
            }

            @Override
            public Identifier getArmorTexture(ItemStack stack, EquipmentClientInfo.LayerType type,
                EquipmentClientInfo.Layer layer, Identifier original) {
                return DragonLootMain.id("textures/entity/dragon_helmet_3d.png");
            }
        }, ItemInit.DRAGON_HELMET.get());
        event.registerItem(new IClientItemExtensions() {
            @Override
            public Model<?> getHumanoidArmorModel(ItemStack stack, EquipmentClientInfo.LayerType type, Model original) {
                if (type != EquipmentClientInfo.LayerType.HUMANOID_BABY) return original;
                if (babyChestModel == null || !(original instanceof HumanoidModel<?> humanoid)) return original;
                var slot = stack.is(ItemInit.DRAGON_LEGGINGS.get()) ? net.minecraft.world.entity.EquipmentSlot.LEGS
                    : stack.is(ItemInit.DRAGON_BOOTS.get()) ? net.minecraft.world.entity.EquipmentSlot.FEET : net.minecraft.world.entity.EquipmentSlot.CHEST;
                var cache = slot == net.minecraft.world.entity.EquipmentSlot.LEGS ? babyLegsModels
                    : slot == net.minecraft.world.entity.EquipmentSlot.FEET ? babyFeetModels : babyChestModels;
                // Bind each replacement to its actual native armor model; a
                // shared mutable delegate could mix different mob render states.
                return cache.computeIfAbsent(original, model -> new DragonBabyArmorModel(
                    DragonBabyArmorModel.createLayerDefinition(slot).bakeRoot(), humanoid));
            }

            @Override
            public Identifier getArmorTexture(ItemStack stack, EquipmentClientInfo.LayerType type,
                EquipmentClientInfo.Layer layer, Identifier original) {
                if (type != EquipmentClientInfo.LayerType.HUMANOID_BABY) return original;
                return DragonLootMain.id(stack.is(ItemInit.DRAGON_LEGGINGS.get())
                    ? "textures/models/armor/dragon_layer_2.png" : "textures/models/armor/dragon_layer_1.png");
            }
        }, ItemInit.DRAGON_CHESTPLATE.get(), ItemInit.UPGRADED_DRAGON_CHESTPLATE.get(),
            ItemInit.DRAGON_LEGGINGS.get(), ItemInit.DRAGON_BOOTS.get());
    }

    private static void adjustBowFov(ComputeFovModifierEvent event) {
        var player = event.getPlayer();
        if (player.isUsingItem() && player.getUseItem().is(ItemInit.DRAGON_BOW_ITEM.get())) {
            float draw = Mth.clamp(player.getTicksUsingItem() / 20.0F, 0.0F, 1.0F);
            event.setNewFovModifier(event.getNewFovModifier() - event.getFovModifier() * draw * draw * 0.15F * event.getFovScale());
        }
    }
}
