package dev.fantasyfeudalism.biomes.worldgen;

import net.minecraft.world.level.block.RotatedPillarBlock;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Intertwined dark oak wood root arches, decorated with outward-facing spikes on their exposed faces. */
public final class GiantThorns {
    public static boolean place(WorldGenLevel level, BlockPos base, RandomSource random, boolean variants) {
        Direction along = variants ? Direction.from2DDataValue(random.nextInt(4)) : Direction.EAST;
        Direction side = along.getClockWise();
        int span = variants ? 9 + random.nextInt(6) : 12;
        int height = variants ? 9 + random.nextInt(8) : 12;
        BlockPos end = RootlandsScatterFeature.ground(level, base.getX() + along.getStepX() * span, base.getZ() + along.getStepZ() * span);
        if (end == null || Math.abs(end.getY() - base.getY()) > 5) return false;
        Map<BlockPos, BlockState> plan = new LinkedHashMap<>();
        for (int vine = 0; vine < 2; vine++) {
            BlockPos previous = base.relative(side, vine);
            plan.put(previous, Blocks.DARK_OAK_WOOD.defaultBlockState());
            for (int step = 0; step <= 28; step++) {
                double t = step / 28.0;
                int forward = (int) Math.round(t * span);
                int twist = (int) Math.round(Math.sin(t * Math.PI * 3 + vine * Math.PI) * (variants ? 3 : 2));
                int rise = (int) Math.round(Math.sin(t * Math.PI) * (height - vine * 2) + (end.getY() - base.getY()) * t);
                BlockPos point = base.relative(along, forward).relative(side, twist + vine).above(rise);
                line(plan, previous, point);
                if (step > 3 && step < 25 && step % 5 == 0) {
                    Direction spur = step % 2 == 0 ? side : side.getOpposite();
                    BlockPos tip = point.relative(spur, 2 + (variants ? random.nextInt(2) : 0)).above(2 + vine);
                    line(plan, point, tip);
                    line(plan, tip, tip.relative(along.getOpposite()).above());
                }
                previous = point;
            }
        }
        for (BlockPos pos : plan.keySet()) {
            if (!level.ensureCanWrite(pos) || !JaggedTree.replaceable(level.getBlockState(pos))) return false;
        }
        for (var entry : plan.entrySet()) level.setBlock(entry.getKey(), entry.getValue(), 2);
        level.setBlock(base.below(), Blocks.ROOTED_DIRT.defaultBlockState(), 2);
        level.setBlock(end.below(), Blocks.ROOTED_DIRT.defaultBlockState(), 2);
        RootThorns.decorate(level, plan.keySet(), random, 0.35);
        return true;
    }

    private static void line(Map<BlockPos, BlockState> plan, BlockPos from, BlockPos to) {
        BlockPos cursor = from;
        while (!cursor.equals(to)) {
            Direction.Axis axis;
            if (cursor.getX() != to.getX()) { cursor = cursor.offset(Integer.signum(to.getX() - cursor.getX()), 0, 0); axis = Direction.Axis.X; }
            else if (cursor.getZ() != to.getZ()) { cursor = cursor.offset(0, 0, Integer.signum(to.getZ() - cursor.getZ())); axis = Direction.Axis.Z; }
            else { cursor = cursor.offset(0, Integer.signum(to.getY() - cursor.getY()), 0); axis = Direction.Axis.Y; }
            plan.put(cursor, Blocks.DARK_OAK_WOOD.defaultBlockState().setValue(RotatedPillarBlock.AXIS, axis));
        }
    }
    private GiantThorns() {}
}
