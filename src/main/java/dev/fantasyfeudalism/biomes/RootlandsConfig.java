package dev.fantasyfeudalism.biomes;

import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.ModConfigSpec;

/** Common configuration: shared by world generation and dedicated servers. Restart after editing. */
public final class RootlandsConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED;
    public static final int DEFAULT_BIOME_WEIGHT = 600, DEFAULT_BIOME_SIZE = 4096;
    public static final ModConfigSpec.IntValue GENERATION_DEFAULTS_REVISION;
    public static final ModConfigSpec.IntValue BIOME_WEIGHT, BIOME_SIZE, EDGE_BLEND, TREE_DENSITY, ROOT_DENSITY, THORN_DENSITY, GIANT_THORN_DENSITY, GRASS_DENSITY;
    public static final ModConfigSpec.BooleanValue GENTLE_SHORES;
    public static final ModConfigSpec.IntValue SHORE_WIDTH;
    public static final ModConfigSpec.DoubleValue STRUCTURE_FREQUENCY, THORN_DAMAGE;
    public static final ModConfigSpec.BooleanValue VARIANTS, THORNS_HURT, THORNS_SLOW;
    public static final ModConfigSpec.DoubleValue FLOWER_FREQUENCY, FLOWER_DAMAGE, FLOWER_SLOW;
    public static final ModConfigSpec.IntValue FLOWER_MIN_SIZE, FLOWER_MAX_SIZE;
    public static final ModConfigSpec.BooleanValue FLOWER_PLAYERS, FLOWER_PASSIVE, FLOWER_HOSTILE;
    public static final ModConfigSpec.BooleanValue WORM_FIRE_WEAKNESS, WORM_FEAR_HEAT;
    public static final ModConfigSpec.DoubleValue WORM_FIRE_MULTIPLIER, WORM_LAVA_MULTIPLIER;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> WORM_HEAT_SOURCES;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("rootlands");
        GENERATION_DEFAULTS_REVISION = builder.comment("Internal migration marker for untouched legacy generation defaults.")
                .defineInRange("generationDefaultsRevision", 0, 0, 1);
        ENABLED = builder.comment("Select Rootlands during new world generation.").define("enabled", true);
        BIOME_WEIGHT = builder.comment("Relative coverage of eligible temperate forests. Higher values cover more woodland; 0 disables placement. Not a world percentage.")
                .defineInRange("biomeWeight", DEFAULT_BIOME_WEIGHT, 0, 1000);
        BIOME_SIZE = builder.comment("Horizontal scale of broad Rootlands areas in blocks; climate and rivers can limit actual biome dimensions.")
                .defineInRange("biomeSize", DEFAULT_BIOME_SIZE, 256, 8192);
        EDGE_BLEND = builder.comment("Distance in blocks over which custom decoration tapers near other biomes; 0 disables tapering.")
                .defineInRange("edgeBlendWidth", 24, 0, 96);
        GENTLE_SHORES = builder.comment("Create dry sandy banks and shallow seabeds near Rootlands after caves, before vegetation. Preserve vanilla water height and existing land/water coverage.")
                .define("gentleShores", true);
        SHORE_WIDTH = builder.comment("Shoreline transition width in blocks. Also blends into neighboring biomes to avoid a seam at the biome border.")
                .defineInRange("shoreWidth", 64, 16, 128);
        TREE_DENSITY = builder.comment("Jagged tree attempts per Rootlands chunk, before frequency scaling.")
                .defineInRange("treeDensity", 5, 0, 24);
        ROOT_DENSITY = builder.comment("Exposed root cluster attempts per chunk.")
                .defineInRange("rootDensity", 8, 0, 64);
        THORN_DENSITY = builder.comment("Thorn-heavy dark oak wood root patch attempts per chunk.")
                .defineInRange("thornDensity", 10, 0, 64);
        GIANT_THORN_DENSITY = builder.comment("Large twisting above-ground wood root arch attempts per chunk, decorated with thorns.").defineInRange("giantThornDensity", 2, 0, 12);
        GRASS_DENSITY = builder.comment("Dark grass plant patch attempts per chunk; this is not a grass-block variant.").defineInRange("darkGrassDensity", 14, 0, 64);
        STRUCTURE_FREQUENCY = builder.comment("Scales all procedural tree, root and thorn attempts; 0 disables them. Dirt surface patches remain.")
                .defineInRange("structureFrequency", 1.0, 0.0, 3.0);
        VARIANTS = builder.comment("Vary tree heights, lean directions, roots and thorn wall layouts. False uses a consistent base layout.")
                .define("structureVariants", true);
        THORNS_HURT = builder.comment("Thorns damage living entities on contact (vanilla damage cooldown applies).")
                .define("thornDamageEnabled", true);
        THORN_DAMAGE = builder.comment("Damage per accepted hit, in health points (2 health points = 1 heart).")
                .defineInRange("thornDamage", 1.0, 0.0, 8.0);
        THORNS_SLOW = builder.comment("Thorns slow entities touching them, independently of damage.")
                .define("thornSlowEnabled", true);
        builder.pop();
        builder.push("rootFlower");
        FLOWER_FREQUENCY = builder.comment("Patch attempts per Rootlands chunk, independent of structureFrequency; 0 disables patches.")
                .defineInRange("patchFrequency", 2.0, 0.0, 16.0);
        FLOWER_MIN_SIZE = builder.comment("Minimum plants attempted per patch. Reversed minimum/maximum values are normalized.")
                .defineInRange("minimumPatchSize", 3, 1, 32);
        FLOWER_MAX_SIZE = builder.defineInRange("maximumPatchSize", 7, 1, 32);
        FLOWER_DAMAGE = builder.comment("Contact damage in health points; 0 disables damage.").defineInRange("damage", 1.0, 0.0, 8.0);
        FLOWER_SLOW = builder.comment("Horizontal movement reduction, from 0 (none) to 0.95 (95 percent).")
                .defineInRange("slowAmount", 0.2, 0.0, 0.95);
        FLOWER_PLAYERS = builder.define("affectPlayers", true);
        FLOWER_PASSIVE = builder.comment("Includes neutral creatures such as Root Worms.").define("affectPassiveMobs", true);
        FLOWER_HOSTILE = builder.define("affectHostileMobs", true);
        builder.pop();
        builder.push("rootWorm");
        WORM_FIRE_WEAKNESS = builder.define("fireWeaknessEnabled", true);
        WORM_FIRE_MULTIPLIER = builder.defineInRange("fireDamageMultiplier", 2.0, 1.0, 8.0);
        WORM_LAVA_MULTIPLIER = builder.comment("Lava uses this multiplier instead of stacking both multipliers.")
                .defineInRange("lavaDamageMultiplier", 2.0, 1.0, 8.0);
        WORM_FEAR_HEAT = builder.comment("Flee nearby heat, avoid it while pathfinding, and panic when burning.").define("avoidHeat", true);
        WORM_HEAT_SOURCES = builder.comment("Additional block IDs to fear. Fire, lava and the root_worm_heat_sources block tag are also checked.")
                .defineListAllowEmpty("additionalHeatSources", List.of("minecraft:torch", "minecraft:soul_torch"),
                        () -> "minecraft:torch", value -> value instanceof String id && ResourceLocation.tryParse(id) != null);
        builder.pop();
        SPEC = builder.build();
    }

    /** Upgrade only the exact 0.5.x default pair, once. Player-tuned sizes/weights are retained. */
    public static boolean migrateLegacyGenerationDefaults() {
        if (GENERATION_DEFAULTS_REVISION.get() != 0) return false;
        if (BIOME_WEIGHT.get() == 35 && BIOME_SIZE.get() == 2048) {
            BIOME_WEIGHT.set(DEFAULT_BIOME_WEIGHT);
            BIOME_SIZE.set(DEFAULT_BIOME_SIZE);
        }
        GENERATION_DEFAULTS_REVISION.set(1);
        return true;
    }

    private RootlandsConfig() {}
}
