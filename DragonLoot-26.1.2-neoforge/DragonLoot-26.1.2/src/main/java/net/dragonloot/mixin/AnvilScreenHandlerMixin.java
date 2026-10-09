package net.dragonloot.mixin;

import net.dragonloot.access.DragonAnvilInterface;
import net.dragonloot.init.BlockInit;
import net.dragonloot.init.ConfigInit;
import net.dragonloot.init.NetworkInit;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.ItemCombinerMenuSlotDefinition;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AnvilMenu.class)
public abstract class AnvilScreenHandlerMixin extends ItemCombinerMenu implements DragonAnvilInterface {
    @Shadow
    @Final
    private DataSlot cost;

    @Unique
    private boolean dragonloot$isDragonAnvil;
    @Unique
    private boolean dragonloot$sentFlag;
    @Unique
    private boolean dragonloot$lastSentFlag;

    protected AnvilScreenHandlerMixin(MenuType<?> menuType, int containerId, Inventory inventory, ContainerLevelAccess access, ItemCombinerMenuSlotDefinition slots) {
        super(menuType, containerId, inventory, access, slots);
    }

    @Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V", at = @At("TAIL"))
    private void dragonloot$initializeDragonAnvil(int id, Inventory inventory, ContainerLevelAccess access, CallbackInfo info) {
        this.dragonloot$isDragonAnvil = access.evaluate((level, pos) -> level.getBlockState(pos).is(BlockInit.DRAGON_ANVIL_BLOCK.get()), false);
    }

    @Inject(method = "isValidBlock", at = @At("HEAD"), cancellable = true)
    private void dragonloot$trackDragonAnvil(BlockState state, CallbackInfoReturnable<Boolean> cir) {
        this.dragonloot$isDragonAnvil = state.is(BlockInit.DRAGON_ANVIL_BLOCK.get());
        // The menu-open packet must precede this sync. Avoid sending a flag for a
        // menu that has not yet become the player's active menu.
        if (this.player instanceof ServerPlayer serverPlayer && serverPlayer.containerMenu == (Object) this
            && (!this.dragonloot$sentFlag || this.dragonloot$lastSentFlag != this.dragonloot$isDragonAnvil)) {
            NetworkInit.sendDragonAnvilSync(serverPlayer, this.dragonloot$isDragonAnvil);
            this.dragonloot$sentFlag = true;
            this.dragonloot$lastSentFlag = this.dragonloot$isDragonAnvil;
        }
        if (this.dragonloot$isDragonAnvil) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "createResultInternal", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/DataSlot;set(I)V", shift = At.Shift.AFTER))
    private void dragonloot$clampCost(CallbackInfo info) {
        if (this.cost.get() > 30 && this.dragonloot$isDragonAnvil && ConfigInit.CONFIG.dragon_anvil_no_cap) {
            this.cost.set(30);
        }
    }

    @Inject(method = "getCost", at = @At("HEAD"), cancellable = true)
    private void dragonloot$getCost(CallbackInfoReturnable<Integer> info) {
        if (this.cost.get() > 30 && this.dragonloot$isDragonAnvil && ConfigInit.CONFIG.dragon_anvil_no_cap) {
            info.setReturnValue(30);
        }
    }

    @Override
    public void setDragonAnvil(boolean isDragonAnvil) {
        this.dragonloot$isDragonAnvil = isDragonAnvil;
    }
}
