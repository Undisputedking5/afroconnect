#!/usr/bin/env python3
"""Tiny UI driver for the CI emulator, built on `uiautomator dump`.

  ui.py tap "Text"            tap the element whose text / content-desc matches (scrolls to find it)
  ui.py type "Label" "value"  tap a field by its label, then type the value
  ui.py wait "Text" [secs]    wait until the text is on screen
  ui.py shot name             save a screenshot to shots/name.png
"""
import os
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET

PKG = "com.example.africonnect"


def adb(*args, capture=False):
    r = subprocess.run(["adb", *args], capture_output=True, text=not capture)
    return r.stdout


def dump():
    for _ in range(5):
        adb("shell", "uiautomator", "dump", "/sdcard/ui.xml")
        xml = adb("exec-out", "cat", "/sdcard/ui.xml")
        if xml and xml.strip().startswith("<?xml"):
            try:
                return ET.fromstring(xml[xml.index("<hierarchy"):])
            except Exception:
                pass
        time.sleep(1)
    return None


def find(target):
    root = dump()
    if root is None:
        return None
    nodes = list(root.iter("node"))
    t = target.lower()
    for exact in (True, False):
        for n in nodes:
            for attr in ("text", "content-desc"):
                v = (n.get(attr) or "").strip().lower()
                if v and (v == t if exact else t in v):
                    m = re.findall(r"\d+", n.get("bounds", ""))
                    if len(m) == 4:
                        x1, y1, x2, y2 = map(int, m)
                        if y2 > y1:
                            return (x1 + x2) // 2, (y1 + y2) // 2
    return None


def screen_size():
    out = adb("shell", "wm", "size")
    w, h = map(int, re.findall(r"(\d+)x(\d+)", out)[-1])
    return w, h


def swipe_up():
    w, h = screen_size()
    adb("shell", "input", "swipe", str(w // 2), str(int(h * 0.7)), str(w // 2), str(int(h * 0.35)), "300")
    time.sleep(0.8)


def locate(target, scroll=True, tries=6):
    for i in range(tries):
        pos = find(target)
        if pos:
            return pos
        if scroll and i >= 1:
            swipe_up()
        else:
            time.sleep(1.5)
    return None


def hide_keyboard():
    if "mInputShown=true" in adb("shell", "dumpsys", "input_method"):
        adb("shell", "input", "keyevent", "4")
        time.sleep(0.8)


def tap(target):
    hide_keyboard()
    pos = locate(target)
    if not pos:
        print(f"!! could not find '{target}'")
        shot(f"missing_{re.sub(r'[^a-z0-9]+', '_', target.lower())}")
        sys.exit(1)
    adb("shell", "input", "tap", str(pos[0]), str(pos[1]))
    print(f"tap '{target}' at {pos}")
    time.sleep(1.2)


def type_into(label, value):
    tap(label)
    escaped = value.replace(" ", "%s").replace("'", "").replace("&", "and")
    adb("shell", "input", "text", escaped)
    time.sleep(0.6)


def wait(target, secs=20):
    end = time.time() + secs
    while time.time() < end:
        if find(target):
            print(f"saw '{target}'")
            return
        time.sleep(1.5)
    print(f"!! timed out waiting for '{target}'")
    shot(f"timeout_{re.sub(r'[^a-z0-9]+', '_', target.lower())}")
    sys.exit(1)


def shot(name):
    os.makedirs("shots", exist_ok=True)
    png = adb("exec-out", "screencap", "-p", capture=True)
    with open(f"shots/{name}.png", "wb") as f:
        f.write(png)
    print(f"shot {name}")


if __name__ == "__main__":
    cmd, *rest = sys.argv[1:]
    if cmd == "tap":
        tap(rest[0])
    elif cmd == "type":
        type_into(rest[0], rest[1])
    elif cmd == "wait":
        wait(rest[0], int(rest[1]) if len(rest) > 1 else 20)
    elif cmd == "shot":
        shot(rest[0])
    elif cmd == "back":
        adb("shell", "input", "keyevent", "4"); time.sleep(1)
    elif cmd == "hidekb":
        hide_keyboard()
    elif cmd == "swipe":
        swipe_up()
