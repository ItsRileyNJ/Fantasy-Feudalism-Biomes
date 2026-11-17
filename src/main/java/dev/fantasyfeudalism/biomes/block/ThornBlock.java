package dev.fantasyfeudalism.biomes.block;

import dev.fantasyfeudalism.biomes.RootlandsConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.BlockGetter;

public final class ThornBlock extends Block {
    private static final VoxelShape OUTLINE = Block.box(1, 0, 1, 15, 15, 15);
    public ThornBlock(Properties properties) { super(properties); }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return OUTLINE;
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!(entity instanceof LivingEntity)) return;
        if (RootlandsConfig.THORNS_SLOW.get()) entity.makeStuckInBlock(state, new Vec3(0.65, 0.9, 0.65));
        if (!level.isClientSide && RootlandsConfig.THORNS_HURT.get() && RootlandsConfig.THORN_DAMAGE.get() > 0) {
            entity.hurt(level.damageSources().sweetBerryBush(), RootlandsConfig.THORN_DAMAGE.get().floatValue());
        }
    }
}
