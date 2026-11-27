"""Offline ASTRA UI sanity check. Only for an empty disposable CI emulator."""
import io
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


def restart_day():
    shell('am', 'force-stop', PKG)
    launch()
    label('Добавить дело')


def open_settings():
    tap(label('Настройки'))
    label('Тема приложения')


def set_system_night(mode):
    if mode not in ('no', 'yes'):
        raise ValueError(mode)
    shell('cmd', 'uimode', 'night', mode)
    status = shell('cmd', 'uimode', 'night').strip().lower()
    if not re.search(rf'\b{mode}\b', status):
        raise RuntimeError(f'Android не подтвердил night={mode}: {status}')


def center_luma():
    # Loaded only in the optional theme step. seed/verify need no image dependencies.
    from PIL import Image, ImageStat
    png = subprocess.check_output(['adb', 'exec-out', 'screencap', '-p'], timeout=15)
    with Image.open(io.BytesIO(png)) as image:
        w, h = image.size
        # Ignore system bars, cutouts, screen edges, transient button highlights and labels.
        region = image.crop((w//6, h//3, 5*w//6, 2*h//3)).convert('L')
        return ImageStat.Stat(region).median[0]


def expect_palette(screen_marker, *, dark, case):
    label(screen_marker)  # do not mistake a blank, locked or crashed screen for a dark theme
    readings = []
    streak = 0
    for _ in range(10):
        y = center_luma()
        readings.append(y)
        ok = y <= 90 if dark else y >= 165
        streak = streak + 1 if ok else 0
        if streak == 2:
            print(f'{case}: median Y={readings[-2:]}', flush=True)
            return
        time.sleep(.35)
    raise AssertionError(f'{case}: ожидалась {"тёмная" if dark else "светлая"} палитра, медиана Y={readings}')


def style_selected(text):
    # Compose may expose the selected/checkable chip and its text as one node or as parent/child.
    wait_node(lambda n: (n.get('selected') == 'true' or n.get('checked') == 'true')
              and any(text in (v.get('text'), v.get('content-desc')) for v in n.iter('node')),
              f'выбран стиль «{text}»')


def check_theme():
    settings = 'Тема приложения'
    day = 'Добавить дело'
    set_system_night('no')
    try:
        restart_day()
        open_settings()
        tap(label('Обычный'))
        style_selected('Обычный')
        tap(label('Как в системе'))
        expect_palette(settings, dark=False, case='Светлая система, обычный стиль')
        tap(label('Тёмная'))
        expect_palette(settings, dark=True, case='Ручная тёмная при светлой системе')
        shell('input', 'keyevent', '4')
        expect_palette(day, dark=True, case='Тёмный экран дня')
        restart_day()
        expect_palette(day, dark=True, case='Тёмная сохранилась после перезапуска')
        open_settings()
        tap(label('С эффектами'))
        style_selected('С эффектами')
        expect_palette(settings, dark=True, case='Тёмная со включёнными эффектами')
        set_system_night('yes')
        restart_day()
        open_settings()
        style_selected('С эффектами')
        tap(label('Светлая'))
        expect_palette(settings, dark=False, case='Ручная светлая при тёмной системе')
        restart_day()
        expect_palette(day, dark=False, case='Светлая сохранилась после перезапуска')
        open_settings()
        tap(label('Как в системе'))
        expect_palette(settings, dark=True, case='Возврат к системной тёмной')
        shell('input', 'keyevent', '4')
        label(day)
        set_system_night('no')
        expect_palette(day, dark=False, case='Системный режим отреагировал на смену темы Android')
        print('Темы проверены на реальных кадрах эмулятора Android 35; оба стиля доступны.', flush=True)
    finally:
        set_system_night('no')  # CI emulator only; no settings of a user's phone are read or changed


if __name__ == '__main__':
    mode = sys.argv[1]
    if mode == 'seed':
        launch()
        label('На этот день дел нет')
        tap(label('Добавить дело'))
        label('Новое дело')
        tap(wait_node(lambda n: n.get('class') == 'android.widget.EditText', 'первое поле ввода'))
        shell('input', 'text', TITLE)
        tap(label('Сохранить'))
        label(TITLE)
        print('Тестовое дело создано и появилось в списке.')
    elif mode == 'verify':
        shell('am', 'force-stop', PKG)
        launch()
        tap(label(TITLE))
        label('Редактировать')
        label(TITLE)
        print('Сохранённая карточка и команда редактирования найдены после перезапуска.')
    elif mode == 'theme':
        check_theme()
    else:
        raise ValueError('Ожидались seed, verify или theme')
