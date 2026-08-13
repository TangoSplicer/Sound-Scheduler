from collections import deque
from pathlib import Path
from PIL import Image

PROJECT = Path('/home/ubuntu/Sound-Scheduler')
SOURCE = PROJECT / 'design' / 'sound_scheduler_icon.png'
MASTER = PROJECT / 'design' / 'sound_scheduler_icon_transparent.png'
DENSITIES = {
    'mipmap-mdpi': 48,
    'mipmap-hdpi': 72,
    'mipmap-xhdpi': 96,
    'mipmap-xxhdpi': 144,
    'mipmap-xxxhdpi': 192,
}

image = Image.open(SOURCE).convert('RGBA')
pixels = image.load()
width, height = image.size
visited = set()
queue = deque()

for x in range(width):
    queue.append((x, 0))
    queue.append((x, height - 1))
for y in range(height):
    queue.append((0, y))
    queue.append((width - 1, y))

while queue:
    x, y = queue.popleft()
    if (x, y) in visited or not (0 <= x < width and 0 <= y < height):
        continue
    visited.add((x, y))
    r, g, b, a = pixels[x, y]
    if a == 0 or min(r, g, b) < 238:
        continue
    pixels[x, y] = (r, g, b, 0)
    queue.extend(((x - 1, y), (x + 1, y), (x, y - 1), (x, y + 1)))

image.save(MASTER, 'PNG')
for directory_name, size in DENSITIES.items():
    destination = PROJECT / 'app' / 'src' / 'main' / 'res' / directory_name / 'ic_launcher_art.png'
    destination.parent.mkdir(parents=True, exist_ok=True)
    image.resize((size, size), Image.Resampling.LANCZOS).save(destination, 'PNG', optimize=True)
print(f'Wrote {MASTER} and {len(DENSITIES)} launcher assets.')
