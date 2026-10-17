#!/usr/bin/env python3
"""Packs companion/ into the module ZIP that the app installs (app/src/main/assets/coogle-companion.zip).

Shell scripts, awk files and module.prop are written with LF endings; scripts get mode 0755 and
everything else 0644. The ZIP is reproducible: entries are sorted and carry a fixed timestamp.
"""
import os
import sys
import zipfile

ROOT = os.path.dirname(os.path.abspath(__file__))
SOURCE = os.path.join(ROOT, "companion")
TARGET = os.path.join(ROOT, "app", "src", "main", "assets", "coogle-companion.zip")
TEXT = (".sh", ".awk", ".prop")
FIXED_TIME = (2026, 1, 1, 0, 0, 0)


def entries():
    for folder, _, files in os.walk(SOURCE):
        for name in files:
            path = os.path.join(folder, name)
            yield os.path.relpath(path, SOURCE).replace(os.sep, "/"), path


def main():
    os.makedirs(os.path.dirname(TARGET), exist_ok=True)
    if os.path.exists(TARGET):
        os.remove(TARGET)
    with zipfile.ZipFile(TARGET, "w", zipfile.ZIP_DEFLATED) as archive:
        for name, path in sorted(entries()):
            with open(path, "rb") as handle:
                data = handle.read()
            if name.endswith(TEXT):
                data = data.replace(b"\r\n", b"\n")
            info = zipfile.ZipInfo(name, FIXED_TIME)
            info.create_system = 3  # Unix, so the mode bits below are honoured
            info.compress_type = zipfile.ZIP_DEFLATED
            mode = 0o755 if name.endswith(".sh") else 0o644
            info.external_attr = (0o100000 | mode) << 16
            archive.writestr(info, data)
    print(TARGET)


if __name__ == "__main__":
    sys.exit(main())
