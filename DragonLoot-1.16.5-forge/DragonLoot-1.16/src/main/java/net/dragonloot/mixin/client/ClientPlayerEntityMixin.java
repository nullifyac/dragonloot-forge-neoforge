package net.dragonloot.mixin.client;

import com.mojang.authlib.GameProfile;
import net.dragonloot.init.ItemInit;
import net.minecraft.client.entity.player.AbstractClientPlayerEntity;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.network.play.ClientPlayNetHandler;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.CEntityActionPacket;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@OnlyIn(Dist.CLIENT)
@Mixin(ClientPlayerEntity.class)
public abstract class ClientPlayerEntityMixin extends AbstractClientPlayerEntity {

    @Shadow
    @Mutable
    @Final
    public ClientPlayNetHandler connection;

    protected ClientPlayerEntityMixin(ClientWorld level, GameProfile profile) {
        super(level, profile);
    }

    @Inject(method = "aiStep", at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/client/entity/player/ClientPlayerEntity;getItemBySlot(Lnet/minecraft/inventory/EquipmentSlotType;)Lnet/minecraft/item/ItemStack;"))
    private void dragonloot$enableDragonFlight(CallbackInfo info) {
        ItemStack chest = this.getItemBySlot(EquipmentSlotType.CHEST);
        if (chest.getItem() == ItemInit.UPGRADED_DRAGON_CHESTPLATE.get() && this.tryToStartFallFlying()) {
            this.connection.send(new CEntityActionPacket(this, CEntityActionPacket.Action.START_FALL_FLYING));
        }
    }
}
