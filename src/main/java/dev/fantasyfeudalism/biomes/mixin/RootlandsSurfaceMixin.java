package dev.fantasyfeudalism.biomes.mixin;

import dev.fantasyfeudalism.biomes.worldgen.RootlandsShoreFeature;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** FEATURES are not globally ordered: a neighbor can already have placed trees into this chunk.
 * SURFACE is a prerequisite for CARVERS, which in turn is required before any adjacent FEATURES.
 */
@Mixin(value = NoiseBasedChunkGenerator.class, remap = false)
public abstract class RootlandsSurfaceMixin {
    @Inject(method = "buildSurface(Lnet/minecraft/server/level/WorldGenRegion;Lnet/minecraft/world/level/StructureManager;Lnet/minecraft/world/level/levelgen/RandomState;Lnet/minecraft/world/level/chunk/ChunkAccess;)V",
            at = @At("HEAD"), require = 1, remap = false)
    private void fantasyBiomes$gradeBeforeSurface(WorldGenRegion region, StructureManager structures, RandomState random, ChunkAccess chunk, CallbackInfo callback) {
        RootlandsShoreFeature.gradeSurface(region, (NoiseBasedChunkGenerator) (Object) this, random, chunk);
    }
}
