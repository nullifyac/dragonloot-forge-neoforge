package net.dragonloot.entity;

import net.dragonloot.init.EntityInit;
import net.dragonloot.init.ItemInit;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

public class DragonTridentEntity extends AbstractArrow {
    private static final EntityDataAccessor<Byte> LOYALTY = SynchedEntityData.defineId(DragonTridentEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Boolean> ENCHANTED = SynchedEntityData.defineId(DragonTridentEntity.class, EntityDataSerializers.BOOLEAN);

    private boolean dealtDamage;
    public int returnTimer;

    public DragonTridentEntity(EntityType<? extends DragonTridentEntity> entityType, Level level) {
        super(entityType, level);
    }

    public DragonTridentEntity(Level level, LivingEntity owner, ItemStack stack) {
        super(EntityInit.DRAGONTRIDENT_ENTITY.get(), owner, level, stack, null);
        this.entityData.set(LOYALTY, this.getLoyaltyFromItem(stack));
        this.entityData.set(ENCHANTED, stack.hasFoil());
    }

    public DragonTridentEntity(Level level, double x, double y, double z, ItemStack stack) {
        super(EntityInit.DRAGONTRIDENT_ENTITY.get(), x, y, z, level, stack, stack);
        this.entityData.set(LOYALTY, this.getLoyaltyFromItem(stack));
        this.entityData.set(ENCHANTED, stack.hasFoil());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(LOYALTY, (byte) 0);
        builder.define(ENCHANTED, false);
    }

    @Override
    public void tick() {
        if (this.inGroundTime > 4) {
            this.dealtDamage = true;
        }

        Entity owner = this.getOwner();
        int loyalty = this.entityData.get(LOYALTY);
        if (loyalty > 0 && (this.dealtDamage || this.isNoPhysics()) && owner != null) {
            if (!owner.isAlive() || owner.isSpectator()) {
                if (!this.level().isClientSide && this.pickup == Pickup.ALLOWED) {
                    this.spawnAtLocation(this.getPickupItem(), 0.1F);
                }
                this.discard();
            } else {
                this.setNoPhysics(true);
                Vec3 motion = owner.getEyePosition().subtract(this.position());
                this.setPosRaw(this.getX(), this.getY() + motion.y * 0.015D * loyalty, this.getZ());
                if (this.level().isClientSide) {
                    this.yOld = this.getY();
                }
                this.setDeltaMovement(this.getDeltaMovement().scale(0.95D).add(motion.normalize().scale(0.05D * loyalty)));
                if (this.returnTimer == 0) {
                    this.playSound(SoundEvents.TRIDENT_RETURN, 10.0F, 1.0F);
                }
                ++this.returnTimer;
            }
        }
        super.tick();
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        // AbstractArrow calls this while its constructor is running, before subclass fields exist.
        return ItemInit.DRAGON_TRIDENT_ITEM.get().getDefaultInstance();
    }

    @Override
    public ItemStack getWeaponItem() {
        return this.getPickupItemStackOrigin();
    }

    public boolean isEnchanted() {
        return this.entityData.get(ENCHANTED);
    }

    @Override
    protected EntityHitResult findHitEntity(Vec3 startVec, Vec3 endVec) {
        return this.dealtDamage ? null : super.findHitEntity(startVec, endVec);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        Entity target = result.getEntity();
        float damage = 8.0F;
        Entity owner = this.getOwner();
        DamageSource source = this.damageSources().trident(this, owner == null ? this : owner);
        if (this.level() instanceof ServerLevel serverLevel) {
            damage = EnchantmentHelper.modifyDamage(serverLevel, this.getWeaponItem(), target, source, damage);
        }

        this.dealtDamage = true;
        if (target.hurt(source, damage)) {
            if (target.getType() == EntityType.ENDERMAN) {
                return;
            }
            if (this.level() instanceof ServerLevel serverLevel) {
                // Channeling and other enchantment effects are data driven in 1.21.
                EnchantmentHelper.doPostAttackEffectsWithItemSource(serverLevel, target, source, this.getWeaponItem());
            }
            if (target instanceof LivingEntity livingTarget) {
                this.doKnockback(livingTarget, source);
                this.doPostHurtEffects(livingTarget);
            }
        }
        this.setDeltaMovement(this.getDeltaMovement().multiply(-0.01D, -0.1D, -0.01D));
        this.playSound(SoundEvents.TRIDENT_HIT, 1.0F, 1.0F);
    }

    @Override
    protected void hitBlockEnchantmentEffects(ServerLevel level, BlockHitResult result, ItemStack stack) {
        Vec3 hitLocation = result.getBlockPos().clampLocationWithin(result.getLocation());
        EnchantmentHelper.onHitBlock(level, stack, this.getOwner() instanceof LivingEntity owner ? owner : null,
            this, null, hitLocation, level.getBlockState(result.getBlockPos()), entity -> this.kill());
    }

    @Override
    protected SoundEvent getDefaultHitGroundSoundEvent() {
        return SoundEvents.TRIDENT_HIT_GROUND;
    }

    @Override
    protected boolean tryPickup(Player player) {
        return super.tryPickup(player)
            || this.isNoPhysics() && this.ownedBy(player) && player.getInventory().add(this.getPickupItem());
    }

    @Override
    public void playerTouch(Player player) {
        if (this.getOwner() == null || this.ownedBy(player)) {
            super.playerTouch(player);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        // Earlier releases stored an extra Trident tag; the base class now owns item persistence.
        if (!tag.contains("item", 10) && tag.contains("Trident", 10)) {
            this.setPickupItemStack(ItemStack.parse(this.registryAccess(), tag.getCompound("Trident")).orElse(this.getDefaultPickupItem()));
        }
        this.dealtDamage = tag.getBoolean("DealtDamage");
        this.entityData.set(LOYALTY, this.getLoyaltyFromItem(this.getWeaponItem()));
        this.entityData.set(ENCHANTED, this.getWeaponItem().hasFoil());
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("DealtDamage", this.dealtDamage);
    }

    @Override
    protected void tickDespawn() {
        if (this.pickup != Pickup.ALLOWED || this.entityData.get(LOYALTY) <= 0) {
            super.tickDespawn();
        }
    }

    @Override
    protected float getWaterInertia() {
        return 0.99F;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return true;
    }

    private byte getLoyaltyFromItem(ItemStack stack) {
        return this.level() instanceof ServerLevel serverLevel
            ? (byte) Mth.clamp(EnchantmentHelper.getTridentReturnToOwnerAcceleration(serverLevel, stack, this), 0, 127)
            : 0;
    }
}
