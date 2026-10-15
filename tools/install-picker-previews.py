#!/usr/bin/env python3
"""Install real-renderer output as widget-picker preview resources after tests.
Run ./gradlew :app:testDebugUnitTest first. No image-generation dependency.
"""
from pathlib import Path
import shutil
root = Path(__file__).resolve().parents[1]
source = root / 'app/build/design-review'
target = root / 'app/src/main/res/drawable-nodpi'
target.mkdir(exist_ok=True)
mapping = dict(driver='driver', constructor='constructor', driverstandings='driver_standings', constructorstandings='constructor_standings', weekend='weekend', countdown='countdown', results='results', editorial='editorial', glass='glass', vinyl='vinyl', brutalist='brutalist', matrix='matrix', cassette='cassette', compact='compact', clock='clock', date='date', battery='battery', agenda='agenda', swiss='swiss')
for provider, kind in mapping.items():
    image = source / f'{kind}.png'
    if not image.exists():
        raise SystemExit(f'Missing {image}; run renderer tests first.')
    shutil.copyfile(image, target / f'preview_{provider}.png')
    path = root / f'app/src/main/res/xml/provider_{provider}.xml'
    text = path.read_text()
    if 'android:previewImage=' not in text:
        text = text.replace('android:initialLayout=', f'android:previewImage="@drawable/preview_{provider}" android:initialLayout=')
        path.write_text(text)
print(f'Installed {len(mapping)} previews from the shared Canvas renderer.')
