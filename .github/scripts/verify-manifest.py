"""Read decoded packaged manifest; never consult the unmerged source manifest."""
import sys
from xml.etree import ElementTree as ET

ns = '{http://schemas.android.com/apk/res/android}'
m = ET.parse(sys.argv[1]).getroot()
if m.tag != 'manifest': raise SystemExit('Некорректный манифест')
sdk = m.find('uses-sdk')
if sdk is None: raise SystemExit('В APK отсутствует uses-sdk')
checks = {'package': (m.get('package'), 'ru.dayplanner.astra'), 'versionCode': (m.get(ns + 'versionCode'), sys.argv[2]), 'minSdkVersion': (sdk.get(ns + 'minSdkVersion'), '29'), 'targetSdkVersion': (sdk.get(ns + 'targetSdkVersion'), '37')}
for name, (value, expected) in checks.items():
    print(f'{name}: {value} (ожидалось {expected})')
    if value != expected: raise SystemExit(f'Неверное значение {name}')
perms = {p.get(ns + 'name') for p in m if p.tag.startswith('uses-permission')}
if 'android.permission.RECEIVE_BOOT_COMPLETED' not in perms: raise SystemExit('Не найдены разрешения из упакованного манифеста')
blocked = {'android.permission.' + s for s in ('INTERNET','CAMERA','READ_EXTERNAL_STORAGE','READ_MEDIA_IMAGES','WRITE_EXTERNAL_STORAGE')}
if perms & blocked: raise SystemExit(f'Лишние разрешения: {sorted(perms & blocked)}')
print('Манифест упакованного APK: проверки пройдены.')
