package dev.fantasyfeudalism.biomes.gametest;

import dev.fantasyfeudalism.biomes.FantasyBiomes;
import dev.fantasyfeudalism.biomes.block.ThornBlock;
import dev.fantasyfeudalism.biomes.worldgen.GiantThorns;
import dev.fantasyfeudalism.biomes.worldgen.RootThorns;
import dev.fantasyfeudalism.biomes.worldgen.RootlandsScatterFeature;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(FantasyBiomes.MOD_ID)
@PrefixGameTestTemplate(false)
public final class RootAttachmentGameTests {
    @GameTest(template = "test_arena", timeoutTicks = 100, skyAccess = true)
    public static void spikesAttachToAllSixRootFacesAndLoseTheirSupport(GameTestHelper helper) {
        BlockPos relative = new BlockPos(16, 8, 16);
        BlockPos root = helper.absolutePos(relative);
        helper.setBlock(relative, Blocks.DARK_OAK_WOOD);
        helper.assertTrue(RootThorns.decorate(helper.getLevel(), List.of(root), RandomSource.create(3), 1.0), "Exposed wood must accept spikes");
        for (Direction face : Direction.values()) {
            var state = helper.getLevel().getBlockState(root.relative(face));
            helper.assertTrue(state.is(FantasyBiomes.THORN.get()) && state.getValue(ThornBlock.FACING) == face,
                    "Missing outward thorn on " + face);
            helper.assertTrue(state.canSurvive(helper.getLevel(), root.relative(face)), "Thorn must be supported by its root");
        }
        helper.destroyBlock(relative);
        for (Direction face : Direction.values()) helper.assertBlockPresent(Blocks.AIR, relative.relative(face));
        helper.succeed();
    }

    @GameTest(template = "test_arena", timeoutTicks = 100, skyAccess = true)
    public static void handPlacementUsesClickedRootFaceAndRejectsStone(GameTestHelper helper) {
        BlockPos relative = new BlockPos(16, 8, 16);
        BlockPos root = helper.absolutePos(relative);
        helper.setBlock(relative, Blocks.DARK_OAK_WOOD);
        var player = helper.makeMockPlayer(GameType.CREATIVE);
        for (Direction face : Direction.values()) {
            var context = new BlockPlaceContext(helper.getLevel(), player, InteractionHand.MAIN_HAND,
                    FantasyBiomes.THORN_ITEM.get().getDefaultInstance(), new BlockHitResult(Vec3.atCenterOf(root), face, root, false));
            var state = FantasyBiomes.THORN.get().getStateForPlacement(context);
            helper.assertTrue(state != null && state.getValue(ThornBlock.FACING) == face, "Hand placement should follow " + face);
        }
        helper.setBlock(relative, Blocks.STONE);
        var context = new BlockPlaceContext(helper.getLevel(), player, InteractionHand.MAIN_HAND,
                FantasyBiomes.THORN_ITEM.get().getDefaultInstance(), new BlockHitResult(Vec3.atCenterOf(root), Direction.UP, root, false));
        helper.assertTrue(FantasyBiomes.THORN.get().getStateForPlacement(context) == null, "Thorns must not become freestanding spikes on stone");
        helper.succeed();
    }

    @GameTest(template = "test_arena", timeoutTicks = 100, skyAccess = true)
    public static void generatedArchesAreWoodWithSupportedTopSideAndBottomThorns(GameTestHelper helper) {
        helper.assertTrue(GiantThorns.place(helper.getLevel(), helper.absolutePos(new BlockPos(8, 2, 16)), RandomSource.create(5), false), "Root arch should generate");
        var faces = EnumSet.noneOf(Direction.class);
        var twists = new java.util.HashSet<Integer>();
        int wood = 0, spikes = 0;
        for (int x = 0; x < 32; x++) for (int z = 0; z < 32; z++) for (int y = 1; y < 25; y++) {
            BlockPos pos = helper.absolutePos(new BlockPos(x, y, z));
            var state = helper.getLevel().getBlockState(pos);
            if (state.is(Blocks.DARK_OAK_WOOD)) wood++;
            if (state.is(Blocks.DARK_OAK_LOG) || state.is(Blocks.STRIPPED_DARK_OAK_LOG)) helper.fail("Root arches must use bark-covered wood, not logs");
            if (state.is(FantasyBiomes.THORN.get())) {
                spikes++;
                Direction face = state.getValue(ThornBlock.FACING);
                faces.add(face);
                twists.add(state.getValue(ThornBlock.TWIST));
                helper.assertTrue(helper.getLevel().getBlockState(pos.relative(face.getOpposite())).is(Blocks.DARK_OAK_WOOD), "Every generated thorn must connect to root wood");
            }
        }
        helper.assertTrue(twists.size() >= 3, "Generated thorns must vary their roll around their attachment axis");
        helper.assertTrue(wood > 70 && spikes > 20, "Arch needs a solid wood body with visible attached thorns");
        helper.assertTrue(faces.contains(Direction.UP) && faces.contains(Direction.DOWN) && faces.stream().anyMatch(f -> f.getAxis().isHorizontal()), "Arch must carry top, underside and side thorns");
        helper.succeed();
    }

    @GameTest(template = "test_arena", timeoutTicks = 100, skyAccess = true)
    public static void smallThornPatchesAreAlsoRootedWoodNotSpikeWalls(GameTestHelper helper) {
        helper.assertTrue(RootlandsScatterFeature.thorns(helper.getLevel(), helper.absolutePos(new BlockPos(16, 2, 16)), RandomSource.create(13), false), "Root patch should generate");
        int wood = 0, spikes = 0;
        for (int x = 8; x <= 24; x++) for (int z = 8; z <= 24; z++) for (int y = 1; y <= 6; y++) {
            BlockPos pos = helper.absolutePos(new BlockPos(x, y, z));
            var state = helper.getLevel().getBlockState(pos);
            if (state.is(Blocks.DARK_OAK_WOOD)) wood++;
            if (state.is(FantasyBiomes.THORN.get())) {
                spikes++;
                helper.assertTrue(state.canSurvive(helper.getLevel(), pos), "Small-patch thorns must also have root support");
            }
        }
        helper.assertTrue(wood >= 20 && spikes > 0, "Patch needs a wood root body, not a wall made from thorn blocks");
        helper.succeed();
    }
}
