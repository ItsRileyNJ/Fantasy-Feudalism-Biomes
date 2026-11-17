package dev.fantasyfeudalism.biomes.worldgen;

import com.mojang.serialization.Codec;
import dev.fantasyfeudalism.biomes.FantasyBiomes;
import dev.fantasyfeudalism.biomes.RootlandsConfig;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** First decoration stage: grade shores before trees, plants, ores and structure pieces are placed. */
public final class RootlandsShoreFeature extends Feature<NoneFeatureConfiguration> {
    private static final Map<ServerLevel, ConcurrentHashMap<Long, ShorelineProfile.Sample>> CACHE =
            Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<ServerLevel, ConcurrentHashMap<Long, Boolean>> BIOME_CACHE =
            Collections.synchronizedMap(new WeakHashMap<>());

    public RootlandsShoreFeature(Codec<NoneFeatureConfiguration> codec) { super(codec); }

    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        if (!RootlandsConfig.GENTLE_SHORES.get() || !(context.chunkGenerator() instanceof NoiseBasedChunkGenerator generator)) return false;
        WorldGenLevel region = context.level();
        ServerLevel level = region.getLevel();
        var random = level.getChunkSource().randomState();
        int sea = generator.getSeaLevel();
        int chunkX = context.origin().getX() & ~15;
        int chunkZ = context.origin().getZ() & ~15;
        int width = RootlandsConfig.SHORE_WIDTH.get();
        int margin = ShorelineProfile.sampleMargin(width);
        ConcurrentHashMap<Long, Boolean> rootBiomes;
        synchronized (BIOME_CACHE) { rootBiomes = BIOME_CACHE.computeIfAbsent(level, ignored -> new ConcurrentHashMap<>()); }
        if (rootBiomes.size() > 131072) rootBiomes.clear();
        // Cache biome-only queries first, so far-away biomes never need expensive terrain height queries.
        java.util.function.BiPredicate<Integer, Integer> isRootlands = (x, z) -> rootBiomes.computeIfAbsent(BlockPos.asLong(x, 0, z), ignored ->
                generator.getBiomeSource().getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(sea + 8), QuartPos.fromBlock(z), random.sampler()).is(FantasyBiomes.ROOTLANDS));
        boolean nearby = false;
        for (int x = chunkX - margin; x <= chunkX + 16 + margin && !nearby; x += ShorelineProfile.STEP) {
            for (int z = chunkZ - margin; z <= chunkZ + 16 + margin; z += ShorelineProfile.STEP) {
                if (isRootlands.test(x, z)) { nearby = true; break; }
            }
        }
        if (!nearby) return false;
        ConcurrentHashMap<Long, ShorelineProfile.Sample> samples;
        synchronized (CACHE) { samples = CACHE.computeIfAbsent(level, ignored -> new ConcurrentHashMap<>()); }
        if (samples.size() > 65536) samples.clear();
        ShorelineProfile profile = new ShorelineProfile(chunkX, chunkZ, sea, width, (x, z) ->
                samples.computeIfAbsent(BlockPos.asLong(x, 0, z), ignored -> {
                    boolean wet = waterAtSeaLevel(random, x, z, sea);
                    boolean rootlands = isRootlands.test(x, z);
                    return new ShorelineProfile.Sample(wet, rootlands);
                }));
        boolean changed = false;
        // Only write the current chunk; all neighboring samples are deterministic generator queries, not chunk loads.
        for (int x = chunkX; x < chunkX + 16; x++) for (int z = chunkZ; z < chunkZ + 16; z++) {
            if (profile.strength(x, z) <= 0) continue;
            int originalTop = region.getHeight(Heightmap.Types.OCEAN_FLOOR, x, z) - 1;
            changed |= reshapeColumn(region, x, z, originalTop, profile.targetHeight(x, z, originalTop), sea);
        }
        return changed;
    }

    /** Sample the original waterline without scanning hundreds of vertical terrain cells.
     * The uncaved surface density rejects underground caves as coastlines; near the surface, final density
     * refines the water/land decision. World-aligned sampling and interpolation absorb small roughness differences.
     */
    public static boolean waterAtSeaLevel(RandomState random, int x, int z, int sea) {
        var point = new DensityFunction.SinglePointContext(x, sea, z);
        double surface = random.router().initialDensityWithoutJaggedness().compute(point);
        return Math.abs(surface) < 0.125 ? random.router().finalDensity().compute(point) <= 0 : surface < 0;
    }

    /** Atomically validate a column before changing natural ground; preserve foreign blocks and block entities. */
    public static boolean reshapeColumn(WorldGenLevel level, int x, int z, int originalTop, int targetTop, int sea) {
        if (targetTop == originalTop || targetTop <= level.getMinBuildHeight() || targetTop >= level.getMaxBuildHeight() - 1) return false;
        if (!level.ensureCanWrite(new BlockPos(x, targetTop, z))) return false;
        int lower = Math.min(originalTop, targetTop);
        int upper = Math.max(originalTop, targetTop);
        // Reject a column containing built structures, logs, ores or mod blocks we don't explicitly know are terrain.
        for (int y = Math.max(lower - 3, level.getMinBuildHeight()); y <= Math.max(upper, sea); y++) {
            BlockState state = level.getBlockState(new BlockPos(x, y, z));
            if (state.hasBlockEntity() || !natural(state)) return false;
            if (targetTop > originalTop && y > originalTop && !state.isAir() && !state.getFluidState().is(FluidTags.WATER)) return false;
        }
        if (targetTop < originalTop) {
            for (int y = originalTop; y > targetTop; y--) {
                level.setBlock(new BlockPos(x, y, z), y <= sea ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState(), 2);
            }
        } else {
            for (int y = originalTop + 1; y <= targetTop; y++) level.setBlock(new BlockPos(x, y, z), Blocks.STONE.defaultBlockState(), 2);
        }
        // A shallow gravel bed continues below the waterline; turf and dirt continue above it.
        level.setBlock(new BlockPos(x, targetTop, z), (targetTop < sea ? Blocks.GRAVEL : Blocks.GRASS_BLOCK).defaultBlockState(), 2);
        // Support the new cap even when a shallow cave crossed the old cliff; gravel must not collapse into it.
        for (int y = targetTop - 1; y >= Math.max(targetTop - 3, level.getMinBuildHeight() + 1); y--) {
            level.setBlock(new BlockPos(x, y, z), (targetTop < sea ? Blocks.STONE : Blocks.DIRT).defaultBlockState(), 2);
        }
        for (int y = targetTop + 1; y <= sea; y++) level.setBlock(new BlockPos(x, y, z), Blocks.WATER.defaultBlockState(), 2);
        return true;
    }

    private static boolean natural(BlockState state) {
        return state.isAir() || state.is(Blocks.WATER)
                || state.is(BlockTags.BASE_STONE_OVERWORLD) || state.is(BlockTags.DIRT) || state.is(BlockTags.SAND)
                || state.is(Blocks.GRAVEL) || state.is(Blocks.CLAY) || state.is(Blocks.SANDSTONE)
                || state.is(Blocks.RED_SANDSTONE) || state.is(Blocks.SNOW) || state.is(Blocks.SNOW_BLOCK)
                || state.is(Blocks.ICE) || state.is(Blocks.PACKED_ICE);
    }
}
