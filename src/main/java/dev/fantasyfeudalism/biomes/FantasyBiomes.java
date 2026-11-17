package dev.fantasyfeudalism.biomes;

import dev.fantasyfeudalism.biomes.block.ThornBlock;
import dev.fantasyfeudalism.biomes.block.RootFlowerBlock;
import dev.fantasyfeudalism.biomes.block.RootlandsGrassBlock;
import dev.fantasyfeudalism.biomes.entity.RootWorm;
import dev.fantasyfeudalism.biomes.worldgen.RootlandsIntegration;
import dev.fantasyfeudalism.biomes.worldgen.RootlandsScatterFeature;
import dev.fantasyfeudalism.biomes.worldgen.RootlandsShoreFeature;
import dev.fantasyfeudalism.biomes.worldgen.RootlandsTreeFeature;
import dev.fantasyfeudalism.biomes.worldgen.DarkForestInterior;
import com.mojang.serialization.MapCodec;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.DensityFunction;
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
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);
    private static final DeferredRegister<MapCodec<? extends DensityFunction>> DENSITIES = DeferredRegister.create(Registries.DENSITY_FUNCTION_TYPE, MOD_ID);
    public static final DeferredHolder<MapCodec<? extends DensityFunction>, MapCodec<DarkForestInterior>> FOREST_INTERIOR = DENSITIES.register("dark_forest_interior", () -> DarkForestInterior.CODEC);
    public static final ResourceKey<ConfiguredFeature<?, ?>> SAPLING_TREE = ResourceKey.create(Registries.CONFIGURED_FEATURE, id("sapling_tree"));
    public static final TreeGrower ROOTLANDS_GROWER = new TreeGrower(MOD_ID + ":rootlands", Optional.empty(), Optional.of(SAPLING_TREE), Optional.empty());
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
    public static final DeferredHolder<Block, SaplingBlock> ROOTLANDS_SAPLING = BLOCKS.register("rootlands_sapling", () ->
            new SaplingBlock(ROOTLANDS_GROWER, BlockBehaviour.Properties.ofFullCopy(Blocks.DARK_OAK_SAPLING)));
    public static final DeferredHolder<Item, BlockItem> SAPLING_ITEM = ITEMS.register("rootlands_sapling", () -> new BlockItem(ROOTLANDS_SAPLING.get(), new Item.Properties()));
    public static final DeferredHolder<Block, LeavesBlock> ROOTLANDS_LEAVES = BLOCKS.register("rootlands_leaves", () ->
            new LeavesBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.DARK_OAK_LEAVES)));
    public static final DeferredHolder<Item, BlockItem> LEAVES_ITEM = ITEMS.register("rootlands_leaves", () -> new BlockItem(ROOTLANDS_LEAVES.get(), new Item.Properties()));
    public static final DeferredHolder<Block, RootlandsGrassBlock> DARK_GRASS = BLOCKS.register("dark_grass", () ->
            new RootlandsGrassBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.SHORT_GRASS)));
    public static final DeferredHolder<Item, BlockItem> DARK_GRASS_ITEM = ITEMS.register("dark_grass", () -> new BlockItem(DARK_GRASS.get(), new Item.Properties()));
    public static final DeferredHolder<EntityType<?>, EntityType<RootWorm>> ROOT_WORM = ENTITIES.register("root_worm", () ->
            EntityType.Builder.of(RootWorm::new, MobCategory.CREATURE).sized(0.7F, 0.45F).clientTrackingRange(8)
                    .build(id("root_worm").toString()));
    public static final DeferredHolder<Item, DeferredSpawnEggItem> ROOT_WORM_EGG = ITEMS.register("root_worm_spawn_egg", () ->
            new DeferredSpawnEggItem(ROOT_WORM, 0x59412D, 0xA58A5C, new Item.Properties()));
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> CREATIVE_TAB = TABS.register("main", () ->
            CreativeModeTab.builder().title(Component.translatable("itemGroup." + MOD_ID))
                    .icon(() -> ROOT_FLOWER_ITEM.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(ROOT_WORM_EGG.get());
                        output.accept(ROOT_FLOWER_ITEM.get());
                        output.accept(THORN_ITEM.get());
                        output.accept(SAPLING_ITEM.get());
                        output.accept(LEAVES_ITEM.get());
                        output.accept(DARK_GRASS_ITEM.get());
                    }).build());

    public static final DeferredHolder<Feature<?>, RootlandsScatterFeature> JAGGED_TREES = FEATURES.register("jagged_trees", () ->
            new RootlandsScatterFeature(NoneFeatureConfiguration.CODEC, RootlandsScatterFeature.Kind.TREE));
    public static final DeferredHolder<Feature<?>, RootlandsScatterFeature> EXPOSED_ROOTS = FEATURES.register("exposed_roots", () ->
            new RootlandsScatterFeature(NoneFeatureConfiguration.CODEC, RootlandsScatterFeature.Kind.ROOT));
    public static final DeferredHolder<Feature<?>, RootlandsScatterFeature> THORN_PATCHES = FEATURES.register("thorn_patches", () ->
            new RootlandsScatterFeature(NoneFeatureConfiguration.CODEC, RootlandsScatterFeature.Kind.THORN));
    public static final DeferredHolder<Feature<?>, RootlandsScatterFeature> ROOT_FLOWERS = FEATURES.register("root_flowers", () ->
            new RootlandsScatterFeature(NoneFeatureConfiguration.CODEC, RootlandsScatterFeature.Kind.FLOWER));
    public static final DeferredHolder<Feature<?>, RootlandsShoreFeature> SHORELINE = FEATURES.register("shoreline", () ->
            new RootlandsShoreFeature(NoneFeatureConfiguration.CODEC));
    public static final DeferredHolder<Feature<?>, RootlandsTreeFeature> SAPLING_GROWTH = FEATURES.register("sapling_tree", () -> new RootlandsTreeFeature(NoneFeatureConfiguration.CODEC));
    public static final DeferredHolder<Feature<?>, RootlandsScatterFeature> GIANT_THORNS = FEATURES.register("giant_thorns", () -> new RootlandsScatterFeature(NoneFeatureConfiguration.CODEC, RootlandsScatterFeature.Kind.GIANT_THORN));
    public static final DeferredHolder<Feature<?>, RootlandsScatterFeature> DARK_GRASS_PATCHES = FEATURES.register("dark_grass", () -> new RootlandsScatterFeature(NoneFeatureConfiguration.CODEC, RootlandsScatterFeature.Kind.GRASS));

    public FantasyBiomes(IEventBus bus, ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, RootlandsConfig.SPEC);
        BLOCKS.register(bus);
        ITEMS.register(bus);
        FEATURES.register(bus);
        ENTITIES.register(bus);
        TABS.register(bus);
        DENSITIES.register(bus);
        bus.addListener(FantasyBiomes::creativeContents);
        bus.addListener(FantasyBiomes::entityAttributes);
        bus.addListener(FantasyBiomes::spawnPlacements);
        RootlandsIntegration.register();
    }

    private static void creativeContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(CreativeModeTabs.NATURAL_BLOCKS)) {
            event.accept(THORN_ITEM.get());
            event.accept(ROOT_FLOWER_ITEM.get());
            event.accept(SAPLING_ITEM.get());
            event.accept(LEAVES_ITEM.get());
            event.accept(DARK_GRASS_ITEM.get());
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
