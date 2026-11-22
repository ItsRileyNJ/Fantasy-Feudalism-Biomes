package dev.fantasyfeudalism.biomes.gametest;

import dev.fantasyfeudalism.biomes.FantasyBiomes;
import dev.fantasyfeudalism.biomes.RootlandsConfig;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(FantasyBiomes.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SurfaceOrderingGameTests {
    @GameTest(template = "test_arena", timeoutTicks = 200, skyAccess = true)
    public static void productionCarverHookGradesBeforeTreesMushroomsAndGrass(GameTestHelper helper) {
        boolean enabled = RootlandsConfig.GENTLE_SHORES.get();
        try {
            RootlandsConfig.GENTLE_SHORES.set(true);
            var registries = helper.getLevel().registryAccess();
            var biomes = registries.registryOrThrow(Registries.BIOME);
            var settings = registries.registryOrThrow(Registries.NOISE_SETTINGS).getHolderOrThrow(NoiseGeneratorSettings.OVERWORLD);
            var generator = new NoiseBasedChunkGenerator(new FixedBiomeSource(biomes.getHolderOrThrow(FantasyBiomes.ROOTLANDS)), settings);
            var random = RandomState.create(settings.value(), registries.lookupOrThrow(Registries.NOISE), 12345L);
            var height = LevelHeightAccessor.create(-64, 384);
            var pos = new ChunkPos(-124, -72); // Raised coastal land in seed 12345.
            StructureManager structures = new StructureManager(helper.getLevel(), new WorldOptions(12345, false, false), null) {
                @Override public List<StructureStart> startsForStructure(ChunkPos chunk, Predicate<Structure> predicate) { return List.of(); }
            };
            var chunk = new ProtoChunk(pos, UpgradeData.EMPTY, height, biomes, null);
            generator.createBiomes(random, Blender.empty(), structures, chunk).join();
            generator.fillFromNoise(Blender.empty(), random, structures, chunk).join();
            chunk.setPersistedStatus(ChunkStatus.NOISE);
            int[] before = new int[256];
            for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) before[x * 16 + z] = chunk.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z);
            var region = new NoiseSurfaceTestRegion(helper.getLevel(), chunk, generator, random, 12345);
            generator.buildSurface(region, structures, random, chunk);
            chunk.setPersistedStatus(ChunkStatus.SURFACE);
            // Exact runtime entry point: caves finish before shore grading; neighboring FEATURES wait for it.
            generator.applyCarvers(region, 12345L, random, region.getBiomeManager(), structures, chunk,
                    net.minecraft.world.level.levelgen.GenerationStep.Carving.AIR);
            int lowered = 0, decorated = 0;
            for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
                int top = chunk.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z);
                if (top < before[x * 16 + z] - 3) lowered++;
                if (top < 63 || top >= before[x * 16 + z] - 3 || decorated >= 3) continue;
                BlockPos ground = pos.getBlockAt(x, top, z);
                chunk.setBlockState(ground, Blocks.GRASS_BLOCK.defaultBlockState(), false);
                if (decorated == 0) {
                    for (int y = 1; y <= 5; y++) chunk.setBlockState(ground.above(y), Blocks.DARK_OAK_LOG.defaultBlockState(), false);
                } else if (decorated == 1) {
                    for (int y = 1; y <= 4; y++) chunk.setBlockState(ground.above(y), Blocks.MUSHROOM_STEM.defaultBlockState(), false);
                } else {
                    chunk.setBlockState(ground.above(), FantasyBiomes.DARK_GRASS.get().defaultBlockState(), false);
                    helper.assertTrue(FantasyBiomes.DARK_GRASS.get().defaultBlockState().canSurvive(region, ground.above()), "New grass must rest on the graded ground");
                }
                helper.assertTrue(top < before[x * 16 + z] - 3, "Decoration must not sit on an old-height stone pillar");
                decorated++;
            }
            helper.assertTrue(lowered > 0, "The actual carver hook must grade coastal terrain");
            helper.assertTrue(decorated == 3, "Need lowered dry land for a tree, mushroom and grass regression");
            int[] decoratedHeights = new int[256];
            for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) decoratedHeights[x * 16 + z] = chunk.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z);
            var legacy = new FeaturePlaceContext<>(Optional.empty(), region, generator, RandomSource.create(1), pos.getWorldPosition(), NoneFeatureConfiguration.INSTANCE);
            helper.assertTrue(!FantasyBiomes.SHORELINE.get().place(legacy), "Old decoration-time shoreline feature must be inert");
            for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
                helper.assertTrue(chunk.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z) == decoratedHeights[x * 16 + z], "No terrain edits may run after vegetation");
            }
        } finally { RootlandsConfig.GENTLE_SHORES.set(enabled); }
        helper.succeed();
    }
}
