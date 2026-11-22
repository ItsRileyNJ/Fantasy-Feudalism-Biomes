package dev.fantasyfeudalism.biomes.mixin;

import dev.fantasyfeudalism.biomes.worldgen.RootlandsShoreFeature;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Finish shore grading after caves, while CARVERS is still a prerequisite for adjacent FEATURES. */
@Mixin(value = NoiseBasedChunkGenerator.class, remap = false)
public abstract class RootlandsShoreMixin {
    @Inject(method = "applyCarvers", at = @At("TAIL"), require = 1, remap = false)
    private void fantasyBiomes$gradeAfterCarvers(WorldGenRegion region, long seed, RandomState random, BiomeManager biomes,
            StructureManager structures, ChunkAccess chunk, GenerationStep.Carving carving, CallbackInfo callback) {
        RootlandsShoreFeature.gradeAfterCarvers(region, (NoiseBasedChunkGenerator) (Object) this, random, chunk);
    }
}
