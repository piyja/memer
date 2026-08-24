# memer

A simple meme generator app built with **Kotlin Multiplatform (KMP)** + **Compose Multiplatform**.
Runs on both **Android** and **iOS** from a single shared codebase.

The user picks or imports a template image, adds any number of draggable text
boxes, sees a live preview, then **Save**, **Share**, or **Copy** the rendered meme.

## Features

- Template picker (grid of meme templates) with thumbnails
- Import templates from the device photo gallery (Android Photo Picker / iOS photo library)
- Editor with multiple draggable text boxes, live preview, and a **Reset** button
- Per-template layouts autosave and restore when you re-enter the editor
- Classic meme text style (white fill, black stroke, bold uppercase)
- Save straight into the system photo gallery
- Share to chat apps (WhatsApp, Telegram, Signal, etc.) via the system share sheet
- Copy image to clipboard for pasting into chats

## Project structure

```
memer/
  composeApp/                       Kotlin Multiplatform module
    src/
      commonMain/                   Shared UI + logic (Compose Multiplatform)
        kotlin/com/piyja/memer/
          App.kt                    Shared app composable + navigation state
          data/                     MemeTemplate, TemplateCatalog
          ui/screen/                TemplatePickerScreen, MemeEditorScreen
          ui/theme/                 Color, Type, Theme (expect)
          util/                     MemeText, MemeFileNaming, Platform (expect)
      androidMain/                  Android-specific implementations
        AndroidManifest.xml
        kotlin/com/piyja/memer/
          MainActivity.kt           Android entry point
          util/Platform.android.kt  actuals: Bitmap, Canvas, FileProvider, Intent, Clipboard
        res/                        Android resources (icons, themes, strings)
        assets/templates/           Meme template images (drop .jpg/.png here)
      iosMain/                      iOS-specific implementations
        kotlin/com/piyja/memer/
          MainViewController.kt     Exposes Compose UI to Swift
          util/Platform.ios.kt      actuals: UIImage, Core Graphics, UIPasteboard, UIActivityViewController
          ui/theme/Theme.ios.kt     Theme actuals (no dynamic color on iOS)
      commonTest/                   Shared unit tests (run on all platforms)
        kotlin/com/piyja/memer/
          data/                     MemeTemplateTest, TemplateCatalogTest
          util/                     MemeTextTest, MemeFileNamingTest
  iosApp/                           Thin Xcode project (Swift launcher)
    iosApp.xcodeproj/
    iosApp/
      iOSApp.swift                  SwiftUI app hosting ComposeUIViewController
      Info.plist
      Assets.xcassets/
```

## Requirements

| Tool | Android | iOS |
|---|---|---|
| OS | Any | **macOS** (Xcode requires a Mac) |
| JDK | 11+ | 11+ |
| Android Studio | Hedgehog / Iguana+ (with KMP plugin) | — |
| Xcode | — | 15+ |
| Kotlin | 2.2.10 | 2.2.10 |

Install the **Kotlin Multiplatform** plugin in Android Studio (Settings > Plugins)
for the best KMP tooling support.

## How to run on Android

### From the command line
```bash
./gradlew :composeApp:assembleDebug
```
The APK is generated at `composeApp/build/outputs/apk/debug/composeApp-debug.apk`.
Install on a connected device/emulator:
```bash
adb install composeApp/build/outputs/apk/debug/composeApp-debug.apk
```

### From Android Studio
1. Open the project root folder.
2. Select the `composeApp` configuration.
3. Choose an emulator or connected device and click **Run**.

## How to run on iOS

> iOS builds require a **Mac** with **Xcode** installed.

### From Xcode (recommended)
1. Open `iosApp/iosApp.xcodeproj` in Xcode
   (or open `iosApp/iosApp.xcworkspace`).
2. Select a simulator (e.g. iPhone 15) or a connected device.
3. Press **Cmd+R** to build and run.

The Xcode project runs a "Run Script" build phase that invokes:
```bash
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64
```
to build the shared Kotlin framework before compiling the Swift app.

### From the command line (simulator)
```bash
./gradlew :composeApp:linkDebugExecutableIosSimulatorArm64
```

### First-time iOS setup notes
- If Xcode prompts to update the project settings, accept.
- Set your **Signing Team** under *Target > Signing & Capabilities*.
- If the `ComposeApp` framework is not found, run the Gradle task above once
  manually so the framework is generated under
  `composeApp/build/bin/iosSimulatorArm64/debugFramework/`.

## Adding meme template images

Template images are loaded from the platform bundle:

- **Android**: drop `.jpg`/`.png` files into
  `composeApp/src/androidMain/assets/templates/`
- **iOS**: add the same images to the `iosApp` target in Xcode
  (drag them into the project navigator, ensure *Target Membership* is checked
  for `iosApp`).

Update the default list in
`composeApp/src/commonMain/kotlin/com/piyja/memer/data/TemplateCatalog.kt`
to match your file names, or implement platform asset scanning via
`expect`/`actual`.

## Running tests

Shared unit tests run on the JVM (no device needed) and cover the pure logic
shared across platforms:

```bash
./gradlew :composeApp:testDebugUnitTest
```

Current test suites (34 tests, all passing):
- `MemeTemplateTest` — data model equality/copy
- `TemplateCatalogTest` — default catalog contents, custom template registration
- `MemeTextTest` — text formatting (uppercase, trimming, special chars)
- `MemeFileNamingTest` — generated file name format and uniqueness
- `MemeTextBoxTest` — positioned-text mapping and blank-box filtering
- `TemplateStateCodecTest` — encode/decode round trip of saved layouts

## How saving / sharing / copying works

- **Save** — renders the meme at full resolution and inserts it into the system
  photo gallery:
  - Android: `MediaStore` (`Pictures/Memer`); no permissions needed on API 29+,
    legacy `WRITE_EXTERNAL_STORAGE` only on API ≤ 28
  - iOS: `Documents/memes/` (Photos-framework integration pending Xcode setup)
- **Share** — renders to a staging file and hands it to the platform share sheet:
  - Android: staged under `cacheDir/shared_memes/`, then `Intent.ACTION_SEND`
    + `FileProvider`
  - iOS: `UIActivityViewController`
- **Copy** — copies the image so it can be pasted in chat apps:
  - Android: `ClipboardManager` with a content URI
  - iOS: `UIPasteboard` with the `UIImage`

Text-box layouts are stored per template (Android: `filesDir/templates_state/`,
iOS: `Documents/templates_state/`) as Base64-encoded records, debounced while
editing and flushed when leaving the editor. **Reset** clears the saved layout.

## Maintaining both platforms

- Write new features in `commonMain` first. Only drop to `expect`/`actual`
  when you need an OS-specific API.
- Keep the `expect` surface small — every `expect` is a platform-specific
  implementation point.
- Shared logic tests live in `commonTest` and run on every platform.
- The asset catalog stays unified: same template images on both platforms
  (Android `assets/`, iOS bundle resources).

## Troubleshooting

**AGP / KMP conflict on Android build:**
This project sets `android.builtInKotlin=false` and `android.newDsl=false` in
`gradle.properties` so the standalone Kotlin Multiplatform plugin works with
AGP 9. Do not remove these unless you migrate to AGP's built-in KMP DSL.

**Kotlin/Native targets disabled on non-Mac:**
On Linux/Windows the iOS targets are skipped automatically
(`iosArm64`, iosSimulatorArm64, iosX64`). Use a Mac to build iOS.

**iOS framework not found in Xcode:**
Run `./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64` once, then
build again in Xcode.
