package dev.fantasyfeudalism.biomes.worldgen;

import dev.fantasyfeudalism.biomes.FantasyBiomes;
import dev.fantasyfeudalism.biomes.RootlandsConfig;
import dev.worldgen.lithostitched.api.event.AddBiomeInjectorsEvent;
import dev.worldgen.lithostitched.api.worldgen.biomeinjector.BiomeInjector;
import dev.worldgen.lithostitched.api.worldgen.biomeinjector.ParameterBuilder;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
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
    public static final List<ResourceKey<Biome>> TARGETS = List.of(Biomes.DARK_FOREST);

    public static void register() {
        AddBiomeInjectorsEvent.EVENT.register((registries, consumer) -> {
            if (!RootlandsConfig.ENABLED.get() || RootlandsConfig.BIOME_WEIGHT.get() <= 0) return;
            var biomes = registries.registryOrThrow(Registries.BIOME);
            var preset = registries.registryOrThrow(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST)
                    .getHolderOrThrow(ResourceKey.create(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST, ResourceLocation.withDefaultNamespace("overworld")));
            // Codec fallback only: DarkForestInterior binds the active world's sampler during injection.
            var settings = registries.registryOrThrow(Registries.NOISE_SETTINGS).getHolderOrThrow(NoiseGeneratorSettings.OVERWORLD);
            consumer.accept(FantasyBiomes.id("rootlands"), BiomeInjector.builder(Level.OVERWORLD)
                    .replacePartially(HolderSet.direct(TARGETS.stream().map(biomes::getHolderOrThrow).toList()),
                            biomes.getHolderOrThrow(FantasyBiomes.ROOTLANDS), areaParameters(registries).densityFunctionMin(Holder.direct(new DarkForestInterior(preset, settings, 32)), 1.0)));
        });
    }

    /** Spatial area selector kept separate from the parent-forest adjacency rule. */
    public static ParameterBuilder areaParameters(RegistryAccess registries) {
        var noise = registries.registryOrThrow(Registries.NOISE).getHolderOrThrow(AREA_NOISE);
        Holder<DensityFunction> area = Holder.direct(DensityFunctions.noise(noise, 1024.0 / RootlandsConfig.BIOME_SIZE.get(), 0));
        double coverage = Math.clamp(0.2 * Math.log(RootlandsConfig.BIOME_WEIGHT.get() / 100.0), -0.95, 0.95);
        return ParameterBuilder.create().densityFunctionMax(area, coverage);
    }

    private RootlandsIntegration() {}
}
