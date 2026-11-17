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
                    dev.worldgen.lithostitched.api.event.AddBiomeInjectorsEvent.EVENT.invoker().addInjectors(registries, injectors::put);
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
