package net.dragonloot.gametest;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import net.dragonloot.DragonLootMain;
import net.dragonloot.entity.DragonTridentEntity;
import net.dragonloot.entity.model.DragonElytraEntityModel;
import net.dragonloot.entity.model.DragonHelmetModel;
import net.dragonloot.entity.model.DragonBabyArmorModel;
import net.dragonloot.entity.render.DragonTridentEntityRenderer;
import net.dragonloot.entity.render.DragonWingsLayer;
import net.dragonloot.init.EntityInit;
import net.dragonloot.init.ItemInit;
import net.dragonloot.init.RenderInit;
import net.dragonloot.item.render.DragonTridentSpecialRenderer;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.item.SpecialModelWrapper;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.common.NeoForge;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/** Opt-in native client renderer checks. This source set is excluded from the player JAR. */
@EventBusSubscriber(modid = "dragonloot_game_tests", value = Dist.CLIENT)
public final class DragonClientSmoke {
    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();
    private static int ticks;
    private static int state;
    private static int entered;
    private static int worldReadyTicks;
    private static boolean finished;
    private static CompletableFuture<Void> reload;
    private static Object oldSpecialRenderer;
    private static Model<?> oldHelmetModel;
    private static final List<String> checks = new ArrayList<>();
    private static final List<String> captures = new ArrayList<>();
    private static final List<CompletableFuture<Void>> screenshotWrites = new ArrayList<>();
    private static int caseIndex;
    private static VisualCase currentCase;

    private record VisualCase(String name, CameraType camera, boolean offhand, String item,
                              int useTicks, String chargedProjectile, String expectedSprite) {}

    private static final List<VisualCase> CASES = List.of(
        new VisualCase("trident-main-held", CameraType.FIRST_PERSON, false, "trident", -1, "", ""),
        new VisualCase("trident-main-charging", CameraType.FIRST_PERSON, false, "trident", 20, "", ""),
        new VisualCase("trident-offhand-held", CameraType.FIRST_PERSON, true, "trident", -1, "", ""),
        new VisualCase("trident-offhand-charging", CameraType.FIRST_PERSON, true, "trident", 20, "", ""),
        new VisualCase("helmet-wings-trident-third-person", CameraType.THIRD_PERSON_FRONT, false, "trident", -1, "", ""),
        new VisualCase("helmet-wings-trident-charging-third-person", CameraType.THIRD_PERSON_FRONT, false, "trident", 20, "", ""),
        new VisualCase("bow-pulling-0", CameraType.FIRST_PERSON, false, "bow", 1, "", "dragon_bow_pulling_0"),
        new VisualCase("bow-pulling-1", CameraType.FIRST_PERSON, false, "bow", 14, "", "dragon_bow_pulling_1"),
        new VisualCase("bow-pulling-2", CameraType.FIRST_PERSON, false, "bow", 20, "", "dragon_bow_pulling_2"),
        new VisualCase("bow-pose-third-person", CameraType.THIRD_PERSON_FRONT, false, "bow", 20, "", "dragon_bow_pulling_2"),
        new VisualCase("crossbow-pulling-0", CameraType.FIRST_PERSON, false, "crossbow", 1, "", "dragon_crossbow_pulling_0"),
        new VisualCase("crossbow-pulling-1", CameraType.FIRST_PERSON, false, "crossbow", 16, "", "dragon_crossbow_pulling_1"),
        new VisualCase("crossbow-pulling-2", CameraType.FIRST_PERSON, false, "crossbow", 25, "", "dragon_crossbow_pulling_2"),
        new VisualCase("crossbow-arrow-first-person", CameraType.FIRST_PERSON, false, "crossbow", -1, "arrow", "dragon_crossbow_arrow"),
        new VisualCase("crossbow-firework-first-person", CameraType.FIRST_PERSON, false, "crossbow", -1, "rocket", "dragon_crossbow_firework"),
        new VisualCase("crossbow-arrow-pose-third-person", CameraType.THIRD_PERSON_FRONT, false, "crossbow", -1, "arrow", "dragon_crossbow_arrow"),
        new VisualCase("crossbow-firework-pose-third-person", CameraType.THIRD_PERSON_FRONT, false, "crossbow", -1, "rocket", "dragon_crossbow_firework"),
        new VisualCase("helmet-wings-rear", CameraType.THIRD_PERSON_BACK, false, "trident", -1, "", "")
    );

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("dragonloot.test.clientSmoke") || finished) return;
        Minecraft minecraft = Minecraft.getInstance();
        ticks++;
        try {
            if (ticks > 6000) throw new AssertionError("Client smoke timed out at state " + state + ", screen=" + minecraft.screen);
            if (state >= 2 && minecraft.player != null && minecraft.level != null) {
                check(minecraft.player.isAlive() && !minecraft.player.isDeadOrDying() && minecraft.player.getHealth() > 0,
                        "Native client fixture player died at state " + state);
                check(!(minecraft.screen instanceof net.minecraft.client.gui.screens.DeathScreen),
                        "Native death screen cannot count as a world renderer check");
            }
            if (state == 2) {
                worldReadyTicks = minecraft.player != null && minecraft.level != null
                        && minecraft.screen == null && minecraft.getOverlay() == null ? worldReadyTicks + 1 : 0;
            }
            if (state == 0 && minecraft.screen instanceof TitleScreen && minecraft.getOverlay() == null) {
                capture(minecraft, "title-before-connection");
                record("Native title initialization completed; item-component model checks require joined server data");
                writeResult(minecraft, "client-title-result.json", true,
                        "native title initialization only; item models require a connected client world", null);
                String address = System.getProperty("dragonloot.test.clientServerAddress", "");
                if (address.isEmpty()) {
                    advance(1);
                } else {
                    check(address.matches("127\\.0\\.0\\.1:[0-9]{1,5}"), "Client smoke server must be an explicit loopback address");
                    ConnectScreen.startConnecting(minecraft.screen, minecraft, ServerAddress.parseString(address),
                            new ServerData("Isolated Dragon Loot renderer verification", address, ServerData.Type.OTHER), false, null);
                    advance(2);
                }
            } else if (state == 1) {
                check(ticks - entered <= 200, "Native title screenshot callback timed out");
                if (screenshotWrites.stream().allMatch(CompletableFuture::isDone)) {
                    validateScreenshotWrites();
                    writeResult(minecraft, "client-title-result.json", true,
                            "native title initialization and screenshot completion only; item models require a connected client world", null);
                    finish(minecraft);
                }
            } else if (state == 2 && worldReadyTicks > 60) {
                // Native item-holder components bind during server registry/data synchronization.
                // Constructing item stacks at the title screen would test unbound holders.
                validateBoundModels(minecraft);
                oldSpecialRenderer = tridentRenderer(minecraft);
                oldHelmetModel = helmetModel(minecraft);
                capture(minecraft, "world-before-model-reload");
                reload = minecraft.reloadResourcePacks();
                advance(6);
            } else if (state == 6 && reload.isDone() && minecraft.getOverlay() == null) {
                reload.join();
                check(minecraft.player != null && minecraft.level != null, "Client world was lost during model reload");
                validateBoundModels(minecraft);
                check(tridentRenderer(minecraft) != oldSpecialRenderer, "Resource reload retained the old special trident renderer");
                check(helmetModel(minecraft) != oldHelmetModel, "Resource reload retained the old baked helmet model");
                record("Native connected-world resource reload rebuilt the special trident renderer and helmet model");
                writeResult(minecraft, "client-model-result.json", true, "connected client-world item model and reload checks", null);
                check(minecraft.getEntityRenderDispatcher().getRenderer(
                        new DragonTridentEntity(EntityInit.DRAGONTRIDENT_ENTITY.get(), minecraft.level)) instanceof DragonTridentEntityRenderer,
                        "Client registry did not select the dragon projectile renderer");
                checkPlayerLayers(minecraft);
                record("Actual client-world renderer selects the dragon projectile renderer and one wide-wing layer");
                caseIndex = 0;
                beginCase(minecraft);
                advance(3);
            } else if (state == 3 && minecraft.player != null && minecraft.level != null) {
                holdCase(minecraft);
                if (ticks - entered > 30) {
                    validateCase(minecraft);
                    capture(minecraft, currentCase.name());
                    caseIndex++;
                    if (caseIndex < CASES.size()) {
                        beginCase(minecraft);
                        entered = ticks;
                    } else {
                        minecraft.player.stopUsingItem();
                        oldSpecialRenderer = tridentRenderer(minecraft);
                        oldHelmetModel = helmetModel(minecraft);
                        reload = minecraft.reloadResourcePacks();
                        advance(4);
                    }
                }
            } else if (state == 4 && reload.isDone() && minecraft.getOverlay() == null) {
                reload.join();
                check(minecraft.player != null && minecraft.level != null, "Client world was lost during reload");
                check(tridentRenderer(minecraft) != oldSpecialRenderer && helmetModel(minecraft) != oldHelmetModel,
                        "World resource reload retained old models");
                checkPlayerLayers(minecraft);
                currentCase = CASES.get(4);
                setCaseEquipment(minecraft);
                advance(5);
            } else if (state == 5 && minecraft.player != null && ticks - entered > 30) {
                holdCase(minecraft);
                validateCase(minecraft);
                capture(minecraft, "world-after-resource-reload");
                record("World reload retained registered layers and valid Dragon models");
                advance(7);
            } else if (state == 7) {
                check(minecraft.player != null && minecraft.level != null, "Client world was lost while saving native screenshots");
                check(ticks - entered <= 200, "Native screenshot write callbacks timed out");
                holdCase(minecraft);
                if (screenshotWrites.stream().allMatch(CompletableFuture::isDone)) {
                    validateScreenshotWrites();
                    record("Every requested native screenshot callback completed with a fresh PNG of the captured framebuffer dimensions");
                    writeResult(minecraft, "client-render-result.json", true,
                            "actual client-world renderer and completed screenshot checks; visual-only item/use-clock fixtures", null);
                    finish(minecraft);
                }
            }
        } catch (Throwable failure) {
            LOGGER.error("Dragon Loot client smoke failed", failure);
            try {
                writeResult(minecraft, state <= 1 ? "client-title-result.json" : state == 2 || state == 6 ? "client-model-result.json" : "client-render-result.json", false,
                        "state " + state + (currentCase == null ? "" : ", case " + currentCase.name()), failure);
            } catch (Throwable reportFailure) {
                LOGGER.error("Cannot write client smoke failure", reportFailure);
            }
            finish(minecraft);
        }
    }

    private static void validateBoundModels(Minecraft minecraft) throws Exception {
        check(minecraft.player != null && minecraft.level != null, "Item model checks require a connected client world");
        int items = 0;
        for (var item : BuiltInRegistries.ITEM) {
            if (!BuiltInRegistries.ITEM.getKey(item).getNamespace().equals("dragonloot")) continue;
            items++;
            ItemStack stack = item.getDefaultInstance();
            for (ItemDisplayContext context : ItemDisplayContext.values()) {
                InspectedState output = resolve(minecraft, stack, context, true);
                check(!output.isEmpty(), "Empty registered item model: " + stack + "/" + context);
                check(output.identities.stream().noneMatch(identity -> identity.getClass().getSimpleName().contains("Missing")),
                        "Missing registered item model: " + stack + "/" + context);
                if (stack.is(ItemInit.DRAGON_TRIDENT_ITEM.get())) {
                    boolean sprite = context == ItemDisplayContext.GUI || context == ItemDisplayContext.GROUND
                            || context == ItemDisplayContext.FIXED || context.name().equals("ON_SHELF");
                    check(output.specialRenderers().isEmpty() == sprite, "Trident special/sprite context selection differs: " + context);
                    if (!sprite) check(output.specialRenderers().getFirst() instanceof DragonTridentSpecialRenderer,
                            "Handheld trident must select its Dragon special renderer");
                }
                var particle = output.pickParticleMaterial(RandomSource.create(0));
                check(particle != null && !particle.sprite().contents().name().getPath().contains("missing"),
                        "Missing particle/model texture: " + stack + "/" + context);
            }
        }
        check(items == 16, "Registered Dragon item count differs: " + items);
        try (var reader = minecraft.getResourceManager().getResource(DragonLootMain.id("equipment/dragon.json")).orElseThrow().openAsReader()) {
            var layers = JsonParser.parseReader(reader).getAsJsonObject().getAsJsonObject("layers");
            check(layers.has("humanoid") && layers.has("humanoid_baby") && layers.has("humanoid_leggings") && layers.has("horse_body"),
                    "Native armor equipment layers are incomplete");
            check(!layers.has("wings"), "Equipment asset must not draw duplicate vanilla wings beside the custom wide wings");
        }
        helmetModel(minecraft);
        validateBabyArmor(minecraft);
        record("All 16 registered items resolve native models/textures in every display context; trident hand/sprite selection and equipment layers verified");
    }

    private static Object tridentRenderer(Minecraft minecraft) throws Exception {
        return resolve(minecraft, ItemInit.DRAGON_TRIDENT_ITEM.get().getDefaultInstance(),
                ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, true).specialRenderers().getFirst();
    }

    private static Model<?> helmetModel(Minecraft minecraft) {
        ItemStack stack = ItemInit.DRAGON_HELMET.get().getDefaultInstance();
        DragonHelmetModel original = new DragonHelmetModel(minecraft.getEntityModels().bakeLayer(RenderInit.DRAGON_HELMET_LAYER));
        Model<?> actual = IClientItemExtensions.of(stack).getHumanoidArmorModel(stack, EquipmentClientInfo.LayerType.HUMANOID, original);
        check(actual instanceof DragonHelmetModel && actual != original, "Registered helmet extension did not supply its current baked horn model");
        check(actual.root().getChild("head").hasChild("dragon_helmet"), "Helmet model lacks the original horn geometry");
        return actual;
    }

    private static void validateBabyArmor(Minecraft minecraft) throws Exception {
        var nativeMeshes = HumanoidModel.createBabyArmorMeshSet(
                new net.minecraft.client.model.geom.builders.CubeDeformation(0.5F),
                new net.minecraft.client.model.geom.builders.CubeDeformation(1), net.minecraft.client.model.geom.PartPose.ZERO);
        Field cubes = net.minecraft.client.model.geom.ModelPart.class.getDeclaredField("cubes");
        cubes.setAccessible(true);
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack stack = switch (slot) {
                case HEAD -> ItemInit.DRAGON_HELMET.get().getDefaultInstance();
                case CHEST -> ItemInit.DRAGON_CHESTPLATE.get().getDefaultInstance();
                case LEGS -> ItemInit.DRAGON_LEGGINGS.get().getDefaultInstance();
                default -> ItemInit.DRAGON_BOOTS.get().getDefaultInstance();
            };
            HumanoidModel<net.minecraft.client.renderer.entity.state.HumanoidRenderState> original = new HumanoidModel<>(
                    net.minecraft.client.model.geom.builders.LayerDefinition.create(nativeMeshes.get(slot), 64, 64).bakeRoot());
            Model<?> actual = IClientItemExtensions.of(stack).getHumanoidArmorModel(stack, EquipmentClientInfo.LayerType.HUMANOID_BABY, original);
            check(actual != original && (slot == EquipmentSlot.HEAD ? actual instanceof DragonHelmetModel : actual instanceof DragonBabyArmorModel),
                    "Baby equipment did not select the Dragon mesh for " + slot);
            check(slot != EquipmentSlot.HEAD || actual.root().getChild("head").getInitialPose().y() == 15,
                    "Baby helmet has an adult head origin");
            int faces = 0;
            for (var part : actual.allParts()) {
                @SuppressWarnings("unchecked")
                List<net.minecraft.client.model.geom.ModelPart.Cube> partCubes = (List<net.minecraft.client.model.geom.ModelPart.Cube>) cubes.get(part);
                for (var cube : partCubes) for (var polygon : cube.polygons) {
                    faces++;
                    for (var vertex : polygon.vertices()) check(vertex.u() >= 0 && vertex.u() <= 1 && vertex.v() >= 0 && vertex.v() <= 1,
                            "Baby equipment UV exceeds the preserved 64x32 texture for " + slot);
                }
            }
            check(faces > 0, "Baby equipment mesh is empty for " + slot);
        }
        record("Native baby armor dimensions, head origin and all UV bounds use the preserved Dragon artwork");
    }

    private static void beginCase(Minecraft minecraft) throws Exception {
        currentCase = CASES.get(caseIndex);
        setCaseEquipment(minecraft);
        holdCase(minecraft);
    }

    private static void setCaseEquipment(Minecraft minecraft) {
        var player = minecraft.player;
        player.stopUsingItem();
        ItemStack weapon = switch (currentCase.item()) {
            case "bow" -> ItemInit.DRAGON_BOW_ITEM.get().getDefaultInstance();
            case "crossbow" -> ItemInit.DRAGON_CROSSBOW_ITEM.get().getDefaultInstance();
            default -> ItemInit.DRAGON_TRIDENT_ITEM.get().getDefaultInstance();
        };
        if (!currentCase.chargedProjectile().isEmpty()) {
            weapon.set(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.ofNonEmpty(List.of(
                    new ItemStack(currentCase.chargedProjectile().equals("arrow") ? Items.ARROW : Items.FIREWORK_ROCKET))));
        }
        player.setItemInHand(InteractionHand.MAIN_HAND, currentCase.offhand() ? ItemStack.EMPTY : weapon);
        player.setItemInHand(InteractionHand.OFF_HAND, currentCase.offhand() ? weapon : ItemStack.EMPTY);
        player.setItemSlot(EquipmentSlot.HEAD, ItemInit.DRAGON_HELMET.get().getDefaultInstance());
        player.setItemSlot(EquipmentSlot.CHEST, ItemInit.UPGRADED_DRAGON_CHESTPLATE.get().getDefaultInstance());
        player.setItemSlot(EquipmentSlot.LEGS, ItemInit.DRAGON_LEGGINGS.get().getDefaultInstance());
        player.setItemSlot(EquipmentSlot.FEET, ItemInit.DRAGON_BOOTS.get().getDefaultInstance());
        minecraft.options.setCameraType(currentCase.camera());
        minecraft.mouseHandler.releaseMouse();
    }

    private static void holdCase(Minecraft minecraft) throws Exception {
        var player = minecraft.player;
        minecraft.mouseHandler.releaseMouse();
        player.setXRot(0);
        player.setYRot(0);
        player.xRotO = 0;
        player.yRotO = 0;
        player.yBodyRot = 0;
        player.yBodyRotO = 0;
        player.yHeadRot = 0;
        player.yHeadRotO = 0;
        player.swinging = false;
        if (currentCase.useTicks() >= 0) {
            InteractionHand hand = currentCase.offhand() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
            if (!player.isUsingItem()) player.startUsingItem(hand);
            // Rendering tests deliberately control only the local native use clock.
            // Throwing, consumption, damage and flight are separate server tests.
            Field remaining = LivingEntity.class.getDeclaredField("useItemRemaining");
            remaining.setAccessible(true);
            remaining.setInt(player, player.getUseItem().getUseDuration(player) - currentCase.useTicks());
        } else {
            player.stopUsingItem();
        }
    }

    private static void validateCase(Minecraft minecraft) throws Exception {
        var player = minecraft.player;
        InteractionHand hand = currentCase.offhand() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack item = player.getItemInHand(hand);
        check(!item.isEmpty(), "Client fixture lost the held weapon");
        ItemDisplayContext context = currentCase.offhand() ? ItemDisplayContext.FIRST_PERSON_LEFT_HAND : ItemDisplayContext.FIRST_PERSON_RIGHT_HAND;
        InspectedState resolved = resolve(minecraft, item, context, true);
        check(!resolved.isEmpty(), "Visual case resolved an empty model");
        if (!currentCase.expectedSprite().isEmpty()) {
            var particle = resolved.pickParticleMaterial(RandomSource.create(0));
            check(particle != null && particle.sprite().contents().name().equals(DragonLootMain.id("item/" + currentCase.expectedSprite())),
                    "Native model property selected the wrong weapon texture for " + currentCase.name());
        } else {
            check(resolved.specialRenderers().getFirst() instanceof DragonTridentSpecialRenderer, "Visual trident lost its special renderer");
        }
        var renderer = minecraft.getEntityRenderDispatcher().getPlayerRenderer(player);
        AvatarRenderState state = renderer.createRenderState();
        renderer.extractRenderState(player, state, 0);
        if (!currentCase.chargedProjectile().isEmpty()) {
            check(CrossbowItem.isCharged(item), "Charged crossbow fixture is empty");
            check(state.leftArmPose == HumanoidModel.ArmPose.CROSSBOW_HOLD || state.rightArmPose == HumanoidModel.ArmPose.CROSSBOW_HOLD,
                    "Native renderer did not select Dragon crossbow hold pose");
        }
        if (currentCase.item().equals("bow") && currentCase.useTicks() >= 0) {
            check(state.leftArmPose == HumanoidModel.ArmPose.BOW_AND_ARROW || state.rightArmPose == HumanoidModel.ArmPose.BOW_AND_ARROW,
                    "Native renderer did not select Dragon bow pose");
            ComputeFovModifierEvent fov = new ComputeFovModifierEvent(player, 1, 1);
            NeoForge.EVENT_BUS.post(fov);
            float fraction = Math.min(currentCase.useTicks() / 20.0F, 1);
            check(Math.abs(fov.getNewFovModifier() - (1 - fraction * fraction * 0.15F)) < 0.001F,
                    "Dragon bow FOV did not follow the native draw state");
        }
        check(state.chestEquipment.is(ItemInit.UPGRADED_DRAGON_CHESTPLATE.get()) && state.headEquipment.is(ItemInit.DRAGON_HELMET.get()),
                "Native avatar state lost Dragon equipment");
        record("Native client renderer case " + currentCase.name());
    }

    private static void checkPlayerLayers(Minecraft minecraft) throws Exception {
        var renderer = minecraft.getEntityRenderDispatcher().getPlayerRenderer(minecraft.player);
        Field layersField = LivingEntityRenderer.class.getDeclaredField("layers");
        layersField.setAccessible(true);
        List<?> layers = (List<?>) layersField.get(renderer);
        check(layers.stream().filter(DragonWingsLayer.class::isInstance).count() == 1, "Player renderer must have exactly one custom wide-wing layer");
        DragonElytraEntityModel model = new DragonElytraEntityModel(minecraft.getEntityModels().bakeLayer(RenderInit.DRAGON_ELYTRA_LAYER));
        float adultWidth = modelWidth(model);
        check(adultWidth > 1.8F, "Wide-wing geometry unexpectedly shrank to the vanilla mesh");
        DragonElytraEntityModel baby = new DragonElytraEntityModel(minecraft.getEntityModels().bakeLayer(RenderInit.DRAGON_BABY_ELYTRA_LAYER));
        float babyWidth = modelWidth(baby);
        check(Math.abs(babyWidth / adultWidth - 0.5F) < 0.001F,
                "Baby wide wings must retain exactly the native half-scale extent");
        record("Native baby wing transform halves the preserved Dragon wing extent");
    }

    private static float modelWidth(DragonElytraEntityModel model) {
        List<org.joml.Vector3fc> extents = new ArrayList<>();
        model.root().getExtentsForGui(new PoseStack(), point -> extents.add(new org.joml.Vector3f(point)));
        check(!extents.isEmpty(), "Wide-wing model has no geometry");
        float min = Float.POSITIVE_INFINITY, max = Float.NEGATIVE_INFINITY;
        for (var point : extents) { min = Math.min(min, point.x()); max = Math.max(max, point.x()); }
        return max - min;
    }

    private static InspectedState resolve(Minecraft minecraft, ItemStack stack, ItemDisplayContext context, boolean world) {
        InspectedState result = new InspectedState();
        minecraft.getItemModelResolver().updateForTopItem(result, stack, context,
                world ? minecraft.level : null, world ? minecraft.player : null, 0);
        return result;
    }

    private static final class InspectedState extends ItemStackRenderState {
        private final List<Object> identities = new ArrayList<>();

        @Override
        public void appendModelIdentityElement(Object identity) { identities.add(identity); }

        private List<Object> specialRenderers() throws Exception {
            List<Object> result = new ArrayList<>();
            for (Object identity : identities) {
                if (identity instanceof SpecialModelWrapper<?>) {
                    Field renderer = SpecialModelWrapper.class.getDeclaredField("specialRenderer");
                    renderer.setAccessible(true);
                    result.add(renderer.get(identity));
                }
            }
            return result;
        }
    }

    private static void capture(Minecraft minecraft, String name) throws Exception {
        if (!name.equals("title-before-connection")) {
            check(minecraft.player != null && minecraft.level != null, "World capture requires a joined native client world");
            check(minecraft.player.isAlive() && !minecraft.player.isDeadOrDying() && minecraft.player.getHealth() > 0,
                    "World capture rejected a dead native player: " + name);
            check(minecraft.screen == null, "World capture is obscured by a native screen: " + name + ": " + minecraft.screen);
            check(minecraft.getOverlay() == null, "World capture is obscured by a native overlay: " + name);
            check(minecraft.getCameraEntity() == minecraft.player, "World capture camera must follow the fixture player: " + name);
        }
        String filename = "dragonloot-" + name + ".png";
        Path output = minecraft.gameDirectory.toPath().resolve(Screenshot.SCREENSHOT_DIR).resolve(filename);
        // These exact fixture-owned filenames must not accept a previous run's PNG.
        Files.deleteIfExists(output);
        int width = minecraft.getMainRenderTarget().width;
        int height = minecraft.getMainRenderTarget().height;
        CompletableFuture<Void> saved = new CompletableFuture<>();
        captures.add(filename);
        screenshotWrites.add(saved);
        Screenshot.grab(minecraft.gameDirectory, filename, minecraft.getMainRenderTarget(), 1, message -> {
            LOGGER.info("Dragon client smoke screenshot: {}", message.getString());
            try {
                check(!(message.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents translated)
                        || !translated.getKey().equals("screenshot.failure"), "Native screenshot reported a write error: " + message.getString());
                check(Files.isRegularFile(output) && Files.size(output) > 24,
                        "Native screenshot callback did not produce its requested PNG: " + filename + ": " + message.getString());
                byte[] header;
                try (var input = Files.newInputStream(output)) {
                    header = input.readNBytes(24);
                }
                byte[] signature = {(byte) 137, 80, 78, 71, 13, 10, 26, 10};
                check(java.util.Arrays.equals(java.util.Arrays.copyOf(header, 8), signature)
                        && java.nio.ByteBuffer.wrap(header).getInt(12) == 0x49484452,
                        "Native screenshot is not a PNG with an IHDR chunk: " + filename);
                check(java.nio.ByteBuffer.wrap(header).getInt(16) == width
                        && java.nio.ByteBuffer.wrap(header).getInt(20) == height,
                        "Native screenshot dimensions differ from its captured framebuffer: " + filename);
                try (var input = Files.newByteChannel(output)) {
                    var end = java.nio.ByteBuffer.allocate(12);
                    input.position(input.size() - 12);
                    while (end.hasRemaining() && input.read(end) > 0) {}
                    check(!end.hasRemaining() && end.getInt(0) == 0 && end.getInt(4) == 0x49454E44 && end.getInt(8) == 0xAE426082,
                            "Native screenshot PNG was not completely written: " + filename);
                }
                saved.complete(null);
            } catch (Throwable failure) {
                saved.completeExceptionally(failure);
            }
        });
    }

    private static void validateScreenshotWrites() {
        check(!screenshotWrites.isEmpty() && screenshotWrites.size() == captures.size(), "Native screenshot request accounting differs");
        screenshotWrites.forEach(CompletableFuture::join);
    }

    private static void writeResult(Minecraft minecraft, String filename, boolean passed, String scope, Throwable failure) throws Exception {
        JsonObject result = new JsonObject();
        result.addProperty("passed", passed);
        result.addProperty("scope", scope);
        result.addProperty("state", state);
        result.addProperty("gameplayPacketsVerified", false);
        result.addProperty("worldCapturesRequireLiveUnobstructedPlayer", true);
        result.addProperty("nativePlayerAlive", minecraft.player != null && minecraft.player.isAlive() && !minecraft.player.isDeadOrDying());
        result.addProperty("nativeScreen", minecraft.screen == null ? "none" : minecraft.screen.getClass().getName());
        result.addProperty("nativeOverlay", minecraft.getOverlay() == null ? "none" : minecraft.getOverlay().getClass().getName());
        result.addProperty("requestedScreenshotCount", captures.size());
        result.addProperty("completedScreenshotCount", screenshotWrites.stream().filter(write -> write.isDone() && !write.isCompletedExceptionally()).count());
        result.addProperty("allScreenshotWritesCompleted", !screenshotWrites.isEmpty()
                && screenshotWrites.stream().allMatch(write -> write.isDone() && !write.isCompletedExceptionally()));
        result.addProperty("fixtureUseClock", "Client-only native startUsingItem plus explicit use-duration state");
        result.add("checks", new com.google.gson.Gson().toJsonTree(checks));
        result.add("screenshots", new com.google.gson.Gson().toJsonTree(captures));
        if (failure != null) result.addProperty("failure", failure.toString());
        Files.writeString(minecraft.gameDirectory.toPath().resolve(filename),
                new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(result) + "\n", StandardCharsets.UTF_8);
    }

    private static void finish(Minecraft minecraft) {
        finished = true;
        if (minecraft.player != null) minecraft.player.stopUsingItem();
        if (Boolean.getBoolean("dragonloot.test.clientSmokeExit")) minecraft.stop();
    }

    private static void record(String check) {
        checks.add(check);
        LOGGER.info("Dragon client smoke: {}", check);
    }

    private static void advance(int next) { state = next; entered = ticks; }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
