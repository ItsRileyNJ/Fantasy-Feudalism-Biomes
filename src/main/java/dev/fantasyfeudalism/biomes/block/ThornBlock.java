package dev.fantasyfeudalism.biomes.block;

import dev.fantasyfeudalism.biomes.FantasyBiomes;
import dev.fantasyfeudalism.biomes.RootlandsConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A single outward-facing root spike; the wooden root and its thorn are separate blocks. */
public final class ThornBlock extends Block {
    // Retained so pre-0.5 saved thorn states remain readable. Facing controls the attachment and model.
    public static final EnumProperty<Direction.Axis> AXIS = RotatedPillarBlock.AXIS;
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final TagKey<Block> SUPPORTS = TagKey.create(Registries.BLOCK, FantasyBiomes.id("thorn_supports"));

    public ThornBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(AXIS, Direction.Axis.Y).setValue(FACING, Direction.UP));
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(AXIS, FACING); }

    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction face = context.getClickedFace();
        BlockState state = defaultBlockState().setValue(FACING, face).setValue(AXIS, face.getAxis());
        return state.canSurvive(context.getLevel(), context.getClickedPos()) ? state : null;
    }

    @Override protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.relative(state.getValue(FACING).getOpposite())).is(SUPPORTS);
    }

    @Override protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return direction == state.getValue(FACING).getOpposite() && !state.canSurvive(level, pos)
                ? Blocks.AIR.defaultBlockState() : super.updateShape(state, direction, neighbor, level, pos, neighborPos);
    }

    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case UP -> Block.box(4, 0, 4, 12, 14, 12);
            case DOWN -> Block.box(4, 2, 4, 12, 16, 12);
            case NORTH -> Block.box(4, 4, 2, 12, 12, 16);
            case SOUTH -> Block.box(4, 4, 0, 12, 12, 14);
            case WEST -> Block.box(2, 4, 4, 16, 12, 12);
            case EAST -> Block.box(0, 4, 4, 14, 12, 12);
        };
    }

    @Override protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!(entity instanceof LivingEntity)) return;
        if (RootlandsConfig.THORNS_SLOW.get()) entity.makeStuckInBlock(state, new Vec3(0.65, 0.9, 0.65));
        if (!level.isClientSide && RootlandsConfig.THORNS_HURT.get() && RootlandsConfig.THORN_DAMAGE.get() > 0) {
            entity.hurt(level.damageSources().sweetBerryBush(), RootlandsConfig.THORN_DAMAGE.get().floatValue());
        }
    }
}
