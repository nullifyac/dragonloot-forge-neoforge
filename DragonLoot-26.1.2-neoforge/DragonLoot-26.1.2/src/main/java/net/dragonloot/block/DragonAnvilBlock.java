package net.dragonloot.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class DragonAnvilBlock extends AnvilBlock {

    public static final MapCodec<AnvilBlock> CODEC = simpleCodec(DragonAnvilBlock::new);

    public DragonAnvilBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<AnvilBlock> codec() {
        return CODEC;
    }

}
