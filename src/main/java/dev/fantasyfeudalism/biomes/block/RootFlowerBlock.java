package dev.fantasyfeudalism.biomes.block;

import com.mojang.serialization.MapCodec;
import dev.fantasyfeudalism.biomes.RootlandsConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class RootFlowerBlock extends BushBlock {
    public static final MapCodec<RootFlowerBlock> CODEC = simpleCodec(RootFlowerBlock::new);
    private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 11, 14);

    public RootFlowerBlock(Properties properties) { super(properties); }
    @Override public MapCodec<RootFlowerBlock> codec() { return CODEC; }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return SHAPE; }
    @Override protected boolean mayPlaceOn(BlockState soil, BlockGetter level, BlockPos pos) { return soil.is(BlockTags.DIRT); }

    public static boolean affects(Entity entity) {
        if (!(entity instanceof LivingEntity)) return false;
        if (entity instanceof Player) return RootlandsConfig.FLOWER_PLAYERS.get();
        if (entity instanceof Enemy) return RootlandsConfig.FLOWER_HOSTILE.get();
        return entity instanceof Mob && RootlandsConfig.FLOWER_PASSIVE.get();
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!affects(entity)) return;
        double slow = RootlandsConfig.FLOWER_SLOW.get();
        if (slow > 0) entity.makeStuckInBlock(state, new Vec3(1.0 - slow, 1.0, 1.0 - slow));
        if (!level.isClientSide && RootlandsConfig.FLOWER_DAMAGE.get() > 0) {
            entity.hurt(level.damageSources().sweetBerryBush(), RootlandsConfig.FLOWER_DAMAGE.get().floatValue());
        }
    }

    @Override
    public PathType getBlockPathType(BlockState state, BlockGetter level, BlockPos pos, Mob mob) {
        return mob != null && affects(mob) ? PathType.DAMAGE_OTHER : PathType.OPEN;
    }
}
