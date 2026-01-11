package net.dragonloot.mixin;

import net.dragonloot.init.BlockInit;
import net.minecraft.block.AnvilBlock;
import net.minecraft.block.BlockState;
import net.minecraft.state.DirectionProperty;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AnvilBlock.class)
public class AnvilBlockMixin {
	@Shadow
	@Final
	public static DirectionProperty FACING;

	@Inject(method = "damage", at = @At("HEAD"), cancellable = true)
	private static void dragonloot$redirectLandingState(BlockState fallingState, CallbackInfoReturnable<BlockState> info) {
		if (fallingState.is(BlockInit.DRAGON_ANVIL_BLOCK.get())) {
			info.setReturnValue(BlockInit.DRAGON_ANVIL_BLOCK.get().defaultBlockState().setValue(FACING, fallingState.getValue(FACING)));
		}
	}

}
