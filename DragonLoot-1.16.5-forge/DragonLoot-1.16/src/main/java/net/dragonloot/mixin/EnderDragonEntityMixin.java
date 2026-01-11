package net.dragonloot.mixin;

import java.util.List;
import net.dragonloot.init.ConfigInit;
import net.dragonloot.init.ItemInit;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EnderDragonEntity.class)
public abstract class EnderDragonEntityMixin extends MobEntity {
    @Shadow
    private int dragonDeathTime;

    protected EnderDragonEntityMixin(EntityType<? extends MobEntity> entityType, World level) {
        super(entityType, level);
    }

    @Inject(method = "tickDeath", at = @At("HEAD"))
    private void dragonloot$dropDragonScales(CallbackInfo info) {
        if (!this.level.isClientSide && this.dragonDeathTime == 150) {
            AxisAlignedBB box = new AxisAlignedBB(this.blockPosition()).inflate(128.0D);
            List<PlayerEntity> players = this.level.getEntitiesOfClass(PlayerEntity.class, box, player -> player.isAlive() && !player.isSpectator());
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
