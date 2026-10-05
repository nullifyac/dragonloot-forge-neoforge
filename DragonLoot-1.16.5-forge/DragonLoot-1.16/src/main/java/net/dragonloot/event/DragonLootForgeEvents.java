package net.dragonloot.event;

import java.util.List;
import net.dragonloot.DragonLootMain;
import net.dragonloot.init.ConfigInit;
import net.dragonloot.init.ItemInit;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = DragonLootMain.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DragonLootForgeEvents {

    private DragonLootForgeEvents() {
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.isCanceled() || !(event.getEntityLiving() instanceof EnderDragonEntity)) {
            return;
        }

        EnderDragonEntity dragon = (EnderDragonEntity) event.getEntityLiving();
        World level = dragon.level;
        if (level.isClientSide) {
            return;
        }

        AxisAlignedBB box = new AxisAlignedBB(dragon.blockPosition()).inflate(128.0D);
        List<PlayerEntity> players = level.getEntitiesOfClass(PlayerEntity.class, box, player -> player.isAlive() && !player.isSpectator());
        int bonusRolls = players.size() * ConfigInit.CONFIG.additional_scales_per_player;

        for (int i = 0; i < ConfigInit.CONFIG.scale_minimum_drop_amount; i++) {
            spawnDragonScale(level, dragon);
        }

        for (int i = 0; i < bonusRolls; i++) {
            if (level.random.nextFloat() <= ConfigInit.CONFIG.additional_scale_drop_chance) {
                spawnDragonScale(level, dragon);
            }
        }
    }

    private static void spawnDragonScale(World level, EnderDragonEntity dragon) {
        ItemEntity drop = new ItemEntity(level, dragon.getX(), dragon.getY(), dragon.getZ(), new ItemStack(ItemInit.DRAGON_SCALE_ITEM.get()));
        drop.setDefaultPickUpDelay();
        level.addFreshEntity(drop);
    }
}
