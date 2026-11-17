package dev.fantasyfeudalism.biomes.gametest;

import dev.fantasyfeudalism.biomes.FantasyBiomes;
import dev.fantasyfeudalism.biomes.RootlandsConfig;
import dev.fantasyfeudalism.biomes.worldgen.GiantThorns;
import dev.fantasyfeudalism.biomes.worldgen.JaggedTree;
import dev.fantasyfeudalism.biomes.worldgen.RootlandsScatterFeature;
import java.util.HashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SaplingBlock;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(FantasyBiomes.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ForestVegetationGameTests {
    @GameTest(template = "test_arena", timeoutTicks = 100, skyAccess = true)
    public static void fourDetailedTreeVariantsHaveDifferentGeometry(GameTestHelper helper) {
        var fingerprints = new HashSet<Long>();
        int index = 0;
        for (var variant : JaggedTree.Variant.values()) {
            int x = (index % 2 == 0) ? 12 : 36;
            int z = index < 2 ? 12 : 36;
            BlockPos base = new BlockPos(x, 2, z);
            helper.assertTrue(JaggedTree.placeVariant(helper.getLevel(), helper.absolutePos(base), RandomSource.create(11), variant, false), "Variant must generate: " + variant);
            long hash = 1;
            int branches = 0, leaves = 0, top = 0;
            for (int dx = -10; dx <= 10; dx++) for (int dz = -10; dz <= 10; dz++) for (int y = 0; y < 42; y++) {
                var state = helper.getBlockState(base.offset(dx, y, dz));
                if (state.is(Blocks.DARK_OAK_LOG)) {
                    hash = hash * 31 + new BlockPos(dx, y, dz).asLong();
                    if (Math.abs(dx) + Math.abs(dz) > 3) branches++;
                    top = Math.max(top, y);
                }
                if (state.is(FantasyBiomes.ROOTLANDS_LEAVES.get())) leaves++;
            }
            helper.assertTrue(top >= 23 && branches > 40 && leaves > 100, "Variant should be tall, branched and leafy: " + variant);
            fingerprints.add(hash);
            index++;
        }
        helper.assertTrue(fingerprints.size() == 4, "Each tree variant must have a distinct structure");
        helper.assertBlockPresent(Blocks.AIR, new BlockPos(36, 5, 36));
        helper.succeed();
    }

    @GameTest(template = "test_arena", timeoutTicks = 100, skyAccess = true)
    public static void saplingGrowsOneTreeOutsideRootlands(GameTestHelper helper) {
        boolean variants = RootlandsConfig.VARIANTS.get();
        try {
            RootlandsConfig.VARIANTS.set(false);
            BlockPos relative = new BlockPos(16, 2, 16);
            BlockPos pos = helper.absolutePos(relative);
            var sapling = FantasyBiomes.ROOTLANDS_SAPLING.get();
            helper.assertTrue(!helper.getLevel().getBiome(pos).is(FantasyBiomes.ROOTLANDS), "Test should grow in an ordinary biome");
            var state = sapling.defaultBlockState().setValue(SaplingBlock.STAGE, 1);
            helper.setBlock(relative, state);
            sapling.performBonemeal(helper.getLevel(), RandomSource.create(42), pos, state);
            helper.assertBlockPresent(Blocks.DARK_OAK_LOG, relative);
            helper.assertTrue(count(helper, FantasyBiomes.ROOTLANDS_LEAVES.get()) > 100, "Sapling must produce our custom canopy");
        } finally { RootlandsConfig.VARIANTS.set(variants); }
        helper.succeed();
    }

    @GameTest(template = "test_arena", timeoutTicks = 100, skyAccess = true)
    public static void blockedSaplingIsRestoredWithoutPartialTree(GameTestHelper helper) {
        boolean variants = RootlandsConfig.VARIANTS.get();
        try {
            RootlandsConfig.VARIANTS.set(false);
            BlockPos relative = new BlockPos(16, 2, 16);
            var sapling = FantasyBiomes.ROOTLANDS_SAPLING.get();
            var state = sapling.defaultBlockState().setValue(SaplingBlock.STAGE, 1);
            helper.setBlock(relative, state);
            helper.setBlock(16, 12, 15, Blocks.STONE);
            sapling.performBonemeal(helper.getLevel(), RandomSource.create(42), helper.absolutePos(relative), state);
            helper.assertBlockPresent(sapling, relative);
            helper.assertTrue(count(helper, Blocks.DARK_OAK_LOG) == 0, "Blocked growth must not leave partial trees");
        } finally { RootlandsConfig.VARIANTS.set(variants); }
        helper.succeed();
    }

    @GameTest(template = "test_arena", timeoutTicks = 100, skyAccess = true)
    public static void leavesCanDropSaplingsAndBeCollectedWithShears(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(8, 3, 8));
        var state = FantasyBiomes.ROOTLANDS_LEAVES.get().defaultBlockState();
        var sheared = Block.getDrops(state, helper.getLevel(), pos, null, null, new ItemStack(Items.SHEARS));
        helper.assertTrue(sheared.stream().anyMatch(stack -> stack.is(FantasyBiomes.LEAVES_ITEM.get())), "Shears should collect Rootlands Leaves");
        boolean sapling = false;
        for (int i = 0; i < 512 && !sapling; i++) {
            sapling = Block.getDrops(state, helper.getLevel(), pos, null, null, ItemStack.EMPTY).stream()
                    .anyMatch(stack -> stack.is(FantasyBiomes.SAPLING_ITEM.get()));
        }
        helper.assertTrue(sapling, "Ordinary leaf drops must make the tree renewable");
        helper.succeed();
    }

    @GameTest(template = "test_arena", timeoutTicks = 100, skyAccess = true)
    public static void darkGrassIsAPlantAndKeepsItsGround(GameTestHelper helper) {
        var level = helper.getLevel();
        var fill = net.minecraft.server.commands.FillBiomeCommand.fill(level,
                helper.absolutePos(new BlockPos(0, 1, 0)), helper.absolutePos(new BlockPos(31, 7, 31)),
                level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.BIOME).getHolderOrThrow(FantasyBiomes.ROOTLANDS));
        helper.assertTrue(fill.left().isPresent(), "Biome fill should succeed");
        helper.assertTrue(RootlandsScatterFeature.grass(level, helper.absolutePos(new BlockPos(16, 2, 16)), RandomSource.create(8)), "Dark grass patch should generate");
        helper.assertTrue(count(helper, FantasyBiomes.DARK_GRASS.get()) > 0, "Patch should contain the dark plant block");
        helper.assertBlockPresent(Blocks.GRASS_BLOCK, new BlockPos(16, 1, 16));
        helper.assertTrue(FantasyBiomes.DARK_GRASS.get() instanceof net.minecraft.world.level.block.BushBlock, "Dark Grass must be a plant, not a grass-block replacement");
        helper.succeed();
    }

    @GameTest(template = "test_arena", timeoutTicks = 100, skyAccess = true)
    public static void giantThornsFormTallBentAboveGroundArches(GameTestHelper helper) {
        helper.assertTrue(GiantThorns.place(helper.getLevel(), helper.absolutePos(new BlockPos(8, 2, 16)), RandomSource.create(5), false), "Giant thorn arch should generate");
        int high = 0, horizontal = 0, total = 0;
        for (int x = 0; x < 32; x++) for (int z = 0; z < 32; z++) for (int y = 2; y < 24; y++) {
            var state = helper.getBlockState(new BlockPos(x, y, z));
            if (!state.is(Blocks.DARK_OAK_WOOD)) continue;
            total++;
            if (y > 10) high++;
            if (state.getValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS) != net.minecraft.core.Direction.Axis.Y) horizontal++;
        }
        helper.assertTrue(total > 70 && high > 20 && horizontal > 20, "Thorned wood roots need large elevated curves and sideways bends");
        helper.assertBlockPresent(Blocks.AIR, new BlockPos(14, 5, 16));
        helper.succeed();
    }

    private static int count(GameTestHelper helper, Block block) {
        int count = 0;
        for (int x = 0; x < 48; x++) for (int z = 0; z < 48; z++) for (int y = 1; y < 44; y++) {
            if (helper.getBlockState(new BlockPos(x, y, z)).is(block)) count++;
        }
        return count;
    }
}
