"""Deterministic first-biome resources; run with Python 3 (no dependencies)."""
import json
from pathlib import Path
ROOT = Path(__file__).resolve().parents[1] / 'src/main/resources'
NS = 'fantasy_feudalism_biomes'

def write(path, value):
    dst = ROOT / path
    dst.parent.mkdir(parents=True, exist_ok=True)
    dst.write_text(json.dumps(value, indent=2) + '\n')

def data(path, value):
    write(f'data/{NS}/{path}.json', value)

def block(name):
    return {'type': 'minecraft:block', 'result_state': {'Name': name}}

def condition(test, result):
    return {'type': 'minecraft:condition', 'if_true': test, 'then_run': result}

write('pack.mcmeta', {'pack': {'pack_format': 48, 'description': 'Fantasy Feudalism - Biomes — Rootlands'}})
write(f'assets/{NS}/lang/en_us.json', {
    f'biome.{NS}.rootlands': 'Rootlands', f'block.{NS}.rootlands_thorn': 'Thorn'
})
features = [[] for _ in range(11)]
features[1] = ['minecraft:lake_lava_underground', 'minecraft:lake_lava_surface']
features[2] = ['minecraft:amethyst_geode']
features[3] = ['minecraft:monster_room', 'minecraft:monster_room_deep']
features[6] = ['minecraft:' + name for name in [
    'ore_dirt', 'ore_gravel', 'ore_granite_upper', 'ore_granite_lower', 'ore_diorite_upper', 'ore_diorite_lower',
    'ore_andesite_upper', 'ore_andesite_lower', 'ore_tuff', 'ore_coal_upper', 'ore_coal_lower',
    'ore_iron_upper', 'ore_iron_middle', 'ore_iron_small', 'ore_gold', 'ore_gold_lower', 'ore_redstone',
    'ore_redstone_lower', 'ore_diamond', 'ore_diamond_medium', 'ore_diamond_large', 'ore_diamond_buried',
    'ore_lapis', 'ore_lapis_buried', 'ore_copper', 'underwater_magma', 'disk_sand', 'disk_clay', 'disk_gravel']]
features[8] = ['minecraft:spring_water', 'minecraft:spring_lava']
features[9] = [f'{NS}:jagged_trees', f'{NS}:exposed_roots', f'{NS}:thorn_patches', 'minecraft:patch_grass_forest',
               'minecraft:brown_mushroom_normal', 'minecraft:red_mushroom_normal']
features[10] = ['minecraft:freeze_top_layer']

def mob(kind, weight, lo, hi):
    return {'type': f'minecraft:{kind}', 'weight': weight, 'minCount': lo, 'maxCount': hi}

spawners = {
    'creature': [mob('sheep', 8, 2, 4), mob('pig', 5, 2, 3), mob('chicken', 6, 2, 4), mob('wolf', 5, 2, 4)],
    'monster': [mob('spider', 100, 4, 4), mob('zombie', 95, 4, 4), mob('zombie_villager', 5, 1, 1),
                mob('skeleton', 100, 4, 4), mob('creeper', 100, 4, 4), mob('slime', 100, 4, 4),
                mob('enderman', 10, 1, 4), mob('witch', 5, 1, 1)],
    'ambient': [mob('bat', 10, 8, 8)], 'underground_water_creature': [mob('glow_squid', 10, 4, 6)]
}
data('worldgen/biome/rootlands', {
    'has_precipitation': True, 'temperature': 0.55, 'downfall': 0.8,
    'effects': {'grass_color': 0x4C5034, 'foliage_color': 0x39452D, 'sky_color': 0x8995A3,
                'fog_color': 0x76806C, 'water_color': 0x465744, 'water_fog_color': 0x263729,
                'mood_sound': {'sound': 'minecraft:ambient.cave', 'tick_delay': 6000, 'block_search_extent': 8, 'offset': 2.0},
                'music': {'sound': 'minecraft:music.overworld.forest', 'min_delay': 12000, 'max_delay': 24000, 'replace_current_music': False}},
    'carvers': {'air': ['minecraft:cave', 'minecraft:cave_extra_underground', 'minecraft:canyon']},
    'features': features, 'spawners': spawners, 'spawn_costs': {}
})
for name in ['jagged_trees', 'exposed_roots', 'thorn_patches']:
    data('worldgen/configured_feature/' + name, {'type': f'{NS}:{name}', 'config': {}})
    # Scatter features do their own surface sampling and biome checks for each attempt.
    data('worldgen/placed_feature/' + name, {'feature': f'{NS}:{name}', 'placement': []})

soil_rule = condition({'type': 'lithostitched:all_of', 'conditions': [
    {'type': 'minecraft:biome', 'biome_is': [f'{NS}:rootlands']},
    {'type': 'minecraft:above_preliminary_surface'},
    {'type': 'minecraft:stone_depth', 'offset': 0, 'add_surface_depth': False, 'secondary_depth_range': 0, 'surface_type': 'floor'},
    {'type': 'minecraft:water', 'offset': -1, 'surface_depth_multiplier': 0, 'add_stone_depth': False}
]}, {'type': 'minecraft:sequence', 'sequence': [
    condition({'type': 'minecraft:noise_threshold', 'noise': 'minecraft:surface', 'min_threshold': 0.15, 'max_threshold': 0.6}, block('minecraft:rooted_dirt')),
    condition({'type': 'minecraft:noise_threshold', 'noise': 'minecraft:surface', 'min_threshold': -0.6, 'max_threshold': -0.1}, block('minecraft:coarse_dirt'))
]})
data('lithostitched/surface_rule/rootlands', soil_rule)
data('lithostitched/worldgen_modifier/rootlands_surface', {
    'type': 'lithostitched:add_surface_rule', 'injection_type': 'prepend', 'levels': ['minecraft:overworld'],
    'surface_rule': {'type': 'lithostitched:reference', 'rules': f'{NS}:rootlands'}
})
for namespace, tag in [('minecraft', 'is_overworld'), ('minecraft', 'is_forest'), ('c', 'is_spooky'), ('c', 'is_dense/overworld')]:
    write(f'data/{namespace}/tags/worldgen/biome/{tag}.json', {'replace': False, 'values': [f'{NS}:rootlands']})
# Permit vanilla woodland mansions, while leaving village placement untouched.
write('data/minecraft/tags/worldgen/biome/has_structure/woodland_mansion.json', {'replace': False, 'values': [f'{NS}:rootlands']})
write('data/minecraft/tags/block/mineable/axe.json', {'replace': False, 'values': [f'{NS}:rootlands_thorn']})
data('loot_table/blocks/rootlands_thorn', {'type': 'minecraft:block', 'pools': [{'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': f'{NS}:rootlands_thorn'}], 'conditions': [{'condition': 'minecraft:survives_explosion'}]}]})
# A bespoke sharp wooden silhouette built from vanilla bark textures; no external texture assets required.
faces = {side: {'texture': '#bark'} for side in ['north', 'south', 'east', 'west', 'up', 'down']}
elements = [
    {'from': [6, 0, 6], 'to': [10, 16, 10], 'faces': faces},
    {'from': [0, 5, 7], 'to': [16, 8, 9], 'rotation': {'origin': [8, 6, 8], 'axis': 'z', 'angle': 22.5}, 'faces': faces},
    {'from': [7, 9, 0], 'to': [9, 12, 16], 'rotation': {'origin': [8, 10, 8], 'axis': 'x', 'angle': -22.5}, 'faces': faces},
    {'from': [2, 8, 6], 'to': [4, 14, 8], 'rotation': {'origin': [3, 9, 7], 'axis': 'z', 'angle': -22.5}, 'faces': faces},
    {'from': [11, 3, 7], 'to': [13, 9, 9], 'rotation': {'origin': [12, 4, 8], 'axis': 'z', 'angle': 22.5}, 'faces': faces}
]
write(f'assets/{NS}/models/block/rootlands_thorn.json', {'parent': 'minecraft:block/block', 'textures': {'bark': 'minecraft:block/dark_oak_log', 'particle': 'minecraft:block/dark_oak_log'}, 'elements': elements})
write(f'assets/{NS}/blockstates/rootlands_thorn.json', {'variants': {'': [{'model': f'{NS}:block/rootlands_thorn', 'y': y} for y in [0, 90, 180, 270]]}})
write(f'assets/{NS}/models/item/rootlands_thorn.json', {'parent': f'{NS}:block/rootlands_thorn'})

# Root Flower: sparse skeletal stems with tiny mixed white/red blossoms.
lang_path = ROOT / f'assets/{NS}/lang/en_us.json'
lang = json.loads(lang_path.read_text())
lang.update({f'block.{NS}.root_flower': 'Root Flower', f'entity.{NS}.root_worm': 'Root Worm', f'item.{NS}.root_worm_spawn_egg': 'Root Worm Spawn Egg'})
write(f'assets/{NS}/lang/en_us.json', lang)
biome_path = ROOT / f'data/{NS}/worldgen/biome/rootlands.json'
biome = json.loads(biome_path.read_text())
biome['features'][9].append(f'{NS}:root_flowers')
biome['spawners']['creature'].append({'type': f'{NS}:root_worm', 'weight': 30, 'minCount': 2, 'maxCount': 4})
write(f'data/{NS}/worldgen/biome/rootlands.json', biome)
data('worldgen/configured_feature/root_flowers', {'type': f'{NS}:root_flowers', 'config': {}})
data('worldgen/placed_feature/root_flowers', {'feature': f'{NS}:root_flowers', 'placement': []})
write(f'data/{NS}/tags/block/root_worm_food.json', {'replace': False, 'values': ['minecraft:rooted_dirt', 'minecraft:dark_oak_log', 'minecraft:stripped_dark_oak_log', 'minecraft:mangrove_roots', 'minecraft:muddy_mangrove_roots']})
write(f'data/{NS}/tags/block/root_worm_heat_sources.json', {'replace': False, 'values': ['minecraft:campfire', 'minecraft:soul_campfire', 'minecraft:magma_block']})
data('loot_table/entities/root_worm', {'type': 'minecraft:entity', 'pools': []})
data('loot_table/blocks/root_flower', {'type': 'minecraft:block', 'pools': [{'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': f'{NS}:root_flower'}], 'conditions': [{'condition': 'minecraft:survives_explosion'}]}]})
write(f'assets/{NS}/models/item/root_worm_spawn_egg.json', {'parent': 'minecraft:item/template_spawn_egg'})
write(f'assets/{NS}/models/item/root_flower.json', {'parent': f'{NS}:block/root_flower'})
write(f'assets/{NS}/blockstates/root_flower.json', {'variants': {'': [{'model': f'{NS}:block/root_flower', 'y': angle} for angle in [0, 90, 180, 270]]}})

def cube(lo, hi, texture, rotation=None):
    result = {'from': lo, 'to': hi, 'faces': {side: {'texture': '#' + texture} for side in ['north', 'south', 'east', 'west', 'up', 'down']}}
    if rotation:
        result['rotation'] = rotation
    return result

stems = []
for x, z, height, angle in [(7, 8, 10, 0), (4, 6, 7, -22.5), (11, 9, 8, 22.5), (8, 12, 6, -22.5), (5, 11, 5, 22.5)]:
    rotation = {'origin': [x, 0, z], 'axis': 'z', 'angle': angle}
    stems.append(cube([x - .4, 0, z - .4], [x + .4, height, z + .4], 'stem', rotation))
    # Thin dry side twigs and tiny blossoms leave generous empty space in the silhouette.
    stems.append(cube([x - 1.6, height * .5, z - .25], [x + 1.6, height * .5 + .5, z + .25], 'stem', rotation))
    for dx, color in [(-.9, 'white'), (.9, 'red')]:
        stems.append(cube([x + dx - .7, height - .2, z - .45], [x + dx + .7, height + .25, z + .45], color, rotation))
        stems.append(cube([x + dx - .35, height - .2, z - .9], [x + dx + .35, height + .25, z + .9], color, rotation))
write(f'assets/{NS}/models/block/root_flower.json', {'parent': 'minecraft:block/block', 'ambientocclusion': False,
    'textures': {'stem': 'minecraft:block/brown_terracotta', 'white': 'minecraft:block/white_wool', 'red': 'minecraft:block/red_wool', 'particle': 'minecraft:block/brown_terracotta'}, 'elements': stems})

# Original 64x32 bark-brown entity atlas, generated deterministically without image dependencies.
import random
import struct
import zlib
rng = random.Random(761)
pixels = []
for y in range(32):
    row = bytearray()
    for x in range(64):
        shade = rng.randint(-9, 9) + (-12 if x % 7 in [0, 1] else 0)
        color = (max(0, 94 + shade), max(0, 69 + shade), max(0, 43 + shade), 255)
        if x >= 48 and y < 5:
            color = (185, 146, 77, 255) if x % 4 else (38, 22, 13, 255)
        if x >= 48 and 8 <= y < 14:
            color = (48, 27, 21, 255) if y < 11 else (128, 91, 51, 255)
        if x >= 40 and 16 <= y < 23:
            color = (23, 13, 10, 255) if (x + y) % 4 else (35, 21, 15, 255)
        if x >= 48 and y >= 24:
            color = (206, 186, 140, 255) if y % 3 else (167, 145, 100, 255)
        row.extend(color)
    pixels.append(b'\0' + bytes(row))

def chunk(kind, payload):
    return struct.pack('>I', len(payload)) + kind + payload + struct.pack('>I', zlib.crc32(kind + payload) & 0xffffffff)

png = b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', 64, 32, 8, 6, 0, 0, 0)) + chunk(b'IDAT', zlib.compress(b''.join(pixels))) + chunk(b'IEND', b'')
texture = ROOT / f'assets/{NS}/textures/entity/root_worm.png'
texture.parent.mkdir(parents=True, exist_ok=True)
texture.write_bytes(png)

# Low-frequency, continuous biome-selection mask. Never connected to the terrain noise router.
data('worldgen/noise/rootlands_area', {'firstOctave': -10, 'amplitudes': [1.0, 0.35]})

# Shore grading is the first generation feature, before lakes, structures and vegetation.
biome = json.loads(biome_path.read_text())
# The runtime modifier adds it once to all Overworld biomes so it also grades neighboring water chunks.
write(f'data/{NS}/worldgen/biome/rootlands.json', biome)
data('worldgen/configured_feature/shoreline', {'type': f'{NS}:shoreline', 'config': {}})
data('worldgen/placed_feature/shoreline', {'feature': f'{NS}:shoreline', 'placement': []})
lang = json.loads(lang_path.read_text())
lang[f'itemGroup.{NS}'] = 'Fantasy Feudalism - Biomes'
write(f'assets/{NS}/lang/en_us.json', lang)

# Detailed Rootlands vegetation and renewable tree materials.
lang = json.loads(lang_path.read_text())
lang.update({f'block.{NS}.rootlands_sapling': 'Rootlands Sapling', f'block.{NS}.rootlands_leaves': 'Rootlands Leaves', f'block.{NS}.dark_grass': 'Dark Grass'})
write(f'assets/{NS}/lang/en_us.json', lang)
biome = json.loads(biome_path.read_text())
biome['effects'].update({'fog_color': 0x3E493F, 'sky_color': 0x5C6871, 'foliage_color': 0x253B29, 'water_fog_color': 0x1A2C22})
biome['features'][9] = [f'{NS}:jagged_trees', f'{NS}:exposed_roots', f'{NS}:giant_thorns', f'{NS}:thorn_patches', f'{NS}:dark_grass', 'minecraft:brown_mushroom_normal', 'minecraft:red_mushroom_normal', f'{NS}:root_flowers']
write(f'data/{NS}/worldgen/biome/rootlands.json', biome)
for name in ['giant_thorns', 'dark_grass', 'sapling_tree']:
    data('worldgen/configured_feature/' + name, {'type': f'{NS}:{name}', 'config': {}})
    if name != 'sapling_tree':
        data('worldgen/placed_feature/' + name, {'feature': f'{NS}:{name}', 'placement': []})

# Keep standard dark-oak log materials; custom leaves let these trees drop their own sapling.
write(f'assets/{NS}/blockstates/rootlands_leaves.json', {'variants': {'': {'model': f'{NS}:block/rootlands_leaves'}}})
write(f'assets/{NS}/models/block/rootlands_leaves.json', {'parent': 'minecraft:block/leaves', 'render_type': 'minecraft:cutout_mipped', 'textures': {'all': 'minecraft:block/dark_oak_leaves'}})
write(f'assets/{NS}/models/item/rootlands_leaves.json', {'parent': f'{NS}:block/rootlands_leaves'})
for name in ['rootlands_sapling', 'dark_grass']:
    write(f'assets/{NS}/blockstates/{name}.json', {'variants': {'': {'model': f'{NS}:block/{name}'}}})
    write(f'assets/{NS}/models/block/{name}.json', {'parent': 'minecraft:block/cross', 'render_type': 'minecraft:cutout', 'textures': {'cross': f'{NS}:block/{name}'}})
    write(f'assets/{NS}/models/item/{name}.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'{NS}:block/{name}'}})

for tag_name, value in [('leaves', 'rootlands_leaves'), ('mineable/hoe', 'rootlands_leaves'), ('saplings', 'rootlands_sapling'), ('replaceable', 'dark_grass')]:
    write(f'data/minecraft/tags/block/{tag_name}.json', {'replace': False, 'values': [f'{NS}:{value}']})
for tag_name, value in [('leaves', 'rootlands_leaves'), ('saplings', 'rootlands_sapling')]:
    write(f'data/minecraft/tags/item/{tag_name}.json', {'replace': False, 'values': [f'{NS}:{value}']})
for name in ['dark_grass', 'rootlands_sapling']:
    data(f'loot_table/blocks/{name}', {'type': 'minecraft:block', 'pools': [{'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': f'{NS}:{name}'}], 'conditions': [{'condition': 'minecraft:survives_explosion'}]}]})
shears_or_silk = {'condition': 'minecraft:any_of', 'terms': [
    {'condition': 'minecraft:match_tool', 'predicate': {'items': 'minecraft:shears'}},
    {'condition': 'minecraft:match_tool', 'predicate': {'predicates': {'minecraft:enchantments': [{'enchantments': 'minecraft:silk_touch', 'levels': {'min': 1}}]}}}
]}
data('loot_table/blocks/rootlands_leaves', {'type': 'minecraft:block', 'pools': [{'rolls': 1, 'entries': [{'type': 'minecraft:alternatives', 'children': [
    {'type': 'minecraft:item', 'name': f'{NS}:rootlands_leaves', 'conditions': [shears_or_silk]},
    {'type': 'minecraft:item', 'name': f'{NS}:rootlands_sapling', 'conditions': [{'condition': 'minecraft:random_chance', 'chance': 0.075}, {'condition': 'minecraft:survives_explosion'}]}
]}]}]})

# Orient the same damaging thorn along each growth direction; its registry ID is retained for existing saves.
write(f'assets/{NS}/blockstates/rootlands_thorn.json', {'variants': {
    'axis=y': {'model': f'{NS}:block/rootlands_thorn'},
    'axis=x': {'model': f'{NS}:block/rootlands_thorn', 'x': 90, 'y': 90},
    'axis=z': {'model': f'{NS}:block/rootlands_thorn', 'x': 90}
}})
thorn_model = json.loads((ROOT / f'assets/{NS}/models/block/rootlands_thorn.json').read_text())
thorn_model['textures'] = {'bark': f'{NS}:block/thorn_bark', 'particle': f'{NS}:block/thorn_bark'}
write(f'assets/{NS}/models/block/rootlands_thorn.json', thorn_model)

# Original deterministic pixel textures for the dark grass plant, sapling and almost-black thorn bark.
def save_pixels(name, width, height, pixel):
    rows = b''.join(b'\0' + b''.join(bytes(pixel(x, y)) for x in range(width)) for y in range(height))
    payload = b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', width, height, 8, 6, 0, 0, 0)) + chunk(b'IDAT', zlib.compress(rows)) + chunk(b'IEND', b'')
    dst = ROOT / f'assets/{NS}/textures/block/{name}.png'
    dst.parent.mkdir(parents=True, exist_ok=True)
    dst.write_bytes(payload)

def grass_pixel(x, y):
    blades = [(2, 7, -1), (5, 3, 1), (8, 1, 0), (11, 5, -1), (14, 8, 1)]
    for base, top, bend in blades:
        if y >= top and abs(x - (base + bend * (15 - y) // 6)) <= (1 if y > 11 else 0):
            return (25 + (x * 5 + y) % 14, 42 + y % 9, 27 + x % 8, 255)
    return (0, 0, 0, 0)

def sapling_pixel(x, y):
    if (7 <= x <= 8 and y >= 6) or (y >= 11 and abs(x - 8) <= (15 - y) // 2): return (58, 42, 29, 255)
    for cx, cy in [(5, 5), (9, 3), (11, 7), (5, 9)]:
        if abs(x - cx) + abs(y - cy) < 4 and (x + y) % 5 != 0: return (34 + x % 8, 56 + y % 12, 35 + y % 7, 255)
    return (0, 0, 0, 0)

save_pixels('dark_grass', 16, 16, grass_pixel)
save_pixels('rootlands_sapling', 16, 16, sapling_pixel)
save_pixels('thorn_bark', 16, 16, lambda x, y: (19 + (x * 3 + y * 7) % 10, 15 + (x + y) % 8, 19 + (x * 2 + y) % 9, 255))

# Root thorns attach to bark-covered wood on any of its six faces, including the underside of arches.
write(f'data/{NS}/tags/block/thorn_supports.json', {'replace': False, 'values': ['minecraft:dark_oak_wood']})
food_path = ROOT / f'data/{NS}/tags/block/root_worm_food.json'
food = json.loads(food_path.read_text())
food['values'].append('minecraft:dark_oak_wood')
write(f'data/{NS}/tags/block/root_worm_food.json', food)

thorn_faces = {side: {'texture': '#bark'} for side in ['north', 'south', 'east', 'west', 'up', 'down']}
lean = {'origin': [8, 3, 8], 'axis': 'z', 'angle': 22.5}
write(f'assets/{NS}/models/block/rootlands_thorn.json', {'parent': 'minecraft:block/block',
    'textures': {'bark': f'{NS}:block/thorn_bark', 'particle': f'{NS}:block/thorn_bark'},
    'elements': [
        {'from': [5, 0, 5], 'to': [11, 3, 11], 'faces': thorn_faces},
        {'from': [6, 2, 6], 'to': [10, 7, 10], 'rotation': lean, 'faces': thorn_faces},
        {'from': [7, 6, 7], 'to': [9, 11, 9], 'rotation': lean, 'faces': thorn_faces},
        {'from': [7.5, 10, 7.5], 'to': [8.5, 14, 8.5], 'rotation': lean, 'faces': thorn_faces}
    ]})
# Axis is retained in saved states for compatibility; facing is the model/attachment direction.
write(f'assets/{NS}/blockstates/rootlands_thorn.json', {'variants': {
    'facing=up': {'model': f'{NS}:block/rootlands_thorn'},
    'facing=down': {'model': f'{NS}:block/rootlands_thorn', 'x': 180},
    'facing=north': {'model': f'{NS}:block/rootlands_thorn', 'x': 90},
    'facing=south': {'model': f'{NS}:block/rootlands_thorn', 'x': 90, 'y': 180},
    'facing=east': {'model': f'{NS}:block/rootlands_thorn', 'x': 90, 'y': 90},
    'facing=west': {'model': f'{NS}:block/rootlands_thorn', 'x': 90, 'y': 270}
}})

# A simpler friendly face, with detail concentrated in the worm's bark, moss and segment rings.
# Atlas areas match RootWormModel: dark eyes, ivory highlights, small mouth, warm cheeks and muted moss.
def worm_pixel(x, y):
    grain = ((x * 13 + y * 7) % 17) - 8
    shade = grain - (10 if x % 7 in [0, 1] else 0)
    color = (94 + shade, 69 + shade, 43 + shade, 255)
    if x < 40 and y >= 24:
        color = (115 + grain, 87 + grain, 54 + grain, 255) if y % 3 else (68, 47, 30, 255)
    if 48 <= x < 56 and y < 7: color = (25, 23, 18, 255)
    if x >= 56 and y < 7: color = (244, 237, 215, 255)
    if 48 <= x < 56 and 8 <= y < 14: color = (46, 29, 19, 255)
    if x >= 56 and 8 <= y < 14: color = (147, 104, 77, 255)
    if x >= 48 and 16 <= y < 24: color = (48 + grain // 2, 68 + grain // 2, 35 + grain // 2, 255)
    return color

# The common PNG writer's block directory is used for plants; the worm has a separate entity atlas.
rows = b''.join(b'\0' + b''.join(bytes(worm_pixel(x, y)) for x in range(64)) for y in range(32))
png = b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', 64, 32, 8, 6, 0, 0, 0)) + chunk(b'IDAT', zlib.compress(rows)) + chunk(b'IEND', b'')
(ROOT / f'assets/{NS}/textures/entity/root_worm.png').write_bytes(png)
