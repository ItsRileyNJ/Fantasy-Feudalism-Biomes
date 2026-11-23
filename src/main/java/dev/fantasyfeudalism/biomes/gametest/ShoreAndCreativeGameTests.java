package dev.fantasyfeudalism.biomes.gametest;

import dev.fantasyfeudalism.biomes.FantasyBiomes;
import dev.fantasyfeudalism.biomes.entity.RootWorm;
import dev.fantasyfeudalism.biomes.worldgen.RootlandsShoreFeature;
import dev.fantasyfeudalism.biomes.worldgen.ShorelineProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(FantasyBiomes.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ShoreAndCreativeGameTests {
    @GameTest(template = "test_arena", timeoutTicks = 100, skyAccess = true)
    public static void tallCoastBecomesGradualOnLandAndUnderwater(GameTestHelper helper) {
        // The shoreline is outside Rootlands itself because its dark-oak forest rim is preserved.
        ShorelineProfile.Sampler cliff = (x, z) -> new ShorelineProfile.Sample(x >= 0, x < -32);
        int previous = 112;
        for (int chunkX = -80; chunkX < 80; chunkX += 16) {
            var profile = new ShorelineProfile(chunkX, 0, 63, 64, cliff);
            for (int x = chunkX; x < chunkX + 16; x++) {
                int target = profile.targetHeight(x, 8, x < 0 ? 112 : 24);
                helper.assertTrue(target <= previous + 1 && previous - target <= 4,
                        "An 88-block coastal cliff should grade continuously, x=" + x + " previous=" + previous + " target=" + target);
                if (x == -1) helper.assertTrue(target <= 64, "Land edge must descend to water level");
                if (x == 1) helper.assertTrue(target >= 58 && target < 63, "Seabed must continue as a shallow slope");
                if (x == -80) helper.assertTrue(target == 112, "Inland terrain beyond transition stays intact");
                previous = target;
            }
        }
        helper.succeed();
    }

    @GameTest(template = "test_arena", timeoutTicks = 100, skyAccess = true)
    public static void gentleCoastsAndShallowWaterKeepTheirOriginalShape(GameTestHelper helper) {
        for (int chunkX = -64; chunkX < 64; chunkX += 16) {
            var profile = new ShorelineProfile(chunkX, 0, 63, 64, (x, z) -> new ShorelineProfile.Sample(x >= 0, x < 0));
            for (int x = chunkX; x < chunkX + 16; x++) {
                for (int original = 63; original <= 66; original++) {
                    helper.assertTrue(profile.targetHeight(x, 8, original) == original, "Gentle banks must retain their natural shape, x=" + x);
                }
                for (int original = 59; original <= 61; original++) {
                    helper.assertTrue(profile.targetHeight(x, 8, original) == original, "Shallow beds must not be forced into a shelf, x=" + x);
                }
            }
        }
        helper.succeed();
    }

    @GameTest(template = "test_arena", timeoutTicks = 100, skyAccess = true)
    public static void narrowRiverGetsSlopedBanks(GameTestHelper helper) {
        ShorelineProfile.Sampler river = (x, z) -> new ShorelineProfile.Sample(Math.abs(x) < 4, Math.abs(x) >= 4);
        var west = new ShorelineProfile(-16, 0, 63, 64, river);
        var east = new ShorelineProfile(0, 0, 63, 64, river);
        int bank = east.targetHeight(5, 8, 100);
        int bed = east.targetHeight(1, 8, 30);
        helper.assertTrue(bank <= 65 && bed >= 58 && bank - bed <= 5, "River bank and bed should meet near water level");
        helper.assertTrue(Math.abs(west.targetHeight(-5, 8, 100) - bank) <= 2, "Both banks should be graded");
        helper.succeed();
    }

    @GameTest(template = "test_arena", timeoutTicks = 100, skyAccess = true)
    public static void shorelineProfilesAgreeAcrossChunkBoundaries(GameTestHelper helper) {
        ShorelineProfile.Sampler coast = (x, z) -> new ShorelineProfile.Sample(z >= 0, x < 12 && z < 0);
        // An odd width also verifies world-grid alignment when players choose a non-multiple of four.
        var west = new ShorelineProfile(-16, 0, 63, 65, coast);
        var east = new ShorelineProfile(0, 0, 63, 65, coast);
        for (int z = 0; z <= 16; z++) {
            helper.assertTrue(Math.abs(west.strength(0, z) - east.strength(0, z)) < 1e-12,
                    "Adjacent chunk profiles must have identical shared edge values");
            helper.assertTrue(Math.abs(west.signedDistance(0, z) - east.signedDistance(0, z)) < 1e-12,
                    "The sampled shoreline must also agree at a shared chunk edge");
            helper.assertTrue(west.targetHeight(0, z, 80) == east.targetHeight(0, z, 80), "Grading must agree at a shared chunk edge");
        }
        var distant = new ShorelineProfile(0, 0, 63, 64, (x, z) -> new ShorelineProfile.Sample(x >= 0, false));
        helper.assertTrue(distant.strength(1, 8) == 0, "Unrelated shores far from Rootlands must stay unchanged");
        helper.succeed();
    }

    @GameTest(template = "test_arena", timeoutTicks = 100, skyAccess = true)
    public static void shoreWritesLandWaterAndBedWithoutDeletingStructures(GameTestHelper helper) {
        var level = helper.getLevel();
        for (int y = 2; y <= 20; y++) helper.setBlock(8, y, 8, Blocks.STONE);
        helper.setBlock(8, 5, 8, Blocks.AIR);
        helper.setBlock(8, 20, 8, Blocks.GRASS_BLOCK);
        for (int y = 17; y < 20; y++) helper.setBlock(8, y, 8, Blocks.DIRT);
        BlockPos land = helper.absolutePos(new BlockPos(8, 0, 8));
        int offset = land.getY();
        helper.assertTrue(!RootlandsShoreFeature.reshapeColumn(level, land.getX(), land.getZ(), offset + 20, offset + 6, offset + 8), "Dry land must never be flooded into an artificial river");
        helper.assertBlockPresent(Blocks.GRASS_BLOCK, new BlockPos(8, 20, 8));
        helper.assertTrue(RootlandsShoreFeature.reshapeColumn(level, land.getX(), land.getZ(), offset + 20, offset + 8, offset + 8), "Land cliff should become a dry grassy bank");
        helper.assertBlockPresent(Blocks.GRASS_BLOCK, new BlockPos(8, 8, 8));
        helper.assertBlockPresent(Blocks.DIRT, new BlockPos(8, 7, 8));
        helper.assertBlockPresent(Blocks.DIRT, new BlockPos(8, 5, 8));
        helper.assertBlockPresent(Blocks.AIR, new BlockPos(8, 9, 8));

        helper.setBlock(12, 2, 8, Blocks.GRAVEL);
        helper.setBlock(12, 1, 8, Blocks.STONE);
        helper.setBlock(12, 0, 8, Blocks.STONE);
        for (int y = 3; y < 8; y++) helper.setBlock(12, y, 8, Blocks.WATER);
        BlockPos bed = helper.absolutePos(new BlockPos(12, 0, 8));
        helper.assertTrue(RootlandsShoreFeature.reshapeColumn(level, bed.getX(), bed.getZ(), offset + 2, offset + 5, offset + 8), "Underwater bed should rise toward shore");
        helper.assertBlockPresent(Blocks.GRAVEL, new BlockPos(12, 5, 8));
        helper.assertBlockPresent(Blocks.WATER, new BlockPos(12, 7, 8));
        helper.assertBlockPresent(Blocks.AIR, new BlockPos(12, 8, 8));
        helper.assertTrue(!RootlandsShoreFeature.reshapeColumn(level, bed.getX(), bed.getZ(), offset + 5, offset + 8, offset + 8), "A river bed must remain submerged");

        for (int y = 2; y <= 15; y++) helper.setBlock(20, y, 8, Blocks.STONE);
        helper.setBlock(20, 10, 8, Blocks.DARK_OAK_LOG);
        BlockPos protectedColumn = helper.absolutePos(new BlockPos(20, 0, 8));
        helper.assertTrue(!RootlandsShoreFeature.reshapeColumn(level, protectedColumn.getX(), protectedColumn.getZ(), offset + 15, offset + 8, offset + 8), "A structure column must be rejected atomically");
        helper.assertBlockPresent(Blocks.DARK_OAK_LOG, new BlockPos(20, 10, 8));
        helper.assertBlockPresent(Blocks.STONE, new BlockPos(20, 15, 8));
        for (int y = 2; y <= 15; y++) helper.setBlock(24, y, 8, Blocks.STONE);
        helper.setBlock(24, 10, 8, Blocks.DARK_OAK_STAIRS.defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED, true));
        BlockPos waterlogged = helper.absolutePos(new BlockPos(24, 0, 8));
        helper.assertTrue(!RootlandsShoreFeature.reshapeColumn(level, waterlogged.getX(), waterlogged.getZ(), offset + 15, offset + 8, offset + 8), "Waterlogged structures must not be mistaken for ordinary water");
        helper.assertBlockPresent(Blocks.DARK_OAK_STAIRS, new BlockPos(24, 10, 8));
        helper.succeed();
    }

    @GameTest(template = "test_arena", timeoutTicks = 100, skyAccess = true)
    public static void unchangedGrassAtTheWaterEdgeIsNeverPaintedSand(GameTestHelper helper) {
        for (int y = 2; y <= 8; y++) helper.setBlock(8, y, 8, Blocks.DIRT);
        helper.setBlock(8, 8, 8, Blocks.GRASS_BLOCK);
        var pos = helper.absolutePos(new BlockPos(8, 8, 8));
        helper.assertTrue(!RootlandsShoreFeature.reshapeColumn(helper.getLevel(), pos.getX(), pos.getZ(), pos.getY(), pos.getY(), pos.getY()), "An unchanged bank must not trigger any material replacement");
        helper.assertBlockPresent(Blocks.GRASS_BLOCK, new BlockPos(8, 8, 8));
        helper.assertBlockPresent(Blocks.DIRT, new BlockPos(8, 7, 8));
        helper.assertBlockPresent(Blocks.AIR, new BlockPos(8, 9, 8));
        helper.succeed();
    }

    @GameTest(template = "test_arena", timeoutTicks = 100, skyAccess = true)
    public static void gradingRetainsNaturalSandAndRootedSoilInsteadOfApplyingAShorePalette(GameTestHelper helper) {
        var caps = new net.minecraft.world.level.block.Block[]{Blocks.SAND, Blocks.RED_SAND, Blocks.ROOTED_DIRT, Blocks.COARSE_DIRT};
        for (int i = 0; i < caps.length; i++) {
            int x = 8 + i * 5;
            for (int y = 2; y <= 15; y++) helper.setBlock(x, y, 8, Blocks.STONE);
            for (int y = 13; y <= 15; y++) helper.setBlock(x, y, 8, caps[i]);
            var pos = helper.absolutePos(new BlockPos(x, 0, 8));
            helper.assertTrue(RootlandsShoreFeature.reshapeColumn(helper.getLevel(), pos.getX(), pos.getZ(), pos.getY() + 15, pos.getY() + 8, pos.getY() + 8), "A steep native bank should be gradeable");
            for (int y = 6; y <= 8; y++) helper.assertBlockPresent(caps[i], new BlockPos(x, y, 8));
            helper.assertBlockPresent(Blocks.STONE, new BlockPos(x, 5, 8));
        }
        helper.succeed();
    }

    @GameTest(template = "test_arena", timeoutTicks = 100, skyAccess = true)
    public static void naturalBeachWidthsVaryAndGrassCanMeetWaterWithoutAnySand(GameTestHelper helper) {
        int[] widths = {0, 2, 7, 4};
        for (int row = 0; row < widths.length; row++) {
            int sandAfter = 0;
            for (int column = 0; column < 16; column++) {
                int x = 8 + column, z = 8 + row * 3;
                var nativeCap = column < widths[row] ? Blocks.SAND : Blocks.GRASS_BLOCK;
                for (int y = 2; y <= 15; y++) helper.setBlock(x, y, z, Blocks.STONE);
                helper.setBlock(x, 15, z, nativeCap);
                helper.setBlock(x, 14, z, column < widths[row] ? Blocks.SAND : Blocks.DIRT);
                helper.setBlock(x, 13, z, column < widths[row] ? Blocks.SAND : Blocks.DIRT);
                var pos = helper.absolutePos(new BlockPos(x, 0, z));
                helper.assertTrue(RootlandsShoreFeature.reshapeColumn(helper.getLevel(), pos.getX(), pos.getZ(), pos.getY() + 15, pos.getY() + 8, pos.getY() + 8), "Steep shore should be gradeable");
                helper.assertBlockPresent(nativeCap, new BlockPos(x, 8, z));
                if (helper.getBlockState(new BlockPos(x, 8, z)).is(Blocks.SAND)) sandAfter++;
            }
            helper.assertTrue(sandAfter == widths[row], "Shore grading must retain each natural beach width, including zero; row=" + row);
        }
        helper.succeed();
    }

    @GameTest(template = "test_arena", timeoutTicks = 100, skyAccess = true)
    public static void creativeTabContainsSpawnEggFlowerAndThorn(GameTestHelper helper) {
        var tab = FantasyBiomes.CREATIVE_TAB.get();
        tab.buildContents(new CreativeModeTab.ItemDisplayParameters(FeatureFlags.DEFAULT_FLAGS, true, helper.getLevel().registryAccess()));
        helper.assertTrue(tab.getDisplayItems().size() == 6, "Custom tab should list all six mod items");
        helper.assertTrue(tab.contains(FantasyBiomes.ROOT_WORM_EGG.get().getDefaultInstance()), "Missing Root Worm spawn egg");
        helper.assertTrue(tab.contains(FantasyBiomes.ROOT_FLOWER_ITEM.get().getDefaultInstance()), "Missing Root Flower");
        helper.assertTrue(tab.contains(FantasyBiomes.THORN_ITEM.get().getDefaultInstance()), "Missing Thorn");
        helper.assertTrue(tab.contains(FantasyBiomes.SAPLING_ITEM.get().getDefaultInstance()), "Missing Rootlands Sapling");
        helper.assertTrue(tab.contains(FantasyBiomes.DARK_GRASS_ITEM.get().getDefaultInstance()), "Missing Dark Grass plant");
        helper.assertTrue(tab.contains(FantasyBiomes.LEAVES_ITEM.get().getDefaultInstance()), "Missing Rootlands Leaves");
        helper.succeed();
    }

    @GameTest(template = "test_arena", timeoutTicks = 100, skyAccess = true)
    public static void rootWormSpawnSettingsAndAcceptanceAreHigher(GameTestHelper helper) {
        var level = helper.getLevel();
        var biome = level.registryAccess().registryOrThrow(Registries.BIOME).getOrThrow(FantasyBiomes.ROOTLANDS);
        var spawn = biome.getMobSettings().getMobs(MobCategory.CREATURE).unwrap().stream()
                .filter(entry -> entry.type == FantasyBiomes.ROOT_WORM.get()).findFirst().orElseThrow();
        helper.assertTrue(spawn.getWeight().asInt() == 30 && spawn.minCount == 2 && spawn.maxCount == 4, "Worm spawn weight/group size should increase");
        var fill = net.minecraft.server.commands.FillBiomeCommand.fill(level,
                helper.absolutePos(new BlockPos(0, 1, 0)), helper.absolutePos(new BlockPos(31, 7, 31)),
                level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(FantasyBiomes.ROOTLANDS));
        helper.assertTrue(fill.left().isPresent(), "Biome fill should succeed");
        int accepted = 0;
        var random = RandomSource.create(678);
        for (int i = 0; i < 1000; i++) if (RootWorm.canSpawn(FantasyBiomes.ROOT_WORM.get(), level, MobSpawnType.NATURAL,
                helper.absolutePos(new BlockPos(16, 2, 16)), random)) accepted++;
        helper.assertTrue(accepted > 400 && accepted < 600, "Ordinary ground should now accept about half of spawn attempts, got " + accepted);
        helper.succeed();
    }

    @GameTest(template = "test_arena", timeoutTicks = 100, skyAccess = true)
    public static void shorePassNeverRunsInDecoration(GameTestHelper helper) {
        var registry = helper.getLevel().registryAccess().registryOrThrow(Registries.BIOME);
        var feature = helper.getLevel().registryAccess().registryOrThrow(Registries.PLACED_FEATURE)
                .getOrThrow(net.minecraft.resources.ResourceKey.create(Registries.PLACED_FEATURE, FantasyBiomes.id("shoreline")));
        for (var key : java.util.List.of(FantasyBiomes.ROOTLANDS, Biomes.BEACH, Biomes.OCEAN, Biomes.RIVER)) {
            var entries = registry.getOrThrow(key).getGenerationSettings().features().get(GenerationStep.Decoration.RAW_GENERATION.ordinal());
            long count = entries.stream().filter(holder -> holder.value() == feature).count();
            helper.assertTrue(count == 0, "Late terrain edits must not be scheduled in " + key.location());
        }
        helper.succeed();
    }
}
