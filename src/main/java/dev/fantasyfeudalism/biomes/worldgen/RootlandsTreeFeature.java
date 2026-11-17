package dev.fantasyfeudalism.biomes.worldgen;

import com.mojang.serialization.Codec;
import dev.fantasyfeudalism.biomes.RootlandsConfig;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** One tree at the sapling position; unlike biome scatter, this also works outside Rootlands. */
public final class RootlandsTreeFeature extends Feature<NoneFeatureConfiguration> {
    public RootlandsTreeFeature(Codec<NoneFeatureConfiguration> codec) { super(codec); }
    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        boolean placed = JaggedTree.place(context.level(), context.origin(), context.random(), RootlandsConfig.VARIANTS.get());
        if (placed) RootlandsScatterFeature.roots(context.level(), context.origin(), context.random(), RootlandsConfig.VARIANTS.get());
        return placed;
    }
}
