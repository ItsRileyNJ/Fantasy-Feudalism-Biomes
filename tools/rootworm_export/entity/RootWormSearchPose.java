package dev.fantasyfeudalism.rootworm.entity;

import net.minecraft.util.Mth;

/** Shared search centerline, in model pixels. Rendering and damage parts follow the same curve. */
public final class RootWormSearchPose {
    // Rear three segments stay planted; the middle arches up and the snout tips slightly downward.
    private static final float[] PITCH = {-0.20F, 0.10F, 0.65F, 1.00F, 0.85F, 0.40F, 0, 0, 0};
    public record Pose(float x, float lift, float z, float pitch, float yaw) {}

    public static Pose sample(int segment, float amount, float age) {
        amount = Mth.clamp(amount, 0, 1);
        float scan = Mth.sin(age * 0.09F) * 0.45F * amount;
        float x = 0, lift = 0, z = RootWormShape.SEGMENTS.get(segment).z();
        if (segment < 6 && amount > 0) {
            z = RootWormShape.SEGMENTS.get(6).z();
            for (int i = 5; i >= segment; i--) {
                float length = RootWormShape.SEGMENTS.get(i + 1).z() - RootWormShape.SEGMENTS.get(i).z();
                float pitch = (PITCH[i] + PITCH[i + 1]) * 0.5F * amount;
                float yaw = scan * (5.5F - i) / 6.0F;
                float horizontal = length * Mth.cos(pitch);
                x -= horizontal * Mth.sin(yaw);
                lift += length * Mth.sin(pitch);
                z -= horizontal * Mth.cos(yaw);
            }
        }
        return new Pose(x, lift, z, PITCH[segment] * amount, scan * Math.max(0, 6 - segment) / 6.0F);
    }

    /** Extra AABB coverage for tilted body segments; unchanged when the worm is resting. */
    public static float width(int segment, Pose pose) {
        return RootWormShape.hitWidth(segment) + Math.abs(Mth.sin(pose.pitch())) * RootWormShape.SEGMENTS.get(segment).height() / 16;
    }
    public static float height(int segment, Pose pose) {
        return RootWormShape.hitHeight(segment) + Math.abs(Mth.sin(pose.pitch())) * RootWormShape.SEGMENTS.get(segment).length() / 16;
    }
    public static float bottom(int segment, Pose pose) {
        return RootWormShape.hitBottom(segment) + pose.lift() / 16
                - Math.abs(Mth.sin(pose.pitch())) * RootWormShape.SEGMENTS.get(segment).length() / 32;
    }
    private RootWormSearchPose() {}
}
