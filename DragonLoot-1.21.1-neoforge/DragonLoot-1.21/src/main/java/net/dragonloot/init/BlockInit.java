package net.dragonloot.init;

import net.dragonloot.DragonLootMain;
import net.dragonloot.block.DragonAnvilBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class BlockInit {

	private BlockInit() {
	}

	public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, DragonLootMain.MOD_ID);

	public static final DeferredHolder<Block, DragonAnvilBlock> DRAGON_ANVIL_BLOCK = BLOCKS.register("dragon_anvil", () -> new DragonAnvilBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.ANVIL)));
}
