package dev.fantasyfeudalism.biomes.gametest;

import dev.fantasyfeudalism.biomes.FantasyBiomes;
import dev.fantasyfeudalism.biomes.RootlandsConfig;
import dev.fantasyfeudalism.biomes.block.RootFlowerBlock;
import dev.fantasyfeudalism.biomes.entity.AvoidHeatGoal;
import dev.fantasyfeudalism.biomes.entity.RootWorm;
import dev.fantasyfeudalism.biomes.worldgen.RootlandsScatterFeature;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(FantasyBiomes.MOD_ID)
@PrefixGameTestTemplate(false)
public final class RootLifeGameTests {
    @GameTest(template = "test_arena", timeoutTicks = 120, skyAccess = true)
    public static void wormRemainsNeutralThenRetaliates(GameTestHelper helper) {
        RootWorm worm = helper.spawn(FantasyBiomes.ROOT_WORM.get(), 14, 2, 16);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(helper.absolutePos(new BlockPos(16, 2, 16)).getCenter());
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(worm.getTarget() == null, "Unprovoked worm must not target a nearby player");
            worm.hurt(helper.getLevel().damageSources().playerAttack(player), 1.0F);
        });
        helper.runAfterDelay(35, () -> {
            helper.assertTrue(worm.getTarget() == player, "Attacked worm should defend itself against the attacker");
            helper.succeed();
        });
    }

    @GameTest(template = "test_arena", timeoutTicks = 100, skyAccess = true)
    public static void wormIgnoresThornDamageAndSlowWhileOtherMobsAreAffected(GameTestHelper helper) {
        boolean hurt = RootlandsConfig.THORNS_HURT.get(), slow = RootlandsConfig.THORNS_SLOW.get();
        double damage = RootlandsConfig.THORN_DAMAGE.get();
        try {
            RootlandsConfig.THORNS_HURT.set(true);
            RootlandsConfig.THORNS_SLOW.set(true);
            RootlandsConfig.THORN_DAMAGE.set(2.0);
            var worm = helper.spawn(FantasyBiomes.ROOT_WORM.get(), 8, 2, 8);
            var pig = helper.spawn(EntityType.PIG, 12, 2, 8);
            var thorn = FantasyBiomes.THORN.get().defaultBlockState();
            float hp = worm.getHealth(), pigHp = pig.getHealth();
            double start = worm.getX(), pigStart = pig.getX();
            thorn.entityInside(helper.getLevel(), worm.blockPosition(), worm);
            thorn.entityInside(helper.getLevel(), pig.blockPosition(), pig);
            helper.assertTrue(worm.getHealth() == hp && pig.getHealth() == pigHp - 2, "Only the Root Worm should be immune to thorn damage");
            worm.move(MoverType.SELF, new Vec3(1, 0, 0));
            pig.move(MoverType.SELF, new Vec3(1, 0, 0));
            helper.assertTrue(Math.abs(worm.getX() - start - 1.0) < 0.01, "Thorns must not slow Root Worms");
            helper.assertTrue(Math.abs(pig.getX() - pigStart - 0.65) < 0.01, "Other mobs must still be slowed");
        } finally {
            RootlandsConfig.THORNS_HURT.set(hurt);
            RootlandsConfig.THORNS_SLOW.set(slow);
            RootlandsConfig.THORN_DAMAGE.set(damage);
        }
        helper.succeed();
    }

    @GameTest(template = "test_arena", timeoutTicks = 100, skyAccess = true)
    public static void wormOpensMouthBeforeBitingThenClosesIt(GameTestHelper helper) {
        var worm = helper.spawn(FantasyBiomes.ROOT_WORM.get(), 15, 2, 16);
        var target = helper.spawn(EntityType.PIG, 22, 2, 16);
        target.setNoAi(true);
        helper.assertTrue(!worm.isBiting() && worm.getMouthOpenAmount(0) == 0, "Idle mouth must be closed");
        float hp = target.getHealth();
        boolean[] observed = new boolean[2];
        worm.setTarget(target); // Simulate a retaliation target, without introducing player aggression.
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(!worm.isBiting() && worm.getMouthOpenAmount(0) == 0, "Pursuit alone must not open the mouth");
            target.setPos(Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(16, 2, 16))));
        });
        helper.onEachTick(() -> {
            if (worm.isBiting() && target.getHealth() == hp && worm.getMouthOpenAmount(0) > 0) observed[0] = true;
            if (target.getHealth() < hp && !observed[1]) {
                helper.assertTrue(observed[0], "Mouth must open during the windup before bite damage");
                helper.assertTrue(worm.getMouthOpenAmount(0) > 0.5F, "Bite must land with the mouth open");
                observed[1] = true;
                worm.setTarget(null);
                worm.setNoAi(true);
            }
        });
        helper.runAfterDelay(60, () -> {
            helper.assertTrue(observed[0] && observed[1], "The actual melee goal must open the mouth and deliver its bite");
            helper.assertTrue(!worm.isBiting() && worm.getMouthOpenAmount(0) == 0, "Mouth must close after the attack");
            helper.succeed();
        });
    }

    @GameTest(template = "test_arena", timeoutTicks = 100, skyAccess = true)
    public static void wormFireAndLavaDamageAreAmplifiedAndConfigurable(GameTestHelper helper) {
        boolean enabled = RootlandsConfig.WORM_FIRE_WEAKNESS.get();
        double fire = RootlandsConfig.WORM_FIRE_MULTIPLIER.get();
        double lava = RootlandsConfig.WORM_LAVA_MULTIPLIER.get();
        try {
            RootlandsConfig.WORM_FIRE_WEAKNESS.set(true);
            RootlandsConfig.WORM_FIRE_MULTIPLIER.set(2.0);
            RootlandsConfig.WORM_LAVA_MULTIPLIER.set(3.0);
            var burnt = helper.spawn(FantasyBiomes.ROOT_WORM.get(), 8, 2, 8);
            var submerged = helper.spawn(FantasyBiomes.ROOT_WORM.get(), 12, 2, 8);
            float hp = burnt.getHealth();
            burnt.hurt(helper.getLevel().damageSources().inFire(), 2.0F);
            submerged.hurt(helper.getLevel().damageSources().lava(), 2.0F);
            helper.assertTrue(burnt.getHealth() == hp - 4, "Fire should use fire multiplier");
            helper.assertTrue(submerged.getHealth() == hp - 6, "Lava should use its own multiplier without stacking");
            RootlandsConfig.WORM_FIRE_WEAKNESS.set(false);
            var normal = helper.spawn(FantasyBiomes.ROOT_WORM.get(), 16, 2, 8);
            normal.hurt(helper.getLevel().damageSources().inFire(), 2.0F);
            helper.assertTrue(normal.getHealth() == hp - 2, "Disabled weakness should restore ordinary fire damage");
        } finally {
            RootlandsConfig.WORM_FIRE_WEAKNESS.set(enabled);
            RootlandsConfig.WORM_FIRE_MULTIPLIER.set(fire);
            RootlandsConfig.WORM_LAVA_MULTIPLIER.set(lava);
        }
        helper.succeed();
    }

    @GameTest(template = "test_arena", timeoutTicks = 100, skyAccess = true)
    public static void wormPlansEscapeFromHeatAndHonorsToggle(GameTestHelper helper) {
        boolean previous = RootlandsConfig.WORM_FEAR_HEAT.get();
        try {
            RootlandsConfig.WORM_FEAR_HEAT.set(true);
            var worm = helper.spawn(FantasyBiomes.ROOT_WORM.get(), 16, 2, 16);
            // Navigation is queried immediately, before the first physics tick establishes ground contact.
            worm.setOnGround(true);
            BlockPos hot = helper.absolutePos(new BlockPos(19, 2, 16));
            helper.setBlock(19, 2, 16, Blocks.CAMPFIRE);
            helper.assertTrue(RootWorm.isHeat(helper.getLevel(), hot), "Lit campfire should be feared");
            helper.assertTrue(!RootWorm.isHeat(helper.getLevel(), helper.absolutePos(new BlockPos(20, 2, 16))), "Air is not a heat source");
            var flee = new AvoidHeatGoal(worm);
            helper.assertTrue(flee.canUse(), "Nearby heat should produce a reachable escape path");
            flee.start();
            var destination = worm.getNavigation().getPath().getEndNode().asBlockPos();
            helper.assertTrue(destination.distSqr(hot) > worm.blockPosition().distSqr(hot), "Escape destination must be farther from heat");
            RootlandsConfig.WORM_FEAR_HEAT.set(false);
            helper.assertTrue(!new AvoidHeatGoal(worm).canUse(), "Disabled avoidance should not start fleeing");
            helper.setBlock(19, 2, 16, Blocks.LAVA);
            helper.assertTrue(RootWorm.isHeat(helper.getLevel(), hot), "Lava should be recognized as heat");
            helper.setBlock(19, 2, 16, Blocks.TORCH);
            helper.assertTrue(RootWorm.isHeat(helper.getLevel(), hot), "Configured torch should be recognized as heat");
        } finally { RootlandsConfig.WORM_FEAR_HEAT.set(previous); }
        helper.succeed();
    }

    @GameTest(template = "test_arena", timeoutTicks = 160, skyAccess = true)
    public static void wormPrefersRootsAndDoesNotConsumeThem(GameTestHelper helper) {
        for (int x = 13; x <= 19; x++) for (int z = 13; z <= 19; z++) helper.setBlock(x, 1, z, Blocks.ROOTED_DIRT);
        helper.setBlock(16, 2, 16, Blocks.DARK_OAK_LOG);
        var worm = helper.spawn(FantasyBiomes.ROOT_WORM.get(), 15, 2, 16);
        helper.assertTrue(worm.getWalkTargetValue(helper.absolutePos(new BlockPos(15, 2, 16)))
                > worm.getWalkTargetValue(helper.absolutePos(new BlockPos(5, 2, 5))), "Root-heavy ground should have a higher preference");
        helper.runAfterDelay(110, () -> {
            for (int x = 13; x <= 19; x++) for (int z = 13; z <= 19; z++) helper.assertBlockPresent(Blocks.ROOTED_DIRT, new BlockPos(x, 1, z));
            helper.assertBlockPresent(Blocks.DARK_OAK_LOG, new BlockPos(16, 2, 16));
            helper.succeed();
        });
    }

    @GameTest(template = "test_arena", timeoutTicks = 100, skyAccess = true)
    public static void rootFlowerFiltersContactCategoriesAndDealsConfiguredDamage(GameTestHelper helper) {
        boolean players = RootlandsConfig.FLOWER_PLAYERS.get();
        boolean passive = RootlandsConfig.FLOWER_PASSIVE.get();
        boolean hostile = RootlandsConfig.FLOWER_HOSTILE.get();
        double damage = RootlandsConfig.FLOWER_DAMAGE.get();
        try {
            RootlandsConfig.FLOWER_PLAYERS.set(false);
            RootlandsConfig.FLOWER_PASSIVE.set(true);
            RootlandsConfig.FLOWER_HOSTILE.set(false);
            RootlandsConfig.FLOWER_DAMAGE.set(2.0);
            var pig = helper.spawn(EntityType.PIG, 8, 2, 8);
            var zombie = helper.spawn(EntityType.ZOMBIE, 12, 2, 8);
            var player = helper.makeMockPlayer(GameType.SURVIVAL);
            helper.assertTrue(RootFlowerBlock.affects(pig) && !RootFlowerBlock.affects(zombie) && !RootFlowerBlock.affects(player), "Contact categories must be independent");
            var flower = FantasyBiomes.ROOT_FLOWER.get().defaultBlockState();
            float before = pig.getHealth();
            flower.entityInside(helper.getLevel(), pig.blockPosition(), pig);
            helper.assertTrue(pig.getHealth() == before - 2, "Flower should deal configured damage to enabled passive mobs");
            before = zombie.getHealth();
            flower.entityInside(helper.getLevel(), zombie.blockPosition(), zombie);
            helper.assertTrue(zombie.getHealth() == before, "Disabled hostile category must take no contact damage");
            RootlandsConfig.FLOWER_PLAYERS.set(true);
            RootlandsConfig.FLOWER_PASSIVE.set(false);
            RootlandsConfig.FLOWER_HOSTILE.set(true);
            helper.assertTrue(!RootFlowerBlock.affects(pig) && RootFlowerBlock.affects(zombie) && RootFlowerBlock.affects(player), "Toggles should also work in reverse");
        } finally {
            RootlandsConfig.FLOWER_PLAYERS.set(players);
            RootlandsConfig.FLOWER_PASSIVE.set(passive);
            RootlandsConfig.FLOWER_HOSTILE.set(hostile);
            RootlandsConfig.FLOWER_DAMAGE.set(damage);
        }
        helper.succeed();
    }

    @GameTest(template = "test_arena", timeoutTicks = 100, skyAccess = true)
    public static void rootFlowerSlowAmountChangesMovement(GameTestHelper helper) {
        double slow = RootlandsConfig.FLOWER_SLOW.get();
        double damage = RootlandsConfig.FLOWER_DAMAGE.get();
        boolean passive = RootlandsConfig.FLOWER_PASSIVE.get();
        try {
            RootlandsConfig.FLOWER_PASSIVE.set(true);
            RootlandsConfig.FLOWER_DAMAGE.set(0.0);
            RootlandsConfig.FLOWER_SLOW.set(0.35);
            var pig = helper.spawn(EntityType.PIG, 8, 2, 8);
            pig.setNoAi(true);
            double start = pig.getX();
            FantasyBiomes.ROOT_FLOWER.get().defaultBlockState().entityInside(helper.getLevel(), pig.blockPosition(), pig);
            pig.move(MoverType.SELF, new Vec3(1, 0, 0));
            helper.assertTrue(Math.abs(pig.getX() - start - 0.65) < 0.01, "35 percent slow should reduce one-block movement to 0.65");
            RootlandsConfig.FLOWER_SLOW.set(0.0);
            start = pig.getX();
            FantasyBiomes.ROOT_FLOWER.get().defaultBlockState().entityInside(helper.getLevel(), pig.blockPosition(), pig);
            pig.move(MoverType.SELF, new Vec3(1, 0, 0));
            helper.assertTrue(Math.abs(pig.getX() - start - 1.0) < 0.01, "Zero slow should preserve movement");
        } finally {
            RootlandsConfig.FLOWER_SLOW.set(slow);
            RootlandsConfig.FLOWER_DAMAGE.set(damage);
            RootlandsConfig.FLOWER_PASSIVE.set(passive);
        }
        helper.succeed();
    }

    @GameTest(template = "test_arena", timeoutTicks = 100, skyAccess = true)
    public static void rootFlowerPatchesRespectSizeAndFrequency(GameTestHelper helper) {
        int minimum = RootlandsConfig.FLOWER_MIN_SIZE.get();
        int maximum = RootlandsConfig.FLOWER_MAX_SIZE.get();
        double frequency = RootlandsConfig.FLOWER_FREQUENCY.get();
        try {
            var level = helper.getLevel();
            var fill = net.minecraft.server.commands.FillBiomeCommand.fill(level,
                    helper.absolutePos(new BlockPos(0, 1, 0)), helper.absolutePos(new BlockPos(31, 7, 31)),
                    level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(FantasyBiomes.ROOTLANDS));
            helper.assertTrue(fill.left().isPresent(), "Biome fill should succeed");
            RootlandsConfig.FLOWER_FREQUENCY.set(0.0);
            var placed = level.registryAccess().registryOrThrow(Registries.PLACED_FEATURE)
                    .getOrThrow(ResourceKey.create(Registries.PLACED_FEATURE, FantasyBiomes.id("root_flowers")));
            helper.assertTrue(!placed.place(level, level.getChunkSource().getGenerator(), RandomSource.create(1), helper.absolutePos(new BlockPos(16, 2, 16))), "Zero patch frequency must skip generation");
            RootlandsConfig.FLOWER_MIN_SIZE.set(4);
            RootlandsConfig.FLOWER_MAX_SIZE.set(4);
            helper.assertTrue(RootlandsScatterFeature.flowers(level, helper.absolutePos(new BlockPos(16, 2, 16)), RandomSource.create(17)), "Flower patch should generate on Rootlands dirt");
            int count = 0;
            for (int x = 12; x <= 20; x++) for (int z = 12; z <= 20; z++) {
                if (helper.getBlockState(new BlockPos(x, 2, z)).is(FantasyBiomes.ROOT_FLOWER.get())) count++;
            }
            helper.assertTrue(count == 4, "Fixed min/max patch size should place four flowers on clear ground, got " + count);
            helper.assertTrue(!FantasyBiomes.ROOT_FLOWER.get().defaultBlockState().canSurvive(level, helper.absolutePos(new BlockPos(16, 5, 16))), "Flower cannot float in air");
        } finally {
            RootlandsConfig.FLOWER_MIN_SIZE.set(minimum);
            RootlandsConfig.FLOWER_MAX_SIZE.set(maximum);
            RootlandsConfig.FLOWER_FREQUENCY.set(frequency);
        }
        helper.succeed();
    }
}
