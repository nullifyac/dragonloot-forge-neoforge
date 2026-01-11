package net.dragonloot.mixin;

import java.util.List;
import net.dragonloot.init.ConfigInit;
import net.dragonloot.init.ItemInit;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EnderDragon.class)
public abstract class EnderDragonEntityMixin extends Mob {
    @Shadow
    private int dragonDeathTime;

    protected EnderDragonEntityMixin(EntityType<? extends Mob> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "tickDeath", at = @At("HEAD"))
    private void dragonloot$dropDragonScales(CallbackInfo info) {
        if (!this.level.isClientSide() && this.dragonDeathTime == 150) {
            AABB box = new AABB(this.blockPosition()).inflate(128.0D);
            List<Player> players = this.level.getEntitiesOfClass(Player.class, box, player -> player.isAlive() && !player.isSpectator());
            int bonus = players.size() * ConfigInit.CONFIG.additional_scales_per_player;

            for (int i = 0; i < ConfigInit.CONFIG.scale_minimum_drop_amount; i++) {
                this.spawnAtLocation(new ItemStack(ItemInit.DRAGON_SCALE_ITEM.get()));
            }

            for (int i = 0; i < bonus; i++) {
                if (this.level.random.nextFloat() <= ConfigInit.CONFIG.additional_scale_drop_chance) {
                    this.spawnAtLocation(new ItemStack(ItemInit.DRAGON_SCALE_ITEM.get()));
                }
            }
        }
    }
}