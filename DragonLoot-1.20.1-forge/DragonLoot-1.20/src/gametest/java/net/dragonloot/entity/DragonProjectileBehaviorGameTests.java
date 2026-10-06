package net.dragonloot.entity;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.dragonloot.DragonLootMain;
import net.dragonloot.init.EntityInit;
import net.dragonloot.init.ItemInit;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(DragonLootMain.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DragonProjectileBehaviorGameTests {
    private DragonProjectileBehaviorGameTests() {
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
            check(helper, target.getHealth() < initialHealth, "Normal entity ticks must detect collision and deal damage");
            CompoundTag saved = new CompoundTag();
            projectile.addAdditionalSaveData(saved);
            check(helper, saved.getBoolean("DealtDamage"), "A real collision must mark the trident as already having hit");
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
        check(helper, Math.abs(aquaticDamage - 20.5F) < 0.001F,
            "Impaling V must add 12.5 damage to a guardian using the loaded enchantment predicate");
        check(helper, Math.abs(landDamage - 8) < 0.001F, "Impaling must leave an unaffected pig at the base projectile damage");
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
        check(helper, owner.isInWater(), "Riptide fixture must put the player in actual water");
        ItemStack stack = enchanted(helper, "wet offhand riptide", Enchantments.RIPTIDE, 3);
        stack.setDamageValue(7);
        owner.setItemInHand(InteractionHand.OFF_HAND, stack);
        check(helper, stack.getItem().use(helper.getLevel(), owner, InteractionHand.OFF_HAND).getResult().consumesAction(),
            "Wet Riptide must start charging in the offhand");
        release(helper, owner, stack);
        check(helper, owner.isAutoSpinAttack(), "Wet Riptide must start the vanilla spin attack state");
        check(helper, owner.getDeltaMovement().length() > 2, "Riptide III must propel the player with enchantment-driven strength");
        assertEqual(helper, stack.getDamageValue(), 8, "Riptide must consume one durability");
        check(helper, owner.getOffhandItem() == stack, "Riptide must retain the offhand trident");
        assertNoProjectile(helper, owner);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void dryRiptideRejectsUseAndRelease(GameTestHelper helper) {
        Player owner = player(helper, GameType.SURVIVAL);
        ItemStack stack = enchanted(helper, "dry riptide", Enchantments.RIPTIDE, 3);
        owner.setItemInHand(InteractionHand.OFF_HAND, stack);
        try (WeatherState ignored = weather(helper, false)) {
            check(helper, !owner.isInWaterRainOrBubble(), "Dry Riptide fixture must have no water or rain");
            check(helper, stack.getItem().use(helper.getLevel(), owner, InteractionHand.OFF_HAND).getResult() == InteractionResult.FAIL,
                "Dry Riptide must reject starting use");
            // Simulate a charge that started wet, but was released after the player became dry.
            owner.startUsingItem(InteractionHand.OFF_HAND);
            release(helper, owner, stack);
            check(helper, !owner.isAutoSpinAttack(), "A dry release must not start a spin attack");
            assertEqual(helper, stack.getDamageValue(), 0, "a dry Riptide release must not consume durability");
            check(helper, owner.getDeltaMovement().lengthSqr() == 0, "A dry release must not propel the player");
            check(helper, owner.getOffhandItem() == stack, "A dry release must retain the item");
            assertNoProjectile(helper, owner);
        }
        helper.succeed();
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
        assertEqual(helper, projectiles.size(), 1, "creative thrown projectile count");
        DragonTridentEntity projectile = projectiles.get(0);
        check(helper, projectile.pickup == AbstractArrow.Pickup.CREATIVE_ONLY, "Creative throws must use creative-only pickup");
        check(helper, owner.getMainHandItem() == stack, "Creative throw must retain the held item");
        assertEqual(helper, stack.getDamageValue(), 7, "creative throw must not damage the item");
        projectile.setNoPhysics(true);
        projectile.playerTouch(owner);
        check(helper, projectile.isRemoved(), "Creative owner must be able to clear their returning projectile");
        assertEqual(helper, countDragonTridents(owner), 1, "creative pickup must not duplicate the retained item");
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
        check(helper, !projectile.isRemoved(), "A different player must not collect an owned returning trident");
        assertEqual(helper, countDragonTridents(other), 0, "another player's inventory must remain unchanged");
        for (int slot = 0; slot < owner.getInventory().items.size(); slot++) {
            owner.getInventory().items.set(slot, new ItemStack(Items.COBBLESTONE, 64));
        }
        projectile.playerTouch(owner);
        check(helper, !projectile.isRemoved(), "A full inventory must leave the returning projectile available");
        owner.getInventory().items.set(0, ItemStack.EMPTY);
        projectile.playerTouch(owner);
        check(helper, projectile.isRemoved(), "Freeing an inventory slot must allow the owner to collect it");
        assertEqual(helper, countDragonTridents(owner), 1, "owner must receive exactly one trident");
        check(helper, owner.getInventory().items.stream().anyMatch(item -> sameStack(original, item)),
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
        check(helper, projectile.isRemoved(), "Loyalty must discard its projectile after the owner dies");
        var drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, owner.getBoundingBox().inflate(2),
            item -> sameStack(original, item.getItem()));
        assertEqual(helper, drops.size(), 1, "dead owner's loyalty item drop count");
        assertEqual(helper, drops.get(0).getItem().getCount(), 1, "dead owner's dropped item count");
        drops.forEach(ItemEntity::discard);
        helper.succeed();
    }

    private static void channeling(GameTestHelper helper, boolean storm, boolean covered, boolean mob) {
        BlockPos column = helper.absolutePos(new BlockPos(2, 2, 2));
        int surface = helper.getLevel().getHeight(Heightmap.Types.MOTION_BLOCKING, column.getX(), column.getZ());
        BlockPos absolute = new BlockPos(column.getX(), Math.max(column.getY(), surface) + 4, column.getZ());
        check(helper, absolute.getY() < helper.getLevel().getMaxBuildHeight() - 4, "Channeling fixture must fit above the highest obstruction");
        BlockState previousBlock = helper.getLevel().getBlockState(absolute);
        BlockState previousRoof = helper.getLevel().getBlockState(absolute.above(3));
        if (covered) {
            helper.getLevel().setBlockAndUpdate(absolute.above(3), Blocks.STONE.defaultBlockState());
        }
        if (!mob) {
            helper.getLevel().setBlockAndUpdate(absolute, Blocks.LIGHTNING_ROD.defaultBlockState());
        }
        helper.runAfterDelay(10, () -> {
            ItemStack stack = enchanted(helper, "channeling " + storm + covered + mob, Enchantments.CHANNELING, 1);
            BlockImpactTrident projectile = new BlockImpactTrident(helper.getLevel(), player(helper, GameType.SURVIVAL), stack);
            projectile.setPos(Vec3.atCenterOf(absolute));
            AABB bounds = new AABB(absolute).inflate(2);
            var before = helper.getLevel().getEntities(EntityType.LIGHTNING_BOLT, bounds, bolt -> true);
            LivingEntity target = mob ? EntityType.PIG.create(helper.getLevel()) : null;
            try (WeatherState ignored = weather(helper, storm)) {
                check(helper, helper.getLevel().canSeeSky(absolute) != covered,
                    "Channeling fixture must have the requested sky exposure");
                if (mob) {
                    check(helper, target != null, "Channeling target must be created");
                    target.moveTo(Vec3.atBottomCenterOf(absolute));
                    helper.getLevel().addFreshEntity(target);
                    projectile.onHitEntity(new EntityHitResult(target));
                } else {
                    projectile.hitBlock(new BlockHitResult(Vec3.atCenterOf(absolute), Direction.UP, absolute, false));
                }
                var after = helper.getLevel().getEntities(EntityType.LIGHTNING_BOLT, bounds, bolt -> true);
                assertEqual(helper, after.size() - before.size(), storm && !covered ? 1 : 0, "new Channeling lightning bolts");
                after.stream().filter(bolt -> !before.contains(bolt)).forEach(bolt -> bolt.discard());
            } finally {
                if (target != null) { target.discard(); }
                projectile.discard();
                helper.getLevel().setBlockAndUpdate(absolute, previousBlock);
                helper.getLevel().setBlockAndUpdate(absolute.above(3), previousRoof);
            }
            helper.succeed();
        });
    }

    private static Player player(GameTestHelper helper, GameType mode) {
        Player player = new Player(helper.getLevel(), BlockPos.ZERO, 0,
            new GameProfile(UUID.randomUUID(), "projectile-behavior")) {
            @Override
            public boolean isSpectator() { return false; }
            @Override
            public boolean isCreative() { return mode.isCreative(); }
        };
        player.getAbilities().instabuild = mode.isCreative();
        player.setPos(Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(1, 2, 1))));
        return player;
    }

    private static ItemStack stack(String name) {
        ItemStack stack = ItemInit.DRAGON_TRIDENT_ITEM.get().getDefaultInstance();
        stack.setHoverName(Component.literal(name));
        return stack;
    }

    private static ItemStack enchanted(GameTestHelper helper, String name, Enchantment enchantment, int level) {
        ItemStack stack = stack(name);
        stack.enchant(enchantment, level);
        return stack;
    }

    private static void release(GameTestHelper helper, Player owner, ItemStack stack) {
        stack.getItem().releaseUsing(stack, helper.getLevel(), owner, stack.getUseDuration() - 10);
    }

    private static java.util.List<DragonTridentEntity> ownedProjectiles(GameTestHelper helper, Player owner) {
        return helper.getLevel().getEntities(EntityInit.DRAGONTRIDENT_ENTITY.get(), owner.getBoundingBox().inflate(4),
            projectile -> projectile.getOwner() == owner);
    }

    private static void assertNoProjectile(GameTestHelper helper, Player owner) {
        check(helper, ownedProjectiles(helper, owner).isEmpty(), "This release must not spawn a projectile");
    }

    private static int countDragonTridents(Player player) {
        return player.getInventory().items.stream().filter(item -> item.is(ItemInit.DRAGON_TRIDENT_ITEM.get()))
            .mapToInt(ItemStack::getCount).sum();
    }

    private static boolean sameStack(ItemStack first, ItemStack second) {
        return ItemStack.matches(first, second);
    }

    private static float hitDamage(GameTestHelper helper, LivingEntity target, ItemStack stack) {
        float health = target.getHealth();
        DragonTridentEntity projectile = new DragonTridentEntity(helper.getLevel(), player(helper, GameType.SURVIVAL), stack);
        projectile.onHitEntity(new EntityHitResult(target));
        projectile.discard();
        return health - target.getHealth();
    }

    private static void check(GameTestHelper helper, boolean condition, String message) {
        if (!condition) { helper.fail(message); }
    }

    private static void assertEqual(GameTestHelper helper, int actual, int expected, String description) {
        check(helper, actual == expected, description + ": expected " + expected + ", found " + actual);
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
