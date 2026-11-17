package dev.fantasyfeudalism.biomes.worldgen;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;

/** A bent, tapering tree with angular climbing limbs. Plan first so blocked trees leave no partial trunks. */
public final class JaggedTree {
    public static boolean place(WorldGenLevel level, BlockPos base, RandomSource random, boolean variants) {
        int height = variants ? 20 + random.nextInt(11) : 24;
        Direction lean = variants ? Direction.from2DDataValue(random.nextInt(4)) : Direction.NORTH;
        Map<BlockPos, BlockState> logs = new LinkedHashMap<>();
        Map<BlockPos, BlockState> leaves = new LinkedHashMap<>();
        for (int y = 0; y < height; y++) {
            int bend = y / 6;
            BlockPos trunk = base.above(y).relative(lean, bend);
            log(logs, trunk, Direction.Axis.Y);
            if (y < height / 2) {
                log(logs, trunk.east(), Direction.Axis.Y);
                log(logs, trunk.south(), Direction.Axis.Y);
                log(logs, trunk.east().south(), Direction.Axis.Y);
            }
            // Horizontal bridges keep the angular changes in the trunk connected.
            if (y > 0 && y % 6 == 0) log(logs, trunk.relative(lean.getOpposite()), Direction.Axis.Y);
        }
        for (int i = 0; i < 6; i++) {
            int startY = height / 2 + i * 2;
            if (startY >= height - 2) break;
            Direction out = Direction.from2DDataValue((lean.get2DDataValue() + i + 1) % 4);
            BlockPos tip = base.above(startY).relative(lean, startY / 6);
            int length = variants ? 3 + random.nextInt(4) : 5;
            for (int step = 0; step < length; step++) {
                tip = tip.relative(out);
                log(logs, tip, out.getAxis());
                if (step % 2 == 0) {
                    tip = tip.above();
                    log(logs, tip, Direction.Axis.Y);
                }
            }
            crown(leaves, tip, random, variants, 2);
            // A bare upward spur extends beyond the foliage to keep a pointed silhouette.
            log(logs, tip.above(), Direction.Axis.Y);
            log(logs, tip.above(2), Direction.Axis.Y);
        }
        BlockPos top = base.above(height - 3).relative(lean, (height - 3) / 6);
        crown(leaves, top, random, variants, 3);
        for (BlockPos pos : logs.keySet()) {
            if (!level.ensureCanWrite(pos) || pos.getY() >= level.getMaxBuildHeight() || !replaceable(level.getBlockState(pos))) return false;
        }
        for (var entry : logs.entrySet()) level.setBlock(entry.getKey(), entry.getValue(), 2);
        for (BlockPos pos : leaves.keySet()) {
            if (!level.ensureCanWrite(pos) || logs.containsKey(pos) || !replaceable(level.getBlockState(pos))) continue;
            int distance = 7;
            for (BlockPos log : logs.keySet()) distance = Math.min(distance, pos.distManhattan(log));
            if (distance < 7) level.setBlock(pos, Blocks.DARK_OAK_LEAVES.defaultBlockState()
                    .setValue(LeavesBlock.DISTANCE, Math.max(1, distance)), 2);
        }
        return true;
    }

    private static void log(Map<BlockPos, BlockState> logs, BlockPos pos, Direction.Axis axis) {
        logs.put(pos, Blocks.DARK_OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, axis));
    }

    private static void crown(Map<BlockPos, BlockState> leaves, BlockPos center, RandomSource random, boolean variants, int radius) {
        for (int dy = -1; dy <= 2; dy++) {
            int reach = dy == 2 ? 1 : radius;
            for (int dx = -reach; dx <= reach; dx++) for (int dz = -reach; dz <= reach; dz++) {
                if (Math.abs(dx) + Math.abs(dz) > reach + 1) continue;
                if (variants && random.nextInt(7) == 0) continue;
                leaves.put(center.offset(dx, dy, dz), Blocks.DARK_OAK_LEAVES.defaultBlockState());
            }
        }
    }

    public static boolean replaceable(BlockState state) {
        return state.isAir() || state.is(BlockTags.LEAVES) || state.canBeReplaced();
    }

    private JaggedTree() {}
}
