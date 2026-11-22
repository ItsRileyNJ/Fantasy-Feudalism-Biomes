package dev.fantasyfeudalism.biomes.mixin;

import dev.worldgen.lithostitched.api.worldgen.util.DensityFunctionWrapper;
import net.minecraft.world.level.levelgen.RandomState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Lithostitched binds injector densities with this world's RandomState, including Large Biomes climate. */
@Mixin(value = DensityFunctionWrapper.class, remap = false)
public interface DensityFunctionWrapperAccessor {
    @Accessor("randomState")
    RandomState fantasyBiomes$getRandomState();
}
