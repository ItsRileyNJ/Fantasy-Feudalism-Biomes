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
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** Shore grading finishes CARVERS, before any neighboring chunk can decorate. The old feature ID is a no-op. */
public final class RootlandsShoreFeature extends Feature<NoneFeatureConfiguration> {
    private static final Map<RandomState, ConcurrentHashMap<Long, ShorelineProfile.Sample>> CACHE =
            Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<RandomState, ConcurrentHashMap<Long, Boolean>> BIOME_CACHE =
            Collections.synchronizedMap(new WeakHashMap<>());

    public RootlandsShoreFeature(Codec<NoneFeatureConfiguration> codec) { super(codec); }

    /** Compatibility for old data-pack references. Terrain must never be edited during FEATURES. */
    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) { return false; }

    public static boolean gradeAfterCarvers(WorldGenLevel region, NoiseBasedChunkGenerator generator, RandomState random, ChunkAccess chunk) {
        if (!RootlandsConfig.GENTLE_SHORES.get() || !region.getLevel().dimension().equals(net.minecraft.world.level.Level.OVERWORLD)) return false;
        int sea = generator.getSeaLevel();
        int chunkX = chunk.getPos().getMinBlockX();
        int chunkZ = chunk.getPos().getMinBlockZ();
        int width = RootlandsConfig.SHORE_WIDTH.get();
        int margin = ShorelineProfile.sampleMargin(width);
        ConcurrentHashMap<Long, Boolean> rootBiomes;
        synchronized (BIOME_CACHE) { rootBiomes = BIOME_CACHE.computeIfAbsent(random, ignored -> new ConcurrentHashMap<>()); }
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
        synchronized (CACHE) { samples = CACHE.computeIfAbsent(random, ignored -> new ConcurrentHashMap<>()); }
        if (samples.size() > 65536) samples.clear();
        ShorelineProfile profile = new ShorelineProfile(chunkX, chunkZ, sea, width, (x, z) ->
                samples.computeIfAbsent(BlockPos.asLong(x, 0, z), ignored -> {
                    boolean wet = waterAtSeaLevel(random, x, z, sea);
                    boolean rootlands = isRootlands.test(x, z);
                    return new ShorelineProfile.Sample(wet, rootlands);
                }));
        boolean changed = false;
        int[] originalHeights = new int[256];
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
            originalHeights[x * 16 + z] = chunk.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z);
        }
        // CARVERS still owns the chunk before decoration. Caves cannot subsequently drain newly graded shores.
        for (int x = chunkX; x < chunkX + 16; x++) for (int z = chunkZ; z < chunkZ + 16; z++) {
            if (profile.strength(x, z) <= 0) continue;
            int originalTop = originalHeights[(x - chunkX) * 16 + z - chunkZ];
            // Do not flood dry depressions or underground caves merely because they lie below sea level.
            if (originalTop < sea - 1 && !region.getFluidState(new BlockPos(x, sea - 1, z)).is(FluidTags.WATER)) continue;
            var biome = generator.getBiomeSource().getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(sea), QuartPos.fromBlock(z), random.sampler());
            boolean stony = biome.is(net.minecraft.world.level.biome.Biomes.STONY_SHORE);
            boolean sandy = biome.is(net.minecraft.tags.BiomeTags.IS_BEACH) && !stony;
            changed |= reshapeColumn(region, x, z, originalTop, profile.targetHeight(x, z, originalTop), sea, sandy, stony);
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
        return reshapeColumn(level, x, z, originalTop, targetTop, sea, false, false);
    }

    private static boolean reshapeColumn(WorldGenLevel level, int x, int z, int originalTop, int targetTop, int sea, boolean sandy, boolean stony) {
        int waterTop = sea - 1;
        // Reject any request to inundate existing land or make an island from an existing water column.
        if ((originalTop >= waterTop && targetTop < waterTop) || (originalTop < waterTop && targetTop >= waterTop)) return false;
        boolean beach = !stony && (sandy || (targetTop >= waterTop - 1 && targetTop <= sea + 2));
        if (targetTop == originalTop && (!beach || level.getBlockState(new BlockPos(x, targetTop, z)).is(Blocks.SAND))) return false;
        if (targetTop <= level.getMinBuildHeight() || targetTop >= level.getMaxBuildHeight() - 1) return false;
        if (!level.ensureCanWrite(new BlockPos(x, targetTop, z))) return false;
        int lower = Math.min(originalTop, targetTop);
        int upper = Math.max(originalTop, targetTop);
        // Reject built structures, logs, block entities and blocks outside the explicit natural-terrain set.
        for (int y = Math.max(lower - 3, level.getMinBuildHeight()); y <= Math.max(upper, sea); y++) {
            BlockState state = level.getBlockState(new BlockPos(x, y, z));
            if (state.hasBlockEntity() || !natural(state)) return false;
            if (targetTop > originalTop && y > originalTop && !state.isAir() && !state.getFluidState().is(FluidTags.WATER)) return false;
        }
        if (targetTop < originalTop) {
            for (int y = originalTop; y > targetTop; y--) {
                level.setBlock(new BlockPos(x, y, z), y < sea ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState(), 2);
            }
        } else {
            for (int y = originalTop + 1; y <= targetTop; y++) level.setBlock(new BlockPos(x, y, z), Blocks.STONE.defaultBlockState(), 2);
        }
        // Preserve beach materials and make a sandy intertidal strip on ordinary woodland banks.
        BlockState cap = (stony ? Blocks.STONE : beach ? Blocks.SAND : targetTop < waterTop ? Blocks.GRAVEL : Blocks.GRASS_BLOCK).defaultBlockState();
        level.setBlock(new BlockPos(x, targetTop, z), cap, 2);
        // Three solid supporting blocks keep the cap above shallow caves, including sand/gravel.
        for (int y = targetTop - 1; y >= Math.max(targetTop - 3, level.getMinBuildHeight() + 1); y--) {
            level.setBlock(new BlockPos(x, y, z), (beach ? (y == targetTop - 3 ? Blocks.SANDSTONE : Blocks.SAND) : targetTop < waterTop || stony ? Blocks.STONE : Blocks.DIRT).defaultBlockState(), 2);
        }
        for (int y = targetTop + 1; y < sea; y++) level.setBlock(new BlockPos(x, y, z), Blocks.WATER.defaultBlockState(), 2);
        return true;
    }

    private static boolean natural(BlockState state) {
        return state.isAir() || state.is(Blocks.WATER)
                || state.is(BlockTags.IRON_ORES) || state.is(BlockTags.COPPER_ORES)
                || state.is(Blocks.RAW_IRON_BLOCK) || state.is(Blocks.RAW_COPPER_BLOCK)
                || state.is(BlockTags.BASE_STONE_OVERWORLD) || state.is(BlockTags.DIRT) || state.is(BlockTags.SAND)
                || state.is(Blocks.GRAVEL) || state.is(Blocks.CLAY) || state.is(Blocks.SANDSTONE)
                || state.is(Blocks.RED_SANDSTONE) || state.is(Blocks.SNOW) || state.is(Blocks.SNOW_BLOCK)
                || state.is(Blocks.ICE) || state.is(Blocks.PACKED_ICE);
    }
}
