package dev.fantasyfeudalism.biomes.gametest;

import com.mojang.serialization.Lifecycle;
import dev.fantasyfeudalism.biomes.FantasyBiomes;
import dev.fantasyfeudalism.biomes.RootlandsConfig;
import dev.worldgen.lithostitched.impl.worldgen.biomeinjector.internal.BiomeInjectorManager;
import java.util.ArrayDeque;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(FantasyBiomes.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GenerationScaleGameTests {
    @GameTest(template = "test_arena", timeoutTicks = 300, skyAccess = true)
    public static void largerDefaultsIncreaseConnectedLargeBiomesRegions(GameTestHelper helper) {
        int weight = RootlandsConfig.BIOME_WEIGHT.get(), size = RootlandsConfig.BIOME_SIZE.get();
        try {
            for (long seed : new long[]{2754680807422324057L}) {
                var previous = measure(helper, seed, 35, 2048);
                var enlarged = measure(helper, seed, RootlandsConfig.DEFAULT_BIOME_WEIGHT, RootlandsConfig.DEFAULT_BIOME_SIZE);
                System.out.println("Rootlands area seed=" + seed + " old=" + previous + " enlarged=" + enlarged);
                helper.assertTrue(previous.total() > 0 && previous.largest() > 0, "Need legacy Rootlands samples for comparison");
                helper.assertTrue(enlarged.total() >= previous.total() * 2, "Larger defaults must at least double sampled Rootlands area");
                helper.assertTrue(enlarged.largest() >= previous.largest() * 2, "The largest connected region must at least double, not merely add isolated patches");
            }
        } finally {
            RootlandsConfig.BIOME_WEIGHT.set(weight);
            RootlandsConfig.BIOME_SIZE.set(size);
        }
        helper.succeed();
    }

    private record Area(int total, int largest) {}

    /** One sample every 64 blocks over an 8192x8192 area, identical grid for both configurations. */
    private static Area measure(GameTestHelper helper, long seed, int weight, int size) {
        RootlandsConfig.BIOME_WEIGHT.set(weight);
        RootlandsConfig.BIOME_SIZE.set(size);
        var registries = helper.getLevel().registryAccess();
        var preset = registries.registryOrThrow(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST).getHolderOrThrow(
                ResourceKey.create(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST, ResourceLocation.withDefaultNamespace("overworld")));
        var settings = registries.registryOrThrow(Registries.NOISE_SETTINGS).getHolderOrThrow(NoiseGeneratorSettings.LARGE_BIOMES);
        var generator = new NoiseBasedChunkGenerator(MultiNoiseBiomeSource.createFromPreset(preset), settings);
        var dimensions = new MappedRegistry<LevelStem>(Registries.LEVEL_STEM, Lifecycle.stable());
        dimensions.register(LevelStem.OVERWORLD, new LevelStem(helper.getLevel().dimensionTypeRegistration(), generator), RegistrationInfo.BUILT_IN);
        dimensions.freeze();
        if (weight == 35 && size == 2048) {
            // Reconstruct the actual 0.5.3 selector, including its wider eroded forest core.
            var source = new dev.worldgen.lithostitched.impl.worldgen.biomeinjector.internal.InjectorBiomeSource(generator.getBiomeSource());
            var biomes = registries.registryOrThrow(Registries.BIOME);
            var injector = dev.worldgen.lithostitched.api.worldgen.biomeinjector.BiomeInjector.builder(net.minecraft.world.level.Level.OVERWORLD)
                    .replacePartially(biomes.getHolderOrThrow(net.minecraft.world.level.biome.Biomes.DARK_FOREST), biomes.getHolderOrThrow(FantasyBiomes.ROOTLANDS),
                            dev.fantasyfeudalism.biomes.worldgen.RootlandsIntegration.areaParameters(registries)
                                    .densityFunctionMin(net.minecraft.core.Holder.direct(new dev.fantasyfeudalism.biomes.worldgen.DarkForestInterior(preset, settings, 32)), 1.0));
            var random = RandomState.create(settings.value(), registries.lookupOrThrow(Registries.NOISE), seed);
            source.applyInjectors(java.util.Map.of(FantasyBiomes.id("legacy_scale_comparison"), injector), java.util.Optional.empty(), java.util.Map.of(),
                    new dev.worldgen.lithostitched.api.worldgen.util.DensityFunctionWrapper(seed, false, random,
                            ((dev.worldgen.lithostitched.mixin.common.RandomStateAccessor) (Object) random).getRandom()));
            generator = new NoiseBasedChunkGenerator(source, settings);
        } else {
            BiomeInjectorManager.applyBiomeInjectors(registries, dimensions, seed);
        }
        var sampler = RandomState.create(settings.value(), registries.lookupOrThrow(Registries.NOISE), seed).sampler();
        boolean[][] selected = new boolean[129][129];
        int total = 0, largest = 0;
        for (int x = 0; x < 129; x++) for (int z = 0; z < 129; z++) {
            selected[x][z] = generator.getBiomeSource().getNoiseBiome(-1024 + x * 16, 20, -1024 + z * 16, sampler).is(FantasyBiomes.ROOTLANDS);
            if (selected[x][z]) total++;
        }
        for (int x = 0; x < 129; x++) for (int z = 0; z < 129; z++) {
            if (!selected[x][z]) continue;
            int component = 0;
            var queue = new ArrayDeque<Integer>();
            selected[x][z] = false;
            queue.add(x * 129 + z);
            while (!queue.isEmpty()) {
                int cell = queue.removeFirst(), cx = cell / 129, cz = cell % 129;
                component++;
                for (int[] step : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                    int nx = cx + step[0], nz = cz + step[1];
                    if (nx < 0 || nz < 0 || nx >= 129 || nz >= 129 || !selected[nx][nz]) continue;
                    selected[nx][nz] = false;
                    queue.add(nx * 129 + nz);
                }
            }
            largest = Math.max(largest, component);
        }
        return new Area(total, largest);
    }

    @GameTest(template = "test_arena", timeoutTicks = 100, skyAccess = true)
    public static void legacyDefaultPairUpgradesOnceAndCustomValuesAreRetained(GameTestHelper helper) {
        int weight = RootlandsConfig.BIOME_WEIGHT.get(), size = RootlandsConfig.BIOME_SIZE.get();
        int revision = RootlandsConfig.GENERATION_DEFAULTS_REVISION.get();
        try {
            RootlandsConfig.GENERATION_DEFAULTS_REVISION.set(0);
            RootlandsConfig.BIOME_WEIGHT.set(35);
            RootlandsConfig.BIOME_SIZE.set(2048);
            helper.assertTrue(RootlandsConfig.migrateLegacyGenerationDefaults(), "Untouched legacy settings need migration");
            helper.assertTrue(RootlandsConfig.BIOME_WEIGHT.get() == RootlandsConfig.DEFAULT_BIOME_WEIGHT
                    && RootlandsConfig.BIOME_SIZE.get() == RootlandsConfig.DEFAULT_BIOME_SIZE, "Both old defaults must upgrade together");
            RootlandsConfig.BIOME_WEIGHT.set(35);
            RootlandsConfig.BIOME_SIZE.set(2048);
            helper.assertTrue(!RootlandsConfig.migrateLegacyGenerationDefaults() && RootlandsConfig.BIOME_WEIGHT.get() == 35, "A later explicit legacy choice must not be overridden");
            RootlandsConfig.GENERATION_DEFAULTS_REVISION.set(0);
            RootlandsConfig.BIOME_WEIGHT.set(77);
            RootlandsConfig.BIOME_SIZE.set(1024);
            RootlandsConfig.migrateLegacyGenerationDefaults();
            helper.assertTrue(RootlandsConfig.BIOME_WEIGHT.get() == 77 && RootlandsConfig.BIOME_SIZE.get() == 1024, "Player-tuned generation values must remain intact");
        } finally {
            RootlandsConfig.BIOME_WEIGHT.set(weight);
            RootlandsConfig.BIOME_SIZE.set(size);
            RootlandsConfig.GENERATION_DEFAULTS_REVISION.set(revision);
        }
        helper.succeed();
    }
}
