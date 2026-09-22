#!/usr/bin/python3
"""
Turn Lucide icons into the Kotlin file the app draws from.

The app does not depend on an icon library. It holds the path data of the
icons it uses, in `design/icons/LucideIcons.kt`, and draws them itself. This
script writes that file, and the app icon, from one pinned Lucide release.

Usage:
    tools/lucide.py                 fetch the pinned release, write the files
    tools/lucide.py --from <dir>    use an unpacked lucide-static package

To add an icon: add its name to tools/lucide-icons.txt and run this again.
The names are the ones on https://lucide.dev/icons.

Lucide is under the ISC licence, and some icons come from Feather, under the
MIT licence. Both texts are in licenses/lucide/LICENSE. See ASSETS.md.

The shebang is /usr/bin/python3 on purpose: on the dev machine `python3` on
the PATH is a PlatformIO environment. See plan.md, step 1.
"""

import hashlib
import io
import re
import sys
import tarfile
import urllib.request
import xml.etree.ElementTree as ET
from pathlib import Path

VERSION = "1.47.0"
TARBALL = f"https://registry.npmjs.org/lucide-static/-/lucide-static-{VERSION}.tgz"
SHA256 = "b47744c9f7b385c25fb27d212cf9830947030a57a635f8b11a5473a72ec57cfd"

ROOT = Path(__file__).resolve().parent.parent
NAMES_FILE = ROOT / "tools" / "lucide-icons.txt"
KOTLIN_FILE = (
    ROOT / "app/src/main/kotlin/io/github/foxesrcool1/margin/design/icons/LucideIcons.kt"
)
APP_ICON_FILE = ROOT / "app/src/main/res/drawable/ic_launcher_foreground.xml"

# The icon on the launcher tile of the tablet.
APP_ICON = "sprout"

SVG = "{http://www.w3.org/2000/svg}"
ARG_COUNT = {"m": 2, "l": 2, "h": 1, "v": 1, "c": 6, "s": 4, "q": 4, "t": 2, "a": 7, "z": 0}
NUMBER = re.compile(r"[+-]?(?:\d+\.?\d*|\.\d+)(?:[eE][+-]?\d+)?")


def fmt(value: float) -> str:
    text = f"{value:.4f}".rstrip("0").rstrip(".")
    return "0" if text in ("", "-0") else text


def normalise(d: str) -> str:
    """
    Writes path data out the long way: every command named, every number apart.

    Lucide packs its data tight. `a2 2 0 0022 17` is an arc whose two flags
    and the x that follows them are written as one run of digits, and not
    every path reader takes that. Spelled out, every reader does.
    """
    out = []
    i, n = 0, len(d)

    def skip():
        nonlocal i
        while i < n and (d[i].isspace() or d[i] == ","):
            i += 1

    def number() -> str:
        nonlocal i
        skip()
        match = NUMBER.match(d, i)
        if not match:
            raise ValueError(f"expected a number at {i} in {d!r}")
        i = match.end()
        return fmt(float(match.group()))

    def flag() -> str:
        nonlocal i
        skip()
        if i >= n or d[i] not in "01":
            raise ValueError(f"expected an arc flag at {i} in {d!r}")
        i += 1
        return d[i - 1]

    command = None
    while True:
        skip()
        if i >= n:
            break
        if d[i].isalpha():
            command = d[i]
            i += 1
            if command in "zZ":
                out.append("Z")
                continue
        elif command is None:
            raise ValueError(f"path data starts with no command: {d!r}")
        elif command in "mM":
            # More pairs after a move are lines.
            command = "l" if command == "m" else "L"

        count = ARG_COUNT[command.lower()]
        if command in "aA":
            args = [number(), number(), number(), flag(), flag(), number(), number()]
        else:
            args = [number() for _ in range(count)]
        out.append(command + " ".join(args))
    return " ".join(out)


def ellipse_path(cx: float, cy: float, rx: float, ry: float) -> str:
    return (
        f"M{fmt(cx - rx)} {fmt(cy)} "
        f"a{fmt(rx)} {fmt(ry)} 0 1 0 {fmt(2 * rx)} 0 "
        f"a{fmt(rx)} {fmt(ry)} 0 1 0 {fmt(-2 * rx)} 0 Z"
    )


def rect_path(x: float, y: float, w: float, h: float, rx: float, ry: float) -> str:
    rx, ry = min(rx, w / 2), min(ry, h / 2)
    if rx == 0 or ry == 0:
        return f"M{fmt(x)} {fmt(y)} h{fmt(w)} v{fmt(h)} h{fmt(-w)} Z"
    arc = f"a{fmt(rx)} {fmt(ry)} 0 0 1"
    return (
        f"M{fmt(x + rx)} {fmt(y)} h{fmt(w - 2 * rx)} {arc} {fmt(rx)} {fmt(ry)} "
        f"v{fmt(h - 2 * ry)} {arc} {fmt(-rx)} {fmt(ry)} "
        f"h{fmt(-(w - 2 * rx))} {arc} {fmt(-rx)} {fmt(-ry)} "
        f"v{fmt(-(h - 2 * ry))} {arc} {fmt(rx)} {fmt(-ry)} Z"
    )


def points_path(points: str, close: bool) -> str:
    values = [float(v) for v in NUMBER.findall(points)]
    pairs = list(zip(values[0::2], values[1::2]))
    path = " ".join(("M" if k == 0 else "L") + f"{fmt(px)} {fmt(py)}" for k, (px, py) in enumerate(pairs))
    return path + (" Z" if close else "")


def shape_to_path(element) -> str:
    tag = element.tag.replace(SVG, "")
    get = lambda name, default=0.0: float(element.get(name, default))
    if tag == "path":
        return normalise(element.get("d"))
    if tag == "circle":
        return ellipse_path(get("cx"), get("cy"), get("r"), get("r"))
    if tag == "ellipse":
        return ellipse_path(get("cx"), get("cy"), get("rx"), get("ry"))
    if tag == "rect":
        rx = element.get("rx")
        ry = element.get("ry")
        rx_value = float(rx if rx is not None else (ry if ry is not None else 0))
        ry_value = float(ry if ry is not None else (rx if rx is not None else 0))
        return rect_path(get("x"), get("y"), get("width"), get("height"), rx_value, ry_value)
    if tag == "line":
        return f"M{fmt(get('x1'))} {fmt(get('y1'))} L{fmt(get('x2'))} {fmt(get('y2'))}"
    if tag == "polyline":
        return points_path(element.get("points"), close=False)
    if tag == "polygon":
        return points_path(element.get("points"), close=True)
    raise ValueError(f"no rule for <{tag}>")


def read_icon(svg_text: str):
    """Returns (strokes, fills): the path data of every shape in the icon."""
    root = ET.fromstring(svg_text)
    strokes, fills = [], []
    for element in root:
        path = shape_to_path(element)
        if element.get("fill") == "currentColor":
            fills.append(path)
        strokes.append(path)
    return strokes, fills


def kotlin_name(name: str) -> str:
    return "".join(part.capitalize() for part in name.split("-"))


def load_package(args) -> dict:
    """Returns a function-free view of the package: a dict of name to SVG text."""
    wanted = [
        line.split("#")[0].strip()
        for line in NAMES_FILE.read_text().splitlines()
        if line.split("#")[0].strip()
    ]
    wanted = sorted(set(wanted))

    if len(args) == 2 and args[0] == "--from":
        folder = Path(args[1])
        return {name: (folder / "icons" / f"{name}.svg").read_text() for name in wanted}

    print(f"Fetching lucide-static {VERSION}")
    data = urllib.request.urlopen(TARBALL, timeout=120).read()
    digest = hashlib.sha256(data).hexdigest()
    if digest != SHA256:
        sys.exit(f"The download does not match the pinned checksum.\n  got  {digest}\n  want {SHA256}")
    icons = {}
    with tarfile.open(fileobj=io.BytesIO(data), mode="r:gz") as tar:
        for name in wanted:
            member = tar.extractfile(f"package/icons/{name}.svg")
            icons[name] = member.read().decode("utf-8")
    return icons


def write_kotlin(icons: dict) -> None:
    lines = [
        "// Written by tools/lucide.py. Do not edit by hand: add the name to",
        "// tools/lucide-icons.txt and run the script again.",
        "//",
        f"// Lucide {VERSION}, https://lucide.dev. ISC licence, and MIT for the icons that",
        "// come from Feather. The licence text is in licenses/lucide/LICENSE.",
        "package io.github.foxesrcool1.margin.design.icons",
        "",
        "/** Every Lucide icon the app draws. The shapes live in a 24 by 24 box. */",
        "object Lucide {",
    ]
    for name, svg in icons.items():
        strokes, fills = read_icon(svg)
        lines.append(f"    val {kotlin_name(name)} = LucideIcon(")
        lines.append(f'        name = "{name}",')
        lines.append("        strokes = listOf(")
        lines += [f'            "{path}",' for path in strokes]
        lines.append("        ),")
        if fills:
            lines.append("        fills = listOf(")
            lines += [f'            "{path}",' for path in fills]
            lines.append("        ),")
        lines.append("    )")
        lines.append("")
    lines[-1] = "}"
    KOTLIN_FILE.parent.mkdir(parents=True, exist_ok=True)
    KOTLIN_FILE.write_text("\n".join(lines) + "\n")
    print(f"Wrote {len(icons)} icons to {KOTLIN_FILE.relative_to(ROOT)}")


def write_app_icon(icons: dict) -> None:
    """
    The launcher tile. An adaptive icon is 108 dp, and only the middle 66 dp
    is safe from the mask. The 24 unit drawing is scaled by 2 and sits in the
    middle, which makes it 48 dp and keeps its line at 4 dp.
    """
    strokes, _ = read_icon(icons[APP_ICON])
    paths = "\n".join(
        "        <path\n"
        f'            android:pathData="{path}"\n'
        '            android:strokeColor="#FF000000"\n'
        '            android:strokeWidth="2"\n'
        '            android:strokeLineCap="round"\n'
        '            android:strokeLineJoin="round" />'
        for path in strokes
    )
    APP_ICON_FILE.write_text(
        '<?xml version="1.0" encoding="utf-8"?>\n'
        "<!--\n"
        f'  The app icon: "{APP_ICON}" from Lucide {VERSION}, https://lucide.dev. ISC licence.\n'
        "  Written by tools/lucide.py. Do not edit by hand.\n"
        "-->\n"
        '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
        '    android:width="108dp"\n'
        '    android:height="108dp"\n'
        '    android:viewportWidth="108"\n'
        '    android:viewportHeight="108">\n'
        "    <group\n"
        '        android:scaleX="2"\n'
        '        android:scaleY="2"\n'
        '        android:translateX="30"\n'
        '        android:translateY="30">\n'
        f"{paths}\n"
        "    </group>\n"
        "</vector>\n"
    )
    print(f"Wrote the app icon to {APP_ICON_FILE.relative_to(ROOT)}")


def main() -> None:
    icons = load_package(sys.argv[1:])
    if APP_ICON not in icons:
        sys.exit(f"{APP_ICON} must be in {NAMES_FILE.name}: it is the app icon")
    write_kotlin(icons)
    write_app_icon(icons)


if __name__ == "__main__":
    main()
