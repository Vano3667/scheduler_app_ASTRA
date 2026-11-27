"""Smoke test on the clean GitHub-hosted emulator. Never connects to a user's phone."""
import re
import subprocess
import time
import xml.etree.ElementTree as ET


def shell(*cmd):
    return subprocess.check_output(['adb', 'shell', *cmd], text=True, timeout=20)


def wait_node(label):
    deadline = time.monotonic() + 45
    while time.monotonic() < deadline:
        dump = shell('uiautomator', 'dump', '/data/local/tmp/astra-ui.xml')
        if 'dumped' in dump.lower():
            root = ET.fromstring(shell('cat', '/data/local/tmp/astra-ui.xml'))
            for node in root.iter('node'):
                if node.get('package') == 'ru.dayplanner.astra' and label in (node.get('text'), node.get('content-desc')):
                    return node
        time.sleep(1)
    raise RuntimeError(f'Не найден элемент экрана ASTRA: {label}')


def tap(node):
    x1, y1, x2, y2 = map(int, re.findall(r'\d+', node.attrib['bounds']))
    shell('input', 'tap', str((x1+x2)//2), str((y1+y2)//2))


wait_node('На этот день дел нет')
tap(wait_node('Добавить дело'))
wait_node('Новое дело')
shell('input', 'keyevent', '4')
wait_node('На этот день дел нет')
print('APK установлен, открыл экран дня и пустой редактор, возврат на день прошёл (Android 35).')
