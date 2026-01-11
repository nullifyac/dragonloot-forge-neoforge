package net.dragonloot.init;

import net.dragonloot.DragonLootMain;
import net.dragonloot.block.DragonAnvilBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class BlockInit {

	private BlockInit() {
	}

	public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, DragonLootMain.MOD_ID);

	public static final RegistryObject<DragonAnvilBlock> DRAGON_ANVIL_BLOCK = BLOCKS.register("dragon_anvil", () -> new DragonAnvilBlock(BlockBehaviour.Properties.copy(Blocks.ANVIL)));
}
