package dev.fantasyfeudalism.biomes.gametest;

import dev.fantasyfeudalism.biomes.FantasyBiomes;
import dev.fantasyfeudalism.biomes.RootlandsConfig;
import dev.fantasyfeudalism.biomes.worldgen.RootlandsScatterFeature;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Compare complete noise -> surface -> caves -> shore pipelines with grading enabled and disabled. */
@GameTestHolder(FantasyBiomes.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CoastGenerationGameTests {
    @GameTest(template = "test_arena", timeoutTicks = 300, skyAccess = true)
    public static void realCoastsKeepWaterAtVanillaLevelAndDryForestSupportsRoots(GameTestHelper helper) {
        boolean enabled = RootlandsConfig.GENTLE_SHORES.get();
        int wet = 0, dry = 0, graded = 0, beaches = 0, rooted = 0;
        try {
            var registries = helper.getLevel().registryAccess();
            var settings = registries.registryOrThrow(Registries.NOISE_SETTINGS).getHolderOrThrow(NoiseGeneratorSettings.OVERWORLD);
            var rootlands = registries.registryOrThrow(Registries.BIOME).getHolderOrThrow(FantasyBiomes.ROOTLANDS);
            var height = LevelHeightAccessor.create(-64, 384);
            for (long seed : new long[]{12345, 8675309, -94127, 2754680807422324057L}) {
                var generator = new NoiseBasedChunkGenerator(new FixedBiomeSource(rootlands), settings);
                var random = RandomState.create(settings.value(), registries.lookupOrThrow(Registries.NOISE), seed);
                int sea = generator.getSeaLevel();
                BlockPos coast = null, water = null;
                for (int x = -2048; x <= 2048 && coast == null; x += 64) for (int z = -2048; z <= 2048 && coast == null; z += 64) {
                    int top = generator.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, height, random) - 1;
                    if (top < sea + 13) continue;
                    for (int[] delta : new int[][]{{32, 0}, {-32, 0}, {0, 32}, {0, -32}}) {
                        if (generator.getBaseHeight(x + delta[0], z + delta[1], Heightmap.Types.OCEAN_FLOOR_WG, height, random) - 1 < sea - 2) {
                            coast = new BlockPos(x, top, z);
                            water = coast.offset(delta[0], 0, delta[1]);
                            break;
                        }
                    }
                }
                helper.assertTrue(coast != null, "Need an actual raised coastline, seed " + seed);
                System.out.println("Coast pipeline seed=" + seed + " land=" + coast + " water=" + water);
                for (ChunkPos pos : List.of(new ChunkPos(coast), new ChunkPos(water), new ChunkPos(coast.offset((water.getX() - coast.getX()) / 2, 0, (water.getZ() - coast.getZ()) / 2)))) {
                    var baseline = generate(helper, generator, random, seed, pos, false);
                    var actual = generate(helper, generator, random, seed, pos, true);
                    BlockPos rootSoil = null;
                    for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
                        int before = baseline.chunk().getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z);
                        int after = actual.chunk().getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z);
                        BlockPos surface = pos.getBlockAt(x, sea - 1, z);
                        if (before != after) graded++;
                        if (before >= sea - 1) {
                            dry++;
                            helper.assertTrue(after >= sea - 1, "Dry forest must not become a river: " + surface + " seed=" + seed);
                            helper.assertTrue(actual.chunk().getBlockState(pos.getBlockAt(x, after + 1, z)).getFluidState().isEmpty(), "Dry bank must have air above it");
                            if (actual.chunk().getBlockState(pos.getBlockAt(x, after, z)).is(Blocks.SAND)) beaches++;
                            if (actual.chunk().getBlockState(pos.getBlockAt(x, after, z)).is(Blocks.GRASS_BLOCK)) rootSoil = pos.getBlockAt(x, after + 1, z);
                        }
                        if (before < sea - 1 && baseline.chunk().getBlockState(surface).getFluidState().is(FluidTags.WATER)) {
                            wet++;
                            helper.assertTrue(after < sea - 1, "River/ocean beds must stay submerged");
                            var fluid = actual.chunk().getBlockState(surface).getFluidState();
                            helper.assertTrue(fluid.is(FluidTags.WATER) && fluid.isSource(), "Existing surface water must stay full: " + surface + " seed=" + seed);
                        }
                        if (baseline.chunk().getBlockState(surface.above()).isAir()) {
                            helper.assertTrue(actual.chunk().getBlockState(surface.above()).getFluidState().isEmpty(), "Never create the extra water layer above vanilla sea level");
                        }
                    }
                    if (rootSoil != null) {
                        helper.assertTrue(RootlandsScatterFeature.roots(actual.region(), rootSoil, RandomSource.create(4), false), "Graded forest must retain dry dirt for actual root placement");
                        helper.assertTrue(actual.chunk().getBlockState(rootSoil).is(Blocks.DARK_OAK_WOOD), "Root placement must produce root wood");
                        rooted++;
                    }
                }
            }
            helper.assertTrue(wet > 0 && dry > 0 && graded > 0 && beaches > 0 && rooted > 0,
                    "Need real water, dry forest, graded shores, sand beaches and roots; counts=" + wet + "," + dry + "," + graded + "," + beaches + "," + rooted);
            System.out.println("Coast checks wet=" + wet + " dry=" + dry + " graded=" + graded + " sand=" + beaches + " rootedChunks=" + rooted);
        } finally { RootlandsConfig.GENTLE_SHORES.set(enabled); }
        helper.succeed();
    }

    @GameTest(template = "test_arena", timeoutTicks = 300, skyAccess = true)
    public static void reportedSeedAndCoordinatesKeepRootlandsOnDryRootableLand(GameTestHelper helper) {
        checkReportedLocation(helper, NoiseGeneratorSettings.OVERWORLD);
    }

    @GameTest(template = "test_arena", timeoutTicks = 300, skyAccess = true)
    public static void reportedLargeBiomesLocationPreservesWaterAndRootableBanks(GameTestHelper helper) {
        checkReportedLocation(helper, NoiseGeneratorSettings.LARGE_BIOMES);
    }

    private static void checkReportedLocation(GameTestHelper helper, net.minecraft.resources.ResourceKey<NoiseGeneratorSettings> settingsKey) {
        boolean enabled = RootlandsConfig.GENTLE_SHORES.get();
        long seed = 2754680807422324057L;
        try {
            var registries = helper.getLevel().registryAccess();
            var settings = registries.registryOrThrow(Registries.NOISE_SETTINGS).getHolderOrThrow(settingsKey);
            var preset = registries.registryOrThrow(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST).getHolderOrThrow(
                    net.minecraft.resources.ResourceKey.create(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST, net.minecraft.resources.ResourceLocation.withDefaultNamespace("overworld")));
            var generator = new NoiseBasedChunkGenerator(net.minecraft.world.level.biome.MultiNoiseBiomeSource.createFromPreset(preset), settings);
            var dimensions = new net.minecraft.core.MappedRegistry<net.minecraft.world.level.dimension.LevelStem>(Registries.LEVEL_STEM, com.mojang.serialization.Lifecycle.stable());
            dimensions.register(net.minecraft.world.level.dimension.LevelStem.OVERWORLD,
                    new net.minecraft.world.level.dimension.LevelStem(helper.getLevel().dimensionTypeRegistration(), generator), net.minecraft.core.RegistrationInfo.BUILT_IN);
            dimensions.freeze();
            dev.worldgen.lithostitched.impl.worldgen.biomeinjector.internal.BiomeInjectorManager.applyBiomeInjectors(registries, dimensions, seed);
            var random = RandomState.create(settings.value(), registries.lookupOrThrow(Registries.NOISE), seed);
            int rooted = 0, dry = 0, wet = 0;
            for (int cx = 227; cx <= 229; cx++) for (int cz = -145; cz <= -143; cz++) {
                var pos = new ChunkPos(cx, cz);
                var baseline = generate(helper, generator, random, seed, pos, false);
                var actual = generate(helper, generator, random, seed, pos, true);
                BlockPos rootSoil = null;
                for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
                    int before = baseline.chunk().getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z);
                    int after = actual.chunk().getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z);
                    BlockPos surface = pos.getBlockAt(x, 62, z);
                    if (before >= 62) {
                        dry++;
                        helper.assertTrue(after >= 62, "Reported forest must stay above water at " + surface);
                        BlockPos cap = pos.getBlockAt(x, after, z);
                        // A narrow river-edge strip may correctly remain the vanilla forest rim.
                        if (actual.chunk().getBlockState(cap).is(Blocks.GRASS_BLOCK)) rootSoil = cap.above();
                    } else if (baseline.chunk().getBlockState(surface).getFluidState().is(FluidTags.WATER)) {
                        wet++;
                        helper.assertTrue(actual.chunk().getBlockState(surface).getFluidState().is(FluidTags.WATER), "Reported water must not develop a surface hole");
                    }
                    if (baseline.chunk().getBlockState(surface.above()).isAir()) helper.assertTrue(actual.chunk().getBlockState(surface.above()).getFluidState().isEmpty(), "No extra water layer at reported coordinates");
                    if (surface.getX() == 3648 && surface.getZ() == -2302) {
                        System.out.println("Reported location mode=" + settingsKey.location() + " seed=" + seed + " x=3648 z=-2302 originalFloor=" + before + " correctedFloor=" + after + " biome=" + actual.region().getBiome(surface.atY(after + 1)).unwrapKey().orElseThrow().location());
                    }
                }
                if (rootSoil != null) {
                    helper.assertTrue(RootlandsScatterFeature.roots(actual.region(), rootSoil, RandomSource.create(6), false), "Reported banks must retain dry dirt that can support roots");
                    rooted++;
                }
            }
            helper.assertTrue(dry > 0 && rooted > 0, "Reported region must contain dry forest banks supporting roots");
            System.out.println("Reported region mode=" + settingsKey.location() + " dry=" + dry + " wet=" + wet + " rootableChunks=" + rooted);
        } finally { RootlandsConfig.GENTLE_SHORES.set(enabled); }
        helper.succeed();
    }

    record Generated(ProtoChunk chunk, NoiseSurfaceTestRegion region) {}

    static Generated generate(GameTestHelper helper, NoiseBasedChunkGenerator generator, RandomState random, long seed, ChunkPos pos, boolean grade) {
        RootlandsConfig.GENTLE_SHORES.set(grade);
        StructureManager structures = new StructureManager(helper.getLevel(), new WorldOptions(seed, false, false), null) {
            @Override public List<StructureStart> startsForStructure(ChunkPos at, Predicate<Structure> predicate) { return List.of(); }
        };
        var chunk = new ProtoChunk(pos, UpgradeData.EMPTY, LevelHeightAccessor.create(-64, 384), helper.getLevel().registryAccess().registryOrThrow(Registries.BIOME), null);
        generator.createBiomes(random, Blender.empty(), structures, chunk).join();
        generator.fillFromNoise(Blender.empty(), random, structures, chunk).join();
        chunk.setPersistedStatus(ChunkStatus.NOISE);
        var region = new NoiseSurfaceTestRegion(helper.getLevel(), chunk, generator, random, seed);
        generator.buildSurface(region, structures, random, chunk);
        chunk.setPersistedStatus(ChunkStatus.SURFACE);
        generator.applyCarvers(region, seed, random, region.getBiomeManager(), structures, chunk, GenerationStep.Carving.AIR);
        return new Generated(chunk, region);
    }
}
