package net.dragonloot.mixin;

import net.dragonloot.access.DragonAnvilInterface;
import net.dragonloot.init.BlockInit;
import net.dragonloot.init.ConfigInit;
import net.dragonloot.init.NetworkInit;
import net.dragonloot.network.SyncPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.network.PacketDistributor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AnvilMenu.class)
public abstract class AnvilScreenHandlerMixin extends ItemCombinerMenu implements DragonAnvilInterface {

	@Shadow
	@Final
	private DataSlot cost;

	private boolean isDragonAnvil;

	protected AnvilScreenHandlerMixin(MenuType<?> menuType, int containerId, Inventory inventory, ContainerLevelAccess access) {
		super(menuType, containerId, inventory, access);
	}

	@Inject(method = "isValidBlock", at = @At("HEAD"))
	private void dragonloot$trackDragonAnvil(BlockState state, CallbackInfoReturnable<Boolean> cir) {
		this.isDragonAnvil = state.is(BlockInit.DRAGON_ANVIL_BLOCK.get());
		if (this.player instanceof ServerPlayer serverPlayer) {
			NetworkInit.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer), new SyncPacket(this.isDragonAnvil));
		}
	}

	@Inject(method = "onTake(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/DataSlot;set(I)V"), cancellable = true)
	private void dragonloot$onTake(Player player, ItemStack stack, CallbackInfo info) {
		if (this.isDragonAnvil) {
			this.access.execute((level, pos) -> {
				this.cost.set(0);
				level.levelEvent(null, 1030, pos, 0);
			});
			info.cancel();
		}
	}

	@Inject(method = "createResult", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/DataSlot;set(I)V", shift = At.Shift.AFTER))
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
