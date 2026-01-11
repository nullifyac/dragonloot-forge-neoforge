package net.dragonloot.mixin;

import net.dragonloot.access.DragonAnvilInterface;
import net.dragonloot.init.BlockInit;
import net.dragonloot.init.ConfigInit;
import net.dragonloot.init.NetworkInit;
import net.dragonloot.network.SyncPacket;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.AbstractRepairContainer;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.inventory.container.RepairContainer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IWorldPosCallable;
import net.minecraft.util.IntReferenceHolder;
import net.minecraftforge.fml.network.PacketDistributor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RepairContainer.class)
public abstract class AnvilScreenHandlerMixin extends AbstractRepairContainer implements DragonAnvilInterface {

	@Shadow
	@Final
	private IntReferenceHolder cost;

	private boolean isDragonAnvil;

	protected AnvilScreenHandlerMixin(ContainerType<?> menuType, int containerId, PlayerInventory inventory, IWorldPosCallable access) {
		super(menuType, containerId, inventory, access);
	}

	@Inject(method = "isValidBlock", at = @At("HEAD"))
	private void dragonloot$trackDragonAnvil(BlockState state, CallbackInfoReturnable<Boolean> cir) {
		this.isDragonAnvil = state.is(BlockInit.DRAGON_ANVIL_BLOCK.get());
		if (this.player instanceof ServerPlayerEntity) {
			ServerPlayerEntity serverPlayer = (ServerPlayerEntity) this.player;
			NetworkInit.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer), new SyncPacket(this.isDragonAnvil));
		}
	}

	@Inject(method = "onTake", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/IntReferenceHolder;set(I)V"), cancellable = true)
	private void dragonloot$onTake(PlayerEntity player, ItemStack stack, CallbackInfoReturnable<ItemStack> info) {
		if (this.isDragonAnvil) {
			this.access.execute((level, pos) -> {
				this.cost.set(0);
				level.levelEvent(null, 1030, pos, 0);
			});
			info.setReturnValue(stack);
			info.cancel();
		}
	}

	@Inject(method = "createResult", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/IntReferenceHolder;set(I)V", shift = At.Shift.AFTER))
	private void dragonloot$clampCost(CallbackInfo info) {
		if (this.cost.get() > 30 && this.isDragonAnvil && ConfigInit.CONFIG.dragon_anvil_no_cap) {
			this.cost.set(30);
		}
	}

	@Inject(method = "getCost", at = @At("HEAD"), cancellable = true)
	private void dragonloot$getCost(CallbackInfoReturnable<Integer> info) {
		if (this.cost.get() > 30 && this.isDragonAnvil && ConfigInit.CONFIG.dragon_anvil_no_cap) {
			info.setReturnValue(30);
		}
	}

	@Override
	public void setDragonAnvil(boolean isDragonAnvil) {
		this.isDragonAnvil = isDragonAnvil;
	}
}
