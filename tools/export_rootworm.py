#!/usr/bin/env python3
"""Export the Root Worm as a biome/block-independent NeoForge integration kit."""
from pathlib import Path
import hashlib
import json
import re
import shutil
import subprocess
import zipfile

REPO = Path(__file__).resolve().parents[1]
OUT = REPO / 'build/exports'
KIT = OUT / 'RootWorm-Integration-Kit'
JAVA = REPO / 'src/main/java/dev/fantasyfeudalism/biomes'
RES = REPO / 'src/main/resources'
PACKAGE = 'dev.fantasyfeudalism.rootworm'
NS = 'rootworm'


def write(path, text):
    path = KIT / path
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text.strip() + '\n')


def resource(path, data):
    write(path, json.dumps(data, indent=2))


def rewrite(text):
    return (text.replace('dev.fantasyfeudalism.biomes', PACKAGE)
            .replace('FantasyBiomes', 'RootWormModule').replace('RootlandsConfig', 'RootWormConfig')
            .replace('fantasyBiomes$', 'rootWorm$'))


def extract_test(text, name):
    start = text.rfind('    @GameTest', 0, text.index('public static void ' + name))
    body = text.index('{', text.index('public static void ' + name))
    depth = 1
    end = body + 1
    while depth:
        depth += (text[end] == '{') - (text[end] == '}')
        end += 1
    return rewrite(text[start:end])


if KIT.exists():
    shutil.rmtree(KIT)
KIT.mkdir(parents=True)

# Keep the actual model/texture/animation/hitbox implementation; remove habitat coupling.
for relative in ['entity/RootWormShape.java', 'entity/AvoidHeatGoal.java',
                 'client/RootWormRenderer.java', 'mixin/RootWormArrowMixin.java']:
    write('src/main/java/' + PACKAGE.replace('.', '/') + '/' + relative, rewrite((JAVA / relative).read_text()))

for relative in ['client/RootWormModel.java', 'entity/RootWormHearing.java', 'entity/InvestigateSoundGoal.java',
                 'entity/RootWormAttackGoal.java', 'entity/RootWormSearchPose.java']:
    write('src/main/java/' + PACKAGE.replace('.', '/') + '/' + relative, (REPO / 'tools/rootworm_export' / relative).read_text())

worm = rewrite((JAVA / 'entity/RootWorm.java').read_text())
a = worm.index('    public static final TagKey<Block> ROOT_FOOD')
b = worm.index('    private static final EntityDataAccessor<Integer>', a)
worm = worm[:a] + worm[b:]
worm = worm.replace('        goalSelector.addGoal(4, new SeekRootsGoal(this));\n', '')
worm = worm.replace('addGoal(5, new WaterAvoidingRandomStrollGoal', 'addGoal(4, new WaterAvoidingRandomStrollGoal')
worm = worm.replace('addGoal(6, new RandomLookAroundGoal', 'addGoal(5, new RandomLookAroundGoal')
worm = worm.replace(' && rootInterest(level(), blockPosition()) > 0', '')
a = worm.index('    @Override public float getWalkTargetValue(')
worm = worm[:a] + '''    /** Vanilla fire and lava only; no custom blocks, block IDs or habitat tags are needed. */
    public static boolean isHeat(LevelReader level, BlockPos pos) {
        return level.getBlockState(pos).is(BlockTags.FIRE) || level.getFluidState(pos).is(FluidTags.LAVA);
    }
}
'''
# Faster, hearing-guided attacks for the portable variant.
worm = worm.replace('BITE_DURATION = 12', 'BITE_DURATION = 9').replace('elapsed / 3.0F', 'elapsed / 2.0F').replace('(BITE_DURATION - elapsed) / 5.0F', '(BITE_DURATION - elapsed) / 4.0F')
a = worm.index('        goalSelector.addGoal(3, new MeleeAttackGoal')
b = worm.index('        goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal', a)
worm = worm[:a] + '        goalSelector.addGoal(3, new RootWormAttackGoal(this));\n        goalSelector.addGoal(4, new InvestigateSoundGoal(this));\n' + worm[b:]
worm = worm.replace('addGoal(4, new WaterAvoidingRandomStrollGoal', 'addGoal(5, new WaterAvoidingRandomStrollGoal').replace('addGoal(5, new RandomLookAroundGoal', 'addGoal(6, new RandomLookAroundGoal')
worm = worm.replace('    private float mouthOpen, previousMouthOpen;', '''    private float mouthOpen, previousMouthOpen;
    private final net.minecraft.world.level.gameevent.DynamicGameEventListener<RootWormHearing> hearing;
    private java.util.UUID heardActor;
    private Vec3 heardPosition;
    private int heardAt = -100000;''')
worm = worm.replace('        this.xpReward = 2;', '        this.xpReward = 2;\n        hearing = new net.minecraft.world.level.gameevent.DynamicGameEventListener<>(new RootWormHearing(this));')
worm = worm.replace('    private static final int BITE_DURATION = 9;', '''    private static final int BITE_DURATION = 9;
    private static final EntityDataAccessor<Boolean> SEARCHING = SynchedEntityData.defineId(RootWorm.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> SEARCH_BLEND_START = SynchedEntityData.defineId(RootWorm.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Long> SEARCH_CHANGED_AT = SynchedEntityData.defineId(RootWorm.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Long> SEARCH_STARTED_AT = SynchedEntityData.defineId(RootWorm.class, EntityDataSerializers.LONG);''')
worm = worm.replace('        builder.define(BITE_TICKS, 0);', '''        builder.define(BITE_TICKS, 0);
        builder.define(SEARCHING, false);
        builder.define(SEARCH_BLEND_START, 0.0F);
        builder.define(SEARCH_CHANGED_AT, 0L);
        builder.define(SEARCH_STARTED_AT, 0L);''')
worm = worm.replace('            int biteTicks = entityData.get(BITE_TICKS);', '''            if (isSearching() && !canSearchHere()) setSearching(false);
            int biteTicks = entityData.get(BITE_TICKS);''')
worm = worm.replace('boolean feeding = getTarget() == null', 'boolean feeding = !isSearching() && getSearchAmount(0) == 0 && getTarget() == null')
a = worm.index('            float forward = RootWormShape.hitForward(i) * scale;')
b = worm.index('            // Direct teleports', a)
worm = worm[:a] + '''            var pose = getSearchPose(i, 0);
            part.refreshDimensions();
            Vec3 position = searchPartPosition(i, pose);
            part.setPos(position.x, position.y, position.z);
''' + worm[b:]
worm = worm.replace('        float yaw = yBodyRot * Mth.DEG_TO_RAD;\n        float scale = getScale();\n', '')
worm = worm.replace('        return super.hurt(source, damage);', '''        boolean accepted = super.hurt(source, damage);
        if (accepted && source.getEntity() instanceof LivingEntity attacker) hearSound(attacker, attacker.position(), 1.0);
        return accepted;''')
pos = worm.index('    /** Vanilla fire and lava only;')
worm = worm[:pos] + '''    @Override public void updateDynamicGameEventListener(java.util.function.BiConsumer<net.minecraft.world.level.gameevent.DynamicGameEventListener<?>, net.minecraft.server.level.ServerLevel> registration) {
        if (level() instanceof net.minecraft.server.level.ServerLevel server) registration.accept(hearing, server);
    }

    public boolean hearSound(LivingEntity actor, Vec3 position, double volume) {
        double radius = RootWormConfig.HEARING_RADIUS.get() * volume;
        if (actor == this || actor instanceof RootWorm || !actor.isAlive() || actor.isSpectator()
                || actor.level() != level() || distanceToSqr(position) > radius * radius) return false;
        heardActor = actor.getUUID();
        heardPosition = position;
        heardAt = tickCount;
        return true; // Hearing never assigns an attack target: the creature remains neutral until hurt.
    }
    public boolean hasRecentSound() { return heardPosition != null && tickCount - heardAt <= RootWormConfig.SOUND_MEMORY_TICKS.get(); }
    public boolean hasRecentSoundFrom(LivingEntity actor) { return hasRecentSound() && actor.getUUID().equals(heardActor); }
    public Vec3 lastHeardPosition() { return heardPosition; }
    public void beginBite() { setSearching(false); entityData.set(BITE_TICKS, BITE_DURATION); }
    public boolean sensesCloseTarget(LivingEntity actor) {
        for (var part : bodyParts) if (part.getBoundingBox().inflate(0.35, 0.35, 0.35).intersects(actor.getBoundingBox())) return true;
        return false;
    }

    public void faceSound(Vec3 position) {
        Vec3 offset = position.subtract(position());
        float desired = (float) (Mth.atan2(offset.z, offset.x) * Mth.RAD_TO_DEG) - 90;
        float yaw = Mth.approachDegrees(getYRot(), desired, 25);
        setYRot(yaw);
        setYHeadRot(yaw);
        setYBodyRot(yaw);
    }

    public boolean canBite(LivingEntity actor) {
        var head = getParts()[0].getBoundingBox();
        var target = actor.getBoundingBox();
        if (!head.inflate(0.22, 0.15, 0.22).intersects(target)) return false;
        Vec3 from = head.getCenter();
        Vec3 to = new Vec3(Mth.clamp(from.x, target.minX + 0.001, target.maxX - 0.001),
                Mth.clamp(from.y, target.minY + 0.1, target.maxY - 0.1),
                Mth.clamp(from.z, target.minZ + 0.001, target.maxZ - 0.001));
        return level().clip(new net.minecraft.world.level.ClipContext(from, to,
                net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, this))
                .getType() == net.minecraft.world.phys.HitResult.Type.MISS;
    }

    public boolean isSearching() { return entityData.get(SEARCHING); }

    @Override public net.minecraft.world.phys.AABB getBoundingBoxForCulling() {
        var bounds = super.getBoundingBoxForCulling();
        if (bodyParts != null) for (var part : bodyParts) bounds = bounds.minmax(part.getBoundingBox());
        return bounds;
    }

    /** Transition-only synced data and world time keep late-tracking clients on the same pose as the server. */
    private float searchLinearAmount(float partialTick) {
        float elapsed = Math.max(0, level().getGameTime() - entityData.get(SEARCH_CHANGED_AT) + partialTick);
        return Mth.clamp(entityData.get(SEARCH_BLEND_START) + (isSearching() ? elapsed : -elapsed) / 10.0F, 0, 1);
    }
    public float getSearchAmount(float partialTick) {
        float amount = searchLinearAmount(partialTick);
        return amount * amount * (3 - 2 * amount);
    }
    public RootWormSearchPose.Pose getSearchPose(int segment, float partialTick) {
        float age = level().getGameTime() - entityData.get(SEARCH_STARTED_AT) + partialTick;
        return RootWormSearchPose.sample(segment, getSearchAmount(partialTick), age);
    }
    public void setSearching(boolean searching) {
        if (level().isClientSide || searching == isSearching() || searching && !canSearchHere()) return;
        float amount = searchLinearAmount(0);
        entityData.set(SEARCH_BLEND_START, amount);
        entityData.set(SEARCH_CHANGED_AT, level().getGameTime());
        if (searching) entityData.set(SEARCH_STARTED_AT, level().getGameTime());
        entityData.set(SEARCHING, searching);
    }
    private Vec3 searchPartPosition(int segment, RootWormSearchPose.Pose pose) {
        float yaw = yBodyRot * Mth.DEG_TO_RAD, scale = getScale();
        float nose = segment == 0 ? 1.5F : 0;
        float side = (pose.x() - Mth.sin(pose.yaw()) * nose) / 16 * scale;
        float forward = (-pose.z() + Mth.cos(pose.yaw()) * nose) / 16 * scale;
        return new Vec3(getX() - Mth.sin(yaw) * forward - Mth.cos(yaw) * side,
                getY() + RootWormSearchPose.bottom(segment, pose) * scale,
                getZ() + Mth.cos(yaw) * forward - Mth.sin(yaw) * side);
    }
    public boolean canSearchHere() {
        if (!onGround() || isNoAi() || !isAlive() || isOnFire() || isInWaterOrBubble() || isInLava()
                || isBiting() || !getNavigation().isDone()) return false;
        // Check the complete lifting envelope, including side-to-side sweep; never raise through a ceiling.
        float scale = getScale();
        for (int i = 0; i < bodyParts.length; i++) {
            net.minecraft.world.phys.AABB envelope = null;
            for (float amount : new float[]{0, 1}) {
                var pose = RootWormSearchPose.sample(i, amount, 0);
                Vec3 position = searchPartPosition(i, pose);
                double width = RootWormSearchPose.width(i, pose) * scale;
                var box = new net.minecraft.world.phys.AABB(position.x - width / 2, position.y, position.z - width / 2,
                        position.x + width / 2, position.y + RootWormSearchPose.height(i, pose) * scale, position.z + width / 2);
                envelope = envelope == null ? box : envelope.minmax(box);
            }
            envelope = new net.minecraft.world.phys.AABB(envelope.minX, Math.max(getY() + 0.03 * scale, envelope.minY), envelope.minZ,
                    envelope.maxX, envelope.maxY, envelope.maxZ).inflate(0.35 * scale, 0, 0.35 * scale);
            if (!level().noCollision(this, envelope)) return false;
        }
        return true;
    }

''' + worm[pos:]
for unused in ['net.minecraft.core.registries.BuiltInRegistries', 'net.minecraft.core.registries.Registries',
               'net.minecraft.resources.ResourceLocation', 'net.minecraft.tags.TagKey',
               'net.minecraft.world.entity.MobSpawnType', 'net.minecraft.world.entity.ai.goal.MeleeAttackGoal', 'net.minecraft.world.level.ServerLevelAccessor',
               'net.minecraft.world.level.block.Block', 'net.minecraft.world.level.block.Blocks',
               'net.minecraft.world.level.block.state.BlockState', 'net.minecraft.world.level.block.state.properties.BlockStateProperties']:
    worm = worm.replace('import ' + unused + ';\n', '')
worm = worm.replace('A neutral, root-seeking creature.', 'A neutral creature with no biome or custom-block requirements.')
write('src/main/java/' + PACKAGE.replace('.', '/') + '/entity/RootWorm.java', worm)
part = rewrite((JAVA / 'entity/RootWormPart.java').read_text())
part = part.replace('    private final EntityDimensions size;', '    private final int segment;')
part = part.replace('        size = EntityDimensions.scalable(RootWormShape.hitWidth(segment), RootWormShape.hitHeight(segment));',
                    '        this.segment = segment;')
part = part.replace('return size.scale(getParent().getScale());', '''var search = getParent().getSearchPose(segment, 0);
        return EntityDimensions.scalable(RootWormSearchPose.width(segment, search), RootWormSearchPose.height(segment, search)).scale(getParent().getScale());''')
write('src/main/java/' + PACKAGE.replace('.', '/') + '/entity/RootWormPart.java', part)

write('src/main/java/' + PACKAGE.replace('.', '/') + '/RootWormConfig.java', '''
package dev.fantasyfeudalism.rootworm;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Independent behavior settings; no biome IDs, habitat tags, or custom block lists. */
public final class RootWormConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue WORM_FIRE_WEAKNESS, WORM_FEAR_HEAT;
    public static final ModConfigSpec.IntValue HEARING_RADIUS, SOUND_MEMORY_TICKS;
    public static final ModConfigSpec.DoubleValue WORM_FIRE_MULTIPLIER, WORM_LAVA_MULTIPLIER;
    static {
        var builder = new ModConfigSpec.Builder();
        builder.push("rootWorm");
        HEARING_RADIUS = builder.defineInRange("hearingRadius", 16, 4, 16);
        SOUND_MEMORY_TICKS = builder.defineInRange("soundMemoryTicks", 80, 20, 400);
        WORM_FIRE_WEAKNESS = builder.define("fireWeaknessEnabled", true);
        WORM_FIRE_MULTIPLIER = builder.defineInRange("fireDamageMultiplier", 2.0, 1.0, 8.0);
        WORM_LAVA_MULTIPLIER = builder.defineInRange("lavaDamageMultiplier", 2.0, 1.0, 8.0);
        WORM_FEAR_HEAT = builder.comment("Avoid vanilla fire/lava and panic while burning.").define("avoidHeat", true);
        builder.pop();
        SPEC = builder.build();
    }
    private RootWormConfig() {}
}
''')
write('src/main/java/' + PACKAGE.replace('.', '/') + '/RootWormModule.java', '''
package dev.fantasyfeudalism.rootworm;

import dev.fantasyfeudalism.rootworm.entity.RootWorm;
import dev.fantasyfeudalism.rootworm.entity.RootWormPart;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Call register once from the destination mod constructor, after retargeting MOD_ID to that mod. */
public final class RootWormModule {
    public static final String MOD_ID = "rootworm";
    private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, MOD_ID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(net.minecraft.core.registries.Registries.ITEM, MOD_ID);
    public static final DeferredHolder<EntityType<?>, EntityType<RootWorm>> ROOT_WORM = ENTITIES.register("root_worm", () ->
            EntityType.Builder.of(RootWorm::new, MobCategory.CREATURE).sized(0.7F, 0.45F).clientTrackingRange(8)
                    .build(id("root_worm").toString()));
    public static final DeferredHolder<Item, DeferredSpawnEggItem> ROOT_WORM_EGG = ITEMS.register("root_worm_spawn_egg", () ->
            new DeferredSpawnEggItem(ROOT_WORM, 0x59412D, 0xA58A5C, new Item.Properties()));

    public static void register(IEventBus bus, ModContainer container) {
        ENTITIES.register(bus);
        ITEMS.register(bus);
        // A separate filename allows the destination mod to keep its own existing COMMON config.
        container.registerConfig(ModConfig.Type.COMMON, RootWormConfig.SPEC, MOD_ID + "-root-worm-common.toml");
        bus.addListener(RootWormModule::attributes);
        bus.addListener(RootWormModule::placements);
        bus.addListener(RootWormModule::creativeItems);
        NeoForge.EVENT_BUS.addListener(RootWormPart::redirectPlayerAttack);
        NeoForge.EVENT_BUS.addListener(dev.fantasyfeudalism.rootworm.entity.RootWormHearing::onLivingJump);
        NeoForge.EVENT_BUS.addListener(dev.fantasyfeudalism.rootworm.entity.RootWormHearing::onEquipmentChange);
    }
    private static void attributes(EntityAttributeCreationEvent event) { event.put(ROOT_WORM.get(), RootWorm.createAttributes().build()); }
    private static void placements(RegisterSpawnPlacementsEvent event) {
        // Normal Minecraft ground-spawn rules only. The destination biome's spawn list decides WHERE.
        event.register(ROOT_WORM.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Mob::checkMobSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }
    private static void creativeItems(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(CreativeModeTabs.SPAWN_EGGS)) event.accept(ROOT_WORM_EGG.get());
    }
    public static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath(MOD_ID, path); }
    private RootWormModule() {}
}
''')
write('src/main/java/' + PACKAGE.replace('.', '/') + '/client/RootWormClient.java', '''
package dev.fantasyfeudalism.rootworm.client;

import dev.fantasyfeudalism.rootworm.RootWormModule;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Register only on Dist.CLIENT. No blocks, foliage colors or worldgen rendering are included. */
public final class RootWormClient {
    public static void register(IEventBus bus) {
        bus.addListener(RootWormClient::renderers);
        bus.addListener(RootWormClient::layers);
    }
    private static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(RootWormModule.ROOT_WORM.get(), RootWormRenderer::new);
    }
    private static void layers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(RootWormModel.LAYER, RootWormModel::createLayer);
    }
    private RootWormClient() {}
}
''')
# Only the entity texture, spawn egg, two language entries and original empty loot table.
texture = KIT / 'src/main/resources/assets/rootworm/textures/entity/root_worm.png'
texture.parent.mkdir(parents=True, exist_ok=True)
shutil.copyfile(RES / 'assets/fantasy_feudalism_biomes/textures/entity/root_worm.png', texture)
resource('src/main/resources/assets/rootworm/models/item/root_worm_spawn_egg.json', {'parent': 'minecraft:item/template_spawn_egg'})
resource('src/main/resources/assets/rootworm/lang/en_us.json', {'entity.rootworm.root_worm': 'Root Worm', 'item.rootworm.root_worm_spawn_egg': 'Root Worm Spawn Egg'})
resource('src/main/resources/data/rootworm/loot_table/entities/root_worm.json', {'type': 'minecraft:entity', 'pools': []})
resource('src/main/resources/rootworm-module.mixins.json', {'required': True, 'minVersion': '0.8', 'package': PACKAGE + '.mixin', 'compatibilityLevel': 'JAVA_21', 'mixins': ['RootWormArrowMixin'], 'injectors': {'defaultRequire': 1}})

# An isolated, dependency-free host proves the mergeable src tree runs on a dedicated server.
write('example-host/src/main/java/dev/fantasyfeudalism/rootworm/example/RootWormExample.java', '''
package dev.fantasyfeudalism.rootworm.example;

import dev.fantasyfeudalism.rootworm.RootWormModule;
import dev.fantasyfeudalism.rootworm.client.RootWormClient;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

/** Example host only: do not copy this @Mod class into an already existing mod. */
@Mod(RootWormModule.MOD_ID)
public final class RootWormExample {
    public RootWormExample(IEventBus bus, ModContainer container, Dist dist) {
        RootWormModule.register(bus, container);
        if (dist == Dist.CLIENT) RootWormClient.register(bus);
    }
}
''')
write('example-host/src/main/resources/META-INF/neoforge.mods.toml', '''
modLoader="javafml"
loaderVersion="[4,)"
license="All Rights Reserved"
[[mods]]
modId="rootworm"
version="1.0.0"
displayName="Root Worm Integration Example"
description="Isolated example host for the reusable Root Worm module."
[[dependencies.rootworm]]
modId="neoforge"
type="required"
versionRange="[21.1.262,21.2)"
ordering="NONE"
side="BOTH"
[[dependencies.rootworm]]
modId="minecraft"
type="required"
versionRange="[1.21.1,1.21.2)"
ordering="NONE"
side="BOTH"
[[mixins]]
config="rootworm-module.mixins.json"
''')
write('build.gradle', '''
plugins {
    id 'java-library'
    id 'net.neoforged.moddev' version '2.0.148'
}
version = '1.0.0'
group = 'dev.fantasyfeudalism.rootworm'
base { archivesName = 'Root Worm Integration Example' }
java.toolchain.languageVersion = JavaLanguageVersion.of(21)
sourceSets.main {
    java.setSrcDirs(['src/main/java', 'example-host/src/main/java', 'example-host/src/gametest/java'])
    resources.setSrcDirs(['src/main/resources', 'example-host/src/main/resources', 'example-host/src/gametest/resources'])
}
neoForge {
    version = '21.1.262'
    runs {
        client { client() }
        server { server(); programArgument '--nogui' }
        gameTestServer {
            type = 'gameTestServer'
            systemProperty 'neoforge.enabledGameTestNamespaces', 'rootworm'
        }
    }
    mods { create('rootworm') { sourceSet(sourceSets.main) } }
}
// No Lithostitched, biome libraries, GeckoLib, custom blocks or external entity assets are needed.
tasks.withType(JavaCompile).configureEach { options.encoding = 'UTF-8' }
tasks.named('jar', Jar) {
    exclude 'dev/fantasyfeudalism/rootworm/gametest/**'
    exclude 'data/rootworm/structure/test_arena.nbt'
}
''')
write('settings.gradle', (REPO / 'settings.gradle').read_text().replace('fantasy-feudalism-biomes', 'rootworm-integration-example'))
write('.gitignore', '.gradle/\nbuild/\nrun/\nruns/\n.idea/')
for relative in ['gradlew', 'gradlew.bat', 'gradle/wrapper/gradle-wrapper.jar', 'gradle/wrapper/gradle-wrapper.properties']:
    dest = KIT / relative
    dest.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(REPO / relative, dest)
(KIT / 'gradlew').chmod(0o755)
shutil.copyfile(REPO / 'TEMPLATE_LICENSE.txt', KIT / 'example-host/TEMPLATE_LICENSE.txt')
# Reuse existing combat/animation tests in the isolated host, with no original mod on the classpath.
write('example-host/src/gametest/java/' + PACKAGE.replace('.', '/') + '/gametest/RootWormHitboxGameTests.java', rewrite((JAVA / 'gametest/RootWormHitboxGameTests.java').read_text()))
life = (JAVA / 'gametest/RootLifeGameTests.java').read_text()
methods = '\n\n'.join(extract_test(life, name) for name in ['wormRemainsNeutralThenRetaliates', 'wormOpensMouthBeforeBitingThenClosesIt', 'wormFireAndLavaDamageAreAmplifiedAndConfigurable', 'wormPlansEscapeFromHeatAndHonorsToggle'])
methods = methods.replace('        worm.setTarget(target);', '        worm.setTarget(target);\n        worm.hearSound(target, target.position(), 1.0);')
methods = methods.replace('Blocks.CAMPFIRE', 'Blocks.FIRE').replace('Lit campfire should be feared', 'Vanilla fire should be feared')
methods = methods.replace('helper.assertTrue(RootWorm.isHeat(helper.getLevel(), hot), "Configured torch should be recognized as heat");', 'helper.assertTrue(!RootWorm.isHeat(helper.getLevel(), hot), "Ordinary torches impose no custom block rules");')
write('example-host/src/gametest/java/' + PACKAGE.replace('.', '/') + '/gametest/RootWormBehaviorGameTests.java', '''
package dev.fantasyfeudalism.rootworm.gametest;
import dev.fantasyfeudalism.rootworm.RootWormModule;
import dev.fantasyfeudalism.rootworm.RootWormConfig;
import dev.fantasyfeudalism.rootworm.entity.RootWorm;
import dev.fantasyfeudalism.rootworm.entity.AvoidHeatGoal;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
@GameTestHolder(RootWormModule.MOD_ID)
@PrefixGameTestTemplate(false)
public final class RootWormBehaviorGameTests {
''' + methods + '''
    @GameTest(template = "test_arena", timeoutTicks = 100, skyAccess = true)
    public static void spawnRulesUseOrdinaryGroundInsteadOfBiomeOrSoilTags(GameTestHelper helper) {
        for (var block : new net.minecraft.world.level.block.Block[]{Blocks.STONE, Blocks.SAND, Blocks.DEEPSLATE, Blocks.GRASS_BLOCK}) {
            helper.setBlock(16, 1, 16, block);
            var pos = helper.absolutePos(new BlockPos(16, 2, 16));
            helper.assertTrue(net.minecraft.world.entity.SpawnPlacements.checkSpawnRules(RootWormModule.ROOT_WORM.get(), helper.getLevel(),
                    net.minecraft.world.entity.MobSpawnType.NATURAL, pos, net.minecraft.util.RandomSource.create(1)),
                    "No custom biome/soil tag should be required to spawn on " + block);
        }
        helper.succeed();
    }
}
''')
write('example-host/src/gametest/java/' + PACKAGE.replace('.', '/') + '/gametest/RootWormHearingGameTests.java',
      (REPO / 'tools/rootworm_export/gametest/RootWormHearingGameTests.java').read_text())
write('example-host/src/gametest/java/' + PACKAGE.replace('.', '/') + '/gametest/RootWormSearchGameTests.java',
      (REPO / 'tools/rootworm_export/gametest/RootWormSearchGameTests.java').read_text())
arena = KIT / 'example-host/src/gametest/resources/data/rootworm/structure/test_arena.nbt'
arena.parent.mkdir(parents=True, exist_ok=True)
shutil.copyfile(RES / 'data/fantasy_feudalism_biomes/structure/test_arena.nbt', arena)

# Namespace/package conversion for the user's existing mod, without editing its own files.
write('tools/retarget.py', r'''
#!/usr/bin/env python3
"""Run in the extracted kit before merging src into the destination mod."""
from pathlib import Path
import argparse, hashlib, json, re, shutil
root = Path(__file__).resolve().parents[1]
parser = argparse.ArgumentParser()
parser.add_argument('--mod-id', required=True)
parser.add_argument('--package', required=True)
args = parser.parse_args()
if not re.fullmatch(r'[a-z][a-z0-9_]{1,63}', args.mod_id): parser.error('Use a valid NeoForge mod ID.')
if not re.fullmatch(r'[a-zA-Z_]\w*(?:\.[a-zA-Z_]\w*)+', args.package): parser.error('Use a Java package, e.g. com.example.mymod.rootworm.')
manifest = json.loads((root / 'SOURCE_MANIFEST.json').read_text())
old_id, old_package = manifest['namespace'], manifest['java_package']
for path in sorted(root.rglob('*')):
    if not path.is_file() or any(part in {'.gradle', 'build', 'run', 'runs', '.git'} for part in path.relative_to(root).parts): continue
    if path.suffix not in {'.java', '.json', '.toml', '.gradle', '.md'} or path.name == 'SOURCE_MANIFEST.json': continue
    text = path.read_text()
    text = text.replace(old_package, args.package)
    text = text.replace('"' + old_id + '"', '"' + args.mod_id + '"').replace("'" + old_id + "'", "'" + args.mod_id + "'")
    for a, b in [(old_id + ':', args.mod_id + ':'), ('entity.' + old_id + '.', 'entity.' + args.mod_id + '.'),
                 ('item.' + old_id + '.', 'item.' + args.mod_id + '.'), ('dependencies.' + old_id, 'dependencies.' + args.mod_id),
                 ('assets/' + old_id, 'assets/' + args.mod_id), ('data/' + old_id, 'data/' + args.mod_id),
                 ('`' + old_id + '`', '`' + args.mod_id + '`'), (old_package.replace('.', '/'), args.package.replace('.', '/'))]:
        text = text.replace(a, b)
    path.write_text(text)
for source in [root / 'src/main/java', root / 'example-host/src/main/java', root / 'example-host/src/gametest/java']:
    old, new = source / old_package.replace('.', '/'), source / args.package.replace('.', '/')
    if old != new and old.exists():
        temp = source / '.rootworm-retarget-temp'
        old.rename(temp)
        new.parent.mkdir(parents=True, exist_ok=True)
        temp.rename(new)
for source in [root / 'src/main/resources/assets', root / 'src/main/resources/data', root / 'example-host/src/gametest/resources/data']:
    old, new = source / old_id, source / args.mod_id
    if old != new and old.exists(): old.rename(new)
manifest['namespace'], manifest['java_package'] = args.mod_id, args.package
manifest['files'] = {str(p.relative_to(root)): hashlib.sha256(p.read_bytes()).hexdigest() for p in sorted(root.rglob('*'))
    if p.is_file() and p.name != 'SOURCE_MANIFEST.json' and not any(part in {'.gradle','build','run','runs','.git'} for part in p.relative_to(root).parts)}
(root / 'SOURCE_MANIFEST.json').write_text(json.dumps(manifest, indent=2) + '\n')
print('Retargeted kit to', args.mod_id, args.package)
''')
write('README.md', '''
# Root Worm integration kit

Source export from Fantasy Feudalism - Biomes 0.6.1 for **Minecraft 1.21.1, NeoForge 21.1.262+, Java 21**.

## Included

- The original 128x64 Root Worm PNG with a customized Java model: green back meshes removed, identical mirrored eyes with true-black pupils, the original blink, renderer and shared nine-segment geometry.
- Procedural Java animations: slithering, idle movement/blinking, cosmetic idle nibbling, and an attack-only opening mouth with a faster 9-tick animation and 3-tick windup, synchronized from the server (the 20-tick attack cooldown is unchanged).
- A raised searching animation inspired by an inchworm: the front and middle arch upward while the rear three segments stay planted, and the head slowly scans side to side with its mouth closed. It eases up/down over 10 ticks, pauses to listen for 52 ticks after hearing a sound, and searches again upon reaching the remembered location. Combat, heat, water and movement interrupt the pose; low ceilings prevent raising. The model and multipart hitboxes share the search curve and synchronized world-time transitions, including for clients that start tracking mid-animation.
- Neutral, hearing-guided AI: investigate eating, footsteps/running, jumping/landing, damage, block breaking/placing, armor equipping and held-item changes. Running/loud actions reach 16 blocks by default; normal footsteps reach half that and sneaking footsteps are ignored. Remembered sound locations expire after 80 ticks. Hearing alone does not cause aggression. Retaliatory bites use the head reach and a short lunge so slowly retreating targets can be bitten. Solid blocks still stop bites. Configurable fire/lava weakness, vanilla fire/lava avoidance and panic while burning are retained.
- Nine multipart damage boxes, correct part IDs and lifecycle, normal melee handling, arrow/potion/fire forwarding, scaling, teleports and name tags.
- Entity/attribute/spawn-egg registration, spawn placement registration, independent behavior config, client registration, translations and the original empty loot table.
- A small example host and isolated GameTests, plus a retargeting tool and per-file SHA-256 manifest.

The model and animations are Java source (`client/RootWormModel.java`), not GeckoLib/Blockbench animation JSON. No GeckoLib or Lithostitched dependency is required. The green atlas tile is unused after removing the back meshes; pupils are rendered true black by the model, so both eyes match without relying on different texture faces. There are no custom sound assets in the original implementation.

## No habitat dependencies

No Rootlands biome, worldgen, surface rules, custom blocks, root-food tags, heat-source tags or block lists are included. Root-seeking AI has been removed; idle nibbling is cosmetic and never reads or destroys root blocks. Heat avoidance recognizes Minecraft's built-in fire/lava only. Spawn placement uses normal Minecraft ground-spawn rules on ordinary valid ground; there is no custom biome, rooted-dirt, grass-block or root-density check. This kit does not automatically add the creature to any biome.

## Merge into an existing mod

1. Extract this ZIP, then retarget the **kit** to your destination mod ID and Java package:

   ```sh
   python3 tools/retarget.py --mod-id yourmod --package com.example.yourmod.rootworm
   ```

   This updates Java imports, resource namespaces/paths, translations, the example host and checksums. Run it before copying into your actual mod. The unmodified example namespace is `rootworm`.

2. Merge **only `src/main/java/` and `src/main/resources/`** into your existing mod. Keep its current build files and mod entry point. Do not copy `example-host/` or add a second `@Mod` class. Merge language/loot files if those paths already exist.

3. In your existing mod constructor, call the module once:

   ```java
   RootWormModule.register(modBus, container);
   if (dist == net.neoforged.api.distmarker.Dist.CLIENT) {
       RootWormClient.register(modBus);
   }
   ```

   Use the `IEventBus`, `ModContainer` and `Dist` supplied by NeoForge, as demonstrated by `example-host/.../RootWormExample.java`. `RootWormClient` is client-only; keep its registration inside the client branch. The module uses a separate `<modid>-root-worm-common.toml` config, so your existing common config can stay in place.

4. Append this to your existing `META-INF/neoforge.mods.toml` (keep all existing mixin entries):

   ```toml
   [[mixins]]
   config="rootworm-module.mixins.json"
   ```

   This mixin retains normal arrow damage, fire and tipped/spectral effects on multipart hits. The module's common registration also installs the melee forwarding listener; do not omit it.

5. Add Root Worm spawn entries wherever you want in the **new mod's own biome definitions or spawn configuration**. For example, under a biome's `spawners.creature` list, add an entry of this form using your destination namespace:

   ```json
   {"type": "yourmod:root_worm", "weight": 30, "minCount": 2, "maxCount": 4}
   ```

   There are deliberately no biome files or biome modifiers in this kit. Your mod chooses every natural spawn location. Normal Minecraft collision, ground placement, mob caps and spawn timing still apply.

6. Test with `/summon yourmod:root_worm ~ ~ ~` and `/give @s yourmod:root_worm_spawn_egg`. The egg appears in vanilla Spawn Eggs. Add `RootWormModule.ROOT_WORM_EGG` to your own creative tab if desired. Use F3+B to inspect the multipart hitboxes.

## Paths and optional changes

- Texture: `src/main/resources/assets/rootworm/textures/entity/root_worm.png` (128x64 RGBA).
- Model/animations: `src/main/java/dev/fantasyfeudalism/rootworm/client/RootWormModel.java`.
- Renderer: `client/RootWormRenderer.java`; shared model/hitbox sizes: `entity/RootWormShape.java`; raised search curve: `entity/RootWormSearchPose.java`.
- Behavior/attacks: `entity/RootWorm.java`, `entity/RootWormAttackGoal.java`; hearing/investigation: `entity/RootWormHearing.java`, `entity/InvestigateSoundGoal.java`; heat avoidance: `entity/AvoidHeatGoal.java`.
- Multipart damage: `entity/RootWormPart.java`; arrow forwarding: `mixin/RootWormArrowMixin.java`.
- Registration/config: `RootWormModule.java`, `RootWormConfig.java`; client registration: `client/RootWormClient.java`.

The original thorn immunity was implemented in the original Thorn block's contact handler. No thorn block is exported. If the destination mod has thorns, skip contact effects for `entity instanceof RootWorm` in that block's own handler. The worm itself has no custom block dependency.

The empty loot table preserves the original no-item-drop behavior; replace it in your mod if you want drops. Do not register RootWormPart as a separate entity type, spawn it separately, or remove the parent's ID reservation/part lifecycle code.

## Build the optional isolated example

From the extracted kit, run `./gradlew build` and `./gradlew runGameTestServer` (Windows: `gradlew.bat`). `./gradlew runClient` launches the example client. The example uses only NeoForge/Minecraft, without the original biome mod or Lithostitched. GameTests and the test arena are excluded from its built JAR. Client rendering still needs an in-game visual review.

`example-host/` is a validation/demo host, not required when merging the core `src/` tree into your existing mod. Source provenance and exact file hashes are recorded in `SOURCE_MANIFEST.json`; the original source licensing is unchanged. The build scaffold's template license is retained under `example-host/TEMPLATE_LICENSE.txt`.
''')

runtime = KIT / 'src'
for p in runtime.rglob('*'):
    if p.is_file() and p.suffix != '.png':
        content = p.read_text()
        for forbidden in ['fantasy_feudalism_biomes', 'FantasyBiomes', 'RootlandsConfig', 'ROOT_FOOD', 'HEAT_SOURCES', 'SeekRootsGoal', 'lithostitched']:
            assert forbidden not in content, (p, forbidden)
manifest = {
    'source_mod_version': re.search(r"version = '([^']+)'", (REPO / 'build.gradle').read_text()).group(1),
    'source_commit': subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=REPO, text=True).strip(),
    'minecraft': '1.21.1', 'neoforge': '21.1.262', 'java': 21,
    'namespace': NS, 'java_package': PACKAGE,
    'habitat_dependencies': [],
    'export_customizations': ['no green back meshes', 'identical mirrored eyes with true-black pupils and retained blink', '9-tick bite / 3-tick windup / unchanged 20-tick cooldown', 'vanilla game-event hearing and hearing-guided pursuit with short bite lunge', 'synchronized raised searching with a grounded tail, side-to-side scan and matching multipart hitboxes'],
    'removed': ['Rootlands biome/worldgen', 'custom blocks and block lists', 'root-food and heat-source tags', 'root-seeking AI', 'custom habitat spawn checks', 'Lithostitched dependency'],
    'files': {str(p.relative_to(KIT)): hashlib.sha256(p.read_bytes()).hexdigest() for p in sorted(KIT.rglob('*')) if p.is_file()}
}
resource('SOURCE_MANIFEST.json', manifest)
archive = OUT / 'RootWorm-Integration-Kit-NeoForge-1.21.1.zip'
with zipfile.ZipFile(archive, 'w', compression=zipfile.ZIP_DEFLATED, compresslevel=9) as z:
    for p in sorted(KIT.rglob('*')):
        if not p.is_file(): continue
        info = zipfile.ZipInfo(str(p.relative_to(OUT)), (1980, 1, 1, 0, 0, 0))
        info.compress_type = zipfile.ZIP_DEFLATED
        info.external_attr = ((0o755 if p.name in {'gradlew', 'retarget.py'} else 0o644) | 0o100000) << 16
        z.writestr(info, p.read_bytes(), compresslevel=9)
print(json.dumps({'archive': str(archive), 'kit': str(KIT), 'files': len(manifest['files']) + 1, 'bytes': archive.stat().st_size}, indent=2))
