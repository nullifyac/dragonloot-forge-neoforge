package net.dragonloot.entity;

import net.dragonloot.DragonLootMain;
import net.dragonloot.init.EntityInit;
import net.dragonloot.init.ItemInit;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(DragonLootMain.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DragonProjectileBehaviorGameTests {
    private DragonProjectileBehaviorGameTests() {
    }

    @GameTest(template = "empty")
    public static void damagedDuringChargeCannotThrow(GameTestHelper helper) {
        Player owner = player(helper, GameType.SURVIVAL);
        ItemStack stack = stack("charged durability guard");
        owner.setItemInHand(InteractionHand.OFF_HAND, stack);
        helper.assertTrue(stack.getItem().use(helper.getLevel(), owner, InteractionHand.OFF_HAND).getResult().consumesAction(),
            "An undamaged trident must start charging");
        stack.setDamageValue(stack.getMaxDamage() - 1);
        release(helper, owner, stack);
        helper.assertTrue(owner.getOffhandItem() == stack, "Damage during charging must leave the trident in the offhand");
        helper.assertValueEqual(stack.getDamageValue(), stack.getMaxDamage() - 1, "durability after the guarded release");
        assertNoProjectile(helper, owner);
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 30)
    public static void poweredDispenserLaunchesCustomProjectile(GameTestHelper helper) {
        BlockPos relative = new BlockPos(1, 2, 1);
        helper.setBlock(relative, Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING, Direction.EAST));
        DispenserBlockEntity dispenser = helper.getBlockEntity(relative);
        ItemStack original = enchanted(helper, "actual dispenser", Enchantments.IMPALING, 2);
        original.setDamageValue(11);
        dispenser.setItem(0, original.copy());
        helper.setBlock(relative.below(), Blocks.REDSTONE_BLOCK);
        helper.runAfterDelay(5, () -> {
            var projectiles = helper.getLevel().getEntities(EntityInit.DRAGONTRIDENT_ENTITY.get(),
                new AABB(helper.absolutePos(relative)).inflate(8), projectile -> sameStack(original, projectile.getWeaponItem()));
            helper.assertValueEqual(projectiles.size(), 1, "projectiles launched by the powered dispenser");
            DragonTridentEntity projectile = projectiles.getFirst();
            helper.assertTrue(dispenser.getItem(0).isEmpty(), "Dispenser must consume exactly the launched trident");
            helper.assertTrue(projectile.getOwner() == null, "A dispenser projectile must have no player owner");
            helper.assertTrue(projectile.pickup == AbstractArrow.Pickup.ALLOWED, "Dispenser projectile must be collectible");
            helper.assertTrue(projectile.getX() > helper.absolutePos(relative).getX() + 1,
                "Registered dispenser behavior must launch the projectile in its facing direction");
            helper.assertValueEqual(projectile.getWeaponItem().getDamageValue(), 11, "dispenser must retain original item damage");
            projectile.discard();
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 30)
    public static void naturalTrajectoryCollidesAndDamagesTarget(GameTestHelper helper) {
        var target = helper.spawn(EntityType.PIG, new BlockPos(1, 2, 3));
        target.setNoAi(true);
        target.setNoGravity(true);
        float initialHealth = target.getHealth();
        Player owner = player(helper, GameType.SURVIVAL);
        DragonTridentEntity projectile = new DragonTridentEntity(helper.getLevel(), owner, stack("natural collision"));
        Vec3 start = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(1, 2, 1))).add(0, 0.45, 0);
        projectile.setPos(start);
        projectile.shoot(0, 0, 1, 0.75F, 0);
        helper.getLevel().addFreshEntity(projectile);
        helper.runAfterDelay(8, () -> {
            helper.assertTrue(target.getHealth() < initialHealth, "Normal entity ticks must detect collision and deal damage");
            CompoundTag saved = new CompoundTag();
            projectile.addAdditionalSaveData(saved);
            helper.assertTrue(saved.getBoolean("DealtDamage"), "A real collision must mark the trident as already having hit");
            target.discard();
            projectile.discard();
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void impalingAddsDamageOnlyToAffectedAquaticTarget(GameTestHelper helper) {
        var aquatic = helper.spawn(EntityType.GUARDIAN, new BlockPos(1, 2, 1));
        var land = helper.spawn(EntityType.PIG, new BlockPos(3, 2, 3));
        ItemStack stack = enchanted(helper, "impaling damage", Enchantments.IMPALING, 5);
        float aquaticDamage = hitDamage(helper, aquatic, stack);
        float landDamage = hitDamage(helper, land, stack);
        helper.assertTrue(Math.abs(aquaticDamage - 20.5F) < 0.001F,
            "Impaling V must add 12.5 damage to a guardian using the loaded enchantment predicate");
        helper.assertTrue(Math.abs(landDamage - 8) < 0.001F, "Impaling must leave an unaffected pig at the base projectile damage");
        aquatic.discard();
        land.discard();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void wetOffhandRiptideSpinsWithoutThrowing(GameTestHelper helper) {
        helper.setBlock(new BlockPos(1, 2, 1), Blocks.WATER);
        helper.setBlock(new BlockPos(1, 3, 1), Blocks.WATER);
        Player owner = player(helper, GameType.SURVIVAL);
        owner.baseTick();
        helper.assertTrue(owner.isInWater(), "Riptide fixture must put the player in actual water");
        ItemStack stack = enchanted(helper, "wet offhand riptide", Enchantments.RIPTIDE, 3);
        stack.setDamageValue(7);
        owner.setItemInHand(InteractionHand.OFF_HAND, stack);
        helper.assertTrue(stack.getItem().use(helper.getLevel(), owner, InteractionHand.OFF_HAND).getResult().consumesAction(),
            "Wet Riptide must start charging in the offhand");
        release(helper, owner, stack);
        helper.assertTrue(owner.isAutoSpinAttack(), "Wet Riptide must start the vanilla spin attack state");
        helper.assertTrue(owner.getDeltaMovement().length() > 2, "Riptide III must propel the player with enchantment-driven strength");
        helper.assertValueEqual(stack.getDamageValue(), 8, "Riptide must consume one durability");
        helper.assertTrue(owner.getOffhandItem() == stack, "Riptide must retain the offhand trident");
        assertNoProjectile(helper, owner);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void dryRiptideRejectsUseAndRelease(GameTestHelper helper) {
        Player owner = player(helper, GameType.SURVIVAL);
        ItemStack stack = enchanted(helper, "dry riptide", Enchantments.RIPTIDE, 3);
        owner.setItemInHand(InteractionHand.OFF_HAND, stack);
        try (WeatherState ignored = weather(helper, false)) {
            helper.assertTrue(!owner.isInWaterRainOrBubble(), "Dry Riptide fixture must have no water or rain");
            helper.assertTrue(stack.getItem().use(helper.getLevel(), owner, InteractionHand.OFF_HAND).getResult() == InteractionResult.FAIL,
                "Dry Riptide must reject starting use");
            // Simulate a charge that started wet, but was released after the player became dry.
            owner.startUsingItem(InteractionHand.OFF_HAND);
            release(helper, owner, stack);
            helper.assertTrue(!owner.isAutoSpinAttack(), "A dry release must not start a spin attack");
            helper.assertValueEqual(stack.getDamageValue(), 0, "a dry Riptide release must not consume durability");
            helper.assertTrue(owner.getDeltaMovement().lengthSqr() == 0, "A dry release must not propel the player");
            helper.assertTrue(owner.getOffhandItem() == stack, "A dry release must retain the item");
            assertNoProjectile(helper, owner);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 30)
    public static void channelingExposedRodSummonsExactlyOneBolt(GameTestHelper helper) {
        channeling(helper, true, false, false);
    }

    @GameTest(template = "empty", timeoutTicks = 30)
    public static void channelingExposedMobSummonsExactlyOneBolt(GameTestHelper helper) {
        channeling(helper, true, false, true);
    }

    @GameTest(template = "empty", timeoutTicks = 30)
    public static void channelingDryRodSummonsNoBolt(GameTestHelper helper) {
        channeling(helper, false, false, false);
    }

    @GameTest(template = "empty", timeoutTicks = 30)
    public static void channelingDryMobSummonsNoBolt(GameTestHelper helper) {
        channeling(helper, false, false, true);
    }

    @GameTest(template = "empty", timeoutTicks = 30)
    public static void channelingCoveredRodSummonsNoBolt(GameTestHelper helper) {
        channeling(helper, true, true, false);
    }

    @GameTest(template = "empty", timeoutTicks = 30)
    public static void channelingCoveredMobSummonsNoBolt(GameTestHelper helper) {
        channeling(helper, true, true, true);
    }

    @GameTest(template = "empty")
    public static void creativeThrowAndPickupDoesNotDuplicateItem(GameTestHelper helper) {
        Player owner = player(helper, GameType.CREATIVE);
        ItemStack stack = stack("creative non-duplication");
        stack.setDamageValue(7);
        owner.setItemInHand(InteractionHand.MAIN_HAND, stack);
        owner.startUsingItem(InteractionHand.MAIN_HAND);
        release(helper, owner, stack);
        var projectiles = ownedProjectiles(helper, owner);
        helper.assertValueEqual(projectiles.size(), 1, "creative thrown projectile count");
        DragonTridentEntity projectile = projectiles.getFirst();
        helper.assertTrue(projectile.pickup == AbstractArrow.Pickup.CREATIVE_ONLY, "Creative throws must use creative-only pickup");
        helper.assertTrue(owner.getMainHandItem() == stack, "Creative throw must retain the held item");
        helper.assertValueEqual(stack.getDamageValue(), 7, "creative throw must not damage the item");
        projectile.setNoPhysics(true);
        projectile.playerTouch(owner);
        helper.assertTrue(projectile.isRemoved(), "Creative owner must be able to clear their returning projectile");
        helper.assertValueEqual(countDragonTridents(owner), 1, "creative pickup must not duplicate the retained item");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void returningProjectileRejectsOtherOwnerAndFullInventory(GameTestHelper helper) {
        Player owner = player(helper, GameType.SURVIVAL);
        Player other = player(helper, GameType.SURVIVAL);
        ItemStack original = enchanted(helper, "restricted pickup", Enchantments.LOYALTY, 3);
        DragonTridentEntity projectile = new DragonTridentEntity(helper.getLevel(), owner, original);
        projectile.pickup = AbstractArrow.Pickup.ALLOWED;
        projectile.setNoPhysics(true);
        projectile.playerTouch(other);
        helper.assertTrue(!projectile.isRemoved(), "A different player must not collect an owned returning trident");
        helper.assertValueEqual(countDragonTridents(other), 0, "another player's inventory must remain unchanged");
        for (int slot = 0; slot < owner.getInventory().items.size(); slot++) {
            owner.getInventory().items.set(slot, new ItemStack(Items.COBBLESTONE, 64));
        }
        projectile.playerTouch(owner);
        helper.assertTrue(!projectile.isRemoved(), "A full inventory must leave the returning projectile available");
        owner.getInventory().items.set(0, ItemStack.EMPTY);
        projectile.playerTouch(owner);
        helper.assertTrue(projectile.isRemoved(), "Freeing an inventory slot must allow the owner to collect it");
        helper.assertValueEqual(countDragonTridents(owner), 1, "owner must receive exactly one trident");
        helper.assertTrue(owner.getInventory().items.stream().anyMatch(item -> sameStack(original, item)),
            "Pickup after a full inventory must retain every original component");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void loyaltyDropsOriginalItemWhenOwnerDies(GameTestHelper helper) {
        Player owner = player(helper, GameType.SURVIVAL);
        ItemStack original = enchanted(helper, "dead owner drop", Enchantments.LOYALTY, 3);
        original.setDamageValue(17);
        DragonTridentEntity projectile = new DragonTridentEntity(helper.getLevel(), owner, original);
        projectile.setPos(owner.position());
        projectile.pickup = AbstractArrow.Pickup.ALLOWED;
        projectile.setNoPhysics(true);
        owner.setHealth(0);
        projectile.tick();
        helper.assertTrue(projectile.isRemoved(), "Loyalty must discard its projectile after the owner dies");
        var drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, owner.getBoundingBox().inflate(2),
            item -> sameStack(original, item.getItem()));
        helper.assertValueEqual(drops.size(), 1, "dead owner's loyalty item drop count");
        helper.assertValueEqual(drops.getFirst().getItem().getCount(), 1, "dead owner's dropped item count");
        drops.forEach(ItemEntity::discard);
        helper.succeed();
    }

    private static void channeling(GameTestHelper helper, boolean storm, boolean covered, boolean mob) {
        BlockPos relative = new BlockPos(2, 2, 2);
        BlockPos absolute = helper.absolutePos(relative);
        for (BlockPos above = absolute.above(); above.getY() < helper.getLevel().getMaxBuildHeight(); above = above.above()) {
            helper.getLevel().setBlockAndUpdate(above, Blocks.AIR.defaultBlockState());
        }
        if (covered) {
            helper.getLevel().setBlockAndUpdate(absolute.above(3), Blocks.STONE.defaultBlockState());
        }
        if (!mob) {
            helper.setBlock(relative, Blocks.LIGHTNING_ROD);
        }
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(helper.getLevel().canSeeSky(absolute) != covered,
                "Channeling fixture must have the requested sky exposure");
            ItemStack stack = enchanted(helper, "channeling " + storm + covered + mob, Enchantments.CHANNELING, 1);
            BlockImpactTrident projectile = new BlockImpactTrident(helper.getLevel(), player(helper, GameType.SURVIVAL), stack);
            projectile.setPos(Vec3.atCenterOf(absolute));
            AABB bounds = new AABB(absolute).inflate(2);
            var before = helper.getLevel().getEntities(EntityType.LIGHTNING_BOLT, bounds, bolt -> true);
            LivingEntity target = mob ? helper.spawn(EntityType.PIG, relative) : null;
            try (WeatherState ignored = weather(helper, storm)) {
                if (mob) {
                    projectile.onHitEntity(new EntityHitResult(target));
                } else {
                    projectile.hitBlock(new BlockHitResult(Vec3.atCenterOf(absolute), Direction.UP, absolute, false));
                }
                var after = helper.getLevel().getEntities(EntityType.LIGHTNING_BOLT, bounds, bolt -> true);
                helper.assertValueEqual(after.size() - before.size(), storm && !covered ? 1 : 0,
                    "new Channeling lightning bolts");
                after.stream().filter(bolt -> !before.contains(bolt)).forEach(bolt -> bolt.discard());
            } finally {
                if (target != null) {
                    target.discard();
                }
                projectile.discard();
            }
            helper.succeed();
        });
    }

    private static Player player(GameTestHelper helper, GameType mode) {
        Player player = helper.makeMockPlayer(mode);
        player.getAbilities().instabuild = mode.isCreative();
        player.setPos(Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(1, 2, 1))));
        return player;
    }

    private static ItemStack stack(String name) {
        ItemStack stack = ItemInit.DRAGON_TRIDENT_ITEM.get().getDefaultInstance();
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        return stack;
    }

    private static ItemStack enchanted(GameTestHelper helper, String name, ResourceKey<Enchantment> enchantment, int level) {
        ItemStack stack = stack(name);
        stack.enchant(helper.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(enchantment), level);
        return stack;
    }

    private static void release(GameTestHelper helper, Player owner, ItemStack stack) {
        stack.getItem().releaseUsing(stack, helper.getLevel(), owner, stack.getUseDuration(owner) - 10);
    }

    private static java.util.List<DragonTridentEntity> ownedProjectiles(GameTestHelper helper, Player owner) {
        return helper.getLevel().getEntities(EntityInit.DRAGONTRIDENT_ENTITY.get(), owner.getBoundingBox().inflate(4),
            projectile -> projectile.getOwner() == owner);
    }

    private static void assertNoProjectile(GameTestHelper helper, Player owner) {
        helper.assertTrue(ownedProjectiles(helper, owner).isEmpty(), "This release must not spawn a projectile");
    }

    private static int countDragonTridents(Player player) {
        return player.getInventory().items.stream().filter(item -> item.is(ItemInit.DRAGON_TRIDENT_ITEM.get()))
            .mapToInt(ItemStack::getCount).sum();
    }

    private static boolean sameStack(ItemStack first, ItemStack second) {
        return ItemStack.isSameItemSameComponents(first, second);
    }

    private static float hitDamage(GameTestHelper helper, LivingEntity target, ItemStack stack) {
        float health = target.getHealth();
        DragonTridentEntity projectile = new DragonTridentEntity(helper.getLevel(), player(helper, GameType.SURVIVAL), stack);
        projectile.onHitEntity(new EntityHitResult(target));
        projectile.discard();
        return health - target.getHealth();
    }

    private static WeatherState weather(GameTestHelper helper, boolean storm) {
        return new WeatherState(helper.getLevel(), storm);
    }

    // Expose the inherited protected block-impact entry point only to this test suite.
    private static final class BlockImpactTrident extends DragonTridentEntity {
        private BlockImpactTrident(ServerLevel level, Player owner, ItemStack stack) {
            super(level, owner, stack);
        }

        private void hitBlock(BlockHitResult result) {
            super.onHitBlock(result);
        }
    }

    private static final class WeatherState implements AutoCloseable {
        private final ServerLevel level;
        private final float rain;
        private final float previousRain;
        private final float thunder;
        private final float previousThunder;

        private WeatherState(ServerLevel level, boolean storm) {
            this.level = level;
            rain = level.rainLevel;
            previousRain = level.oRainLevel;
            thunder = level.thunderLevel;
            previousThunder = level.oThunderLevel;
            level.setRainLevel(storm ? 1 : 0);
            level.setThunderLevel(storm ? 1 : 0);
        }

        @Override
        public void close() {
            level.rainLevel = rain;
            level.oRainLevel = previousRain;
            level.thunderLevel = thunder;
            level.oThunderLevel = previousThunder;
        }
    }
}
