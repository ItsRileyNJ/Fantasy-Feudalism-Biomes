# Fantasy Feudalism - Biomes — Rootlands

The first biome for Fantasy Feudalism: a dark, old woodland with tall, bent trees, jagged ascending limbs, exposed roots, dirt patches and damaging thorn walls, neutral Root Worms and sparse Root Flowers.

## Requirements

- Minecraft **1.21.1**, Java **21**, NeoForge **21.1.262**.
- **Lithostitched 1.8.1 for NeoForge 21.1**, installed on client and server. The supplied JAR bundles Apollib 1.2.0.
- Gradle is pinned through the included wrapper; no global Gradle installation is needed.

The uploaded Lithostitched artifact is retained in `libs/` for repeatable local builds. It is a compile/runtime development dependency and **is not embedded in our output JAR**. Its SHA-256 is `e0da669fa5facc61a09b9b7bae21b322993de1fcd1eb05787adffb7309598cca`. Its MIT license is recorded in `THIRD_PARTY.md`. Regions Unexplored is reference material only and is not required or bundled.

## Develop and build

```sh
./gradlew build
./gradlew runGameTestServer
./gradlew runClient
```

On Windows, use `gradlew.bat`. Set `JAVA_HOME` to a Java 21 JDK if Gradle cannot find one. In the prepared cloud machine, the JDK is `/workspace/toolchains/jdk-21.0.9+10`; its network proxy launcher is `/workspace/gradle-cloud.py` (for example, `python3 /workspace/gradle-cloud.py build`). No proxy settings or credentials are stored in this project.

The distributable is `build/libs/Fantasy Feudalism - Biomes-0.2.0.jar`. Put it and the supplied Lithostitched JAR into the `mods/` directory of a matching NeoForge installation. The JAR in the repository's `libs/` directory is the Lithostitched dependency, not the compiled Fantasy Feudalism mod. Integration-test classes and their arena are excluded from the distributable.

For dedicated development servers use `./gradlew runServer`; review Minecraft's EULA and configure the server before starting. No normal server EULA is accepted by this project automatically.

## Find Rootlands

Create a **new default Overworld** and use:

```mcfunction
/locate biome fantasy_feudalism_biomes:rootlands
```

Use the coordinates returned by the command to teleport there. Cheats/operator permissions are required. Rootlands takes weighted portions of vanilla dark-forest territory, retaining vanilla terrain shape and surrounding biomes. The default relative weight is 35 against vanilla's 100, **not 35% of the world**. Existing explored chunks are not regenerated. Superflat does not provide the normal climate-based biome distribution.

## Appearance and generation

- Dark olive grass with a restrained warm/brown cast; dark green foliage, muted water and fog colors.
- Noise-based patches of rooted dirt and coarse dirt on the surface.
- Procedural trees roughly 20–30 blocks tall, with thick lower trunks, stepped bends, rising sharp branches and irregular foliage clusters. Branch tips extend beyond the leaves.
- Ground-following dark-oak root clusters with occasional stripped sections and raised knuckles.
- Rough thorn clusters and walls one to three blocks tall. The thorn block has its own model and contact behavior, and appears in the Natural Blocks creative tab.
- Root Worms: neutral creatures that retaliate when attacked, prefer root-heavy ground, and visually nibble without consuming blocks. They are weak to fire/lava and flee nearby heat.
- Root Flowers: small, gaunt brown bushes with tiny red and white blossoms, configurable contact damage and slowing.
- Ordinary Overworld ores, caves, springs and forest wildlife. Vanilla woodland mansions are permitted in the biome.

Trees and roots use vanilla dark oak materials in this first implementation. The design is generated in Java, so no external structure templates are needed. The biome overrides leaf tint; grass colors vary with Minecraft's biome blending settings. The final appearance should be reviewed in a client before treating its visual tuning as final.

## Configuration

The first launch creates `config/fantasy_feudalism_biomes-common.toml` under the game directory (the dev run uses `run/`). Restart the game/server after editing. Worldgen changes affect newly generated chunks.

| Setting under `[rootlands]` | Default | Meaning |
| --- | --- | --- |
| `enabled` | `true` | Allow Rootlands to be selected during generation. |
| `biomeWeight` | `35` | Relative placement weight; 0 disables selection. |
| `treeDensity` | `5` | Tree placement attempts per biome chunk. |
| `rootDensity` | `8` | Root-cluster placement attempts per chunk. |
| `thornDensity` | `10` | Thorn cluster/wall attempts per chunk. |
| `structureFrequency` | `1.0` | Multiply tree, root and thorn attempts; 0 disables them. Surface dirt patches remain. |
| `structureVariants` | `true` | Vary tree height/lean, branch lengths, root layouts and thorn dimensions. False uses base shapes; placement locations still vary. |
| `thornDamageEnabled` | `true` | Contact damage to living entities. |
| `thornDamage` | `1.0` | Health points per hit; 2 points = one heart. Vanilla damage cooldown applies. |
| `thornSlowEnabled` | `true` | Slow living entities on contact, independently of damage. |

Density settings count **attempts**, not guaranteed placements: unsuitable ground, water and occupied tree space cause attempts to be skipped. Server settings control authoritative damage and generation; keep client common settings consistent for movement prediction.

## Exact implementation paths

Paths below are relative to the repository root:

- Biome: `src/main/resources/data/fantasy_feudalism_biomes/worldgen/biome/rootlands.json`.
- Configured and placed features: corresponding `worldgen/configured_feature/` and `worldgen/placed_feature/` directories; IDs are `jagged_trees`, `exposed_roots`, `thorn_patches`.
- Ground rule: `src/main/resources/data/fantasy_feudalism_biomes/lithostitched/surface_rule/rootlands.json`.
- Ground-rule injection: `src/main/resources/data/fantasy_feudalism_biomes/lithostitched/worldgen_modifier/rootlands_surface.json`.
- Biome selection: `src/main/java/dev/fantasyfeudalism/biomes/worldgen/RootlandsIntegration.java`.
- Tree geometry: `src/main/java/dev/fantasyfeudalism/biomes/worldgen/JaggedTree.java`.
- Root/thorn layouts and surface scattering: `src/main/java/dev/fantasyfeudalism/biomes/worldgen/RootlandsScatterFeature.java`.
- Thorn behavior: `src/main/java/dev/fantasyfeudalism/biomes/block/ThornBlock.java`.
- Thorn model: `src/main/resources/assets/fantasy_feudalism_biomes/models/block/rootlands_thorn.json`.

`tools/generate_resources.py` deterministically writes biome, feature, model, loot and tag JSON. Change it and rerun `python3 tools/generate_resources.py` when adjusting those resources; do not hand-edit generated JSON and then overwrite it with the generator. Modify Java tree/root algorithms directly to replace procedural shapes. New custom materials or template structures can be added later while keeping the stable biome ID.

## Validation

`runGameTestServer` runs fourteen integration tests inside Minecraft: tall tree geometry/foliage, atomic rejection of obstructed trunks, root/thorn placement and soil changes, configured thorn damage, zero-frequency generation, real Lithostitched biome selection plus registry/surface-rule decoding, and actual placed-feature decoration restricted to Rootlands. The additional tests cover worm neutrality/retaliation, fire/lava weakness, escape path planning and the heat toggle, root preference without destruction, flower contact categories/damage, actual movement reduction, and patch size/frequency. They are dev-only tests and are not a replacement for an in-client visual review or broad multi-seed performance testing.

Current validation: the Java 21 / NeoForge 21.1.262 build passed and all **14 required GameTests passed** with the supplied Lithostitched/Apollib runtime. Client rendering and a visual playthrough have not yet been checked.

Build scaffolding is based on NeoForgeMDKs/MDK-1.21.1-ModDevGradle at commit `e0000cb81c6ca4bb218046cfd8ade771a42e9c32`; its license is retained in `TEMPLATE_LICENSE.txt`.


## Root Worm (0.2.0)

Root Worms spawn in Rootlands in groups of 1–3. Their spawn checks favor root-rich ground; they also wander toward dense roots. They do not proactively target players, and retaliate only after being attacked. Their bark-brown segmented model slithers when moving and its jaw nibbles when feeding near roots. Feeding is cosmetic: **no root, soil or structure blocks are consumed**, regardless of `mobGriefing`.

Use the creative spawn egg or:

```mcfunction
/summon fantasy_feudalism_biomes:root_worm ~ ~ ~
```

Settings under `[rootWorm]` in the common configuration:

| Setting | Default | Meaning |
| --- | --- | --- |
| `fireWeaknessEnabled` | `true` | Enable extra damage from fire and lava. |
| `fireDamageMultiplier` | `2.0` | Multiply non-lava fire damage. |
| `lavaDamageMultiplier` | `2.0` | Multiply lava damage, without stacking the fire multiplier. |
| `avoidHeat` | `true` | Flee heat and panic when burning. Escape still depends on reachable terrain. |
| `additionalHeatSources` | torch, soul torch | Additional block IDs the worm fears. |

Fire and lava are always recognized as heat sources; lit campfires, soul campfires and magma blocks are included through `data/fantasy_feudalism_biomes/tags/block/root_worm_heat_sources.json`. Unlit blocks with a `lit` property are not treated as active heat sources. Add further heat sources using the config or a datapack tag. Root attraction uses the `root_worm_food` block tag, which includes rooted dirt and our dark oak root materials.

Entity behavior lives in `src/main/java/dev/fantasyfeudalism/biomes/entity/`; the original model, rendering and animation live in `client/RootWormModel.java` and `client/RootWormRenderer.java`. The deterministic texture is `assets/fantasy_feudalism_biomes/textures/entity/root_worm.png` under the resources directory. No Regions Unexplored assets or entity implementations are copied.

## Root Flower (0.2.0)

Root Flowers generate in small Rootlands patches and survive on dirt-family blocks, including rooted/coarse dirt. They have no berry growth stages. Their white/red blooms and thin dry stems fit between roots and thorns. They can be collected and placed, or obtained from Natural Blocks in creative:

```mcfunction
/give @s fantasy_feudalism_biomes:root_flower
```

Settings under `[rootFlower]`:

| Setting | Default | Meaning |
| --- | --- | --- |
| `patchFrequency` | `2.0` | Patch attempts per Rootlands chunk; fractional values are supported; 0 disables patches. |
| `minimumPatchSize` | `3` | Minimum number of plants attempted per patch. |
| `maximumPatchSize` | `7` | Maximum plants attempted; reversed min/max values are normalized. |
| `damage` | `1.0` | Contact damage in health points; 0 disables it. |
| `slowAmount` | `0.2` | Horizontal movement reduction: 0.2 means 20%; 0 disables slowing. |
| `affectPlayers` | `true` | Apply damage and slowing to players. |
| `affectPassiveMobs` | `true` | Apply effects to passive/neutral mobs, including Root Worms. |
| `affectHostileMobs` | `true` | Apply effects to mobs implementing Minecraft's hostile `Enemy` classification. |

Disabled categories receive neither effect. Vanilla damage cooldowns, creative/spectator protections and damage immunity still apply. Patch sizes are placement targets; obstructions, unsuitable ground and biome boundaries can reduce the actual count. Flower frequency is independent of Rootlands' tree/root/thorn `structureFrequency`.

Exact resource IDs: `worldgen/configured_feature/root_flowers.json`, `worldgen/placed_feature/root_flowers.json` and `loot_table/blocks/root_flower.json` under `data/fantasy_feudalism_biomes/`. The model is `assets/fantasy_feudalism_biomes/models/block/root_flower.json`. Change `tools/generate_resources.py` and regenerate to modify the patch resource data, original flower geometry or original worm texture.
