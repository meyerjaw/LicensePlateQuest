State flag images
=================

Drop one PNG per state here, named by lowercase 2-letter code:

    flags/al.png, flags/ak.png, flags/az.png, ... flags/wy.png

The app loads flags/<code>.png automatically (see ui/components/FlagImage.kt,
which builds the path with flagAssetPath(code)). Until a given state's PNG is
present, the State Detail and found-states list show a placeholder card with the
state's 2-letter code. No code changes are needed when you add images.

EASIEST WAY TO GET THEM
-----------------------
Run the bundled downloader from the project root on a machine with internet:

    python3 tools/fetch_flags.py

It pulls all 50 US state flags from Wikimedia Commons (public-domain government
works) at a consistent width and writes them here with the correct names, then
reports any that are missing.

Display note: flags are not a uniform shape, so FlagImage renders each at its own
aspect ratio. PNGs at ~600px wide look crisp; transparent or solid backgrounds
both work.
