package dev.fantasyfeudalism.biomes.entity;

import dev.fantasyfeudalism.biomes.FantasyBiomes;
import dev.fantasyfeudalism.biomes.RootlandsConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.pathfinder.PathType;

/** A neutral, root-seeking creature. Feeding is purely cosmetic and never changes a block. */
public final class RootWorm extends PathfinderMob {
    public static final TagKey<Block> ROOT_FOOD = TagKey.create(Registries.BLOCK, FantasyBiomes.id("root_worm_food"));
    public static final TagKey<Block> HEAT_SOURCES = TagKey.create(Registries.BLOCK, FantasyBiomes.id("root_worm_heat_sources"));
    private static final EntityDataAccessor<Boolean> FEEDING = SynchedEntityData.defineId(RootWorm.class, EntityDataSerializers.BOOLEAN);

    public RootWorm(EntityType<? extends RootWorm> type, Level level) {
        super(type, level);
        this.xpReward = 2;
        setPathfindingMalus(PathType.DAMAGE_OTHER, -1.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createMobAttributes().add(Attributes.MAX_HEALTH, 16.0).add(Attributes.MOVEMENT_SPEED, 0.20)
                .add(Attributes.ATTACK_DAMAGE, 2.0).add(Attributes.FOLLOW_RANGE, 16.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new PanicGoal(this, 1.8) {
            @Override protected boolean shouldPanic() { return RootlandsConfig.WORM_FEAR_HEAT.get() && RootWorm.this.isOnFire(); }
        });
        goalSelector.addGoal(2, new AvoidHeatGoal(this));
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.15, true));
        goalSelector.addGoal(4, new SeekRootsGoal(this));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        // No nearest-player targeting goal: retaliation is the only source of an attack target.
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
    }

    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(FEEDING, false);
    }

    @Override public void aiStep() {
        super.aiStep();
        if (!level().isClientSide) {
            boolean feeding = getTarget() == null && !isOnFire() && getNavigation().isDone()
                    && tickCount % 100 < 30 && rootInterest(level(), blockPosition()) > 0;
            entityData.set(FEEDING, feeding);
        }
    }

    public boolean isFeeding() { return entityData.get(FEEDING); }

    @Override public boolean hurt(DamageSource source, float damage) {
        if (RootlandsConfig.WORM_FIRE_WEAKNESS.get()) {
            if (source.is(DamageTypes.LAVA)) damage *= RootlandsConfig.WORM_LAVA_MULTIPLIER.get().floatValue();
            else if (source.is(DamageTypeTags.IS_FIRE)) damage *= RootlandsConfig.WORM_FIRE_MULTIPLIER.get().floatValue();
        }
        return super.hurt(source, damage);
    }

    @Override public float getWalkTargetValue(BlockPos pos, LevelReader level) {
        if (RootlandsConfig.WORM_FEAR_HEAT.get() && isHeat(level, pos)) return -10.0F;
        return Math.min(10, rootInterest(level, pos));
    }

    /** Local tag-based density gives rooted soil and nearby wooden roots a positive navigation bias. */
    public static int rootInterest(LevelReader level, BlockPos pos) {
        int score = 0;
        for (BlockPos sample : BlockPos.betweenClosed(pos.offset(-2, -1, -2), pos.offset(2, 0, 2))) {
            if (level.getBlockState(sample).is(ROOT_FOOD)) score++;
        }
        return score;
    }

    public static boolean isHeat(LevelReader level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.hasProperty(BlockStateProperties.LIT) && !state.getValue(BlockStateProperties.LIT)) return false;
        if (state.is(BlockTags.FIRE) || state.getFluidState().is(FluidTags.LAVA) || state.is(HEAT_SOURCES)) return true;
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return RootlandsConfig.WORM_HEAT_SOURCES.get().contains(id.toString());
    }

    public static boolean canSpawn(EntityType<RootWorm> type, ServerLevelAccessor level, MobSpawnType reason, BlockPos pos, RandomSource random) {
        if (reason == MobSpawnType.SPAWN_EGG || reason == MobSpawnType.COMMAND) return true;
        if (!level.getBiome(pos).is(FantasyBiomes.ROOTLANDS) || !level.getFluidState(pos).isEmpty()) return false;
        BlockState soil = level.getBlockState(pos.below());
        if (!soil.is(BlockTags.DIRT) && !soil.is(ROOT_FOOD)) return false;
        // Root-rich spots always pass; ordinary Rootlands soil now passes half of the time instead of one fifth.
        return rootInterest(level, pos) >= 3 || random.nextBoolean();
    }
}
