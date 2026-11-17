package dev.fantasyfeudalism.biomes.worldgen;

import dev.fantasyfeudalism.biomes.FantasyBiomes;
import dev.fantasyfeudalism.biomes.RootlandsConfig;
import dev.worldgen.lithostitched.api.event.AddBiomeInjectorsEvent;
import dev.worldgen.lithostitched.api.event.AddRegionsEvent;
import dev.worldgen.lithostitched.api.registry.LithostitchedRegistries;
import dev.worldgen.lithostitched.api.worldgen.biomeinjector.BiomeInjector;
import dev.worldgen.lithostitched.api.worldgen.biomeinjector.ParameterBuilder;
import dev.worldgen.lithostitched.impl.worldgen.biomeinjector.region.Region;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biomes;

public final class RootlandsIntegration {
    private static final ResourceKey<Region> REGION = ResourceKey.create(LithostitchedRegistries.REGION, FantasyBiomes.id("rootlands"));

    public static void register() {
        AddRegionsEvent.EVENT.register((registries, consumer) -> {
            if (!enabled()) return;
            var biomes = registries.registryOrThrow(Registries.BIOME);
            consumer.accept(REGION, Level.OVERWORLD, HolderSet.direct(biomes.getHolderOrThrow(Biomes.DARK_FOREST)),
                    RootlandsConfig.BIOME_WEIGHT.get());
        });
        AddBiomeInjectorsEvent.EVENT.register((registries, consumer) -> {
            if (!enabled()) return;
            var biomes = registries.registryOrThrow(Registries.BIOME);
            consumer.accept(FantasyBiomes.id("rootlands"), BiomeInjector.builder(Level.OVERWORLD)
                    .replacePartially(biomes.getHolderOrThrow(Biomes.DARK_FOREST), biomes.getHolderOrThrow(FantasyBiomes.ROOTLANDS),
                            ParameterBuilder.create().region(REGION)));
        });
    }

    private static boolean enabled() { return RootlandsConfig.ENABLED.get() && RootlandsConfig.BIOME_WEIGHT.get() > 0; }
    private RootlandsIntegration() {}
}
