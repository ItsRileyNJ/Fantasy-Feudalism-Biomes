# Fantasy Feudalism - Biomes

Rootlands is an old, dark woodland of tall crooked trees, exposed roots, twisting thorn arches, dark grass plants, small Root Flowers and neutral Root Worms.

## Install and build

- Minecraft **1.21.1**, Java **21**, NeoForge **21.1.262**.
- Install **Lithostitched 1.8.1 for NeoForge 21.1** on both client and server; the supplied artifact includes Apollib 1.2.0.
- Install `build/libs/Fantasy Feudalism - Biomes-0.5.2.jar` alongside Lithostitched in the game's `mods/` directory. Remove the older Fantasy Feudalism mod JAR when upgrading.

The JAR under this repository's `libs/` directory is the **Lithostitched dependency**, not the Fantasy Feudalism mod. It is used for repeatable local builds and is not embedded in our output. See `THIRD_PARTY.md` and `licenses/`. Regions Unexplored is not required or bundled.

```sh
./gradlew build
./gradlew runGameTestServer
./gradlew runClient
```

Use `gradlew.bat` on Windows and set `JAVA_HOME` to a Java 21 JDK if needed. Gradle is pinned through the included checksum-verified wrapper. On the prepared cloud machine the JDK is `/workspace/toolchains/jdk-21.0.9+10`, and proxy-aware commands can be run as `python3 /workspace/gradle-cloud.py build`. Machine-specific proxy configuration and credentials are not stored in this repository.

For a dedicated development server use `./gradlew runServer`; review Minecraft's EULA and configure the server first. The project does not accept the EULA automatically.

## Find and explore Rootlands

```mcfunction
/locate biome fantasy_feudalism_biomes:rootlands
```

Operator/cheat permissions are needed. Newly generated Rootlands regions occupy the interior of vanilla **dark forest**, preserving a neighboring dark-oak-forest rim. A continuous low-frequency selection mask controls regional scale; a 32-block vanilla-forest buffer prevents the mod from consuming an entire parent forest. Actual biome dimensions are limited by the parent forest, rivers and climate boundaries. Superflat does not provide this distribution.

The base terrain density remains vanilla. Shore grading runs at the **end of CARVERS, after surface rules and caves but before any neighboring chunk can decorate**. Dry woodland slopes down to a dry bank, while existing underwater beds slope up to a submerged shelf. Land is never lowered below the water surface, so the shoreline pass cannot flood a forest and remove the soil needed by roots. Water uses vanilla's exclusive sea-level limit: with sea level 63, the highest water blocks are at Y=62, and no extra Y=63 water layer is added.

Low banks get a sand beach with two additional sand layers and a sandstone foundation; original beach biomes retain sand, and stony shores retain stone. Rivers, oceans and beach biomes are preserved by biome injection. Grading also blends through neighboring shores instead of stopping at a biome boundary. Its world-aligned sampling grid, interpolation and bounded caches keep adjacent chunk profiles consistent. Coast detection uses an approximate original-terrain density field; the actual column's height and water state constrain every write. Dry depressions and underground caves are not filled merely because they lie below sea level. The old decoration-stage shoreline feature remains inert for saved data references.

**Worldgen changes affect new chunks only.** Existing flooded woodland, raised or missing water, pillars, floating grass and already explored terrain are not repaired or rewritten. To compare this fix at a reported location, create a separate fresh world with the same seed; upgrading an existing save does not regenerate those chunks. Foreign blocks, existing logs and block entities are protected from the shore pass. Iron/copper ores and raw ore blocks from noise-stage veins count as natural terrain; other ores are normally placed later, after grading. Other mods that change terrain after our early generation pass can still change the final shore.

## Creative tab and items

The creative tab is named exactly **Fantasy Feudalism - Biomes** and contains:

- Root Worm Spawn Egg
- Root Flower
- Thorn
- Rootlands Sapling
- Rootlands Leaves
- Dark Grass

The plant items are also available under Natural Blocks, and the spawn egg remains under Spawn Eggs. The thorn's displayed name is **Thorn**; its saved ID remains `fantasy_feudalism_biomes:rootlands_thorn` for world compatibility.

```mcfunction
/give @s fantasy_feudalism_biomes:rootlands_sapling
/give @s fantasy_feudalism_biomes:dark_grass
/give @s fantasy_feudalism_biomes:root_flower
/summon fantasy_feudalism_biomes:root_worm ~ ~ ~
```

## Trees, vegetation and atmosphere

The trees have four distinct forms: **Crooked Crown**, **Forked Spire**, **Windswept**, and **Hollow Elder**. They feature thick tapering trunks, connected angular boughs, secondary forks, irregular layered crowns, hanging bare twigs and buttress roots. Height varies roughly from 24 to 40 blocks. Elders have a hollow lower trunk; forked trees split into upper spires; windswept trees lean farther to one side.

A single Rootlands Sapling grows one tree, naturally or with bone meal, including outside Rootlands. There is no two-by-two sapling requirement. Tree space is validated before placement; obstructed growth restores the sapling without leaving a partial tree. Custom leaves can drop Rootlands Saplings; shears or Silk Touch collect the leaves themselves. Trunks and branches use dark oak logs; buttress roots and exposed root structures use dark oak wood blocks with bark on every face.

**Dark Grass is a grass plant, not a grass block variant.** It grows in patches above the existing soil. Fuller clumps have four overlapping blade planes, taller and shorter variants, and a grayscale texture with shaded blade folds, brighter tips and darker bases. Like vanilla grass, the plant receives biome grass tint; an additional darkening keeps its hues subdued. The biome also has darker fog, sky and foliage colors, with denser custom canopy detail. These changes do not force night or alter the world's time.

Large root structures form intertwined arches roughly 9–16 blocks high and 9–14 blocks across, with irregular lateral loops, forks, bends and smaller spurs. The arch bodies, smaller ground roots, and tree buttress roots are made from **dark oak wood blocks**, not thorn blocks or exposed-end logs. Separate dark thorn spikes project outward from exposed root faces: **top, bottom, and all four sides**. Smaller thorn patches are now thorn-bearing wooden root clusters instead of freestanding walls of spikes. All thorn models use the **vanilla dark oak log bark texture**, matching the root wood’s exact palette and grain, including resource-pack replacements. Each attachment direction has four possible twists around its growth axis, so the bent spikes point at varied angles. Contact damage and slowing remain configurable; Root Worms are immune to both effects.

When placing a Thorn by hand, click the desired face of a dark oak wood root. The spike points out from that face and requires its root support; it breaks if that supporting root block is removed. The `thorn_supports` block tag defaults to `minecraft:dark_oak_wood`. The old thorn ID and axis property are retained for saved-state compatibility, with `facing` controlling attachment and `twist` controlling the spike’s roll. Existing generated roots/arches are not automatically converted; explore new chunks to see the new generation.

Root Flowers are small, gaunt brown bushes with sparse white/red blooms. They generate in small patches and can damage and slow entities independently by category. They are not berry-producing bushes and do not have berry growth stages.

## Root Worms

Root Worms are neutral until attacked, retaliate against an attacker, and prefer root-rich ground. Their long nine-segment model slithers while moving and gently bobs when feeding. The natural face has small recessed amber-brown side eyes, layered bark plates and a closed snout. Four articulated mouth plates spread during an actual melee bite, revealing a shaded throat and short, muted root-colored toothlets. A brief windup opens the mouth before damage is dealt; the mouth closes after the bite and remains closed while idle, feeding or merely chasing. Bite timing is synchronized from the server. The body retains raised bark rings, moss flecks, small root-like nubs and subtle idle movement; the eyes occasionally blink. **Feeding is cosmetic and never consumes roots or structure blocks**, regardless of `mobGriefing`.

They are weak to fire/lava, flee nearby heat, and panic while burning. Their natural spawn weight is **30** (previously 10), with groups of **2–4** (previously 1–3). Root-rich ground always passes their habitat check; ordinary suitable Rootlands soil now passes **50%** of checks rather than 20%. Vanilla creature caps and other spawn rules still apply, so those figures are not a guaranteed worms-per-minute rate.

## Configuration

The first launch creates `config/fantasy_feudalism_biomes-common.toml` under the game directory (`run/` in development). Restart after editing. Keep client common settings consistent with the server for movement prediction; the server controls authoritative generation and damage.

| Section / setting | Default | Meaning |
| --- | --- | --- |
| `rootlands.enabled` | `true` | Enable selecting Rootlands during new generation. |
| `rootlands.biomeWeight` | `35` | Coverage bias within eligible dark forest; not a world percentage. Zero disables selection. |
| `rootlands.biomeSize` | `2048` | Horizontal selection scale in blocks; not a guaranteed biome width. |
| `rootlands.edgeBlendWidth` | `24` | Distance over which custom decoration thins near other biomes; zero disables tapering. |
| `rootlands.gentleShores` | `true` | Enable grading nearby shores and shallow beds. |
| `rootlands.shoreWidth` | `64` | Shore transition width in blocks, including blending into adjacent biomes. |
| `rootlands.treeDensity` | `5` | Tree attempts per chunk. |
| `rootlands.rootDensity` | `8` | Standalone root-cluster attempts per chunk; trees also have attached roots. |
| `rootlands.thornDensity` | `10` | Thorn-heavy wooden root patch attempts per chunk. |
| `rootlands.giantThornDensity` | `2` | Large twisting wood root arch attempts per chunk, decorated with attached thorns. |
| `rootlands.darkGrassDensity` | `14` | Dark grass plant patch attempts per chunk. |
| `rootlands.structureFrequency` | `1.0` | Scales tree/root/thorn/grass attempts; zero skips them, but not soil patches, flowers or shore grading. |
| `rootlands.structureVariants` | `true` | Vary tree forms, heights, roots and thorn layouts. False uses base shapes. |
| `rootlands.thornDamageEnabled` | `true` | Contact damage from Thorn blocks. |
| `rootlands.thornDamage` | `1.0` | Health points per accepted hit; two points equal one heart. |
| `rootlands.thornSlowEnabled` | `true` | Slow living entities touching thorns. |
| `rootFlower.patchFrequency` | `2.0` | Patch attempts per chunk; fractional values supported, zero disables patches. |
| `rootFlower.minimumPatchSize` | `3` | Minimum plants attempted per patch. |
| `rootFlower.maximumPatchSize` | `7` | Maximum plants attempted; reversed minimum/maximum values are normalized. |
| `rootFlower.damage` | `1.0` | Contact damage; zero disables it. |
| `rootFlower.slowAmount` | `0.2` | Horizontal movement reduction: 0.2 means 20%; zero disables it. |
| `rootFlower.affectPlayers` | `true` | Apply flower effects to players. |
| `rootFlower.affectPassiveMobs` | `true` | Apply to passive/neutral mobs, including Root Worms. |
| `rootFlower.affectHostileMobs` | `true` | Apply to Minecraft `Enemy` mobs. |
| `rootWorm.fireWeaknessEnabled` | `true` | Enable extra fire/lava damage. |
| `rootWorm.fireDamageMultiplier` | `2.0` | Multiply non-lava fire damage. |
| `rootWorm.lavaDamageMultiplier` | `2.0` | Multiply lava damage without stacking the other multiplier. |
| `rootWorm.avoidHeat` | `true` | Flee reachable heat and panic when burning. |
| `rootWorm.additionalHeatSources` | torch, soul torch | Additional block IDs to fear. |

Densities and patch sizes count **placement attempts**, not guaranteed successful placements. Obstructions, water, biome boundaries and occupied tree space can reduce the actual count. Disabled flower categories receive neither damage nor slowing. Vanilla damage cooldowns, creative/spectator protections and damage immunity still apply.

Fire and lava are recognized as heat. Lit campfires, soul campfires and magma blocks are included through the `root_worm_heat_sources` block tag. Unlit blocks with a `lit` property are not treated as active heat. Root attraction uses `root_worm_food`, including rooted dirt and root log materials.

## Development paths and checks

All Java code is under `src/main/java/dev/fantasyfeudalism/biomes/`:

- `worldgen/RootlandsIntegration.java` and `DarkForestInterior.java`: regional biome selection and preserved dark-oak rim.
- `mixin/RootlandsShoreMixin.java`, `worldgen/RootlandsShoreFeature.java` and `ShorelineProfile.java`: coast grading after caves and chunk-consistent influence fields.
- `worldgen/JaggedTree.java` and `RootlandsTreeFeature.java`: tree variants and sapling growth.
- `worldgen/GiantThorns.java`, `RootThorns.java` and `RootlandsScatterFeature.java`: wood arches and root clusters, six-face thorn attachments, grass and flower placement.
- `block/`: contact behavior and grass plant definition; `entity/`: worm behavior; `client/`: worm model/animation and leaf colors.

Resources are under `src/main/resources/data/fantasy_feudalism_biomes/` and `src/main/resources/assets/fantasy_feudalism_biomes/`. Registry IDs are stable. `tools/generate_resources.py` deterministically writes the worldgen/model/loot/tag resources and original pixel textures. Edit that script and regenerate rather than overwriting its generated files by hand. `tools/generate_test_arena.py` generates the development-only test arena.

The integration suite covers biome selection and dark-forest adjacency, raw terrain density/surface parity, intentional shore grading, real noise-generated coastal columns, narrow rivers, chunk-border continuity, protected structures and supported shore caps, tree variants, sapling growth/failure, renewable leaves, the dark grass plant, giant thorn arches, root/flower/thorn generation, creative-tab contents, spawn settings, worm behavior and contact configuration, hand placement of thorns, all six attachment directions, varied thorn twists, loss of root support, thorn immunity, and mouth-open windup/attack/recovery. A production carver-entry regression checks grading before tree/mushroom/grass placement and ensures the old decoration feature cannot reshape decorated terrain. Complete noise/surface/carver/shore comparisons cover four seeds, dry land and wet beds, vanilla water height, sand beaches and actual root placement. A separate regression covers seed `2754680807422324057` around X=3648, Z=-2302, using the normal Lithostitched biome injection; river/ocean/beach preservation is also checked on that seed. That location has forest ground at Y=107 in the clean test world, so the screenshot’s exact river layout has not been reproduced with these dependencies alone. Test classes and the arena are excluded from the distributable JAR. Client rendering and a visual playthrough remain separate checks; broad compatibility with every other worldgen mod is not established.

Build scaffolding derives from NeoForgeMDKs/MDK-1.21.1-ModDevGradle at `e0000cb81c6ca4bb218046cfd8ade771a42e9c32`; its license is retained in `TEMPLATE_LICENSE.txt`. The mod contains no Regions Unexplored classes, textures or structures.

Current validation: the Java 21 / NeoForge 21.1.262 build and all **44 required GameTests** pass with the supplied Lithostitched/Apollib dependencies. The worm model, grass and thorn assets are compiled and their texture references are checked; an in-client visual review remains pending.
