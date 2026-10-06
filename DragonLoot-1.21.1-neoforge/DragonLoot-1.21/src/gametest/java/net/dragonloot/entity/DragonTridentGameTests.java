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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileItem;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(DragonLootMain.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DragonTridentGameTests {
    private DragonTridentGameTests() {
    }

    @GameTest(template = "empty")
    public static void constructorsAndDispenserKeepCustomItem(GameTestHelper helper) {
        ItemStack stack = enchantedStack(helper);
        Player owner = player(helper);
        DragonTridentEntity defaultEntity = new DragonTridentEntity(EntityInit.DRAGONTRIDENT_ENTITY.get(), helper.getLevel());
        helper.assertTrue(defaultEntity.getWeaponItem().is(ItemInit.DRAGON_TRIDENT_ITEM.get()), "Default constructor must supply a dragon trident");

        DragonTridentEntity thrown = new DragonTridentEntity(helper.getLevel(), owner, stack);
        helper.assertTrue(ItemStack.isSameItemSameComponents(stack, thrown.getWeaponItem()), "Player projectile must copy all item components");
        stack.setDamageValue(stack.getDamageValue() + 1);
        helper.assertTrue(thrown.getWeaponItem().getDamageValue() != stack.getDamageValue(), "Projectile stack must be independent of the player's stack");

        Vec3 position = Vec3.atCenterOf(helper.absolutePos(new BlockPos(1, 2, 1)));
        var projectile = ((ProjectileItem) ItemInit.DRAGON_TRIDENT_ITEM.get()).asProjectile(helper.getLevel(), position, stack, Direction.NORTH);
        helper.assertTrue(projectile instanceof DragonTridentEntity, "Dispenser conversion must create the registered custom projectile");
        helper.assertTrue(((DragonTridentEntity) projectile).getWeaponItem().is(ItemInit.DRAGON_TRIDENT_ITEM.get()), "Dispenser projectile must retain the dragon item");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void chargedOffhandThrowCreatesProjectile(GameTestHelper helper) {
        Player owner = player(helper);
        ItemStack stack = ItemInit.DRAGON_TRIDENT_ITEM.get().getDefaultInstance();
        stack.setDamageValue(7);
        owner.setItemInHand(InteractionHand.OFF_HAND, stack);
        owner.startUsingItem(InteractionHand.OFF_HAND);
        stack.getItem().releaseUsing(stack, helper.getLevel(), owner, stack.getUseDuration(owner) - 10);

        var projectiles = helper.getLevel().getEntities(EntityInit.DRAGONTRIDENT_ENTITY.get(), owner.getBoundingBox().inflate(3), entity -> entity.getOwner() == owner);
        helper.assertValueEqual(projectiles.size(), 1, "spawned dragon trident count");
        DragonTridentEntity projectile = projectiles.getFirst();
        helper.assertTrue(projectile.getOwner() == owner, "Thrown projectile must retain its player owner");
        helper.assertValueEqual(projectile.getWeaponItem().getDamageValue(), 8, "durability after a single throw");
        helper.assertTrue(owner.getOffhandItem().isEmpty(), "Survival throw must remove the trident from the offhand");
        projectile.discard();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void shortChargeLeavesItemUntouched(GameTestHelper helper) {
        Player owner = player(helper);
        ItemStack stack = ItemInit.DRAGON_TRIDENT_ITEM.get().getDefaultInstance();
        owner.setItemInHand(InteractionHand.MAIN_HAND, stack);
        owner.startUsingItem(InteractionHand.MAIN_HAND);
        stack.getItem().releaseUsing(stack, helper.getLevel(), owner, stack.getUseDuration(owner) - 9);
        helper.assertValueEqual(stack.getDamageValue(), 0, "durability after an uncharged release");
        helper.assertTrue(owner.getMainHandItem() == stack, "An uncharged release must leave the player's item in place");
        helper.assertTrue(helper.getLevel().getEntities(EntityInit.DRAGONTRIDENT_ENTITY.get(), owner.getBoundingBox().inflate(3), entity -> entity.getOwner() == owner).isEmpty(),
            "An uncharged release must not spawn a projectile");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void hitDealsDamageAndSavesHitState(GameTestHelper helper) {
        Pig target = helper.spawn(EntityType.PIG, new BlockPos(2, 2, 2));
        float initialHealth = target.getHealth();
        DragonTridentEntity projectile = new DragonTridentEntity(helper.getLevel(), player(helper), ItemInit.DRAGON_TRIDENT_ITEM.get().getDefaultInstance());
        projectile.setDeltaMovement(0, 0, 1);
        projectile.onHitEntity(new EntityHitResult(target));
        helper.assertTrue(target.getHealth() < initialHealth, "Projectile impact must actually damage the target");
        helper.assertTrue(projectile.getDeltaMovement().z < 0, "Projectile must recoil after impact");
        CompoundTag saved = new CompoundTag();
        projectile.addAdditionalSaveData(saved);
        helper.assertTrue(saved.getBoolean("DealtDamage"), "Save data must prevent a hit projectile from dealing damage again after reload");
        target.discard();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void saveRoundTripRetainsLoyaltyAndPickup(GameTestHelper helper) {
        Player owner = player(helper);
        ItemStack stack = enchantedStack(helper);
        DragonTridentEntity projectile = new DragonTridentEntity(helper.getLevel(), owner, stack);
        CompoundTag saved = new CompoundTag();
        projectile.addAdditionalSaveData(saved);
        DragonTridentEntity loaded = new DragonTridentEntity(EntityInit.DRAGONTRIDENT_ENTITY.get(), helper.getLevel());
        loaded.readAdditionalSaveData(saved);
        helper.assertTrue(ItemStack.isSameItemSameComponents(stack, loaded.getWeaponItem()), "Reload must retain enchantments, damage, and custom name");
        helper.assertTrue(loaded.isEnchanted(), "Reload must restore the projectile's enchanted appearance");
        loaded.setOwner(owner);
        loaded.setNoPhysics(true);
        loaded.tick();
        helper.assertTrue(loaded.returnTimer > 0, "Reloaded loyalty projectile must still return to its owner");

        owner.getInventory().clearContent();
        loaded.playerTouch(owner);
        helper.assertTrue(loaded.isRemoved(), "The returning projectile must be collected by its owner");
        helper.assertTrue(owner.getInventory().items.stream().anyMatch(item -> ItemStack.isSameItemSameComponents(stack, item)),
            "Pickup must return the original enchanted and damaged dragon trident");
        helper.succeed();
    }

    private static Player player(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(Vec3.atCenterOf(helper.absolutePos(new BlockPos(1, 2, 1))));
        return player;
    }

    private static ItemStack enchantedStack(GameTestHelper helper) {
        ItemStack stack = ItemInit.DRAGON_TRIDENT_ITEM.get().getDefaultInstance();
        stack.setDamageValue(13);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal("Saved dragon trident"));
        stack.enchant(helper.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.LOYALTY), 3);
        return stack;
    }
}
