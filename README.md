# memer

A meme **and** GIF generator built to **practice Kotlin Multiplatform + Compose Multiplatform** end to end.

Pick a template, drop in draggable text, and **Save / Share / Copy**. Every meme you
create is kept in an in-app **gallery** with its template + text, so you can come back
later and keep editing. You can also turn a video or GIF into an animated meme with
frame-by-frame text.

## Screenshots

| Meme templates | Meme editor | My memes |
|---|---|---|
| <img src="docs/screenshots/meme-templates.png" width="220" alt="Meme templates"> | <img src="docs/screenshots/meme-editor.png" width="220" alt="Meme editor"> | <img src="docs/screenshots/meme-gallery.png" width="220" alt="My memes"> |

| GIF source picker | GIF editor | Your GIFs |
|---|---|---|
| <img src="docs/screenshots/gif-source-picker.png" width="220" alt="GIF source picker"> | <img src="docs/screenshots/gif-editor.png" width="220" alt="GIF editor"> | <img src="docs/screenshots/gif-gallery.png" width="220" alt="Your GIFs"> |

> Screenshots were captured from the Android emulator. The same Compose UI runs on iOS.

## Why this project exists

This is a learning playground with a clear end goal:

- **Master Compose Multiplatform** — one codebase, real UI on Android and iOS.
- **Learn platform storage** — file system now, a local **database** (SQLDelight/Room)
  next, then syncing.
- **Ship to the stores** — publish the Android app **and** the iOS app to their
  respective App Stores, going through the full release + signing flow on both.
- **Keep both platforms first-class** — write shared logic in `commonMain`, drop to
  `expect`/`actual` only when the OS API demands it.

## What you can do

### Memes

- Browse a grid of templates (with a floating search bar), or import your own from the
  device photo gallery.
- Add multiple draggable, editable text boxes with the classic white-on-black meme style
  (semi-transparent block behind the text, matching the rendered output).
- **Save** → stored in your device photos *and* added to the in-app gallery.
- **My Memes** → the gallery: each tile shows the meme name overlaid on the image; tap
  any meme to re-open it with its template + text and keep editing, or remove it.
- **Share** / **Copy** to any chat app.

### GIFs

- Pick a **sample GIF** to edit, or pull in **your own video or GIF** from the device.
- **Trim** the clip to the part you want.
- Add **frame-by-frame text** — captions that appear on chosen frames — in an animated
  preview.
- **Save** → the finished GIF is written to your device gallery (Pictures/Photos) *and*
  added to the in-app gallery.
- **Your GIFs** → the gallery: tiles show the GIF title overlaid on the thumbnail; tap
  any GIF to edit it again, or remove it.
- **Share** / **Copy** the animated GIF.

## Quick start

| | Android | iOS |
|---|---|---|
| OS | Any | macOS + Xcode 15+ |
| JDK | 21 | 21 |
| Kotlin | 2.2.10 | 2.2.10 |

**Android**
```bash
./gradlew :composeApp:assembleDebug
adb install composeApp/build/outputs/apk/debug/composeApp-debug.apk
```
Or open the project in Android Studio and run the `composeApp` configuration.

**iOS** (Mac only)
```bash
# builds the shared Kotlin framework
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64
```
Then open `iosApp/iosApp.xcodeproj` in Xcode and press **Cmd+R**.

## How it's built

```
composeApp/src/
  commonMain/   shared UI + logic (Compose, data models, expect API)
  androidMain/  Bitmap/Canvas, MediaStore, FileProvider, Clipboard
  iosMain/      UIImage, Core Graphics, Photos, UIActivityViewController, UIPasteboard
  commonTest/   shared unit tests (no device needed)
iosApp/         thin Xcode project hosting the Compose view
```

**Storage**
- Layouts (text boxes) are saved per template as Base64 records
  (`filesDir/templates_state` on Android, `Documents/templates_state` on iOS),
  autosaved while you edit and restored when you return.
- Created memes and GIFs are persisted in an in-app **gallery** (`gallery/`): each entry
  stores the rendered image/GIF plus the template reference and encoded text, so it can
  be re-opened and edited. This is the stepping stone toward the planned local database.
- Finished GIFs are exported to the device gallery (`Pictures/Memer` on Android, the
  Photos library on iOS).

## Tests

```bash
./gradlew :composeApp:testDebugUnitTest
```

Covers the pure shared logic: template catalog, text formatting, file naming,
text-box mapping, and layout encode/decode round-trips.

## Roadmap

- [ ] Replace file-based gallery storage with a local database
- [ ] Template search + categories
- [ ] Android release build + Play Store submission
- [ ] iOS release build + App Store submission
- [ ] Image cropping / text styling options (font, color, outline)
- [ ] Template cloud sync across devices
