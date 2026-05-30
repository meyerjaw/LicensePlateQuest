License plate images
=====================

Drop one PNG per state here, named by lowercase 2-letter code to match the
`plate_image_path` values in assets/regions/us_states.json:

    plates/al.png, plates/ak.png, plates/az.png, ... plates/wy.png

The app loads plates/<code>.png automatically (see ui/components/PlateImage.kt).
Until a given state's PNG is present, State Detail shows a styled placeholder
card with the state's 2-letter code. No code changes are needed when you add
images — just drop the files in and rebuild.

Suggested image specs: roughly 2:1 aspect ratio (standard US plate), e.g.
600x300 px, transparent or white background.
