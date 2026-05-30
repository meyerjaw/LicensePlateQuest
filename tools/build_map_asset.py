#!/usr/bin/env python3
"""
Build the bundled US map asset from a raw us-atlas TopoJSON.

Why this exists: the app ships real US state outlines as a single bundled JSON
(app/src/main/assets/maps/us_states_paths.json). This script converts the standard,
*pre-projected* us-atlas file into that format. It needs NO network and NO d3 — pure
Python — so it runs anywhere once you have the source file.

USAGE
  1. Download the pre-projected TopoJSON (already in geoAlbersUsa screen space, with
     Alaska/Hawaii placed as insets):
         https://cdn.jsdelivr.net/npm/us-atlas@3/states-albers-10m.json
     Save it next to this script (or anywhere) as: states-albers-10m.json
  2. Run:
         python3 tools/build_map_asset.py path/to/states-albers-10m.json
     (If you omit the path, it looks for ./states-albers-10m.json and
      ./tools/states-albers-10m.json.)
  3. It writes app/src/main/assets/maps/us_states_paths.json. Rebuild the app.

Output format (consumed by MapShapesDto / MapRepository):
  { "viewBox": {"width": W, "height": H},
    "style": "shapes",
    "states": { "OH": "M..L..Z", ... },   # SVG path per state, commands M/L/Z only
    "centroids": { "OH": [x, y], ... },    # bbox-center per state (label anchor)
    "count": 50 }

Note: style is "shapes" (not "tile-grid"), so the app draws real outlines and hides the
2-letter square labels automatically.
"""

import json
import os
import re
import sys

# FIPS state id -> 2-letter postal code. DC (11) and territories are intentionally absent,
# so they are dropped (MVP is the 50 states only).
FIPS = {
    "01": "AL", "02": "AK", "04": "AZ", "05": "AR", "06": "CA", "08": "CO", "09": "CT",
    "10": "DE", "12": "FL", "13": "GA", "15": "HI", "16": "ID", "17": "IL", "18": "IN",
    "19": "IA", "20": "KS", "21": "KY", "22": "LA", "23": "ME", "24": "MD", "25": "MA",
    "26": "MI", "27": "MN", "28": "MS", "29": "MO", "30": "MT", "31": "NE", "32": "NV",
    "33": "NH", "34": "NJ", "35": "NM", "36": "NY", "37": "NC", "38": "ND", "39": "OH",
    "40": "OK", "41": "OR", "42": "PA", "44": "RI", "45": "SC", "46": "SD", "47": "TN",
    "48": "TX", "49": "UT", "50": "VT", "51": "VA", "53": "WA", "54": "WV", "55": "WI",
    "56": "WY",
}

EXPECTED = set(FIPS.values())
PAD = 8.0  # viewBox padding around the bbox


def find_source(argv):
    if len(argv) > 1:
        return argv[1]
    here = os.path.dirname(os.path.abspath(__file__))
    for cand in (
        os.path.join(os.getcwd(), "states-albers-10m.json"),
        os.path.join(here, "states-albers-10m.json"),
        os.path.join(here, "..", "states-albers-10m.json"),
    ):
        if os.path.isfile(cand):
            return cand
    return None


def load_topojson(path):
    raw = open(path, encoding="utf-8").read()
    # Be tolerant of any leading header lines before the JSON body.
    start = raw.find("{")
    if start < 0:
        raise SystemExit("No JSON object found in source file.")
    return json.loads(raw[start:])


def make_arc_decoder(topo):
    tr = topo.get("transform")
    if not tr:
        # Non-quantized: arcs are already absolute coordinate lists.
        return [[(float(x), float(y)) for x, y in arc] for arc in topo["arcs"]]
    sx, sy = tr["scale"]
    tx, ty = tr["translate"]
    decoded = []
    for arc in topo["arcs"]:
        pts = []
        x = 0
        y = 0
        for dx, dy in arc:
            x += dx
            y += dy
            pts.append((x * sx + tx, y * sy + ty))
        decoded.append(pts)
    return decoded


def fmt(v):
    return "%g" % round(v, 1)


def main():
    src = find_source(sys.argv)
    if not src:
        raise SystemExit(
            "Source file not found. Pass the path to states-albers-10m.json as an argument, "
            "or place it next to this script. See the header of this file for the download URL."
        )

    topo = load_topojson(src)
    if "states" not in topo.get("objects", {}):
        raise SystemExit("Source is not a us-atlas states TopoJSON (objects.states missing).")

    arcs = make_arc_decoder(topo)

    def arc_points(idx):
        return arcs[idx] if idx >= 0 else list(reversed(arcs[~idx]))

    def ring_points(ring):
        pts = []
        for k, idx in enumerate(ring):
            seg = arc_points(idx)
            pts.extend(seg if k == 0 else seg[1:])  # drop shared first point
        return pts

    def ring_path(pts):
        out = ["M", fmt(pts[0][0]), ",", fmt(pts[0][1])]
        for px, py in pts[1:]:
            out += ["L", fmt(px), ",", fmt(py)]
        out.append("Z")
        return "".join(out)

    states = {}
    all_x = []
    all_y = []
    for geom in topo["objects"]["states"]["geometries"]:
        code = FIPS.get(str(geom.get("id")).zfill(2))
        if not code:
            continue
        gtype = geom["type"]
        if gtype == "Polygon":
            polys = [geom["arcs"]]
        elif gtype == "MultiPolygon":
            polys = geom["arcs"]
        else:
            continue
        d = []
        for poly in polys:
            for ring in poly:
                pts = ring_points(ring)
                for px, py in pts:
                    all_x.append(px)
                    all_y.append(py)
                d.append(ring_path(pts))
        states[code] = "".join(d)

    missing = sorted(EXPECTED - set(states))
    extra = sorted(set(states) - EXPECTED)
    if missing or extra:
        raise SystemExit(f"State set mismatch. missing={missing} extra={extra}")

    min_x, max_x = min(all_x), max(all_x)
    min_y, max_y = min(all_y), max(all_y)
    width = round(max_x - min_x + 2 * PAD, 1)
    height = round(max_y - min_y + 2 * PAD, 1)

    def shift(path):
        tokens = re.findall(r"[MLZ]|,|-?\d+\.?\d*", path)
        out = []
        i = 0
        while i < len(tokens):
            t = tokens[i]
            if t in ("M", "L", "Z", ","):
                out.append(t)
                i += 1
            else:
                x = float(tokens[i])
                y = float(tokens[i + 2])
                out += [fmt(x - min_x + PAD), ",", fmt(y - min_y + PAD)]
                i += 3
        return "".join(out)

    norm = {c: shift(d) for c, d in states.items()}

    centroids = {}
    for code, path in norm.items():
        nums = [float(n) for n in re.findall(r"-?\d+\.?\d*", path)]
        xs = nums[0::2]
        ys = nums[1::2]
        centroids[code] = [round((min(xs) + max(xs)) / 2, 1), round((min(ys) + max(ys)) / 2, 1)]

    out = {
        "viewBox": {"width": width, "height": height},
        "style": "shapes",
        "states": norm,
        "centroids": centroids,
        "count": len(norm),
    }

    here = os.path.dirname(os.path.abspath(__file__))
    dest = os.path.normpath(
        os.path.join(here, "..", "app", "src", "main", "assets", "maps", "us_states_paths.json")
    )
    os.makedirs(os.path.dirname(dest), exist_ok=True)
    with open(dest, "w", encoding="utf-8") as f:
        json.dump(out, f)

    print(f"Wrote {dest}")
    print(f"  states={len(norm)} viewBox={width}x{height} bytes={os.path.getsize(dest)}")


if __name__ == "__main__":
    main()
