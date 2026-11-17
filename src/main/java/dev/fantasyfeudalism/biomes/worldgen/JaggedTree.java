package dev.fantasyfeudalism.biomes.worldgen;

import dev.fantasyfeudalism.biomes.FantasyBiomes;
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

/** Four original tree forms, with connected crooked boughs, secondary forks and irregular crowns. */
public final class JaggedTree {
    public enum Variant { CROOKED_CROWN, FORKED_SPIRE, WINDSWEPT, HOLLOW_ELDER }

    public static boolean place(WorldGenLevel level, BlockPos base, RandomSource random, boolean variants) {
        Variant variant = variants ? Variant.values()[random.nextInt(Variant.values().length)] : Variant.CROOKED_CROWN;
        return placeVariant(level, base, random, variant, variants);
    }

    public static boolean placeVariant(WorldGenLevel level, BlockPos base, RandomSource random, Variant variant, boolean varied) {
        int height = switch (variant) {
            case CROOKED_CROWN -> 24;
            case FORKED_SPIRE -> 28;
            case WINDSWEPT -> 25;
            case HOLLOW_ELDER -> 32;
        };
        if (varied) height += random.nextInt(7);
        Direction lean = varied ? Direction.from2DDataValue(random.nextInt(4)) : Direction.NORTH;
        Map<BlockPos, BlockState> logs = new LinkedHashMap<>();
        Map<BlockPos, BlockState> leaves = new LinkedHashMap<>();
        BlockPos previous = base;
        for (int y = 0; y < height; y++) {
            BlockPos center = trunk(base, y, lean, variant);
            line(logs, previous.above(y == 0 ? 0 : 1), center);
            log(logs, center, Direction.Axis.Y);
            if (variant == Variant.HOLLOW_ELDER && y < height / 2) {
                for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dz == 0 && y > 1 && y < height / 3) continue;
                    log(logs, center.offset(dx, 0, dz), Direction.Axis.Y);
                }
                if (y > 1 && y < height / 3) logs.remove(center);
            } else if (y < height / 2) {
                log(logs, center.east(), Direction.Axis.Y);
                log(logs, center.south(), Direction.Axis.Y);
                log(logs, center.east().south(), Direction.Axis.Y);
            }
            previous = center;
        }
        // Broad, gnarled buttresses taper into the trunk instead of ending as a straight pillar.
        for (Direction direction : Direction.Plane.HORIZONTAL) for (int step = 1; step <= 3; step++) {
            BlockPos foot = base.relative(direction, step);
            for (int y = 0; y <= 3 - step; y++) log(logs, foot.above(y), y == 0 ? direction.getAxis() : Direction.Axis.Y);
        }
        int rank = 0;
        for (int y = height / 3; y < height - 3; y += 3) {
            Direction out = Direction.from2DDataValue((lean.get2DDataValue() + rank++ + 1) % 4);
            BlockPos tip = trunk(base, y, lean, variant);
            int length = (variant == Variant.HOLLOW_ELDER ? 7 : 5) + (varied ? random.nextInt(2) : 0);
            for (int step = 1; step <= length; step++) {
                BlockPos next = tip.relative(out).above(step % 2 == 1 ? 1 : 0);
                line(logs, tip, next);
                tip = next;
                if (step == length / 2) {
                    BlockPos fork = tip.relative(out.getClockWise(), 2).above(2);
                    line(logs, tip, fork);
                    crown(leaves, fork, random, varied, 2);
                    log(logs, fork.above(), Direction.Axis.Y);
                }
            }
            crown(leaves, tip, random, varied, variant == Variant.HOLLOW_ELDER ? 4 : 3);
            log(logs, tip.above(), Direction.Axis.Y);
            log(logs, tip.above(2), Direction.Axis.Y);
            if (rank % 2 == 0) {
                // Bare hanging root-like twigs make the lower silhouette ragged and asymmetric.
                line(logs, tip, tip.below(2));
                crown(leaves, tip.below(2), random, varied, 1);
            }
        }
        BlockPos top = trunk(base, height - 3, lean, variant);
        if (variant == Variant.FORKED_SPIRE) {
            for (Direction direction : new Direction[]{lean.getClockWise(), lean.getCounterClockWise()}) {
                BlockPos forkTop = top.relative(direction, 4).above(4);
                line(logs, top.below(3), forkTop);
                crown(leaves, forkTop.below(), random, varied, 3);
                log(logs, forkTop.above(), Direction.Axis.Y);
            }
        } else crown(leaves, top, random, varied, variant == Variant.WINDSWEPT ? 4 : 3);

        for (BlockPos pos : logs.keySet()) {
            if (!level.ensureCanWrite(pos) || pos.getY() >= level.getMaxBuildHeight() || !replaceable(level.getBlockState(pos))) return false;
        }
        for (var entry : logs.entrySet()) level.setBlock(entry.getKey(), entry.getValue(), 2);
        for (BlockPos pos : leaves.keySet()) {
            if (!level.ensureCanWrite(pos) || logs.containsKey(pos) || !replaceable(level.getBlockState(pos))) continue;
            int distance = 7;
            for (BlockPos log : logs.keySet()) distance = Math.min(distance, pos.distManhattan(log));
            if (distance < 7) level.setBlock(pos, FantasyBiomes.ROOTLANDS_LEAVES.get().defaultBlockState()
                    .setValue(LeavesBlock.DISTANCE, Math.max(1, distance)), 2);
        }
        return true;
    }

    private static BlockPos trunk(BlockPos base, int y, Direction lean, Variant variant) {
        int bend = variant == Variant.WINDSWEPT ? y / 4 : y / 6;
        BlockPos pos = base.above(y).relative(lean, bend);
        return variant == Variant.WINDSWEPT ? pos.relative(lean.getClockWise(), (y / 7) % 2) : pos;
    }

    private static void line(Map<BlockPos, BlockState> logs, BlockPos from, BlockPos to) {
        BlockPos cursor = from;
        while (!cursor.equals(to)) {
            Direction.Axis axis;
            if (cursor.getY() != to.getY()) { cursor = cursor.offset(0, Integer.signum(to.getY() - cursor.getY()), 0); axis = Direction.Axis.Y; }
            else if (cursor.getX() != to.getX()) { cursor = cursor.offset(Integer.signum(to.getX() - cursor.getX()), 0, 0); axis = Direction.Axis.X; }
            else { cursor = cursor.offset(0, 0, Integer.signum(to.getZ() - cursor.getZ())); axis = Direction.Axis.Z; }
            log(logs, cursor, axis);
        }
    }

    private static void log(Map<BlockPos, BlockState> logs, BlockPos pos, Direction.Axis axis) {
        logs.put(pos, Blocks.DARK_OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, axis));
    }

    private static void crown(Map<BlockPos, BlockState> leaves, BlockPos center, RandomSource random, boolean varied, int radius) {
        for (int dy = -1; dy <= 2; dy++) {
            int reach = dy == 2 ? Math.max(1, radius - 2) : radius;
            for (int dx = -reach; dx <= reach; dx++) for (int dz = -reach; dz <= reach; dz++) {
                if (Math.abs(dx) + Math.abs(dz) > reach + 1 || (varied && random.nextInt(6) == 0)) continue;
                leaves.put(center.offset(dx, dy, dz), FantasyBiomes.ROOTLANDS_LEAVES.get().defaultBlockState());
            }
        }
    }

    public static boolean replaceable(BlockState state) { return state.isAir() || state.is(BlockTags.LEAVES) || state.canBeReplaced(); }
    private JaggedTree() {}
}
