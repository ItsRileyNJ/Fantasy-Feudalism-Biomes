package dev.fantasyfeudalism.biomes;

import dev.fantasyfeudalism.biomes.block.ThornBlock;
import dev.fantasyfeudalism.biomes.block.RootFlowerBlock;
import dev.fantasyfeudalism.biomes.entity.RootWorm;
import dev.fantasyfeudalism.biomes.worldgen.RootlandsIntegration;
import dev.fantasyfeudalism.biomes.worldgen.RootlandsScatterFeature;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(FantasyBiomes.MOD_ID)
public final class FantasyBiomes {
    public static final String MOD_ID = "fantasy_feudalism_biomes";
    public static final ResourceKey<Biome> ROOTLANDS = ResourceKey.create(Registries.BIOME, id("rootlands"));
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, MOD_ID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, MOD_ID);
    private static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, MOD_ID);
    private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, MOD_ID);
    public static final DeferredHolder<Block, ThornBlock> THORN = BLOCKS.register("rootlands_thorn", () ->
            new ThornBlock(BlockBehaviour.Properties.of().strength(0.8F).sound(SoundType.WOOD)
                    .noCollission().noOcclusion().ignitedByLava()));
    public static final DeferredHolder<Item, BlockItem> THORN_ITEM = ITEMS.register("rootlands_thorn", () ->
            new BlockItem(THORN.get(), new Item.Properties()));
    public static final DeferredHolder<Block, RootFlowerBlock> ROOT_FLOWER = BLOCKS.register("root_flower", () ->
            new RootFlowerBlock(BlockBehaviour.Properties.of().noCollission().noOcclusion().instabreak()
                    .sound(SoundType.GRASS).ignitedByLava()));
    public static final DeferredHolder<Item, BlockItem> ROOT_FLOWER_ITEM = ITEMS.register("root_flower", () ->
            new BlockItem(ROOT_FLOWER.get(), new Item.Properties()));
    public static final DeferredHolder<EntityType<?>, EntityType<RootWorm>> ROOT_WORM = ENTITIES.register("root_worm", () ->
            EntityType.Builder.of(RootWorm::new, MobCategory.CREATURE).sized(0.7F, 0.45F).clientTrackingRange(8)
                    .build(id("root_worm").toString()));
    public static final DeferredHolder<Item, DeferredSpawnEggItem> ROOT_WORM_EGG = ITEMS.register("root_worm_spawn_egg", () ->
            new DeferredSpawnEggItem(ROOT_WORM, 0x59412D, 0xA58A5C, new Item.Properties()));

    public static final DeferredHolder<Feature<?>, RootlandsScatterFeature> JAGGED_TREES = FEATURES.register("jagged_trees", () ->
            new RootlandsScatterFeature(NoneFeatureConfiguration.CODEC, RootlandsScatterFeature.Kind.TREE));
    public static final DeferredHolder<Feature<?>, RootlandsScatterFeature> EXPOSED_ROOTS = FEATURES.register("exposed_roots", () ->
            new RootlandsScatterFeature(NoneFeatureConfiguration.CODEC, RootlandsScatterFeature.Kind.ROOT));
    public static final DeferredHolder<Feature<?>, RootlandsScatterFeature> THORN_PATCHES = FEATURES.register("thorn_patches", () ->
            new RootlandsScatterFeature(NoneFeatureConfiguration.CODEC, RootlandsScatterFeature.Kind.THORN));
    public static final DeferredHolder<Feature<?>, RootlandsScatterFeature> ROOT_FLOWERS = FEATURES.register("root_flowers", () ->
            new RootlandsScatterFeature(NoneFeatureConfiguration.CODEC, RootlandsScatterFeature.Kind.FLOWER));

    public FantasyBiomes(IEventBus bus, ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, RootlandsConfig.SPEC);
        BLOCKS.register(bus);
        ITEMS.register(bus);
        FEATURES.register(bus);
        ENTITIES.register(bus);
        bus.addListener(FantasyBiomes::creativeContents);
        bus.addListener(FantasyBiomes::entityAttributes);
        bus.addListener(FantasyBiomes::spawnPlacements);
        RootlandsIntegration.register();
    }

    private static void creativeContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(CreativeModeTabs.NATURAL_BLOCKS)) {
            event.accept(THORN_ITEM.get());
            event.accept(ROOT_FLOWER_ITEM.get());
        }
        if (event.getTabKey().equals(CreativeModeTabs.SPAWN_EGGS)) event.accept(ROOT_WORM_EGG.get());
    }

    private static void entityAttributes(EntityAttributeCreationEvent event) {
        event.put(ROOT_WORM.get(), RootWorm.createAttributes().build());
    }

    private static void spawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(ROOT_WORM.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                RootWorm::canSpawn, RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    public static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath(MOD_ID, path); }
}
