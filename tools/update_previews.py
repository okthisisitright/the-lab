"""Copy tested renderer output to the actual Android widget-picker assets."""
from pathlib import Path
import re,shutil
root=Path(__file__).resolve().parents[1]
keys=re.findall(r'^    (\w+)\("[^"]+",Collection\.', (root/'app/src/main/java/dev/widgetlab/core/Catalog.kt').read_text(),re.M)
assert len(keys)==50
source=root/'app/build/design-review'
for key in keys:
    path=source/f'picker-{key.lower()}.png'
    assert path.stat().st_size>100,path
for key in keys:
    shutil.copyfile(source/f'picker-{key.lower()}.png',root/f'app/src/main/res/drawable-nodpi/preview_{key.lower().replace("_","")}.png')
for path in source.glob('collection-*.png'):
    shutil.copyfile(path,root/'docs/previews'/path.name)
print('Updated 50 actual picker previews and six review sheets from tested renderer output.')
