package net.dragonloot.gametest;

import net.dragonloot.entity.DragonTridentEntity;
import net.dragonloot.init.EntityInit;
import net.dragonloot.init.ItemInit;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ProjectileItem;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Real registered items, projectile ticks, powered dispensers and native value codecs. */
public final class DragonTridentGameTests {
    private DragonTridentGameTests() {}

    @GameTest
    public static void constructorsRetainIndependentCustomStacks(GameTestHelper helper) {
        ItemStack original = enchanted(helper, "constructor components", Enchantments.LOYALTY, 3);
        original.setDamageValue(13);
        Player owner = player(helper, GameType.SURVIVAL);
        DragonTridentEntity empty = new ProbeTrident(EntityInit.DRAGONTRIDENT_ENTITY.get(), helper.getLevel());
        helper.assertTrue(empty.getWeaponItem().is(ItemInit.DRAGON_TRIDENT_ITEM.get()), "Registry constructor must supply the dragon item");
        DragonTridentEntity thrown = new ProbeTrident(helper.getLevel(), owner, original);
        helper.assertTrue(sameStack(original, thrown.getWeaponItem()), "Owner constructor lost item components");
        original.setDamageValue(14);
        helper.assertValueEqual(thrown.getWeaponItem().getDamageValue(), 13, "Independent projectile stack damage");
        var dispenser = ((ProjectileItem) original.getItem()).asProjectile(helper.getLevel(),
                Vec3.atCenterOf(helper.absolutePos(new BlockPos(1, 2, 1))), original, Direction.NORTH);
        helper.assertTrue(dispenser instanceof DragonTridentEntity, "Dispenser conversion must use the registered dragon projectile");
        helper.assertTrue(sameStack(original, ((DragonTridentEntity) dispenser).getWeaponItem()), "Dispenser conversion lost item components");
        helper.succeed();
    }

    @GameTest
    public static void nineTickChargeDoesNotConsumeOrDamage(GameTestHelper helper) {
        Player owner = player(helper, GameType.SURVIVAL);
        ItemStack stack = stack("short charge");
        owner.setItemInHand(InteractionHand.MAIN_HAND, stack);
        owner.startUsingItem(InteractionHand.MAIN_HAND);
        helper.assertTrue(!release(helper, owner, stack, 9), "Nine ticks must not complete a throw");
        helper.assertTrue(owner.getMainHandItem() == stack, "Short charge removed the held item");
        helper.assertValueEqual(stack.getDamageValue(), 0, "Short charge durability");
        assertNoProjectile(helper, owner);
        helper.succeed();
    }

    @GameTest
    public static void tenTickOffhandThrowConsumesExactlyOnce(GameTestHelper helper) {
        Player owner = player(helper, GameType.SURVIVAL);
        ItemStack stack = stack("offhand throw");
        stack.setDamageValue(7);
        owner.setItemInHand(InteractionHand.OFF_HAND, stack);
        helper.assertTrue(stack.getItem().use(helper.getLevel(), owner, InteractionHand.OFF_HAND).consumesAction(), "Offhand use must start charging");
        helper.assertTrue(release(helper, owner, stack, 10), "Ten ticks must complete a throw");
        var projectiles = projectiles(helper, owner);
        helper.assertValueEqual(projectiles.size(), 1, "Offhand throw projectile count");
        DragonTridentEntity projectile = projectiles.getFirst();
        helper.assertTrue(projectile.getOwner() == owner, "Throw lost its owner");
        helper.assertValueEqual(projectile.getWeaponItem().getDamageValue(), 8, "Single throw durability");
        helper.assertValueEqual(projectile.getWeaponItem().getCount(), 1, "Projectile item count");
        helper.assertTrue(owner.getOffhandItem().isEmpty(), "Survival throw retained a second offhand item");
        projectile.discard();
        helper.succeed();
    }

    @GameTest
    public static void almostBrokenDuringChargeRejectsRelease(GameTestHelper helper) {
        Player owner = player(helper, GameType.SURVIVAL);
        ItemStack stack = stack("charge durability guard");
        owner.setItemInHand(InteractionHand.OFF_HAND, stack);
        helper.assertTrue(stack.getItem().use(helper.getLevel(), owner, InteractionHand.OFF_HAND).consumesAction(), "Undamaged item must start charging");
        stack.setDamageValue(stack.getMaxDamage() - 1);
        helper.assertTrue(!release(helper, owner, stack, 10), "An almost broken item must reject release");
        helper.assertTrue(owner.getOffhandItem() == stack, "Guarded release removed the item");
        helper.assertValueEqual(stack.getDamageValue(), stack.getMaxDamage() - 1, "Guarded release durability");
        assertNoProjectile(helper, owner);
        helper.succeed();
    }

    @GameTest
    public static void creativeThrowAndPickupDoNotDuplicate(GameTestHelper helper) {
        Player owner = player(helper, GameType.CREATIVE);
        ItemStack stack = stack("creative throw");
        stack.setDamageValue(7);
        owner.setItemInHand(InteractionHand.MAIN_HAND, stack);
        owner.startUsingItem(InteractionHand.MAIN_HAND);
        helper.assertTrue(release(helper, owner, stack, 10), "Creative throw must complete");
        var projectiles = projectiles(helper, owner);
        helper.assertValueEqual(projectiles.size(), 1, "Creative throw projectile count");
        DragonTridentEntity projectile = projectiles.getFirst();
        helper.assertTrue(projectile.pickup == AbstractArrow.Pickup.CREATIVE_ONLY, "Creative projectile must require creative pickup");
        helper.assertTrue(owner.getMainHandItem() == stack, "Creative throw removed the held item");
        helper.assertValueEqual(stack.getDamageValue(), 7, "Creative throw durability");
        projectile.setNoPhysics(true);
        projectile.playerTouch(owner);
        helper.assertTrue(projectile.isRemoved(), "Creative owner must collect its projectile");
        helper.assertValueEqual(playerTridentCount(owner), 1, "Creative pickup must not create another item");
        helper.succeed();
    }

    @GameTest
    public static void nativeSaveRetainsOwnerLoyaltyFoilAndHitGuard(GameTestHelper helper) {
        Player owner = player(helper, GameType.SURVIVAL);
        helper.getLevel().addFreshEntity(owner);
        ItemStack original = enchanted(helper, "saved native trident", Enchantments.LOYALTY, 3);
        original.setDamageValue(13);
        DragonTridentEntity projectile = new ProbeTrident(helper.getLevel(), owner, original);
        var target = helper.spawn(EntityType.PIG, new BlockPos(3, 2, 3));
        projectile.setDeltaMovement(0, 0, 1);
        ((ProbeTrident) projectile).hit(target);
        CompoundTag saved = save(helper, projectile);
        helper.assertTrue(saved.getBooleanOr("DealtDamage", false), "Successful hit must be saved");
        helper.assertTrue(saved.contains("Owner"), "Native projectile owner reference must be saved");
        DragonTridentEntity loaded = load(helper, saved);
        helper.assertTrue(sameStack(original, loaded.getWeaponItem()), "Native value codecs lost damage/name/enchantments");
        helper.assertTrue(loaded.isFoil(), "Reload lost enchanted projectile appearance");
        helper.assertTrue(loaded.getOwner() == owner, "Reload lost the registered native owner reference");
        loaded.setPos(owner.getX() + 4, owner.getEyeY(), owner.getZ());
        loaded.tick();
        helper.assertTrue(loaded.returnTimer > 0 && loaded.isNoPhysics(), "Reloaded Loyalty must begin returning");
        helper.assertTrue(((ProbeTrident) loaded).hits(loaded.position(), loaded.position().add(0, 0, 10)).isEmpty(), "Reloaded hit guard must reject collection collisions");
        owner.getInventory().clearContent();
        loaded.playerTouch(owner);
        helper.assertTrue(loaded.isRemoved(), "Returning owner must collect the saved projectile");
        helper.assertTrue(owner.getInventory().getNonEquipmentItems().stream().anyMatch(item -> sameStack(original, item)), "Pickup did not restore the original stack");
        owner.discard();
        target.discard();
        helper.succeed();
    }

    @GameTest
    public static void legacyTridentFieldMigratesWithoutOverridingCurrentItem(GameTestHelper helper) {
        ItemStack original = enchanted(helper, "legacy trident", Enchantments.LOYALTY, 2);
        original.setDamageValue(31);
        CompoundTag legacy = save(helper, new ProbeTrident(helper.getLevel(), player(helper, GameType.SURVIVAL), original));
        legacy.put("Trident", legacy.remove("item"));
        DragonTridentEntity loaded = load(helper, legacy);
        helper.assertTrue(sameStack(original, loaded.getWeaponItem()) && loaded.isFoil(), "Legacy Trident storage must restore its real item components");

        ItemStack current = stack("current authoritative item");
        current.setDamageValue(17);
        CompoundTag both = save(helper, new ProbeTrident(helper.getLevel(), player(helper, GameType.SURVIVAL), current));
        both.put("Trident", legacy.get("Trident"));
        helper.assertTrue(sameStack(current, load(helper, both).getWeaponItem()), "Legacy field must not overwrite current native item storage");
        helper.succeed();
    }

    @GameTest(timeoutTicks = 30)
    public static void naturalSweepDamagesOnlyOneTarget(GameTestHelper helper) {
        // Both targets must remain inside the 4x4x4 template, away from its
        // enclosing barrier; an outside target otherwise takes suffocation damage.
        var first = helper.spawn(EntityType.PIG, new Vec3(2.5, 2, 2.2));
        var second = helper.spawn(EntityType.PIG, new Vec3(2.5, 2, 3.2));
        first.setNoAi(true);
        second.setNoAi(true);
        first.setNoGravity(true);
        second.setNoGravity(true);
        helper.assertTrue(helper.getLevel().noCollision(first) && helper.getLevel().noCollision(second), "Sweep targets must not overlap the fixture barrier");
        float firstHealth = first.getHealth();
        float secondHealth = second.getHealth();
        Player owner = player(helper, GameType.SURVIVAL);
        DragonTridentEntity projectile = new ProbeTrident(helper.getLevel(), owner, stack("native sweep collision"));
        projectile.setPos(helper.absoluteVec(new Vec3(2.5, 2.45, 1)));
        projectile.shoot(0, 0, 1, 5, 0);
        helper.getLevel().addFreshEntity(projectile);
        helper.runAfterDelay(3, () -> {
            helper.assertValueEqual(firstHealth - first.getHealth(), 8.0F, "Natural projectile impact damage");
            helper.assertValueEqual(second.getHealth(), secondHealth, "Second target in the same sweep must remain untouched");
            helper.assertTrue(save(helper, projectile).getBooleanOr("DealtDamage", false), "Natural collision must save its hit state");
            helper.assertTrue(((ProbeTrident) projectile).hits(projectile.position(), projectile.position().add(0, 0, 10)).isEmpty(), "Native collection collision hook must honor the hit guard");
            first.discard();
            second.discard();
            projectile.discard();
            helper.succeed();
        });
    }

    @GameTest(timeoutTicks = 30)
    public static void poweredDispenserLaunchesRegisteredProjectile(GameTestHelper helper) {
        BlockPos position = new BlockPos(1, 2, 1);
        helper.setBlock(position, Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING, Direction.EAST));
        DispenserBlockEntity dispenser = helper.getBlockEntity(position, DispenserBlockEntity.class);
        ItemStack original = enchanted(helper, "powered dispenser", Enchantments.IMPALING, 2);
        original.setDamageValue(11);
        dispenser.setItem(0, original.copy());
        helper.setBlock(position.below(), Blocks.REDSTONE_BLOCK);
        helper.runAfterDelay(5, () -> {
            var projectiles = helper.getLevel().getEntities(EntityInit.DRAGONTRIDENT_ENTITY.get(),
                    new AABB(helper.absolutePos(position)).inflate(10), entity -> sameStack(original, entity.getWeaponItem()));
            helper.assertValueEqual(projectiles.size(), 1, "Powered dispenser projectile count");
            DragonTridentEntity projectile = projectiles.getFirst();
            helper.assertTrue(dispenser.getItem(0).isEmpty(), "Dispenser must consume exactly one trident");
            helper.assertTrue(projectile.getOwner() == null, "Dispenser launch must have no player owner");
            helper.assertTrue(projectile.pickup == AbstractArrow.Pickup.ALLOWED, "Dispenser launch must permit survival pickup");
            helper.assertTrue(projectile.getX() > helper.absolutePos(position).getX() + 1, "Powered dispenser must launch in its facing direction");
            projectile.discard();
            helper.succeed();
        });
    }

    @GameTest
    public static void impalingUsesLoadedAquaticPredicate(GameTestHelper helper) {
        var aquatic = helper.spawn(EntityType.GUARDIAN, new BlockPos(1, 2, 1));
        var land = helper.spawn(EntityType.PIG, new BlockPos(4, 2, 4));
        ItemStack enchanted = enchanted(helper, "native impaling", Enchantments.IMPALING, 5);
        helper.assertValueEqual(hitDamage(helper, aquatic, enchanted), 20.5F, "Impaling V aquatic damage");
        helper.assertValueEqual(hitDamage(helper, land, enchanted), 8.0F, "Impaling unaffected land damage");
        aquatic.discard();
        land.discard();
        helper.succeed();
    }

    @GameTest
    public static void wetOffhandRiptideSpinsWithoutProjectile(GameTestHelper helper) {
        assertRiptide(helper, Blocks.WATER, "wet offhand Riptide");
    }

    @GameTest
    public static void lavaRiptideRetainsDragonExtension(GameTestHelper helper) {
        assertRiptide(helper, Blocks.LAVA, "lava Riptide");
    }

    @GameTest
    public static void dryRiptideRejectsUseAndRelease(GameTestHelper helper) {
        Player owner = player(helper, GameType.SURVIVAL);
        helper.assertTrue(!owner.isInWaterOrRain() && !owner.isInLava(), "Dry Riptide fixture must contain no rain, water or lava");
        ItemStack stack = enchanted(helper, "dry Riptide", Enchantments.RIPTIDE, 3);
        owner.setItemInHand(InteractionHand.OFF_HAND, stack);
        helper.assertTrue(stack.getItem().use(helper.getLevel(), owner, InteractionHand.OFF_HAND) == InteractionResult.FAIL, "Dry Riptide must reject starting use");
        owner.startUsingItem(InteractionHand.OFF_HAND);
        helper.assertTrue(!release(helper, owner, stack, 10), "Dry release must not start Riptide");
        helper.assertTrue(!owner.isAutoSpinAttack() && owner.getDeltaMovement().lengthSqr() == 0, "Dry release must not propel the player");
        helper.assertValueEqual(stack.getDamageValue(), 0, "Dry Riptide durability");
        helper.assertTrue(owner.getOffhandItem() == stack, "Dry release must retain the trident");
        assertNoProjectile(helper, owner);
        helper.succeed();
    }

    @GameTest
    public static void returningPickupRejectsOtherOwnerAndFullInventory(GameTestHelper helper) {
        Player owner = player(helper, GameType.SURVIVAL);
        Player other = player(helper, GameType.SURVIVAL);
        ItemStack original = enchanted(helper, "pickup ownership", Enchantments.LOYALTY, 3);
        DragonTridentEntity projectile = new ProbeTrident(helper.getLevel(), owner, original);
        projectile.setNoPhysics(true);
        projectile.playerTouch(other);
        helper.assertTrue(!projectile.isRemoved() && playerTridentCount(other) == 0, "Another player must not collect an owned returning trident");
        for (int slot = 0; slot < 36; slot++) owner.getInventory().setItem(slot, new ItemStack(Items.STONE, 64));
        projectile.playerTouch(owner);
        helper.assertTrue(!projectile.isRemoved(), "Full inventory must leave the returning projectile in the world");
        owner.getInventory().setItem(0, ItemStack.EMPTY);
        projectile.playerTouch(owner);
        helper.assertTrue(projectile.isRemoved() && sameStack(original, owner.getInventory().getItem(0)), "Opening one inventory slot must restore exactly the original stack");
        helper.succeed();
    }

    @GameTest(timeoutTicks = 200)
    public static void channelingNativeEffectsRespectWeatherAndSky(GameTestHelper helper) {
        channelingScenario(helper, 0);
    }

    @GameTest
    public static void dragonBowConsumesArrowAndOneDurability(GameTestHelper helper) {
        Player owner = player(helper, GameType.SURVIVAL);
        ItemStack bow = ItemInit.DRAGON_BOW_ITEM.get().getDefaultInstance();
        bow.setDamageValue(7);
        owner.setItemInHand(InteractionHand.MAIN_HAND, bow);
        owner.getInventory().setItem(9, new ItemStack(Items.ARROW, 3));
        helper.assertTrue(bow.getItem().use(helper.getLevel(), owner, InteractionHand.MAIN_HAND).consumesAction(), "Dragon bow must accept native arrow ammunition");
        helper.assertTrue(bow.getItem().releaseUsing(bow, helper.getLevel(), owner, bow.getUseDuration(owner) - 20), "Fully charged native bow must fire");
        var arrows = helper.getLevel().getEntities(EntityType.ARROW, owner.getBoundingBox().inflate(5), arrow -> arrow.getOwner() == owner);
        helper.assertValueEqual(arrows.size(), 1, "Native Dragon bow arrow count");
        helper.assertValueEqual(owner.getInventory().getItem(9).getCount(), 2, "Bow remaining arrow ammunition");
        helper.assertValueEqual(bow.getDamageValue(), 8, "Bow durability after one shot");
        helper.assertTrue(arrows.getFirst().getDeltaMovement().length() > 2.9, "Fully charged bow must launch its arrow at native power");
        arrows.forEach(net.minecraft.world.entity.Entity::discard);
        helper.succeed();
    }

    @GameTest
    public static void dragonCrossbowChargesAndFiresNativeArrow(GameTestHelper helper) {
        assertCrossbow(helper, false);
    }

    @GameTest
    public static void dragonCrossbowChargesAndFiresNativeFirework(GameTestHelper helper) {
        assertCrossbow(helper, true);
    }

    private static void assertCrossbow(GameTestHelper helper, boolean firework) {
        Player owner = player(helper, GameType.SURVIVAL);
        ItemStack crossbow = ItemInit.DRAGON_CROSSBOW_ITEM.get().getDefaultInstance();
        crossbow.setDamageValue(7);
        owner.setItemInHand(InteractionHand.MAIN_HAND, crossbow);
        ItemStack ammo = new ItemStack(firework ? Items.FIREWORK_ROCKET : Items.ARROW, 3);
        if (firework) owner.setItemInHand(InteractionHand.OFF_HAND, ammo);
        else owner.getInventory().setItem(9, ammo);
        helper.assertTrue(crossbow.getItem().use(helper.getLevel(), owner, InteractionHand.MAIN_HAND).consumesAction(), "Dragon crossbow must start native ammunition charging");
        int charge = net.minecraft.world.item.CrossbowItem.getChargeDuration(crossbow, owner);
        crossbow.getItem().onUseTick(helper.getLevel(), owner, crossbow, crossbow.getUseDuration(owner) - charge);
        helper.assertTrue(net.minecraft.world.item.CrossbowItem.isCharged(crossbow), "Completed native crossbow charge must store its projectile");
        var charged = crossbow.get(net.minecraft.core.component.DataComponents.CHARGED_PROJECTILES);
        helper.assertTrue(charged != null && charged.contains(firework ? Items.FIREWORK_ROCKET : Items.ARROW), "Native charged component must retain the selected ammunition type");
        helper.assertValueEqual(ammo.getCount(), 2, "Crossbow ammunition consumed at charge");
        owner.stopUsingItem();
        helper.assertTrue(crossbow.getItem().use(helper.getLevel(), owner, InteractionHand.MAIN_HAND).consumesAction(), "Charged native Dragon crossbow must fire");
        helper.assertTrue(!net.minecraft.world.item.CrossbowItem.isCharged(crossbow), "Firing must clear the charged component");
        helper.assertValueEqual(ammo.getCount(), 2, "Crossbow ammunition must not be consumed twice");
        helper.assertValueEqual(crossbow.getDamageValue(), firework ? 10 : 8, "Native arrow/firework crossbow durability");
        if (firework) {
            var shots = helper.getLevel().getEntities(EntityType.FIREWORK_ROCKET, owner.getBoundingBox().inflate(5), shot -> shot.getOwner() == owner);
            helper.assertValueEqual(shots.size(), 1, "Native crossbow firework projectile count");
            helper.assertTrue(shots.getFirst().getDeltaMovement().length() > 1.5, "Crossbow firework must have native launch power");
            shots.forEach(net.minecraft.world.entity.Entity::discard);
        } else {
            var shots = helper.getLevel().getEntities(EntityType.ARROW, owner.getBoundingBox().inflate(5), shot -> shot.getOwner() == owner);
            helper.assertValueEqual(shots.size(), 1, "Native crossbow arrow projectile count");
            helper.assertTrue(shots.getFirst().getDeltaMovement().length() > 3, "Crossbow arrow must have native launch power");
            shots.forEach(net.minecraft.world.entity.Entity::discard);
        }
        helper.succeed();
    }

    private static void channelingScenario(GameTestHelper helper, int index) {
        if (index == 6) {
            helper.succeed();
            return;
        }
        var level = helper.getLevel();
        boolean mob = index >= 3;
        int scenario = index % 3;
        boolean storm = scenario != 1;
        boolean covered = scenario == 2;
        BlockPos column = helper.absolutePos(new BlockPos(2, 2, 2));
        int top = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, column.getX(), column.getZ());
        BlockPos position = new BlockPos(column.getX(), Math.min(top + 20, level.getMaxY() - 5), column.getZ());
        var previousBlock = level.getBlockState(position);
        var previousCover = level.getBlockState(position.above(3));
        level.setBlockAndUpdate(position, (mob ? Blocks.AIR : Blocks.LIGHTNING_ROD).defaultBlockState());
        level.setBlockAndUpdate(position.above(3), (covered ? Blocks.STONE : Blocks.AIR).defaultBlockState());
        // Native skylight propagation runs on its lighting executor. Wait for
        // the actual native predicate, bounded by the test timeout, before impact.
        helper.startSequence().thenWaitUntil(() ->
                helper.assertTrue(level.canSeeSky(position) != covered,
                        "Waiting for native Channeling sky exposure (mob=" + mob + ", scenario=" + scenario + ")"))
            .thenExecute(() -> {
            float rain = level.rainLevel, previousRain = level.oRainLevel;
            float thunder = level.thunderLevel, previousThunder = level.oThunderLevel;
            LivingEntity target = null;
            ProbeTrident projectile = null;
            var created = new java.util.ArrayList<net.minecraft.world.entity.LightningBolt>();
            try {
                helper.assertTrue(level.canSeeSky(position) != covered, "Channeling fixture must have the requested sky exposure (mob=" + mob + ", scenario=" + scenario + ")");
                // Weather is changed only within this synchronous callback and
                // restored before another test gets a server tick.
                level.setRainLevel(storm ? 1 : 0);
                level.setThunderLevel(storm ? 1 : 0);
                helper.assertTrue(level.isThundering() == storm, "Channeling fixture must have actual native thunder state");
                Player owner = player(helper, GameType.SURVIVAL);
                projectile = new ProbeTrident(level, owner,
                        enchanted(helper, "native channeling " + mob + scenario, Enchantments.CHANNELING, 1));
                projectile.setPos(Vec3.atCenterOf(position));
                AABB bounds = new AABB(position).inflate(4);
                var before = level.getEntities(EntityType.LIGHTNING_BOLT, bounds, bolt -> true);
                if (mob) {
                    target = EntityType.PIG.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
                    helper.assertTrue(target != null, "Native Channeling target creation failed");
                    target.setPos(Vec3.atBottomCenterOf(position));
                    level.addFreshEntity(target);
                    projectile.hit(target);
                } else {
                    projectile.hitBlock(new BlockHitResult(Vec3.atCenterOf(position), Direction.UP, position, false));
                }
                for (var bolt : level.getEntities(EntityType.LIGHTNING_BOLT, bounds, candidate -> !before.contains(candidate))) created.add(bolt);
                helper.assertValueEqual(created.size(), storm && !covered ? 1 : 0, "New native Channeling lightning count (mob=" + mob + ", scenario=" + scenario + ")");
                for (var bolt : created) helper.assertTrue(bolt.getCause() == owner, "Native Channeling must retain its actual server-player cause");
            } finally {
                created.forEach(net.minecraft.world.entity.LightningBolt::discard);
                if (projectile != null) projectile.discard();
                if (target != null) target.discard();
                level.setBlockAndUpdate(position, previousBlock);
                level.setBlockAndUpdate(position.above(3), previousCover);
                level.rainLevel = rain;
                level.oRainLevel = previousRain;
                level.thunderLevel = thunder;
                level.oThunderLevel = previousThunder;
            }
            channelingScenario(helper, index + 1);
        });
    }

    private static void assertRiptide(GameTestHelper helper, net.minecraft.world.level.block.Block fluid, String name) {
        helper.setBlock(new BlockPos(1, 2, 1), fluid);
        helper.setBlock(new BlockPos(1, 3, 1), fluid);
        Player owner = player(helper, GameType.SURVIVAL);
        owner.baseTick();
        helper.assertTrue(fluid == Blocks.WATER ? owner.isInWater() : owner.isInLava(), "Riptide fixture must detect its actual fluid");
        ItemStack stack = enchanted(helper, name, Enchantments.RIPTIDE, 3);
        stack.setDamageValue(7);
        owner.setItemInHand(InteractionHand.OFF_HAND, stack);
        helper.assertTrue(stack.getItem().use(helper.getLevel(), owner, InteractionHand.OFF_HAND).consumesAction(), "Fluid Riptide must start offhand use");
        helper.assertTrue(release(helper, owner, stack, 10), "Fluid release must start Riptide");
        helper.assertTrue(owner.isAutoSpinAttack() && owner.getDeltaMovement().length() > 2, "Riptide III must set spin state and enchantment-driven propulsion");
        helper.assertValueEqual(stack.getDamageValue(), 8, "Riptide durability");
        helper.assertTrue(owner.getOffhandItem() == stack, "Riptide must retain its offhand item");
        assertNoProjectile(helper, owner);
        helper.succeed();
    }

    private static Player player(GameTestHelper helper, GameType mode) {
        // Native ItemStack.hurtWithoutBreaking deliberately accepts durability
        // changes only for ServerPlayer, so a plain GameTest mock cannot test wear.
        var player = new net.neoforged.neoforge.common.util.FakePlayer(helper.getLevel(),
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "dragon-trident-test"));
        player.setGameMode(mode);
        helper.assertTrue(player.gameMode() == mode && player.hasInfiniteMaterials() == (mode == GameType.CREATIVE), "Native server-player game mode precondition");
        player.setPos(helper.absoluteVec(new Vec3(1.5, 2, 1.5)));
        return player;
    }

    private static ItemStack stack(String name) {
        ItemStack stack = ItemInit.DRAGON_TRIDENT_ITEM.get().getDefaultInstance();
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        return stack;
    }

    private static ItemStack enchanted(GameTestHelper helper, String name, ResourceKey<Enchantment> enchantment, int level) {
        ItemStack stack = stack(name);
        stack.enchant(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(enchantment), level);
        return stack;
    }

    private static boolean release(GameTestHelper helper, Player owner, ItemStack stack, int charge) {
        return stack.getItem().releaseUsing(stack, helper.getLevel(), owner, stack.getUseDuration(owner) - charge);
    }

    private static java.util.List<DragonTridentEntity> projectiles(GameTestHelper helper, Player owner) {
        return helper.getLevel().getEntities(EntityInit.DRAGONTRIDENT_ENTITY.get(), owner.getBoundingBox().inflate(5), entity -> entity.getOwner() == owner);
    }

    private static void assertNoProjectile(GameTestHelper helper, Player owner) {
        helper.assertTrue(projectiles(helper, owner).isEmpty(), "This release must not create a projectile");
    }

    private static boolean sameStack(ItemStack expected, ItemStack actual) {
        return expected.getCount() == actual.getCount() && ItemStack.isSameItemSameComponents(expected, actual);
    }

    private static int playerTridentCount(Player player) {
        int count = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(ItemInit.DRAGON_TRIDENT_ITEM.get())) count += stack.getCount();
        }
        return count;
    }

    private static float hitDamage(GameTestHelper helper, LivingEntity target, ItemStack stack) {
        float initial = target.getHealth();
        DragonTridentEntity projectile = new ProbeTrident(helper.getLevel(), player(helper, GameType.SURVIVAL), stack);
        ((ProbeTrident) projectile).hit(target);
        return initial - target.getHealth();
    }

    private static CompoundTag save(GameTestHelper helper, DragonTridentEntity entity) {
        ProblemReporter.Collector problems = new ProblemReporter.Collector();
        TagValueOutput output = TagValueOutput.createWithContext(problems, helper.getLevel().registryAccess());
        entity.saveWithoutId(output);
        helper.assertTrue(problems.isEmpty(), "Native projectile serialization failed: " + problems.getReport());
        return output.buildResult();
    }

    private static DragonTridentEntity load(GameTestHelper helper, CompoundTag tag) {
        ProblemReporter.Collector problems = new ProblemReporter.Collector();
        DragonTridentEntity loaded = new ProbeTrident(EntityInit.DRAGONTRIDENT_ENTITY.get(), helper.getLevel());
        loaded.load(TagValueInput.create(problems, helper.getLevel().registryAccess(), tag));
        helper.assertTrue(problems.isEmpty(), "Native projectile deserialization failed: " + problems.getReport());
        return loaded;
    }
    /** Exposes protected collision entry points without overlapping the production module package. */
    private static final class ProbeTrident extends DragonTridentEntity {
        private ProbeTrident(EntityType<? extends DragonTridentEntity> type, net.minecraft.world.level.Level level) {
            super(type, level);
        }

        private ProbeTrident(net.minecraft.world.level.Level level, LivingEntity owner, ItemStack item) {
            super(level, owner, item);
        }

        private void hit(LivingEntity target) {
            super.onHitEntity(new EntityHitResult(target));
        }

        private void hitBlock(BlockHitResult result) {
            super.onHitBlock(result);
        }

        private java.util.Collection<EntityHitResult> hits(Vec3 from, Vec3 to) {
            return super.findHitEntities(from, to);
        }
    }

}
