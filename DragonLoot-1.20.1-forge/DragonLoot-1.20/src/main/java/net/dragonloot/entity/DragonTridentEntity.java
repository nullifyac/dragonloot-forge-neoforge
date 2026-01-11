package net.dragonloot.entity;

import net.dragonloot.init.EntityInit;
import net.dragonloot.init.ItemInit;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.AbstractArrow.Pickup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

public class DragonTridentEntity extends AbstractArrow {
	private static final EntityDataAccessor<Byte> LOYALTY = SynchedEntityData.defineId(DragonTridentEntity.class, EntityDataSerializers.BYTE);
	private static final EntityDataAccessor<Boolean> ENCHANTED = SynchedEntityData.defineId(DragonTridentEntity.class, EntityDataSerializers.BOOLEAN);
	private ItemStack tridentStack = ItemInit.DRAGON_TRIDENT_ITEM.get().getDefaultInstance();
	private boolean dealtDamage;
	public int returnTimer;

	public DragonTridentEntity(EntityType<? extends DragonTridentEntity> entityType, Level level) {
		super(entityType, level);
	}

	public DragonTridentEntity(Level level, LivingEntity owner, ItemStack stack) {
		super(EntityInit.DRAGONTRIDENT_ENTITY.get(), owner, level);
		this.tridentStack = stack.copy();
		this.entityData.set(LOYALTY, (byte) EnchantmentHelper.getLoyalty(stack));
		this.entityData.set(ENCHANTED, stack.hasFoil());
	}

	public DragonTridentEntity(Level level, double x, double y, double z) {
		super(EntityInit.DRAGONTRIDENT_ENTITY.get(), x, y, z, level);
		this.tridentStack = ItemInit.DRAGON_TRIDENT_ITEM.get().getDefaultInstance();
	}

	@Override
	protected void defineSynchedData() {
		super.defineSynchedData();
		this.entityData.define(LOYALTY, (byte) 0);
		this.entityData.define(ENCHANTED, false);
	}

	@Override
	public void tick() {
		if (this.inGroundTime > 4) {
			this.dealtDamage = true;
		}

		Entity owner = this.getOwner();
		if ((this.dealtDamage || this.isNoPhysics()) && owner != null) {
			int loyalty = this.entityData.get(LOYALTY);
			if (loyalty > 0 && !this.isOwnerAlive()) {
				if (!this.level().isClientSide && this.pickup == Pickup.ALLOWED) {
					this.spawnAtLocation(this.getPickupItem(), 0.1F);
				}
				this.discard();
			} else if (loyalty > 0) {
				this.setNoPhysics(true);
				Vec3 motion = new Vec3(owner.getX() - this.getX(), owner.getEyeY() - this.getY(), owner.getZ() - this.getZ());
				this.setPos(this.getX(), this.getY() + motion.y * 0.015D * loyalty, this.getZ());
				if (this.level().isClientSide) {
					this.yo = this.getY();
				}
				Vec3 velocity = this.getDeltaMovement().scale(0.95D).add(motion.normalize().scale(0.05D * loyalty));
				this.setDeltaMovement(velocity);
				if (this.returnTimer == 0) {
					this.playSound(SoundEvents.TRIDENT_RETURN, 10.0F, 1.0F);
				}
				++this.returnTimer;
			}
		}
		super.tick();
	}

	private boolean isOwnerAlive() {
		Entity entity = this.getOwner();
		if (entity instanceof Player player) {
			return player.isAlive() && !player.isSpectator();
		}
		return entity != null && entity.isAlive();
	}

	@Override
	protected ItemStack getPickupItem() {
		return this.tridentStack.copy();
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
		if (target instanceof LivingEntity living) {
			damage += EnchantmentHelper.getDamageBonus(this.tridentStack, living.getMobType());
		}
		Entity owner = this.getOwner();
		DamageSource source = this.level().damageSources().trident(this, owner == null ? this : owner);
		this.dealtDamage = true;
		SoundEvent sound = SoundEvents.TRIDENT_HIT;
		if (target.hurt(source, damage)) {
			if (target.getType() == EntityType.ENDERMAN) {
				return;
			}
			if (target instanceof LivingEntity livingTarget) {
				if (owner instanceof LivingEntity livingOwner) {
					EnchantmentHelper.doPostHurtEffects(livingTarget, owner);
					EnchantmentHelper.doPostDamageEffects(livingOwner, livingTarget);
				}
				this.doPostHurtEffects(livingTarget);
			}
		}
		this.setDeltaMovement(this.getDeltaMovement().multiply(-0.01D, -0.1D, -0.01D));
		float thunderSoundVolume = 1.0F;
		if (this.level() instanceof ServerLevel serverLevel && serverLevel.isThundering() && EnchantmentHelper.hasChanneling(this.tridentStack)) {
			BlockPos pos = target.blockPosition();
			if (serverLevel.canSeeSky(pos)) {
				LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(serverLevel);
				if (lightning != null) {
					lightning.moveTo(Vec3.atBottomCenterOf(pos));
					lightning.setCause(owner instanceof ServerPlayer ? (ServerPlayer) owner : null);
					serverLevel.addFreshEntity(lightning);
					sound = SoundEvents.TRIDENT_THUNDER;
					thunderSoundVolume = 5.0F;
				}
			}
		}
		this.playSound(sound, thunderSoundVolume, 1.0F);
	}

	@Override
	protected SoundEvent getDefaultHitGroundSoundEvent() {
		return SoundEvents.TRIDENT_HIT_GROUND;
	}

	@Override
	public void playerTouch(Player player) {
		Entity owner = this.getOwner();
		if (owner == null || owner.getUUID().equals(player.getUUID())) {
			super.playerTouch(player);
		}
	}

	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		if (tag.contains("Dragon_Trident", 10)) {
			this.tridentStack = ItemStack.of(tag.getCompound("Dragon_Trident"));
		}
		this.dealtDamage = tag.getBoolean("DealtDamage");
		this.entityData.set(LOYALTY, (byte) EnchantmentHelper.getLoyalty(this.tridentStack));
	}

	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		tag.put("Dragon_Trident", this.tridentStack.save(new CompoundTag()));
		tag.putBoolean("DealtDamage", this.dealtDamage);
	}

	@Override
	protected void tickDespawn() {
		int loyalty = this.entityData.get(LOYALTY);
		if (this.pickup != Pickup.ALLOWED || loyalty <= 0) {
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
}
