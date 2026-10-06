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

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(DragonLootMain.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DragonTridentGameTests {
    private DragonTridentGameTests() {
    }

    @GameTest(template = "empty")
    public static void constructorsRetainCustomItem(GameTestHelper helper) {
        DragonTridentEntity empty = new DragonTridentEntity(EntityInit.DRAGONTRIDENT_ENTITY.get(), helper.getLevel());
        check(helper, empty.getPickupItem().is(ItemInit.DRAGON_TRIDENT_ITEM.get()), "Default entity must contain a dragon trident");
        ItemStack stack = enchantedStack();
        DragonTridentEntity thrown = new DragonTridentEntity(helper.getLevel(), player(helper), stack);
        check(helper, ItemStack.matches(stack, thrown.getPickupItem()), "Projectile must copy the named, enchanted, damaged item");
        stack.setDamageValue(14);
        check(helper, thrown.getPickupItem().getDamageValue() == 13, "Projectile must own an independent item copy");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void chargedOffhandThrowCreatesProjectile(GameTestHelper helper) {
        Player owner = player(helper);
        ItemStack stack = ItemInit.DRAGON_TRIDENT_ITEM.get().getDefaultInstance();
        stack.setDamageValue(7);
        owner.setItemInHand(InteractionHand.OFF_HAND, stack);
        owner.startUsingItem(InteractionHand.OFF_HAND);
        stack.getItem().releaseUsing(stack, helper.getLevel(), owner, stack.getUseDuration() - 10);
        var projectiles = helper.getLevel().getEntities(EntityInit.DRAGONTRIDENT_ENTITY.get(), owner.getBoundingBox().inflate(3), entity -> entity.getOwner() == owner);
        check(helper, projectiles.size() == 1, "Exactly one custom projectile must spawn after ten ticks");
        DragonTridentEntity projectile = projectiles.get(0);
        check(helper, projectile.getOwner() == owner, "Projectile must retain its player owner");
        check(helper, projectile.getPickupItem().getDamageValue() == 8, "A throw must cost one durability");
        check(helper, owner.getOffhandItem().isEmpty(), "Survival throw must remove the offhand item");
        projectile.discard();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void shortChargeLeavesItemUntouched(GameTestHelper helper) {
        Player owner = player(helper);
        ItemStack stack = ItemInit.DRAGON_TRIDENT_ITEM.get().getDefaultInstance();
        owner.setItemInHand(InteractionHand.MAIN_HAND, stack);
        owner.startUsingItem(InteractionHand.MAIN_HAND);
        stack.getItem().releaseUsing(stack, helper.getLevel(), owner, stack.getUseDuration() - 9);
        check(helper, stack.getDamageValue() == 0 && owner.getMainHandItem() == stack, "Nine ticks must not consume durability or the item");
        check(helper, helper.getLevel().getEntities(EntityInit.DRAGONTRIDENT_ENTITY.get(), owner.getBoundingBox().inflate(3), entity -> entity.getOwner() == owner).isEmpty(),
            "Nine ticks must not spawn a projectile");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void damagedDuringChargeCannotThrow(GameTestHelper helper) {
        Player owner = player(helper);
        ItemStack stack = ItemInit.DRAGON_TRIDENT_ITEM.get().getDefaultInstance();
        owner.setItemInHand(InteractionHand.MAIN_HAND, stack);
        stack.getItem().use(helper.getLevel(), owner, InteractionHand.MAIN_HAND);
        stack.setDamageValue(stack.getMaxDamage() - 1);
        stack.getItem().releaseUsing(stack, helper.getLevel(), owner, stack.getUseDuration() - 10);
        check(helper, !stack.isEmpty() && stack.getDamageValue() == stack.getMaxDamage() - 1,
            "An item damaged while charging must not break or be thrown");
        check(helper, helper.getLevel().getEntities(EntityInit.DRAGONTRIDENT_ENTITY.get(), owner.getBoundingBox().inflate(3), entity -> entity.getOwner() == owner).isEmpty(),
            "An unusable item must not spawn a projectile");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void hitDamagesTargetAndSavesHitState(GameTestHelper helper) {
        Pig target = helper.spawn(EntityType.PIG, new BlockPos(2, 2, 2));
        float initialHealth = target.getHealth();
        DragonTridentEntity projectile = new DragonTridentEntity(helper.getLevel(), player(helper), ItemInit.DRAGON_TRIDENT_ITEM.get().getDefaultInstance());
        projectile.setDeltaMovement(0, 0, 1);
        projectile.onHitEntity(new EntityHitResult(target));
        check(helper, target.getHealth() < initialHealth, "Projectile impact must damage its target");
        check(helper, projectile.getDeltaMovement().z < 0, "Projectile must recoil after impact");
        CompoundTag saved = new CompoundTag();
        projectile.addAdditionalSaveData(saved);
        check(helper, saved.getBoolean("DealtDamage"), "Reload must retain the already-hit state");
        target.discard();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void saveRoundTripRetainsLoyaltyAndPickup(GameTestHelper helper) {
        Player owner = player(helper);
        ItemStack stack = enchantedStack();
        DragonTridentEntity projectile = new DragonTridentEntity(helper.getLevel(), owner, stack);
        CompoundTag saved = new CompoundTag();
        projectile.addAdditionalSaveData(saved);
        DragonTridentEntity loaded = new DragonTridentEntity(EntityInit.DRAGONTRIDENT_ENTITY.get(), helper.getLevel());
        loaded.readAdditionalSaveData(saved);
        check(helper, ItemStack.matches(stack, loaded.getPickupItem()), "Reload must retain name, enchantments, and durability");
        check(helper, loaded.isEnchanted(), "Reload must restore the enchanted projectile appearance");
        loaded.setOwner(owner);
        loaded.setNoPhysics(true);
        loaded.tick();
        check(helper, loaded.returnTimer > 0, "Reloaded loyalty projectile must return to its owner");
        owner.getInventory().clearContent();
        loaded.playerTouch(owner);
        check(helper, loaded.isRemoved(), "Returning projectile must be collected by its owner");
        check(helper, owner.getInventory().items.stream().anyMatch(item -> ItemStack.matches(stack, item)),
            "Pickup must return the original enchanted, named, damaged trident");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void channelingStrikesExposedLightningRod(GameTestHelper helper) {
        BlockPos relative = new BlockPos(2, 2, 2);
        BlockPos column = helper.absolutePos(relative);
        // GameTest origins can be below terrain, and reused worlds retain old test structures.
        int surface = helper.getLevel().getHeight(Heightmap.Types.MOTION_BLOCKING, column.getX(), column.getZ());
        BlockPos pos = new BlockPos(column.getX(), Math.max(column.getY(), surface) + 4, column.getZ());
        check(helper, pos.getY() < helper.getLevel().getMaxBuildHeight() - 1, "Rod fixture must fit above the column's highest obstruction");
        BlockState previousBlock = helper.getLevel().getBlockState(pos);
        helper.getLevel().setBlockAndUpdate(pos, Blocks.LIGHTNING_ROD.defaultBlockState());
        // Allow skylight updates before exercising the actual block-impact handler.
        helper.runAfterDelay(10, () -> {
            ItemStack stack = ItemInit.DRAGON_TRIDENT_ITEM.get().getDefaultInstance();
            stack.enchant(Enchantments.CHANNELING, 1);
            DragonTridentEntity projectile = new DragonTridentEntity(helper.getLevel(), player(helper), stack);
            // The thunder getter is multiplied by rain; retain the raw interpolation state.
            float rain = helper.getLevel().rainLevel;
            float previousRain = helper.getLevel().oRainLevel;
            float thunder = helper.getLevel().thunderLevel;
            float previousThunder = helper.getLevel().oThunderLevel;
            var bounds = projectile.getBoundingBox().move(Vec3.atCenterOf(pos).subtract(projectile.position())).inflate(3);
            var before = helper.getLevel().getEntities(EntityType.LIGHTNING_BOLT, bounds, entity -> true);
            try {
                check(helper, helper.getLevel().canSeeSky(pos), "Lightning rod fixture must have sky access");
                helper.getLevel().setRainLevel(1);
                helper.getLevel().setThunderLevel(1);
                projectile.onHitBlock(new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
                var after = helper.getLevel().getEntities(EntityType.LIGHTNING_BOLT, bounds, entity -> true);
                check(helper, after.size() == before.size() + 1, "Custom Channeling trident must summon exactly one bolt at an exposed rod");
                after.stream().filter(entity -> !before.contains(entity)).forEach(entity -> entity.discard());
            } finally {
                helper.getLevel().rainLevel = rain;
                helper.getLevel().oRainLevel = previousRain;
                helper.getLevel().thunderLevel = thunder;
                helper.getLevel().oThunderLevel = previousThunder;
                helper.getLevel().setBlockAndUpdate(pos, previousBlock);
                projectile.discard();
            }
            helper.succeed();
        });
    }

    private static Player player(GameTestHelper helper) {
        Player player = new Player(helper.getLevel(), BlockPos.ZERO, 0, new GameProfile(UUID.randomUUID(), "trident-test"), null) {
            @Override
            public boolean isSpectator() { return false; }
            @Override
            public boolean isCreative() { return false; }
        };
        player.setPos(Vec3.atCenterOf(helper.absolutePos(new BlockPos(1, 2, 1))));
        return player;
    }

    private static ItemStack enchantedStack() {
        ItemStack stack = ItemInit.DRAGON_TRIDENT_ITEM.get().getDefaultInstance();
        stack.setDamageValue(13);
        stack.setHoverName(Component.literal("Saved dragon trident"));
        stack.enchant(Enchantments.LOYALTY, 3);
        return stack;
    }

    private static void check(GameTestHelper helper, boolean condition, String message) {
        if (!condition) {
            helper.fail(message);
        }
    }
}
