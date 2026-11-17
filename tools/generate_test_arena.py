"""Generate the dev-only GameTest structure. Minecraft places it one block above the test anchor."""
import gzip
from pathlib import Path
import struct


def string(value):
    raw = value.encode()
    return struct.pack('>H', len(raw)) + raw


def integer(value):
    return struct.pack('>i', value)


def tag(kind, name, value):
    return bytes([kind]) + string(name) + value


def compound(tags):
    return b''.join(tags) + b'\0'


def integers(values):
    return b'\x03' + integer(len(values)) + b''.join(integer(x) for x in values)


blocks = []
for x in range(32):
    for z in range(32):
        for y in range(44):
            blocks.append(compound([tag(9, 'pos', integers([x, y, z])), tag(3, 'state', integer(1 if y == 0 else 0))]))
palette = [compound([tag(8, 'Name', string(name))]) for name in ['minecraft:air', 'minecraft:grass_block']]
root = compound([
    tag(3, 'DataVersion', integer(3955)), tag(9, 'size', integers([32, 44, 32])),
    tag(9, 'palette', b'\x0a' + integer(2) + b''.join(palette)),
    tag(9, 'blocks', b'\x0a' + integer(len(blocks)) + b''.join(blocks)),
    tag(9, 'entities', b'\x0a' + integer(0))
])
path = Path(__file__).resolve().parents[1] / 'src/main/resources/data/fantasy_feudalism_biomes/structure/test_arena.nbt'
path.parent.mkdir(parents=True, exist_ok=True)
path.write_bytes(gzip.compress(b'\x0a' + string('') + root, mtime=0))
