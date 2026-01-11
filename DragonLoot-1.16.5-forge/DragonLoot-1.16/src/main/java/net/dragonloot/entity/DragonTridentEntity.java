package net.dragonloot.entity;

import net.dragonloot.init.EntityInit;
import net.dragonloot.init.ItemInit;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.LightningBoltEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.entity.projectile.AbstractArrowEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.EntityRayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

public class DragonTridentEntity extends AbstractArrowEntity {
	private static final DataParameter<Byte> LOYALTY = EntityDataManager.defineId(DragonTridentEntity.class, DataSerializers.BYTE);
	private static final DataParameter<Boolean> ENCHANTED = EntityDataManager.defineId(DragonTridentEntity.class, DataSerializers.BOOLEAN);
	private ItemStack tridentStack = ItemInit.DRAGON_TRIDENT_ITEM.get().getDefaultInstance();
	private boolean dealtDamage;
	public int returnTimer;

	public DragonTridentEntity(EntityType<? extends DragonTridentEntity> entityType, World level) {
		super(entityType, level);
	}

	public DragonTridentEntity(World level, LivingEntity owner, ItemStack stack) {
		super(EntityInit.DRAGONTRIDENT_ENTITY.get(), owner, level);
		this.tridentStack = stack.copy();
		this.entityData.set(LOYALTY, (byte) EnchantmentHelper.getLoyalty(stack));
		this.entityData.set(ENCHANTED, stack.hasFoil());
	}

	public DragonTridentEntity(World level, double x, double y, double z) {
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
				if (!this.level.isClientSide && this.pickup == PickupStatus.ALLOWED) {
					this.spawnAtLocation(this.getPickupItem(), 0.1F);
				}
				this.remove();
			} else if (loyalty > 0) {
				this.setNoPhysics(true);
				Vector3d motion = new Vector3d(owner.getX() - this.getX(), owner.getEyeY() - this.getY(), owner.getZ() - this.getZ());
				this.setPos(this.getX(), this.getY() + motion.y * 0.015D * loyalty, this.getZ());
				if (this.level.isClientSide) {
					this.yo = this.getY();
				}
				Vector3d velocity = this.getDeltaMovement().scale(0.95D).add(motion.normalize().scale(0.05D * loyalty));
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
		if (entity instanceof PlayerEntity) {
			PlayerEntity player = (PlayerEntity) entity;
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
	protected EntityRayTraceResult findHitEntity(Vector3d startVec, Vector3d endVec) {
		return this.dealtDamage ? null : super.findHitEntity(startVec, endVec);
	}

	@Override
	protected void onHitEntity(EntityRayTraceResult result) {
		Entity target = result.getEntity();
		float damage = 8.0F;
		if (target instanceof LivingEntity) {
			LivingEntity living = (LivingEntity) target;
			damage += EnchantmentHelper.getDamageBonus(this.tridentStack, living.getMobType());
		}
		Entity owner = this.getOwner();
		DamageSource source = DamageSource.trident(this, owner == null ? this : owner);
		this.dealtDamage = true;
		SoundEvent sound = SoundEvents.TRIDENT_HIT;
		if (target.hurt(source, damage)) {
			if (target.getType() == EntityType.ENDERMAN) {
				return;
			}
			if (target instanceof LivingEntity) {
				LivingEntity livingTarget = (LivingEntity) target;
				if (owner instanceof LivingEntity) {
					LivingEntity livingOwner = (LivingEntity) owner;
					EnchantmentHelper.doPostHurtEffects(livingTarget, owner);
					EnchantmentHelper.doPostDamageEffects(livingOwner, livingTarget);
				}
				this.doPostHurtEffects(livingTarget);
			}
		}
		this.setDeltaMovement(this.getDeltaMovement().multiply(-0.01D, -0.1D, -0.01D));
		float thunderSoundVolume = 1.0F;
		if (this.level instanceof ServerWorld && ((ServerWorld) this.level).isThundering() && EnchantmentHelper.hasChanneling(this.tridentStack)) {
			ServerWorld serverLevel = (ServerWorld) this.level;
			BlockPos pos = target.blockPosition();
			if (serverLevel.canSeeSky(pos)) {
				LightningBoltEntity lightning = EntityType.LIGHTNING_BOLT.create(serverLevel);
				if (lightning != null) {
					lightning.setPos(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
					if (owner instanceof ServerPlayerEntity) {
						lightning.setCause((ServerPlayerEntity) owner);
					}
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
	public void playerTouch(PlayerEntity player) {
		Entity owner = this.getOwner();
		if (owner == null || owner.getUUID().equals(player.getUUID())) {
			super.playerTouch(player);
		}
	}

	@Override
	public void readAdditionalSaveData(CompoundNBT tag) {
		super.readAdditionalSaveData(tag);
		if (tag.contains("Dragon_Trident", 10)) {
			this.tridentStack = ItemStack.of(tag.getCompound("Dragon_Trident"));
		}
		this.dealtDamage = tag.getBoolean("DealtDamage");
		this.entityData.set(LOYALTY, (byte) EnchantmentHelper.getLoyalty(this.tridentStack));
	}

	@Override
	public void addAdditionalSaveData(CompoundNBT tag) {
		super.addAdditionalSaveData(tag);
		tag.put("Dragon_Trident", this.tridentStack.save(new CompoundNBT()));
		tag.putBoolean("DealtDamage", this.dealtDamage);
	}

	@Override
	protected void tickDespawn() {
		int loyalty = this.entityData.get(LOYALTY);
		if (this.pickup != PickupStatus.ALLOWED || loyalty <= 0) {
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
