package dev.fantasyfeudalism.biomes.gametest;

import com.mojang.serialization.Lifecycle;
import dev.fantasyfeudalism.biomes.FantasyBiomes;
import dev.fantasyfeudalism.biomes.RootlandsConfig;
import dev.fantasyfeudalism.biomes.worldgen.JaggedTree;
import dev.fantasyfeudalism.biomes.worldgen.RootlandsScatterFeature;
import dev.worldgen.lithostitched.api.registry.LithostitchedRegistries;
import dev.worldgen.lithostitched.impl.worldgen.biomeinjector.internal.BiomeInjectorManager;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Development tests, excluded from the distributable JAR along with their template. */
@GameTestHolder(FantasyBiomes.MOD_ID)
@PrefixGameTestTemplate(false)
public final class RootlandsGameTests {
    @GameTest(template = "test_arena", timeoutTicks = 100, skyAccess = true)
    public static void jaggedTreeHasHeightBranchesAndFoliage(GameTestHelper helper) {
        BlockPos base = helper.absolutePos(new BlockPos(16, 2, 16));
        boolean placed = JaggedTree.place(helper.getLevel(), base, RandomSource.create(42), false);
        helper.assertTrue(placed, "Jagged tree should fit in clear arena");
        helper.assertTrue(count(helper, Blocks.DARK_OAK_LOG) > 90, "Tree needs a substantial branching trunk");
        helper.assertTrue(count(helper, Blocks.DARK_OAK_LEAVES) > 100, "Tree needs living foliage");
        helper.assertBlockPresent(Blocks.DARK_OAK_LOG, new BlockPos(16, 25, 13));
        helper.succeed();
    }

    @GameTest(template = "test_arena", timeoutTicks = 100, skyAccess = true)
    public static void blockedTreeDoesNotLeavePartialLogs(GameTestHelper helper) {
        helper.setBlock(16, 12, 15, Blocks.STONE);
        boolean placed = JaggedTree.place(helper.getLevel(), helper.absolutePos(new BlockPos(16, 2, 16)), RandomSource.create(42), false);
        helper.assertTrue(!placed, "Blocked trunk should reject the whole tree");
        helper.assertTrue(count(helper, Blocks.DARK_OAK_LOG) == 0, "Rejected tree must not leave partial logs");
        helper.assertBlockPresent(Blocks.STONE, new BlockPos(16, 12, 15));
        helper.succeed();
    }

    @GameTest(template = "test_arena", timeoutTicks = 100, skyAccess = true)
    public static void rootsAndThornsGenerateOnGround(GameTestHelper helper) {
        var level = helper.getLevel();
        helper.assertTrue(RootlandsScatterFeature.roots(level, helper.absolutePos(new BlockPos(8, 2, 8)), RandomSource.create(11), false), "Roots should generate");
        helper.assertTrue(RootlandsScatterFeature.thorns(level, helper.absolutePos(new BlockPos(22, 2, 22)), RandomSource.create(13), false), "Thorns should generate");
        helper.assertTrue(count(helper, Blocks.DARK_OAK_LOG) >= 12, "Roots need multiple arms");
        helper.assertTrue(count(helper, Blocks.ROOTED_DIRT) >= 12, "Roots should change supporting soil");
        helper.assertTrue(count(helper, FantasyBiomes.THORN.get()) == 8, "Base thorn wall should be four columns of height two");
        helper.assertTrue(count(helper, Blocks.COARSE_DIRT) == 4, "Thorns should roughen supporting soil");
        helper.succeed();
    }

    @GameTest(template = "test_arena", timeoutTicks = 100, skyAccess = true)
    public static void thornContactRespectsDamageConfiguration(GameTestHelper helper) {
        var enabled = RootlandsConfig.THORNS_HURT.get();
        var amount = RootlandsConfig.THORN_DAMAGE.get();
        try {
            BlockPos relative = new BlockPos(8, 2, 8);
            helper.setBlock(relative, FantasyBiomes.THORN.get());
            var victim = helper.spawn(EntityType.PIG, relative);
            victim.setNoAi(true);
            var state = FantasyBiomes.THORN.get().defaultBlockState();
            float initial = victim.getHealth();
            RootlandsConfig.THORNS_HURT.set(false);
            state.entityInside(helper.getLevel(), helper.absolutePos(relative), victim);
            helper.assertTrue(victim.getHealth() == initial, "Disabled thorn damage must preserve health");
            RootlandsConfig.THORNS_HURT.set(true);
            RootlandsConfig.THORN_DAMAGE.set(2.0);
            state.entityInside(helper.getLevel(), helper.absolutePos(relative), victim);
            helper.assertTrue(victim.getHealth() == initial - 2.0F, "Enabled thorns must deal configured damage");
        } finally {
            RootlandsConfig.THORNS_HURT.set(enabled);
            RootlandsConfig.THORN_DAMAGE.set(amount);
        }
        helper.succeed();
    }

    @GameTest(template = "test_arena", timeoutTicks = 100, skyAccess = true)
    public static void zeroFrequencyDisablesProceduralGeneration(GameTestHelper helper) {
        double previous = RootlandsConfig.STRUCTURE_FREQUENCY.get();
        try {
            RootlandsConfig.STRUCTURE_FREQUENCY.set(0.0);
            var level = helper.getLevel();
            for (var feature : new RootlandsScatterFeature[]{FantasyBiomes.JAGGED_TREES.get(), FantasyBiomes.EXPOSED_ROOTS.get(), FantasyBiomes.THORN_PATCHES.get()}) {
                helper.assertTrue(!feature.place(new FeaturePlaceContext<>(Optional.empty(), level, level.getChunkSource().getGenerator(), RandomSource.create(17),
                        helper.absolutePos(new BlockPos(16, 2, 16)), NoneFeatureConfiguration.INSTANCE)), "Zero frequency must skip generation");
            }
            helper.assertTrue(count(helper, Blocks.DARK_OAK_LOG) == 0 && count(helper, FantasyBiomes.THORN.get()) == 0, "Disabled generation must leave terrain alone");
        } finally {
            RootlandsConfig.STRUCTURE_FREQUENCY.set(previous);
        }
        helper.succeed();
    }

    @GameTest(template = "test_arena", timeoutTicks = 100, skyAccess = true)
    public static void biomeAndSurfaceLoadAndInjectionSelectsRootlands(GameTestHelper helper) {
        var registries = helper.getLevel().registryAccess();
        var biome = registries.registryOrThrow(Registries.BIOME).getHolderOrThrow(FantasyBiomes.ROOTLANDS);
        helper.assertTrue(biome.value().getGrassColor(0, 0) == 0x4C5034, "Biome grass color must load from JSON");
        helper.assertTrue(registries.registryOrThrow(LithostitchedRegistries.SURFACE_RULE).containsKey(FantasyBiomes.id("rootlands")), "Custom surface rule must decode");
        var preset = registries.registryOrThrow(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST)
                .getHolderOrThrow(ResourceKey.create(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST, ResourceLocation.withDefaultNamespace("overworld")));
        var settings = registries.registryOrThrow(Registries.NOISE_SETTINGS).getHolderOrThrow(NoiseGeneratorSettings.OVERWORLD);
        var generator = new NoiseBasedChunkGenerator(MultiNoiseBiomeSource.createFromPreset(preset), settings);
        var dimensions = new MappedRegistry<LevelStem>(Registries.LEVEL_STEM, Lifecycle.stable());
        dimensions.register(LevelStem.OVERWORLD, new LevelStem(helper.getLevel().dimensionTypeRegistration(), generator), RegistrationInfo.BUILT_IN);
        dimensions.freeze();
        BiomeInjectorManager.applyBiomeInjectors(registries, dimensions, 12345L);
        var sampler = RandomState.create(settings.value(), registries.lookupOrThrow(Registries.NOISE), 12345L).sampler();
        boolean found = false;
        for (int x = -2048; x <= 2048 && !found; x += 16) for (int z = -2048; z <= 2048 && !found; z += 16) {
            found = generator.getBiomeSource().getNoiseBiome(x, 20, z, sampler).is(FantasyBiomes.ROOTLANDS);
        }
        helper.assertTrue(found, "Injected Overworld source should select Rootlands for the test seed");
        helper.succeed();
    }

    @GameTest(template = "test_arena", timeoutTicks = 100, skyAccess = true)
    public static void placedFeaturesDecorateOnlyRootlands(GameTestHelper helper) {
        int oldWidth = RootlandsConfig.EDGE_BLEND.get();
        try {
            // This arena has artificial biome borders only a few blocks away; test decoration separately from edge taper.
            RootlandsConfig.EDGE_BLEND.set(0);
            var level = helper.getLevel();
            var registry = level.registryAccess().registryOrThrow(Registries.PLACED_FEATURE);
            BlockPos corner = helper.absolutePos(new BlockPos(1, 2, 1));
            BlockPos origin = new BlockPos((corner.getX() + 15) & ~15, corner.getY(), (corner.getZ() + 15) & ~15);
            var trees = registry.getOrThrow(ResourceKey.create(Registries.PLACED_FEATURE, FantasyBiomes.id("jagged_trees")));
            helper.assertTrue(!trees.place(level, level.getChunkSource().getGenerator(), RandomSource.create(200), origin),
                    "Rootlands trees must not decorate the vanilla test biome");
            // Fill only the surface layer; the full tall arena exceeds Minecraft's biome-fill volume limit.
            var fill = net.minecraft.server.commands.FillBiomeCommand.fill(level,
                    helper.absolutePos(new BlockPos(0, 1, 0)), helper.absolutePos(new BlockPos(31, 7, 31)),
                    level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(FantasyBiomes.ROOTLANDS));
            helper.assertTrue(fill.left().isPresent(), "Surface biome fill should succeed: " + fill);
            for (String id : new String[]{"jagged_trees", "exposed_roots", "thorn_patches"}) {
                var feature = registry.getOrThrow(ResourceKey.create(Registries.PLACED_FEATURE, FantasyBiomes.id(id)));
                helper.assertTrue(feature.place(level, level.getChunkSource().getGenerator(), RandomSource.create(200), origin),
                        "Placed feature should produce blocks in Rootlands: " + id);
            }
            helper.assertTrue(count(helper, Blocks.DARK_OAK_LOG) > 10, "Rootlands decoration should contain woody structures");
            helper.assertTrue(count(helper, FantasyBiomes.THORN.get()) > 0, "Rootlands decoration should contain thorns");
            helper.succeed();
        } finally { RootlandsConfig.EDGE_BLEND.set(oldWidth); }
    }

    @GameTest(template = "test_arena", timeoutTicks = 100, skyAccess = true)
    public static void customDecorationTapersAtBiomeEdges(GameTestHelper helper) {
        int oldWidth = RootlandsConfig.EDGE_BLEND.get();
        try {
            RootlandsConfig.EDGE_BLEND.set(8);
            var level = helper.getLevel();
            var fill = net.minecraft.server.commands.FillBiomeCommand.fill(level,
                    helper.absolutePos(new BlockPos(0, 1, 0)), helper.absolutePos(new BlockPos(31, 7, 31)),
                    level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(FantasyBiomes.ROOTLANDS));
            helper.assertTrue(fill.left().isPresent(), "Surface biome fill should succeed");
            double center = RootlandsScatterFeature.edgeDensity(level, helper.absolutePos(new BlockPos(16, 2, 16)));
            double edge = RootlandsScatterFeature.edgeDensity(level, helper.absolutePos(new BlockPos(1, 2, 16)));
            helper.assertTrue(center == 1.0 && edge < center && edge >= 0.2, "Woodland decoration should thin out near a biome border");
        } finally { RootlandsConfig.EDGE_BLEND.set(oldWidth); }
        helper.succeed();
    }

    @GameTest(template = "test_arena", timeoutTicks = 100, skyAccess = true)
    public static void woodyDecorationPreservesSlopedGround(GameTestHelper helper) {
        // A real stepped slope catches accidental column excavation or flattening by root/terrain searches.
        for (int x = 0; x < 32; x++) for (int z = 0; z < 32; z++) {
            int top = 2 + x / 8;
            for (int y = 2; y < top; y++) helper.setBlock(x, y, z, Blocks.STONE);
            helper.setBlock(x, top, z, Blocks.GRASS_BLOCK);
        }
        var level = helper.getLevel();
        helper.assertTrue(JaggedTree.place(level, helper.absolutePos(new BlockPos(16, 5, 16)), RandomSource.create(6), false), "Slope should support a jagged tree");
        RootlandsScatterFeature.roots(level, helper.absolutePos(new BlockPos(8, 4, 8)), RandomSource.create(7), true);
        RootlandsScatterFeature.thorns(level, helper.absolutePos(new BlockPos(22, 5, 22)), RandomSource.create(8), true);
        for (int x = 0; x < 32; x++) for (int z = 0; z < 32; z++) {
            int top = 2 + x / 8;
            for (int y = 2; y < top; y++) helper.assertBlockPresent(Blocks.STONE, new BlockPos(x, y, z));
            helper.assertTrue(!helper.getBlockState(new BlockPos(x, top, z)).isAir(), "Decoration must preserve every original ground column");
        }
        helper.succeed();
    }

    private static int count(GameTestHelper helper, Block block) {
        int count = 0;
        for (int x = 0; x < 32; x++) for (int z = 0; z < 32; z++) for (int y = 0; y < 44; y++) {
            if (helper.getBlockState(new BlockPos(x, y, z)).is(block)) count++;
        }
        return count;
    }
}
