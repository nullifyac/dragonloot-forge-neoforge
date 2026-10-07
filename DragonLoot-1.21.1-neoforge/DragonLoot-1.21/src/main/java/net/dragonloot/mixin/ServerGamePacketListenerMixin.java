package net.dragonloot.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalDoubleRef;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerMixin {

    @WrapOperation(
            method = "handleMovePlayer",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V"),
            require = 1,
            allow = 1
    )
    private void dragonloot$captureAcceptedMovement(ServerPlayer player, MoverType type, Vec3 movement,
                                                   Operation<Void> original,
                                                   @Share("acceptedMovementY") LocalDoubleRef acceptedMovementY) {
        acceptedMovementY.set(movement.y);
        original.call(player, type, movement);
    }

    @WrapOperation(
            method = "handleMovePlayer",
            at = @At(value = "FIELD", target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;clientIsFloating:Z", opcode = Opcodes.PUTFIELD),
            require = 1,
            allow = 1
    )
    private void dragonloot$preserveVanillaDescentCheck(ServerGamePacketListenerImpl listener, boolean floating,
                                                      Operation<Void> original,
                                                      @Share("acceptedMovementY") LocalDoubleRef acceptedMovementY) {
        // NeoForm's decompile/recompile loses vanilla's original Y delta and tests
        // the zeroed collision residual instead. Keep every other floating gate;
        // this additional descent check is also idempotent on corrected loaders.
        original.call(listener, floating && acceptedMovementY.get() >= -0.03125D);
    }
}
