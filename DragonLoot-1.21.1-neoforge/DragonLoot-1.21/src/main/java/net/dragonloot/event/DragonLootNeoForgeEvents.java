package net.dragonloot.event;

import java.util.List;
import net.dragonloot.init.ConfigInit;
import net.dragonloot.init.ItemInit;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

public final class DragonLootNeoForgeEvents {

    private DragonLootNeoForgeEvents() {
    }

    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.isCanceled() || !(event.getEntity() instanceof EnderDragon dragon)) {
            return;
        }

        Level level = dragon.level();
        if (level.isClientSide()) {
            return;
        }

        AABB box = new AABB(dragon.blockPosition()).inflate(128.0D);
        List<Player> players = level.getEntitiesOfClass(Player.class, box, player -> player.isAlive() && !player.isSpectator());
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

    private static void spawnDragonScale(Level level, EnderDragon dragon) {
        ItemEntity drop = new ItemEntity(level, dragon.getX(), dragon.getY(), dragon.getZ(), new ItemStack(ItemInit.DRAGON_SCALE_ITEM.get()));
        drop.setDefaultPickUpDelay();
        level.addFreshEntity(drop);
    }
}
