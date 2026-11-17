package dev.fantasyfeudalism.biomes.worldgen;

import com.mojang.serialization.Codec;
import dev.fantasyfeudalism.biomes.FantasyBiomes;
import dev.fantasyfeudalism.biomes.RootlandsConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public final class RootlandsScatterFeature extends Feature<NoneFeatureConfiguration> {
    public enum Kind { TREE, ROOT, THORN, FLOWER }
    private final Kind kind;

    public RootlandsScatterFeature(Codec<NoneFeatureConfiguration> codec, Kind kind) {
        super(codec);
        this.kind = kind;
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        int density = switch (kind) {
            case TREE -> RootlandsConfig.TREE_DENSITY.get();
            case ROOT -> RootlandsConfig.ROOT_DENSITY.get();
            case THORN -> RootlandsConfig.THORN_DENSITY.get();
            case FLOWER -> 0;
        };
        double scaled = kind == Kind.FLOWER ? RootlandsConfig.FLOWER_FREQUENCY.get() : density * RootlandsConfig.STRUCTURE_FREQUENCY.get();
        int attempts = (int) scaled + (random.nextDouble() < scaled % 1 ? 1 : 0);
        boolean placed = false;
        // One invocation per chunk. Scatter inside that chunk and verify each sampled biome at its surface.
        int x0 = context.origin().getX() & ~15;
        int z0 = context.origin().getZ() & ~15;
        boolean variants = RootlandsConfig.VARIANTS.get();
        for (int i = 0; i < attempts; i++) {
            BlockPos pos = ground(level, x0 + random.nextInt(16), z0 + random.nextInt(16));
            if (pos == null || !level.getBiome(pos).is(FantasyBiomes.ROOTLANDS)) continue;
            if (kind == Kind.TREE) {
                if (JaggedTree.place(level, pos, random, variants)) {
                    roots(level, pos, random, variants);
                    placed = true;
                }
            } else if (kind == Kind.ROOT) {
                placed |= roots(level, pos, random, variants);
            } else if (kind == Kind.THORN) {
                placed |= thorns(level, pos, random, variants);
            } else {
                placed |= flowers(level, pos, random);
            }
        }
        return placed;
    }

    /** Ignore canopy and tall plants while locating dirt; never replace arbitrary structures or rock. */
    public static BlockPos ground(WorldGenLevel level, int x, int z) {
        int top = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
        for (int y = top; y >= Math.max(level.getMinBuildHeight() + 1, top - 40); y--) {
            BlockPos soil = new BlockPos(x, y - 1, z);
            if (level.getBlockState(soil).is(BlockTags.DIRT)) {
                BlockPos pos = soil.above();
                if (JaggedTree.replaceable(level.getBlockState(pos)) && level.getFluidState(pos).isEmpty()) return pos;
                return null;
            }
        }
        return null;
    }

    public static boolean roots(WorldGenLevel level, BlockPos center, RandomSource random, boolean variants) {
        boolean placed = false;
        int arms = variants ? 3 + random.nextInt(3) : 4;
        for (int arm = 0; arm < arms; arm++) {
            Direction dir = Direction.from2DDataValue(arm % 4);
            int length = variants ? 3 + random.nextInt(5) : 5;
            BlockPos cursor = center;
            for (int step = 0; step < length; step++) {
                cursor = cursor.relative(dir);
                if (variants && step > 1 && random.nextInt(4) == 0) cursor = cursor.relative(dir.getClockWise());
                BlockPos floor = ground(level, cursor.getX(), cursor.getZ());
                if (floor == null || Math.abs(floor.getY() - cursor.getY()) > 2) break;
                cursor = floor;
                if (!JaggedTree.replaceable(level.getBlockState(cursor))) break;
                BlockState wood = (variants && random.nextInt(5) == 0 ? Blocks.STRIPPED_DARK_OAK_LOG : Blocks.DARK_OAK_LOG)
                        .defaultBlockState().setValue(RotatedPillarBlock.AXIS, dir.getAxis());
                level.setBlock(cursor, wood, 2);
                level.setBlock(cursor.below(), Blocks.ROOTED_DIRT.defaultBlockState(), 2);
                placed = true;
                // Short rising knuckles give some roots a rough, jagged profile.
                if (step == 1 && JaggedTree.replaceable(level.getBlockState(cursor.above()))) {
                    level.setBlock(cursor.above(), Blocks.DARK_OAK_LOG.defaultBlockState(), 2);
                }
            }
        }
        return placed;
    }

    public static boolean thorns(WorldGenLevel level, BlockPos center, RandomSource random, boolean variants) {
        boolean placed = false;
        Direction dir = variants ? Direction.from2DDataValue(random.nextInt(4)) : Direction.EAST;
        int length = variants ? 2 + random.nextInt(5) : 4;
        for (int step = 0; step < length; step++) {
            BlockPos column = center.relative(dir, step);
            BlockPos floor = ground(level, column.getX(), column.getZ());
            if (floor == null || Math.abs(floor.getY() - center.getY()) > 2) continue;
            int height = variants ? 1 + random.nextInt(3) : 2;
            for (int y = 0; y < height; y++) {
                BlockPos pos = floor.above(y);
                if (!JaggedTree.replaceable(level.getBlockState(pos))) break;
                level.setBlock(pos, FantasyBiomes.THORN.get().defaultBlockState(), 2);
                placed = true;
            }
            level.setBlock(floor.below(), Blocks.COARSE_DIRT.defaultBlockState(), 2);
        }
        return placed;
    }

    public static boolean flowers(WorldGenLevel level, BlockPos center, RandomSource random) {
        int minimum = Math.min(RootlandsConfig.FLOWER_MIN_SIZE.get(), RootlandsConfig.FLOWER_MAX_SIZE.get());
        int maximum = Math.max(RootlandsConfig.FLOWER_MIN_SIZE.get(), RootlandsConfig.FLOWER_MAX_SIZE.get());
        int desired = minimum + random.nextInt(maximum - minimum + 1);
        int placed = 0;
        for (int i = 0; i < desired * 4 && placed < desired; i++) {
            BlockPos pos = ground(level, center.getX() + random.nextInt(7) - 3, center.getZ() + random.nextInt(7) - 3);
            if (pos == null || !level.getBiome(pos).is(FantasyBiomes.ROOTLANDS)) continue;
            BlockState flower = FantasyBiomes.ROOT_FLOWER.get().defaultBlockState();
            if (!flower.canSurvive(level, pos)) continue;
            level.setBlock(pos, flower, 2);
            placed++;
        }
        return placed > 0;
    }
}
