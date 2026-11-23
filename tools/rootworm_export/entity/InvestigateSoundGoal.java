package dev.fantasyfeudalism.rootworm.entity;

import java.util.EnumSet;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

/** A neutral worm investigates the last sound location without acquiring an attack target. */
final class InvestigateSoundGoal extends Goal {
    private final RootWorm worm;
    private Vec3 destination;
    private int repath;
    InvestigateSoundGoal(RootWorm worm) {
        this.worm = worm;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }
    @Override public boolean canUse() { return worm.hasRecentSound() && !worm.isOnFire(); }
    @Override public boolean canContinueToUse() { return canUse(); }
    @Override public void start() { destination = null; repath = 0; }
    @Override public void tick() {
        Vec3 sound = worm.lastHeardPosition();
        worm.faceSound(sound);
        worm.getLookControl().setLookAt(sound.x, sound.y, sound.z);
        if (worm.distanceToSqr(sound) < 1.0) { worm.getNavigation().stop(); return; }
        if (--repath <= 0 && (destination == null || destination.distanceToSqr(sound) >= 1 || worm.getNavigation().isDone())) {
            destination = sound;
            repath = 10;
            worm.getNavigation().moveTo(sound.x, sound.y, sound.z, 0.85);
        }
    }
    @Override public void stop() { worm.getNavigation().stop(); }
}
