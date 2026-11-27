"""Offline ASTRA UI sanity check. Only for an empty disposable CI emulator."""
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET

PKG = 'ru.dayplanner.astra'
TITLE = 'AstraUpgradeCheck'


def shell(*argv):
    return subprocess.check_output(['adb', 'shell', *argv], text=True, timeout=25)


def wait_node(predicate, description):
    deadline = time.monotonic() + 45
    while time.monotonic() < deadline:
        try:
            out = shell('uiautomator', 'dump', '/data/local/tmp/astra-ui.xml')
            if 'dumped' in out.lower():
                nodes = ET.fromstring(shell('cat', '/data/local/tmp/astra-ui.xml')).iter('node')
                for node in nodes:
                    if node.get('package') == PKG and predicate(node):
                        return node
        except (ET.ParseError, subprocess.CalledProcessError, subprocess.TimeoutExpired):
            pass
        time.sleep(.6)
    raise RuntimeError(f'Не найден элемент ASTRA: {description}')


def label(text):
    return wait_node(lambda n: text in (n.get('text'), n.get('content-desc')), text)


def tap(node):
    x1, y1, x2, y2 = map(int, re.findall(r'\d+', node.attrib['bounds']))
    shell('input', 'tap', str((x1+x2)//2), str((y1+y2)//2))


def launch():
    shell('am', 'start', '-W', '-n', PKG + '/ru.dayplanner.app.MainActivity')


if __name__ == '__main__':
    launch()
    if sys.argv[1] == 'seed':
        label('На этот день дел нет')
        tap(label('Добавить дело'))
        label('Новое дело')
        tap(wait_node(lambda n: n.get('class') == 'android.widget.EditText', 'первое поле ввода'))
        shell('input', 'text', TITLE)
        tap(label('Сохранить'))
        label(TITLE)
        print('Тестовое дело создано и появилось в списке.')
    elif sys.argv[1] == 'verify':
        shell('am', 'force-stop', PKG)
        launch()
        tap(label(TITLE))
        label('Редактировать')
        label(TITLE)
        print('Сохранённая карточка и команда редактирования найдены после перезапуска.')
    else:
        raise ValueError('Ожидались seed или verify')
