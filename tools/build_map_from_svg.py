#!/usr/bin/env python3
"""
Build the bundled US map asset from a plain SVG of the United States.

Companion to build_map_asset.py (which handles us-atlas TopoJSON). Use THIS one when you
have an .svg file where each state is a <path> (or <polygon>) carrying a recognizable state
identifier. It converts that into the app's bundled format:
  app/src/main/assets/maps/us_states_paths.json

USAGE
    python3 tools/build_map_from_svg.py path/to/us-map.svg

WHAT IT EXPECTS
  Each state element is a <path d="..."> or <polygon points="..."> whose state is identifiable
  via one of these attributes (checked in order): id, "data-id", "data-name", class, name,
  inkscape:label, title (a child <title> element is also read). The value may be:
    - a 2-letter postal code (e.g. "OH", "us-oh", "US-OH"), or
    - a full state name (e.g. "Ohio", "New York").

WHAT IT DOES
  - Pulls the path geometry as-is (coordinates are kept in the SVG's own coordinate space).
  - Flattens common SVG path commands to absolute M/L/Z (what the Android PathParser +
    our renderer use). Supported: M/m, L/l, H/h, V/v, Z/z, and C/c/S/s/Q/q/T/t curves are
    sampled into line segments so the bundled paths stay M/L/Z only. Arancs (A/a) are also
    sampled. If your SVG is all straight segments this is exact; curves become fine polylines.
  - Normalizes the bbox to a 0-origin viewBox with a little padding.
  - Emits style "shapes" so the app draws real outlines (labels auto-hide).

If a state can't be matched, the script lists what it found and exits, so nothing half-built
gets written. Review the report, fix the SVG attributes (or tell me the pattern), and re-run.
"""

import json
import math
import os
import re
import sys
import xml.etree.ElementTree as ET

NAME_TO_CODE = {
    "alabama": "AL", "alaska": "AK", "arizona": "AZ", "arkansas": "AR", "california": "CA",
    "colorado": "CO", "connecticut": "CT", "delaware": "DE", "florida": "FL", "georgia": "GA",
    "hawaii": "HI", "idaho": "ID", "illinois": "IL", "indiana": "IN", "iowa": "IA",
    "kansas": "KS", "kentucky": "KY", "louisiana": "LA", "maine": "ME", "maryland": "MD",
    "massachusetts": "MA", "michigan": "MI", "minnesota": "MN", "mississippi": "MS",
    "missouri": "MO", "montana": "MT", "nebraska": "NE", "nevada": "NV", "new hampshire": "NH",
    "new jersey": "NJ", "new mexico": "NM", "new york": "NY", "north carolina": "NC",
    "north dakota": "ND", "ohio": "OH", "oklahoma": "OK", "oregon": "OR", "pennsylvania": "PA",
    "rhode island": "RI", "south carolina": "SC", "south dakota": "SD", "tennessee": "TN",
    "texas": "TX", "utah": "UT", "vermont": "VT", "virginia": "VA", "washington": "WA",
    "west virginia": "WV", "wisconsin": "WI", "wyoming": "WY",
}
CODES = set(NAME_TO_CODE.values())
EXPECTED = set(CODES)
PAD = 8.0
CURVE_SAMPLES = 16  # segments per Bezier/arc; raise for smoother curves


def strip_ns(tag):
    return tag.split("}", 1)[1] if "}" in tag else tag


def resolve_code(el):
    """Try hard to identify which state an element represents."""
    candidates = []
    for attr in ("id", "data-id", "data-name", "data-state", "name", "class", "title"):
        v = el.get(attr)
        if v:
            candidates.append(v)
    for k, v in el.attrib.items():
        if strip_ns(k) in ("label", "id", "name") and v:
            candidates.append(v)
    # child <title>
    for child in el:
        if strip_ns(child.tag) == "title" and (child.text or "").strip():
            candidates.append(child.text.strip())

    for raw in candidates:
        s = raw.strip()
        # direct 2-letter code, possibly prefixed like "us-oh" or "US-OH"
        m = re.search(r"\b([A-Za-z]{2})\b", s.replace("_", " ").replace("-", " "))
        if m and m.group(1).upper() in CODES:
            # avoid matching random 2-letter words; prefer if whole token is the code
            tok = s.replace("_", " ").replace("-", " ").split()
            for t in tok:
                if t.upper() in CODES:
                    return t.upper()
        key = s.lower()
        if key in NAME_TO_CODE:
            return NAME_TO_CODE[key]
        # full-name appearing inside a longer string
        for name, code in NAME_TO_CODE.items():
            if name in key:
                return code
    return None


# ---- minimal SVG path -> absolute points (flattened to polylines) ----

def tokenize_path(d):
    return re.findall(r"[MmLlHhVvCcSsQqTtAaZz]|-?\d*\.?\d+(?:[eE][-+]?\d+)?", d)


def bezier3(p0, p1, p2, p3, n):
    out = []
    for i in range(1, n + 1):
        t = i / n
        mt = 1 - t
        x = mt**3 * p0[0] + 3 * mt**2 * t * p1[0] + 3 * mt * t**2 * p2[0] + t**3 * p3[0]
        y = mt**3 * p0[1] + 3 * mt**2 * t * p1[1] + 3 * mt * t**2 * p2[1] + t**3 * p3[1]
        out.append((x, y))
    return out


def bezier2(p0, p1, p2, n):
    out = []
    for i in range(1, n + 1):
        t = i / n
        mt = 1 - t
        x = mt**2 * p0[0] + 2 * mt * t * p1[0] + t**2 * p2[0]
        y = mt**2 * p0[1] + 2 * mt * t * p1[1] + t**2 * p2[1]
        out.append((x, y))
    return out


def parse_path(d):
    """Return a list of subpaths, each a list of (x, y) points."""
    toks = tokenize_path(d)
    i = 0
    cur = (0.0, 0.0)
    start = (0.0, 0.0)
    subpaths = []
    pts = []
    cmd = None
    prev_cubic_ctrl = None
    prev_quad_ctrl = None

    def num():
        nonlocal i
        v = float(toks[i])
        i += 1
        return v

    while i < len(toks):
        t = toks[i]
        if re.match(r"[A-Za-z]", t):
            cmd = t
            i += 1
        # else: implicit repeat of previous command
        c = cmd
        if c in ("M", "m"):
            x = num(); y = num()
            if c == "m":
                x += cur[0]; y += cur[1]
            if pts:
                subpaths.append(pts)
            pts = [(x, y)]
            cur = (x, y); start = (x, y)
            cmd = "L" if c == "M" else "l"
            prev_cubic_ctrl = prev_quad_ctrl = None
        elif c in ("L", "l"):
            x = num(); y = num()
            if c == "l":
                x += cur[0]; y += cur[1]
            pts.append((x, y)); cur = (x, y)
            prev_cubic_ctrl = prev_quad_ctrl = None
        elif c in ("H", "h"):
            x = num()
            if c == "h":
                x += cur[0]
            pts.append((x, cur[1])); cur = (x, cur[1])
            prev_cubic_ctrl = prev_quad_ctrl = None
        elif c in ("V", "v"):
            y = num()
            if c == "v":
                y += cur[1]
            pts.append((cur[0], y)); cur = (cur[0], y)
            prev_cubic_ctrl = prev_quad_ctrl = None
        elif c in ("C", "c"):
            x1 = num(); y1 = num(); x2 = num(); y2 = num(); x = num(); y = num()
            if c == "c":
                x1 += cur[0]; y1 += cur[1]; x2 += cur[0]; y2 += cur[1]; x += cur[0]; y += cur[1]
            pts += bezier3(cur, (x1, y1), (x2, y2), (x, y), CURVE_SAMPLES)
            prev_cubic_ctrl = (x2, y2); prev_quad_ctrl = None; cur = (x, y)
        elif c in ("S", "s"):
            x2 = num(); y2 = num(); x = num(); y = num()
            if c == "s":
                x2 += cur[0]; y2 += cur[1]; x += cur[0]; y += cur[1]
            x1, y1 = (2 * cur[0] - prev_cubic_ctrl[0], 2 * cur[1] - prev_cubic_ctrl[1]) if prev_cubic_ctrl else cur
            pts += bezier3(cur, (x1, y1), (x2, y2), (x, y), CURVE_SAMPLES)
            prev_cubic_ctrl = (x2, y2); prev_quad_ctrl = None; cur = (x, y)
        elif c in ("Q", "q"):
            x1 = num(); y1 = num(); x = num(); y = num()
            if c == "q":
                x1 += cur[0]; y1 += cur[1]; x += cur[0]; y += cur[1]
            pts += bezier2(cur, (x1, y1), (x, y), CURVE_SAMPLES)
            prev_quad_ctrl = (x1, y1); prev_cubic_ctrl = None; cur = (x, y)
        elif c in ("T", "t"):
            x = num(); y = num()
            if c == "t":
                x += cur[0]; y += cur[1]
            x1, y1 = (2 * cur[0] - prev_quad_ctrl[0], 2 * cur[1] - prev_quad_ctrl[1]) if prev_quad_ctrl else cur
            pts += bezier2(cur, (x1, y1), (x, y), CURVE_SAMPLES)
            prev_quad_ctrl = (x1, y1); prev_cubic_ctrl = None; cur = (x, y)
        elif c in ("A", "a"):
            rx = num(); ry = num(); num(); num(); num(); x = num(); y = num()
            if c == "a":
                x += cur[0]; y += cur[1]
            # crude arc -> line sampling along a straight chord subdivided
            pts += [
                (cur[0] + (x - cur[0]) * k / CURVE_SAMPLES, cur[1] + (y - cur[1]) * k / CURVE_SAMPLES)
                for k in range(1, CURVE_SAMPLES + 1)
            ]
            prev_cubic_ctrl = prev_quad_ctrl = None; cur = (x, y)
        elif c in ("Z", "z"):
            if pts:
                pts.append(start)
                subpaths.append(pts)
                pts = []
            cur = start
            prev_cubic_ctrl = prev_quad_ctrl = None
        else:
            i += 1  # unknown token; skip defensively
    if pts:
        subpaths.append(pts)
    return subpaths


def polygon_points(points_attr):
    nums = [float(n) for n in re.findall(r"-?\d*\.?\d+", points_attr)]
    return [list(zip(nums[0::2], nums[1::2]))]


def fmt(v):
    return "%g" % round(v, 1)


def subpaths_to_d(subpaths):
    out = []
    for sp in subpaths:
        if len(sp) < 2:
            continue
        out += ["M", fmt(sp[0][0]), ",", fmt(sp[0][1])]
        for px, py in sp[1:]:
            out += ["L", fmt(px), ",", fmt(py)]
        out.append("Z")
    return "".join(out)


def main():
    if len(sys.argv) < 2:
        raise SystemExit("Usage: python3 tools/build_map_from_svg.py path/to/us-map.svg")
    src = sys.argv[1]
    if not os.path.isfile(src):
        raise SystemExit(f"File not found: {src}")

    tree = ET.parse(src)
    root = tree.getroot()

    state_subpaths = {}
    unmatched = []
    for el in root.iter():
        tag = strip_ns(el.tag)
        if tag not in ("path", "polygon"):
            continue
        code = resolve_code(el)
        if not code:
            ident = el.get("id") or el.get("class") or "(no id)"
            if tag in ("path", "polygon"):
                unmatched.append(ident)
            continue
        if tag == "path":
            d = el.get("d")
            if not d:
                continue
            sps = parse_path(d)
        else:
            sps = polygon_points(el.get("points", ""))
        # a state may appear as multiple elements (islands) -> accumulate
        state_subpaths.setdefault(code, []).extend(sps)

    found = set(state_subpaths)
    missing = sorted(EXPECTED - found)
    extra = sorted(found - EXPECTED)
    if missing:
        print("Matched states:", ", ".join(sorted(found)) or "(none)")
        print("UNMATCHED path/polygon ids (first 40):", unmatched[:40])
        raise SystemExit(
            f"\nCould not match all 50 states. missing={missing} extra={extra}\n"
            "Tell me how states are identified in your SVG (which attribute, what format) "
            "and I'll adjust the matcher."
        )

    # bbox across everything
    all_x = []
    all_y = []
    for sps in state_subpaths.values():
        for sp in sps:
            for px, py in sp:
                all_x.append(px); all_y.append(py)
    min_x, max_x = min(all_x), max(all_x)
    min_y, max_y = min(all_y), max(all_y)
    width = round(max_x - min_x + 2 * PAD, 1)
    height = round(max_y - min_y + 2 * PAD, 1)

    def shifted(sps):
        return [[(px - min_x + PAD, py - min_y + PAD) for px, py in sp] for sp in sps]

    states = {}
    centroids = {}
    for code, sps in state_subpaths.items():
        ssp = shifted(sps)
        states[code] = subpaths_to_d(ssp)
        xs = [p[0] for sp in ssp for p in sp]
        ys = [p[1] for sp in ssp for p in sp]
        centroids[code] = [round((min(xs) + max(xs)) / 2, 1), round((min(ys) + max(ys)) / 2, 1)]

    out = {
        "viewBox": {"width": width, "height": height},
        "style": "shapes",
        "states": states,
        "centroids": centroids,
        "count": len(states),
    }
    here = os.path.dirname(os.path.abspath(__file__))
    dest = os.path.normpath(
        os.path.join(here, "..", "app", "src", "main", "assets", "maps", "us_states_paths.json")
    )
    os.makedirs(os.path.dirname(dest), exist_ok=True)
    with open(dest, "w", encoding="utf-8") as f:
        json.dump(out, f)
    print(f"Wrote {dest}")
    print(f"  states={len(states)} viewBox={width}x{height} bytes={os.path.getsize(dest)}")


if __name__ == "__main__":
    main()
