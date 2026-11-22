package dev.fantasyfeudalism.biomes.gametest;

import com.mojang.serialization.Lifecycle;
import dev.fantasyfeudalism.biomes.FantasyBiomes;
import dev.fantasyfeudalism.biomes.RootlandsConfig;
import dev.worldgen.lithostitched.impl.worldgen.biomeinjector.internal.BiomeInjectorManager;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Real noise-filled chunks, not a flat test arena: catch terrain-height changes at injected biome edges. */
@GameTestHolder(FantasyBiomes.MOD_ID)
@PrefixGameTestTemplate(false)
public final class RootlandsTerrainGameTests {
    @GameTest(template = "test_arena", timeoutTicks = 200, skyAccess = true)
    public static void rootlandsRetainsNeighboringDarkOakForest(GameTestHelper helper) {
        var registries = helper.getLevel().registryAccess();
        var preset = registries.registryOrThrow(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST)
                .getHolderOrThrow(ResourceKey.create(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST, ResourceLocation.withDefaultNamespace("overworld")));
        var settings = registries.registryOrThrow(Registries.NOISE_SETTINGS).getHolderOrThrow(NoiseGeneratorSettings.OVERWORLD);
        for (long seed : new long[]{12345, -94127}) {
            var original = MultiNoiseBiomeSource.createFromPreset(preset);
            var generator = new NoiseBasedChunkGenerator(MultiNoiseBiomeSource.createFromPreset(preset), settings);
            var dimensions = new MappedRegistry<LevelStem>(Registries.LEVEL_STEM, Lifecycle.stable());
            dimensions.register(LevelStem.OVERWORLD, new LevelStem(helper.getLevel().dimensionTypeRegistration(), generator), RegistrationInfo.BUILT_IN);
            dimensions.freeze();
            BiomeInjectorManager.applyBiomeInjectors(registries, dimensions, seed);
            var sampler = RandomState.create(settings.value(), registries.lookupOrThrow(Registries.NOISE), seed).sampler();
            int sampled = 0;
            for (int x = -1024; x <= 1024 && sampled < 8; x += 8) for (int z = -1024; z <= 1024 && sampled < 8; z += 8) {
                if (!generator.getBiomeSource().getNoiseBiome(x, 20, z, sampler).is(FantasyBiomes.ROOTLANDS)) continue;
                helper.assertTrue(original.getNoiseBiome(x, 20, z, sampler).is(net.minecraft.world.level.biome.Biomes.DARK_FOREST), "Rootlands must originate in dark oak forest");
                for (int[] direction : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                    boolean foundRim = false;
                    for (int distance = 1; distance <= 1024; distance++) {
                        var neighbor = generator.getBiomeSource().getNoiseBiome(x + direction[0] * distance, 20, z + direction[1] * distance, sampler);
                        if (!neighbor.is(FantasyBiomes.ROOTLANDS)) {
                            helper.assertTrue(neighbor.is(net.minecraft.world.level.biome.Biomes.DARK_FOREST), "Rootlands edge must retain neighboring dark oak forest");
                            foundRim = true;
                            break;
                        }
                    }
                    helper.assertTrue(foundRim, "Rootlands must retain an outer forest rim");
                }
                sampled++;
            }
            helper.assertTrue(sampled > 0, "Need Rootlands samples for adjacency regression, seed " + seed);
        }
        helper.succeed();
    }

    @GameTest(template = "test_arena", timeoutTicks = 300, skyAccess = true)
    public static void largeBiomesUsesItsOwnForestInteriorForRootlands(GameTestHelper helper) {
        var registries = helper.getLevel().registryAccess();
        var preset = registries.registryOrThrow(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST)
                .getHolderOrThrow(ResourceKey.create(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST, ResourceLocation.withDefaultNamespace("overworld")));
        var settings = registries.registryOrThrow(Registries.NOISE_SETTINGS).getHolderOrThrow(NoiseGeneratorSettings.LARGE_BIOMES);
        long seed = 2754680807422324057L;
        var original = MultiNoiseBiomeSource.createFromPreset(preset);
        var generator = new NoiseBasedChunkGenerator(MultiNoiseBiomeSource.createFromPreset(preset), settings);
        var dimensions = new MappedRegistry<LevelStem>(Registries.LEVEL_STEM, Lifecycle.stable());
        dimensions.register(LevelStem.OVERWORLD, new LevelStem(helper.getLevel().dimensionTypeRegistration(), generator), RegistrationInfo.BUILT_IN);
        dimensions.freeze();
        BiomeInjectorManager.applyBiomeInjectors(registries, dimensions, seed);
        var random = RandomState.create(settings.value(), registries.lookupOrThrow(Registries.NOISE), seed);
        var noiseHelper = new dev.worldgen.lithostitched.api.worldgen.util.DensityFunctionWrapper(seed, false, random,
                ((dev.worldgen.lithostitched.mixin.common.RandomStateAccessor) (Object) random).getRandom());
        var area = net.minecraft.world.level.levelgen.DensityFunctions.noise(registries.registryOrThrow(Registries.NOISE)
                .getHolderOrThrow(dev.fantasyfeudalism.biomes.worldgen.RootlandsIntegration.AREA_NOISE), 1024.0 / RootlandsConfig.BIOME_SIZE.get(), 0).mapAll(noiseHelper);
        double coverage = Math.clamp(0.2 * Math.log(RootlandsConfig.BIOME_WEIGHT.get() / 100.0), -0.95, 0.95);
        int eligible = 0, placed = 0;
        BlockPos forest = null;
        for (int x = -1024; x <= 1024; x += 16) for (int z = -1024; z <= 1024; z += 16) {
            var selected = generator.getBiomeSource().getNoiseBiome(x, 20, z, random.sampler());
            boolean core = original.getNoiseBiome(x, 20, z, random.sampler()).is(net.minecraft.world.level.biome.Biomes.DARK_FOREST);
            if (!core) {
                helper.assertTrue(!selected.is(FantasyBiomes.ROOTLANDS), "Large Biomes water/other biomes must not become Rootlands");
                continue;
            }
            for (int dx = -8; dx <= 8 && core; dx++) for (int dz = -8; dz <= 8 && core; dz++) {
                if (!original.getNoiseBiome(x + dx, 20, z + dz, random.sampler()).is(net.minecraft.world.level.biome.Biomes.DARK_FOREST)) core = false;
            }
            if (selected.is(FantasyBiomes.ROOTLANDS)) {
                placed++;
                helper.assertTrue(core, "Rootlands must preserve the 32-block forest rim in the actual Large Biomes layout, quart=" + x + "," + z);
            }
            if (core && area.compute(new net.minecraft.world.level.levelgen.DensityFunction.SinglePointContext(x * 4, 80, z * 4)) <= coverage) {
                eligible++;
                if (forest == null && area.compute(new net.minecraft.world.level.levelgen.DensityFunction.SinglePointContext(x * 4, 80, z * 4)) < coverage - 0.05) {
                    int surface = generator.getBaseHeight(x * 4 + 8, z * 4 + 8, Heightmap.Types.OCEAN_FLOOR_WG, LevelHeightAccessor.create(-64, 384), random);
                    if (surface >= generator.getSeaLevel() + 8 && generator.getBiomeSource().getNoiseBiome(x + 2, surface >> 2, z + 2, random.sampler()).is(FantasyBiomes.ROOTLANDS)) {
                        forest = new BlockPos(x * 4, surface, z * 4);
                    }
                }
                helper.assertTrue(selected.is(FantasyBiomes.ROOTLANDS), "Eligible Large Biomes forest must not be filtered using the normal world's forest map, quart=" + x + "," + z);
            }
        }
        helper.assertTrue(eligible > 0 && placed > 0, "Need eligible Large Biomes forest and Rootlands coverage");
        helper.assertTrue(forest != null, "Need a dry Rootlands interior, away from its selection boundary, for the placement regression");
        boolean shores = RootlandsConfig.GENTLE_SHORES.get();
        try {
            var generated = CoastGenerationGameTests.generate(helper, generator, random, seed, new ChunkPos(forest), true);
            var context = new net.minecraft.world.level.levelgen.feature.FeaturePlaceContext<>(java.util.Optional.empty(),
                    generated.region(), generator, net.minecraft.util.RandomSource.create(25), new ChunkPos(forest).getWorldPosition(),
                    net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration.INSTANCE);
            int rootSoils = 0, grass = 0;
            for (int dx = 0; dx < 16; dx++) for (int dz = 0; dz < 16; dz++) {
                var floor = dev.fantasyfeudalism.biomes.worldgen.RootlandsScatterFeature.ground(generated.region(), new ChunkPos(forest).getMinBlockX() + dx, new ChunkPos(forest).getMinBlockZ() + dz);
                if (floor != null) {
                    grass++;
                    if (generated.region().getBiome(floor).is(FantasyBiomes.ROOTLANDS)) rootSoils++;
                }
            }
            System.out.println("Large Biomes root fixture=" + new ChunkPos(forest) + " dirtColumns=" + grass + " RootlandsDirt=" + rootSoils);
            helper.assertTrue(FantasyBiomes.EXPOSED_ROOTS.get().place(context), "An actual Large Biomes Rootlands chunk must generate roots through its biome-filtered feature");
        } finally { RootlandsConfig.GENTLE_SHORES.set(shores); }
        System.out.println("Large Biomes forest eligibility=" + eligible + " Rootlands=" + placed + " rootFeatureChunk=" + new ChunkPos(forest));
        helper.succeed();
    }

    @GameTest(template = "test_arena", timeoutTicks = 200, skyAccess = true)
    public static void injectionPreservesVanillaRiversOceansAndBeaches(GameTestHelper helper) {
        var registries = helper.getLevel().registryAccess();
        var preset = registries.registryOrThrow(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST)
                .getHolderOrThrow(ResourceKey.create(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST, ResourceLocation.withDefaultNamespace("overworld")));
        var settings = registries.registryOrThrow(Registries.NOISE_SETTINGS).getHolderOrThrow(NoiseGeneratorSettings.OVERWORLD);
        for (long seed : new long[]{12345, 2754680807422324057L}) {
            var original = MultiNoiseBiomeSource.createFromPreset(preset);
            var generator = new NoiseBasedChunkGenerator(MultiNoiseBiomeSource.createFromPreset(preset), settings);
            var dimensions = new MappedRegistry<LevelStem>(Registries.LEVEL_STEM, Lifecycle.stable());
            dimensions.register(LevelStem.OVERWORLD, new LevelStem(helper.getLevel().dimensionTypeRegistration(), generator), RegistrationInfo.BUILT_IN);
            dimensions.freeze();
            BiomeInjectorManager.applyBiomeInjectors(registries, dimensions, seed);
            var sampler = RandomState.create(settings.value(), registries.lookupOrThrow(Registries.NOISE), seed).sampler();
            int rivers = 0, beaches = 0, oceans = 0, roots = 0;
            for (int x = -1024; x <= 1024; x += 8) for (int z = -1024; z <= 1024; z += 8) {
                var before = original.getNoiseBiome(x, 16, z, sampler);
                var after = generator.getBiomeSource().getNoiseBiome(x, 16, z, sampler);
                boolean river = before.is(net.minecraft.tags.BiomeTags.IS_RIVER);
                boolean beach = before.is(net.minecraft.tags.BiomeTags.IS_BEACH);
                boolean ocean = before.is(net.minecraft.tags.BiomeTags.IS_OCEAN);
                if (river) rivers++;
                if (beach) beaches++;
                if (ocean) oceans++;
                if (after.is(FantasyBiomes.ROOTLANDS)) roots++;
                if (river || beach || ocean) helper.assertTrue(before.equals(after), "Rootlands must never replace water/beach biomes, seed=" + seed + " quart=" + x + "," + z);
            }
            helper.assertTrue(rivers > 0 && beaches > 0 && oceans > 0 && roots > 0, "Need rivers, beaches, oceans and Rootlands samples, seed=" + seed);
            System.out.println("Preserved biomes seed=" + seed + " rivers=" + rivers + " beaches=" + beaches + " oceans=" + oceans + " rootlands=" + roots);
        }
        helper.succeed();
    }

    @GameTest(template = "test_arena", timeoutTicks = 200, skyAccess = true)
    public static void shorelineGradesRealMinecraftNoiseTerrain(GameTestHelper helper) {
        var registries = helper.getLevel().registryAccess();
        var biomes = registries.registryOrThrow(Registries.BIOME);
        var settings = registries.registryOrThrow(Registries.NOISE_SETTINGS).getHolderOrThrow(NoiseGeneratorSettings.OVERWORLD);
        var generator = new NoiseBasedChunkGenerator(new net.minecraft.world.level.biome.FixedBiomeSource(biomes.getHolderOrThrow(FantasyBiomes.ROOTLANDS)), settings);
        var random = RandomState.create(settings.value(), registries.lookupOrThrow(Registries.NOISE), 12345L);
        LevelHeightAccessor height = LevelHeightAccessor.create(-64, 384);
        BlockPos coast = null;
        // Find actual vanilla terrain with a raised land column and nearby water, independent of a hand-built test shape.
        for (int x = -2048; x <= 2048 && coast == null; x += 64) for (int z = -2048; z <= 2048 && coast == null; z += 64) {
            int top = generator.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, height, random) - 1;
            if (top < 80) continue;
            for (int[] delta : new int[][]{{32, 0}, {-32, 0}, {0, 32}, {0, -32}}) {
                if (generator.getBaseHeight(x + delta[0], z + delta[1], Heightmap.Types.OCEAN_FLOOR_WG, height, random) - 1 < 63) {
                    coast = new BlockPos(x, top, z);
                    break;
                }
            }
        }
        helper.assertTrue(coast != null, "Need a real raised shoreline for regression");
        var pos = new ChunkPos(coast);
        StructureManager noStructures = new StructureManager(helper.getLevel(), new WorldOptions(12345, false, false), null) {
            @Override public List<StructureStart> startsForStructure(ChunkPos chunk, Predicate<Structure> predicate) { return List.of(); }
        };
        var chunk = new ProtoChunk(pos, UpgradeData.EMPTY, height, biomes, null);
        generator.createBiomes(random, Blender.empty(), noStructures, chunk).join();
        generator.fillFromNoise(Blender.empty(), random, noStructures, chunk).join();
        // Adapt the real noise-filled chunk to the minimal read/write contract used by the shore pass.
        var access = (net.minecraft.world.level.WorldGenLevel) java.lang.reflect.Proxy.newProxyInstance(
                net.minecraft.world.level.WorldGenLevel.class.getClassLoader(), new Class<?>[]{net.minecraft.world.level.WorldGenLevel.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getMinBuildHeight" -> -64;
                    case "getMaxBuildHeight" -> 320;
                    case "ensureCanWrite" -> new ChunkPos((BlockPos) args[0]).equals(pos);
                    case "getBlockState" -> chunk.getBlockState((BlockPos) args[0]);
                    case "setBlock" -> { chunk.setBlockState((BlockPos) args[0], (net.minecraft.world.level.block.state.BlockState) args[1], false); yield true; }
                    case "toString" -> "Noise-filled shore test chunk";
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        int errors = 0, totalSamples = 0;
        for (int dx = -64; dx <= 64; dx += 16) for (int dz = -64; dz <= 64; dz += 16) {
            int x = pos.getMinBlockX() + dx, z = pos.getMinBlockZ() + dz;
            boolean wet = generator.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, height, random) - 1 < 63;
            if (dev.fantasyfeudalism.biomes.worldgen.RootlandsShoreFeature.waterAtSeaLevel(random, x, z, 63) != wet) errors++;
            totalSamples++;
        }
        helper.assertTrue(errors < totalSamples / 5, "Fast waterline field must agree with the actual surface near the coast");
        System.out.println("Fast waterline comparison errors=" + errors + " samples=" + totalSamples);
        long started = System.nanoTime();
        var profile = new dev.fantasyfeudalism.biomes.worldgen.ShorelineProfile(pos.getMinBlockX(), pos.getMinBlockZ(), 63, 64,
                (x, z) -> new dev.fantasyfeudalism.biomes.worldgen.ShorelineProfile.Sample(
                        dev.fantasyfeudalism.biomes.worldgen.RootlandsShoreFeature.waterAtSeaLevel(random, x, z, 63), true));
        int graded = 0;
        for (int x = pos.getMinBlockX(); x <= pos.getMaxBlockX(); x++) for (int z = pos.getMinBlockZ(); z <= pos.getMaxBlockZ(); z++) {
            int original = chunk.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x & 15, z & 15);
            int target = profile.targetHeight(x, z, original);
            if (dev.fantasyfeudalism.biomes.worldgen.RootlandsShoreFeature.reshapeColumn(access, x, z, original, target, 63)) {
                graded++;
                helper.assertTrue(!chunk.getBlockState(new BlockPos(x, target, z)).isAir(), "Graded column must have a solid cap");
                if (target < original && target >= 63) helper.assertTrue(chunk.getBlockState(new BlockPos(x, target + 1, z)).isAir(), "Raised shore should taper down to the new cap");
                if (target < 62) helper.assertTrue(chunk.getBlockState(new BlockPos(x, 62, z)).getFluidState().is(net.minecraft.tags.FluidTags.WATER), "Submerged shore must retain vanilla water height");
            }
        }
        helper.assertTrue(graded > 0, "Real noise-generated shoreline must actually be graded");
        System.out.println("Graded real Rootlands shore chunk=" + pos + " columns=" + graded + " uncached profile/pass ms=" + ((System.nanoTime() - started) / 1000000));
        helper.succeed();
    }

    @GameTest(template = "test_arena", timeoutTicks = 100, skyAccess = true)
    public static void biomeSizeProducesBroaderContinuousAreas(GameTestHelper helper) {
        int previous = RootlandsConfig.BIOME_SIZE.get();
        try {
            var registries = helper.getLevel().registryAccess();
            var settings = registries.registryOrThrow(Registries.NOISE_SETTINGS).getHolderOrThrow(NoiseGeneratorSettings.OVERWORLD);
            var forest = registries.registryOrThrow(Registries.BIOME).getHolderOrThrow(net.minecraft.world.level.biome.Biomes.FOREST);
            for (long seed : new long[]{12345, -94127}) {
                int[] changes = new int[2];
                for (int scale = 0; scale < 2; scale++) {
                    RootlandsConfig.BIOME_SIZE.set(scale == 0 ? 512 : 2048);
                    // Uniform eligible woodland isolates selection scale from vanilla climate boundaries.
                    // Fixed-biome worlds intentionally opt out of normal runtime injection, so apply the callback explicitly here.
                    var source = new dev.worldgen.lithostitched.impl.worldgen.biomeinjector.internal.InjectorBiomeSource(new net.minecraft.world.level.biome.FixedBiomeSource(forest));
                    var injectors = new java.util.HashMap<ResourceLocation, dev.worldgen.lithostitched.api.worldgen.biomeinjector.BiomeInjector>();
                    injectors.put(FantasyBiomes.id("area_scale_test"), dev.worldgen.lithostitched.api.worldgen.biomeinjector.BiomeInjector.builder(net.minecraft.world.level.Level.OVERWORLD)
                            .replacePartially(forest, registries.registryOrThrow(Registries.BIOME).getHolderOrThrow(FantasyBiomes.ROOTLANDS),
                                    dev.fantasyfeudalism.biomes.worldgen.RootlandsIntegration.areaParameters(registries)));
                    var random = RandomState.create(settings.value(), registries.lookupOrThrow(Registries.NOISE), seed);
                    source.applyInjectors(injectors, java.util.Optional.empty(), java.util.Map.of(),
                            new dev.worldgen.lithostitched.api.worldgen.util.DensityFunctionWrapper(seed, false, random,
                                    ((dev.worldgen.lithostitched.mixin.common.RandomStateAccessor) (Object) random).getRandom()));
                    var sampler = random.sampler();
                    int selected = 0;
                    for (int z : new int[]{-512, 0, 512}) {
                        boolean last = false;
                        for (int x = -4096; x <= 4096; x += 4) {
                            boolean current = source.getNoiseBiome(x, 20, z, sampler).is(FantasyBiomes.ROOTLANDS);
                            if (x > -4096 && current != last) changes[scale]++;
                            if (current) selected++;
                            last = current;
                        }
                    }
                    helper.assertTrue(selected > 0 && selected < 6147, "Selection should retain both Rootlands and other woodland");
                }
                System.out.println("Rootlands size seed=" + seed + " boundary crossings small=" + changes[0] + " broad=" + changes[1]);
                helper.assertTrue(changes[1] < changes[0] / 2, "Larger scale must create broader continuous regions, not just change frequency");
            }
        } finally { RootlandsConfig.BIOME_SIZE.set(previous); }
        helper.succeed();
    }

    @GameTest(template = "test_arena", timeoutTicks = 200, skyAccess = true)
    public static void rootlandsBoundaryKeepsVanillaNoiseAndSurfaceHeight(GameTestHelper helper) {
        var registries = helper.getLevel().registryAccess();
        var biomes = registries.registryOrThrow(Registries.BIOME);
        var preset = registries.registryOrThrow(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST)
                .getHolderOrThrow(ResourceKey.create(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST, ResourceLocation.withDefaultNamespace("overworld")));
        var settings = registries.registryOrThrow(Registries.NOISE_SETTINGS).getHolderOrThrow(NoiseGeneratorSettings.OVERWORLD);
        LevelHeightAccessor height = LevelHeightAccessor.create(-64, 384);
        StructureManager noStructures = new StructureManager(helper.getLevel(), new WorldOptions(0, false, false), null) {
            @Override public List<StructureStart> startsForStructure(ChunkPos pos, Predicate<Structure> predicate) { return List.of(); }
        };
        for (long seed : new long[]{12345, 8675309, -94127}) {
            var vanilla = new NoiseBasedChunkGenerator(MultiNoiseBiomeSource.createFromPreset(preset), settings);
            var injected = new NoiseBasedChunkGenerator(MultiNoiseBiomeSource.createFromPreset(preset), settings);
            var dimensions = new MappedRegistry<LevelStem>(Registries.LEVEL_STEM, Lifecycle.stable());
            dimensions.register(LevelStem.OVERWORLD, new LevelStem(helper.getLevel().dimensionTypeRegistration(), injected), RegistrationInfo.BUILT_IN);
            dimensions.freeze();
            BiomeInjectorManager.applyBiomeInjectors(registries, dimensions, seed);
            var random = RandomState.create(settings.value(), registries.lookupOrThrow(Registries.NOISE), seed);
            var sampler = random.sampler();
            BlockPos edge = null;
            for (int x = -1024; x <= 1024 && edge == null; x += 8) for (int z = -1024; z <= 1024 && edge == null; z += 8) {
                boolean here = injected.getBiomeSource().getNoiseBiome(x, 20, z, sampler).is(FantasyBiomes.ROOTLANDS);
                boolean next = injected.getBiomeSource().getNoiseBiome(x + 8, 20, z, sampler).is(FantasyBiomes.ROOTLANDS);
                if (here != next) edge = new BlockPos(x * 4, 80, z * 4);
            }
            helper.assertTrue(edge != null, "Need a Rootlands boundary for terrain regression, seed " + seed);
            System.out.println("Rootlands terrain boundary seed=" + seed + " position=" + edge);
            boolean first = injected.getBiomeSource().getNoiseBiome(edge.getX() / 4, 20, edge.getZ() / 4, sampler).is(FantasyBiomes.ROOTLANDS);
            for (int dx = 1; dx <= 8; dx++) {
                if (injected.getBiomeSource().getNoiseBiome(edge.getX() / 4 + dx, 20, edge.getZ() / 4, sampler).is(FantasyBiomes.ROOTLANDS) != first) {
                    edge = edge.offset(dx * 4, 0, 0);
                    break;
                }
            }
            ChunkPos border = new ChunkPos(edge);
            for (int side = 0; side < 2; side++) {
                ChunkPos pos = new ChunkPos(border.x - 1 + side, border.z);
                ProtoChunk expected = new ProtoChunk(pos, UpgradeData.EMPTY, height, biomes, null);
                ProtoChunk actual = new ProtoChunk(pos, UpgradeData.EMPTY, height, biomes, null);
                vanilla.createBiomes(random, Blender.empty(), noStructures, expected).join();
                injected.createBiomes(random, Blender.empty(), noStructures, actual).join();
                vanilla.fillFromNoise(Blender.empty(), random, noStructures, expected).join();
                injected.fillFromNoise(Blender.empty(), random, noStructures, actual).join();
                for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
                    BlockPos column = pos.getBlockAt(x, 0, z);
                    for (int y = -64; y < 320; y++) {
                        BlockPos sample = new BlockPos(column.getX(), y, column.getZ());
                        helper.assertTrue(actual.getBlockState(sample).equals(expected.getBlockState(sample)), "Biome injection changed terrain density at " + sample + " seed " + seed);
                    }
                }
                int[] before = new int[256];
                for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) before[x * 16 + z] = actual.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
                var manager = new BiomeManager((x, y, z) -> injected.getBiomeSource().getNoiseBiome(x, y, z, sampler), BiomeManager.obfuscateSeed(seed));
                injected.buildSurface(actual, new WorldGenerationContext(injected, height), random, noStructures, manager, biomes, Blender.empty());
                // Recompute height from blocks so cached heightmaps cannot conceal a surface-rule regression.
                for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
                    int top = -65;
                    for (int y = 319; y >= -64; y--) {
                        if (!actual.getBlockState(pos.getBlockAt(x, y, z)).isAir()) { top = y; break; }
                    }
                    helper.assertTrue(top == before[x * 16 + z], "Surface rules changed column height at " + pos + " local " + x + "," + z + " from " + before[x * 16 + z] + " to " + top);
                }
            }
        }
        helper.succeed();
    }
}
