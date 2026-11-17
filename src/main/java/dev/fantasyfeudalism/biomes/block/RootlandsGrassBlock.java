package dev.fantasyfeudalism.biomes.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A dark grass plant, deliberately separate from the ground's grass block. */
public final class RootlandsGrassBlock extends BushBlock {
    public static final MapCodec<RootlandsGrassBlock> CODEC = simpleCodec(RootlandsGrassBlock::new);
    private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 12, 14);
    public RootlandsGrassBlock(Properties properties) { super(properties); }
    @Override public MapCodec<RootlandsGrassBlock> codec() { return CODEC; }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return SHAPE; }
}
