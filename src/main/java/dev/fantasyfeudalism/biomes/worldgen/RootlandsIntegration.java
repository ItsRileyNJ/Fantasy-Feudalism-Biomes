package dev.fantasyfeudalism.biomes.worldgen;

import dev.fantasyfeudalism.biomes.FantasyBiomes;
import dev.fantasyfeudalism.biomes.RootlandsConfig;
import dev.worldgen.lithostitched.api.event.AddBiomeInjectorsEvent;
import dev.worldgen.lithostitched.api.worldgen.biomeinjector.BiomeInjector;
import dev.worldgen.lithostitched.api.worldgen.biomeinjector.ParameterBuilder;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

/** Broad, smoothly bounded woodland regions. This mask selects biomes; it never changes terrain density. */
public final class RootlandsIntegration {
    public static final ResourceKey<NormalNoise.NoiseParameters> AREA_NOISE = ResourceKey.create(Registries.NOISE, FantasyBiomes.id("rootlands_area"));
    public static final List<ResourceKey<Biome>> TARGETS = List.of(Biomes.DARK_FOREST, Biomes.FOREST, Biomes.BIRCH_FOREST, Biomes.OLD_GROWTH_BIRCH_FOREST);

    public static void register() {
        AddBiomeInjectorsEvent.EVENT.register((registries, consumer) -> {
            if (!RootlandsConfig.ENABLED.get() || RootlandsConfig.BIOME_WEIGHT.get() <= 0) return;
            var biomes = registries.registryOrThrow(Registries.BIOME);
            var noise = registries.registryOrThrow(Registries.NOISE).getHolderOrThrow(AREA_NOISE);
            Holder<DensityFunction> area = Holder.direct(DensityFunctions.noise(noise, 1024.0 / RootlandsConfig.BIOME_SIZE.get(), 0));
            // A continuous noise contour avoids the polygonal edges of a cellular region mask.
            double coverage = Math.clamp(0.2 * Math.log(RootlandsConfig.BIOME_WEIGHT.get() / 100.0), -0.95, 0.95);
            consumer.accept(FantasyBiomes.id("rootlands"), BiomeInjector.builder(Level.OVERWORLD)
                    .replacePartially(HolderSet.direct(TARGETS.stream().map(biomes::getHolderOrThrow).toList()),
                            biomes.getHolderOrThrow(FantasyBiomes.ROOTLANDS), ParameterBuilder.create().densityFunctionMax(area, coverage)));
        });
    }

    private RootlandsIntegration() {}
}
