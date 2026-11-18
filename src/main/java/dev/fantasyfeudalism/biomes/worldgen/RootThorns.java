package dev.fantasyfeudalism.biomes.worldgen;

import dev.fantasyfeudalism.biomes.FantasyBiomes;
import dev.fantasyfeudalism.biomes.block.ThornBlock;
import java.util.Collection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;

/** Thorn spikes grow outward from exposed faces of actual bark-covered dark oak wood roots. */
public final class RootThorns {
    public static boolean decorate(WorldGenLevel level, Collection<BlockPos> roots, RandomSource random, double chance) {
        boolean placed = false;
        for (BlockPos root : roots) {
            if (!level.getBlockState(root).is(Blocks.DARK_OAK_WOOD)) continue;
            for (Direction face : Direction.values()) {
                if (random.nextDouble() >= chance) continue;
                BlockPos pos = root.relative(face);
                if (!level.ensureCanWrite(pos) || !level.getFluidState(pos).isEmpty()) continue;
                var existing = level.getBlockState(pos);
                if (!existing.isAir() && !existing.canBeReplaced()) continue;
                var thorn = FantasyBiomes.THORN.get().defaultBlockState()
                        .setValue(ThornBlock.FACING, face).setValue(ThornBlock.AXIS, face.getAxis())
                        .setValue(ThornBlock.TWIST, random.nextInt(4));
                if (thorn.canSurvive(level, pos)) { level.setBlock(pos, thorn, 2); placed = true; }
            }
        }
        return placed;
    }
    private RootThorns() {}
}
