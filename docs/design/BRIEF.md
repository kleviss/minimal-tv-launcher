# TV Home redesign brief

Owner's words: "Keep the simplicity of controlling the TV but make it look fancy with background images,
while keeping the apps and controls visible, with blurred effects like iOS 18 principles. Not glossy like iOS 26.
Make it look like an Apple TV." The pictures are artistic, beige-toned photos his girlfriend is choosing; they
are not here yet, so `wallpapers/placeholder-*.jpg` stand in (1-3 pale and beige; 4 has a dark top, to test the light-on-dark flip). A random one shows each time the TV is turned on.

## The screen

A 1920×1080 TV seen from about three metres, driven with a D-pad remote. One screen, no scrolling, no rows of
content. Everything below exists today and must stay; only the look changes.

| Element | Content today | Notes |
|---|---|---|
| Greeting | Good morning / afternoon / evening / night | Sentence case, not all caps |
| Clock | 14:16 (24h) | The biggest text on screen |
| Date | Monday 19 October | Sentence case |
| Pills, top right | Inputs · Wi-Fi name (e.g. "Home Wi-Fi") · Settings | Focusable; show the focused state on one |
| Dock | Netflix, YouTube, All apps | Netflix and YouTube use the apps' own 16:9 banners; All apps is a 3×3 grid glyph. Netflix is focused |
| Focused app name | "Netflix" | New: big type above the dock, like the "YouTube" on the reference screen |
| Add app | A pill, bottom right of the dock | Opens a picker |
| VPN chip | "VPN off" with a grey dot, or "VPN · United States" with a green dot | Bottom left of the dock; not focusable |

Focus is the only interaction: one element has it, the remote moves it. The focused tile grows a little
and gets a soft shadow (tvOS), never a hard white ring. Use `?wp=1|2|3|4` in the page URL to pick a wallpaper
and a `?focus=netflix|youtube|apps|inputs` query to pick what is focused; default `wp=1`, `focus=netflix`.
With no `wp` the page picks one of the four at random on load (that is what the TV will do).

## Rounded screen corners

The TV's panel has square corners; the owner wants the launcher to draw a black mask with rounded corners
over everything, so the screen itself looks rounded (think a phone display). Radius about 48 px at 1080p,
pure black outside the rounded rectangle, nothing else. Mockups: `?corners=1` turns the mask on; the
implementation is one full-screen overlay view, which costs nothing. Only the launcher gets it: the
corners are square again inside Netflix or YouTube. Final tuning waits for the real photos.

## Material: iOS 18, not iOS 26

Frosted, matte, quiet. Panels are translucent with the wallpaper blurred behind them, a faint one-pixel
light edge, no specular highlights, no refraction, no chrome. Pills, the dock tray (if there is one), the
chips: all the same material. Light or dark material is the designer's call per direction, but it must
read on all three placeholders, including the dark brush stroke in placeholder 2. Text on the wallpaper
itself needs a scrim (a soft gradient) or must sit on material.

On the TV, blur cannot run live (Android 11, 1.4 GB RAM). The app will blur each wallpaper once when it is
loaded and draw that blurred copy behind the panels. So: `backdrop-filter: blur()` in the mockup is fine,
but the blur is static, the panels do not move, and nothing animates except the focus change.

## Type

The TV has Roboto and nothing else bundled; the mockups may use any font installed on this Mac (Roboto is
there), but prefer Roboto so the mockup matches what the TV will show. Big and light for the clock, regular
for the rest. From three metres, 24 px is the smallest readable text; labels 26–30 px, pills 28–32 px,
app name 56–72 px, clock 110–150 px.

## Avoid

All-caps tracked-out labels, middle dots between facts (use "Wi-Fi · 5 GHz" only if it is a real
separator), numbered markers, monospace, neon accents, drop shadows under everything, a hard white focus
ring, cream + serif + terracotta as a palette, busy gradients that fight the photo. The photo is the
memorable thing; the chrome is quiet.

## Deliverable per direction

One self-contained HTML file in `mockups/`, 1920×1080 fixed (`html, body { width:1920px; height:1080px;
overflow:hidden }`), no network, wallpapers referenced as `../wallpapers/placeholder-N-*.jpg`. Render with
headless Chrome at 1920×1080 to check it; the renders go in `renders/`.
