package dev.fantasyfeudalism.biomes.entity;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;

/** Periodically move to a reachable root-heavy patch; never consumes or destroys the root blocks. */
final class SeekRootsGoal extends Goal {
    private final RootWorm worm;
    private BlockPos destination;
    private int nextSearch;

    SeekRootsGoal(RootWorm worm) {
        this.worm = worm;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override public boolean canUse() {
        if (worm.getTarget() != null || worm.isOnFire() || worm.tickCount < nextSearch) return false;
        nextSearch = worm.tickCount + adjustedTickDelay(100);
        double best = Math.max(2, RootWorm.rootInterest(worm.level(), worm.blockPosition()));
        destination = null;
        for (int dx = -8; dx <= 8; dx += 2) for (int dz = -8; dz <= 8; dz += 2) for (int dy = -1; dy <= 1; dy++) {
            BlockPos pos = worm.blockPosition().offset(dx, dy, dz);
            if (worm.distanceToSqr(pos.getCenter()) < 4 || !worm.level().getBlockState(pos).isAir()
                    || !worm.level().getBlockState(pos.below()).isSolidRender(worm.level(), pos.below())) continue;
            double score = RootWorm.rootInterest(worm.level(), pos) - worm.distanceToSqr(pos.getCenter()) * 0.025;
            if (score > best) {
                var path = worm.getNavigation().createPath(pos, 0);
                if (path != null && path.canReach()) { best = score; destination = pos; }
            }
        }
        return destination != null;
    }

    @Override public void start() { worm.getNavigation().moveTo(destination.getX() + 0.5, destination.getY(), destination.getZ() + 0.5, 0.9); }
    @Override public boolean canContinueToUse() { return worm.getTarget() == null && !worm.isOnFire() && !worm.getNavigation().isDone(); }
    @Override public void stop() { worm.getNavigation().stop(); }
}
