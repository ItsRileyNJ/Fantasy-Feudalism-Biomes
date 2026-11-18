package dev.fantasyfeudalism.biomes.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkPyramid;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.material.FluidState;

/** A real noise-filled chunk exposed through the production WorldGenRegion surface entry point. */
final class NoiseSurfaceTestRegion extends WorldGenRegion {
    private final ChunkAccess chunk;
    private final NoiseBasedChunkGenerator generator;
    private final RandomState random;
    private final BiomeManager manager;

    NoiseSurfaceTestRegion(ServerLevel owner, ChunkAccess chunk, NoiseBasedChunkGenerator generator, RandomState random, long seed) {
        super(owner, null, ChunkPyramid.GENERATION_PYRAMID.getStepTo(ChunkStatus.SURFACE), chunk);
        this.chunk = chunk;
        this.generator = generator;
        this.random = random;
        this.manager = new BiomeManager(this, BiomeManager.obfuscateSeed(seed));
    }

    @Override public ChunkAccess getChunk(int x, int z, ChunkStatus status, boolean required) {
        if (chunk.getPos().x != x || chunk.getPos().z != z) throw new IllegalStateException("Surface test requested another chunk");
        return chunk;
    }
    @Override public BlockState getBlockState(BlockPos pos) { return chunk.getBlockState(pos); }
    @Override public FluidState getFluidState(BlockPos pos) { return getBlockState(pos).getFluidState(); }
    @Override public boolean ensureCanWrite(BlockPos pos) {
        return new ChunkPos(pos).equals(chunk.getPos()) && pos.getY() >= getMinBuildHeight() && pos.getY() < getMaxBuildHeight();
    }
    @Override public boolean setBlock(BlockPos pos, BlockState state, int flags, int recursion) {
        if (!ensureCanWrite(pos)) throw new IllegalStateException("Surface write escaped its chunk");
        chunk.setBlockState(pos, state, false);
        return true;
    }
    @Override public int getHeight(Heightmap.Types type, int x, int z) {
        return new ChunkPos(x >> 4, z >> 4).equals(chunk.getPos()) ? chunk.getHeight(type, x & 15, z & 15) + 1
                : generator.getBaseHeight(x, z, type, this, random);
    }
    @Override public Holder<Biome> getUncachedNoiseBiome(int x, int y, int z) { return generator.getBiomeSource().getNoiseBiome(x, y, z, random.sampler()); }
    @Override public Holder<Biome> getNoiseBiome(int x, int y, int z) { return getUncachedNoiseBiome(x, y, z); }
    @Override public BiomeManager getBiomeManager() { return manager; }
    @Override public boolean isOldChunkAround(ChunkPos pos, int radius) { return false; }
}
