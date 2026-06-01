#!/usr/bin/env python3
"""
Download all 50 US state flags into the app's bundled assets.

Writes app/src/main/assets/flags/<code>.png (lowercase 2-letter code) — exactly what
ui/components/FlagImage.kt loads. State flags are US government works and effectively all
public domain, so this is safe to bundle and ship.

USAGE (from the project root, on a machine with internet):
    python3 tools/fetch_flags.py

Options:
    --width N     Pixel width to fetch each flag at (default 640).
    --force       Re-download even if a file already exists.

It uses only the Python standard library (urllib) and pulls from Wikimedia Commons'
thumbnail renderer, which rasterizes each flag's SVG to a PNG at the requested width.
After downloading it reports any of the 50 states that are missing.
"""

import argparse
import os
import sys
import time
import urllib.request
import urllib.parse

# code -> the Wikimedia Commons file name for that state's flag SVG.
FLAG_FILES = {
    "AL": "Flag of Alabama.svg",
    "AK": "Flag of Alaska.svg",
    "AZ": "Flag of Arizona.svg",
    "AR": "Flag of Arkansas.svg",
    "CA": "Flag of California.svg",
    "CO": "Flag of Colorado.svg",
    "CT": "Flag of Connecticut.svg",
    "DE": "Flag of Delaware.svg",
    "FL": "Flag of Florida.svg",
    "GA": "Flag of Georgia (U.S. state).svg",
    "HI": "Flag of Hawaii.svg",
    "ID": "Flag of Idaho.svg",
    "IL": "Flag of Illinois.svg",
    "IN": "Flag of Indiana.svg",
    "IA": "Flag of Iowa.svg",
    "KS": "Flag of Kansas.svg",
    "KY": "Flag of Kentucky.svg",
    "LA": "Flag of Louisiana.svg",
    "ME": "Flag of Maine.svg",
    "MD": "Flag of Maryland.svg",
    "MA": "Flag of Massachusetts.svg",
    "MI": "Flag of Michigan.svg",
    "MN": "Flag of Minnesota.svg",
    "MS": "Flag of Mississippi.svg",
    "MO": "Flag of Missouri.svg",
    "MT": "Flag of Montana.svg",
    "NE": "Flag of Nebraska.svg",
    "NV": "Flag of Nevada.svg",
    "NH": "Flag of New Hampshire.svg",
    "NJ": "Flag of New Jersey.svg",
    "NM": "Flag of New Mexico.svg",
    "NY": "Flag of New York.svg",
    "NC": "Flag of North Carolina.svg",
    "ND": "Flag of North Dakota.svg",
    "OH": "Flag of Ohio.svg",
    "OK": "Flag of Oklahoma.svg",
    "OR": "Flag of Oregon.svg",
    "PA": "Flag of Pennsylvania.svg",
    "RI": "Flag of Rhode Island.svg",
    "SC": "Flag of South Carolina.svg",
    "SD": "Flag of South Dakota.svg",
    "TN": "Flag of Tennessee.svg",
    "TX": "Flag of Texas.svg",
    "UT": "Flag of Utah.svg",
    "VT": "Flag of Vermont.svg",
    "VA": "Flag of Virginia.svg",
    "WA": "Flag of Washington.svg",
    "WV": "Flag of West Virginia.svg",
    "WI": "Flag of Wisconsin.svg",
    "WY": "Flag of Wyoming.svg",
}

# Wikimedia's thumbnail endpoint renders an SVG to a PNG at a given width:
#   https://commons.wikimedia.org/wiki/Special:FilePath/<File>?width=N
THUMB_URL = "https://commons.wikimedia.org/wiki/Special:FilePath/{file}?width={width}"

USER_AGENT = "LicensePlateQuest-flag-fetch/1.0 (personal app asset import)"


def dest_dir() -> str:
    here = os.path.dirname(os.path.abspath(__file__))
    d = os.path.normpath(os.path.join(here, "..", "app", "src", "main", "assets", "flags"))
    os.makedirs(d, exist_ok=True)
    return d


def fetch_one(code: str, file_name: str, width: int, out_dir: str, force: bool) -> bool:
    out_path = os.path.join(out_dir, f"{code.lower()}.png")
    if os.path.exists(out_path) and not force:
        print(f"  {code}: already present, skipping")
        return True
    url = THUMB_URL.format(file=urllib.parse.quote(file_name), width=width)
    req = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    try:
        with urllib.request.urlopen(req, timeout=30) as resp:
            data = resp.read()
        # Sanity check: PNGs start with the 8-byte signature.
        if not data.startswith(b"\x89PNG\r\n\x1a\n"):
            print(f"  {code}: WARNING got non-PNG response ({len(data)} bytes) — skipped")
            return False
        with open(out_path, "wb") as f:
            f.write(data)
        print(f"  {code}: {len(data):,} bytes -> {os.path.basename(out_path)}")
        return True
    except Exception as e:
        print(f"  {code}: FAILED ({e})")
        return False


def main() -> int:
    parser = argparse.ArgumentParser(description="Download US state flags into assets/flags/.")
    parser.add_argument("--width", type=int, default=640, help="PNG width in pixels (default 640).")
    parser.add_argument("--force", action="store_true", help="Re-download even if present.")
    args = parser.parse_args()

    out_dir = dest_dir()
    print(f"Downloading {len(FLAG_FILES)} state flags at width={args.width} into:\n  {out_dir}\n")

    ok = 0
    for code in sorted(FLAG_FILES):
        if fetch_one(code, FLAG_FILES[code], args.width, out_dir, args.force):
            ok += 1
        time.sleep(0.2)  # be polite to Wikimedia

    missing = [c for c in FLAG_FILES if not os.path.exists(os.path.join(out_dir, f"{c.lower()}.png"))]
    print(f"\nDone: {ok}/{len(FLAG_FILES)} succeeded.")
    if missing:
        print(f"MISSING ({len(missing)}): {', '.join(sorted(missing))}")
        print("Re-run the script to retry just the missing ones.")
        return 1
    print("All 50 state flags are present. Rebuild the app to see them.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
