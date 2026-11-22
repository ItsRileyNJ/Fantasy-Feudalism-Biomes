package dev.fantasyfeudalism.biomes.worldgen;

/**
 * A world-aligned shore influence field. Sampling original terrain, never neighboring decorated chunks,
 * makes the result independent of chunk generation order. The same grid corner has the same value in
 * either neighboring chunk, and bilinear interpolation prevents a step at the chunk boundary.
 */
public final class ShorelineProfile {
    public static final int STEP = 4;
    public record Sample(boolean wet, boolean rootlands) {}
    @FunctionalInterface public interface Sampler { Sample sample(int x, int z); }
    private final int chunkX, chunkZ, seaLevel, width;
    private final double[][] distances = new double[5][5];
    private final double[][] influences = new double[5][5];

    public ShorelineProfile(int chunkX, int chunkZ, int seaLevel, int width, Sampler sampler) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.seaLevel = seaLevel;
        this.width = width;
        int margin = sampleMargin(width);
        int firstX = chunkX - margin;
        int firstZ = chunkZ - margin;
        int count = (16 + margin * 2) / STEP + 1;
        Sample[][] terrain = new Sample[count][count];
        for (int x = 0; x < count; x++) for (int z = 0; z < count; z++) {
            terrain[x][z] = sampler.sample(firstX + x * STEP, firstZ + z * STEP);
        }
        for (int cx = 0; cx < 5; cx++) for (int cz = 0; cz < 5; cz++) {
            int wx = chunkX + cx * STEP;
            int wz = chunkZ + cz * STEP;
            boolean wet = terrain[(wx - firstX) / STEP][(wz - firstZ) / STEP].wet();
            double opposite = Double.POSITIVE_INFINITY;
            double root = Double.POSITIVE_INFINITY;
            for (int x = 0; x < count; x++) for (int z = 0; z < count; z++) {
                int dx = firstX + x * STEP - wx;
                int dz = firstZ + z * STEP - wz;
                double distance = (double) dx * dx + (double) dz * dz;
                if ((terrain[x][z].wet()) != wet) opposite = Math.min(opposite, distance);
                if (terrain[x][z].rootlands()) root = Math.min(root, distance);
            }
            // The boundary lies between opposite grid samples. Signed distances interpolate through it,
            // avoiding a four-block-wide constant weight on both sides of every shoreline.
            double distance = Math.min(width + STEP, Math.max(0, Math.sqrt(opposite) - STEP / 2.0));
            distances[cx][cz] = wet ? -distance : distance;
            // Extend through neighboring beach/river biomes, then fade, rather than creating a wall at their border.
            // Keep full grading through the preserved dark-forest rim, then fade into unrelated shores.
            double influence = 1.0 - smooth((Math.sqrt(root) - width) / width);
            influences[cx][cz] = influence;
        }
    }

    public double strength(int x, int z) {
        // A linear approach to the water keeps rising inland instead of easing into a broad sea-level pad.
        return Math.max(0, 1 - Math.abs(signedDistance(x, z)) / width) * interpolate(influences, x, z);
    }

    public double signedDistance(int x, int z) { return interpolate(distances, x, z); }

    /** Sand is a narrow shoreline trim, independent of the much wider terrain blending band. */
    public boolean beachBand(int x, int z, int top) {
        return Math.abs(signedDistance(x, z)) <= 6 && top >= seaLevel - 2 && top <= seaLevel + 2
                && interpolate(influences, x, z) > 0;
    }

    private double interpolate(double[][] grid, int x, int z) {
        double gx = (x - chunkX) / (double) STEP;
        double gz = (z - chunkZ) / (double) STEP;
        int ix = Math.clamp((int) Math.floor(gx), 0, 3);
        int iz = Math.clamp((int) Math.floor(gz), 0, 3);
        double tx = Math.clamp(gx - ix, 0, 1);
        double tz = Math.clamp(gz - iz, 0, 1);
        double north = grid[ix][iz] * (1 - tx) + grid[ix + 1][iz] * tx;
        double south = grid[ix][iz + 1] * (1 - tx) + grid[ix + 1][iz + 1] * tx;
        return north * (1 - tz) + south * tz;
    }

    public int targetHeight(int x, int z, int originalTop) {
        // seaLevel is the first air block: vanilla water ends at seaLevel - 1.
        // Land approaches a dry bank; an existing bed approaches a shallow submerged shelf.
        // Never turn dry forest into a river or lift a wet bed above its water surface.
        boolean land = originalTop >= seaLevel - 1;
        double distance = Math.max(0, land ? signedDistance(x, z) : -signedDistance(x, z));
        double amount = Math.max(0, 1 - distance / width) * interpolate(influences, x, z);
        int anchor = land ? seaLevel : seaLevel - 2;
        int target = (int) Math.round(originalTop * (1 - amount) + anchor * amount);
        return land ? Math.min(originalTop, Math.max(seaLevel - 1, target))
                : Math.max(originalTop, Math.min(seaLevel - 2, target));
    }

    public static int sampleMargin(int width) { return ((width * 2 + 12 + STEP - 1) / STEP) * STEP; }

    private static double smooth(double value) {
        double t = Math.clamp(value, 0, 1);
        return t * t * (3 - 2 * t);
    }
}
