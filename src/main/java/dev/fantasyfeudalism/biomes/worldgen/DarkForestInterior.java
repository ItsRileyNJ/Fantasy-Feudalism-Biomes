package dev.fantasyfeudalism.biomes.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.Holder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterList;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseRouter;

/** Keep a vanilla dark-oak woodland rim around Rootlands, instead of consuming a whole parent forest. */
public final class DarkForestInterior implements DensityFunction.SimpleFunction {
    public static final MapCodec<DarkForestInterior> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            MultiNoiseBiomeSourceParameterList.CODEC.fieldOf("preset").forGetter(value -> value.preset),
            NoiseGeneratorSettings.CODEC.fieldOf("settings").forGetter(value -> value.settings),
            Codec.intRange(16, 64).fieldOf("buffer").forGetter(value -> value.buffer)
    ).apply(instance, DarkForestInterior::new));
    private final Holder<MultiNoiseBiomeSourceParameterList> preset;
    private final Holder<NoiseGeneratorSettings> settings;
    private final int buffer;
    private final NoiseRouter router;
    private final MultiNoiseBiomeSource source;
    private final Climate.Sampler sampler;
    private final ConcurrentHashMap<Long, Boolean> cache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Boolean> vanillaForest = new ConcurrentHashMap<>();

    public DarkForestInterior(Holder<MultiNoiseBiomeSourceParameterList> preset, Holder<NoiseGeneratorSettings> settings, int buffer) {
        this(preset, settings, buffer, settings.value().noiseRouter());
    }

    private DarkForestInterior(Holder<MultiNoiseBiomeSourceParameterList> preset, Holder<NoiseGeneratorSettings> settings, int buffer, NoiseRouter router) {
        this.preset = preset;
        this.settings = settings;
        this.buffer = buffer;
        this.router = router;
        this.source = MultiNoiseBiomeSource.createFromPreset(preset);
        this.sampler = new Climate.Sampler(router.temperature(), router.vegetation(), router.continents(), router.erosion(), router.depth(), router.ridges(), settings.value().spawnTarget());
    }

    @Override public double compute(FunctionContext context) {
        int qx = context.blockX() >> 2;
        int qz = context.blockZ() >> 2;
        long key = ((long) qx << 32) ^ (qz & 0xffffffffL);
        if (cache.size() > 65536) cache.clear();
        return cache.computeIfAbsent(key, ignored -> {
            int radius = buffer >> 2;
            for (int dx = -radius; dx <= radius; dx++) for (int dz = -radius; dz <= radius; dz++) {
                int x = qx + dx, z = qz + dz;
                long sample = ((long) x << 32) ^ (z & 0xffffffffL);
                if (vanillaForest.size() > 131072) vanillaForest.clear();
                if (!vanillaForest.computeIfAbsent(sample, unused -> source.getNoiseBiome(x, 20, z, sampler).is(Biomes.DARK_FOREST))) return false;
            }
            return true;
        }) ? 1.0 : 0.0;
    }

    @Override public DensityFunction mapAll(Visitor visitor) { return visitor.apply(new DarkForestInterior(preset, settings, buffer, router.mapAll(visitor))); }
    @Override public double minValue() { return 0; }
    @Override public double maxValue() { return 1; }
    @Override public KeyDispatchDataCodec<DarkForestInterior> codec() { return KeyDispatchDataCodec.of(CODEC); }
}
