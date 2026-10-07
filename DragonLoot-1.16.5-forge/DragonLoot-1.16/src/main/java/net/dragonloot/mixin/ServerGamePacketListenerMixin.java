package net.dragonloot.mixin;

import net.minecraft.network.play.ServerPlayNetHandler;
import net.minecraft.util.math.vector.Vector3d;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayNetHandler.class)
public abstract class ServerGamePacketListenerMixin {

    @Shadow
    private boolean clientIsFloating;

    @Unique
    private double dragonloot$acceptedMovementY;

    @ModifyArg(
            method = "handleMovePlayer",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/ServerPlayerEntity;move(Lnet/minecraft/entity/MoverType;Lnet/minecraft/util/math/vector/Vector3d;)V"),
            index = 1,
            require = 1,
            allow = 1
    )
    private Vector3d dragonloot$captureAcceptedMovement(Vector3d movement) {
        dragonloot$acceptedMovementY = movement.y;
        return movement;
    }

    @Inject(
            method = "handleMovePlayer",
            at = @At(value = "FIELD", target = "Lnet/minecraft/network/play/ServerPlayNetHandler;clientIsFloating:Z", opcode = Opcodes.PUTFIELD, shift = At.Shift.AFTER),
            require = 1,
            allow = 1
    )
    private void dragonloot$preserveVanillaDescentCheck(CallbackInfo info) {
        // The loader checks a zeroed collision residual instead of vanilla's
        // original accepted Y delta. Keep all other gates and hover detection;
        // this additional descent check is idempotent on corrected loaders.
        clientIsFloating = clientIsFloating && dragonloot$acceptedMovementY >= -0.03125D;
    }
}

