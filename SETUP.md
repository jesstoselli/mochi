# Setup & Development — Mochi

How to build, run and work on the project. For what the app is and what it does, see
**[README.md](README.md)**.

## Architecture

The project follows the module layout recommended for **AGP 9+**, where the Kotlin
Multiplatform plugin is no longer compatible with `com.android.application` in the same
module. So the code is split in two:

- **`composeApp/`** — the shared Kotlin Multiplatform **library** (`com.android.kotlin.multiplatform.library`).
  Holds all cross-platform code: UI, data layer, SQLDelight schema, resources, and the
  `expect`/`actual` declarations. Produces the `ComposeApp` framework consumed by iOS.
- **`androidApp/`** — a pure Android **application** (`com.android.application`) that depends on
  `composeApp` and only contains the Android entry point (`MainActivity`), manifest and resources.
- **`iosApp/`** — the SwiftUI app that hosts the same Compose UI on iOS.

```
Mochi/
├─ settings.gradle.kts            include(":composeApp", ":androidApp")
├─ build.gradle.kts               project plugins (apply false)
├─ gradle/libs.versions.toml      centralized versions
├─ composeApp/                    shared KMP library (Android-KMP library plugin + iOS)
│  ├─ build.gradle.kts
│  └─ src/
│     ├─ commonMain/
│     │  ├─ kotlin/com/mochi/
│     │  │  ├─ App.kt             thin host: bottom nav + routes tabs to screens
│     │  │  ├─ review/            ReviewViewModel (state machine for the study flow)
│     │  │  ├─ ui/screens/        Home, Flashcard, SessionComplete, Stats, Settings
│     │  │  ├─ ui/components/     FlipCard, AnswerButtons, SuccessAnimation, charts
│     │  │  ├─ ui/theme/          Mochi Box colors/shapes/typography + fonts (expect)
│     │  │  ├─ util/              Time (epoch-day helpers)
│     │  │  └─ data/              DriverFactory (expect), DeckRepository, stores, Seed
│     │  ├─ sqldelight/com/mochi/db/   *.sq schema + queries, *.sqm migrations
│     │  └─ composeResources/files/    deck.json (1500 cards), audio/, fonts/
│     ├─ androidMain/             DriverFactory + AudioPlayer + fonts (Android actuals)
│     └─ iosMain/                 DriverFactory + AudioPlayer + fonts (iOS actuals), MainViewController
├─ androidApp/                    pure Android application
│  └─ src/main/                   MainActivity, AndroidManifest, res/
└─ iosApp/                        SwiftUI app that hosts Compose (pre-wired .xcodeproj)
```

## Run on Android

1. Open the `Mochi` folder in Android Studio.
2. Run **Gradle Sync** (the IDE downloads dependencies).
3. Run the **`androidApp`** configuration on an emulator/device.

On first launch the app seeds itself: it reads `deck.json` and populates SQLite. The Review
tab opens on Home; start a session to see one card at a time — tap to flip, tap **Listen**
for pronunciation, and rate your recall to advance (animated card transition).

## Run on iOS (needs macOS + Xcode)

The iOS app builds and runs. `composeApp` produces the static `ComposeApp` framework and
`composeApp/src/iosMain/.../MainViewController.kt` exposes the Compose UI; the pre-wired
`iosApp/iosApp.xcodeproj` hosts it (its `ContentView` calls `MainViewControllerKt.MainViewController()`).

1. Open `iosApp/iosApp.xcodeproj` in Xcode, pick the `iosApp` scheme + an iOS Simulator, and Run.
2. A "Run Script" build phase invokes `./gradlew :composeApp:embedAndSignAppleFrameworkForXcode`
   to build and embed the framework (first build is slow — it compiles Kotlin/Native). The script
   sets `JAVA_HOME` by scanning for Android Studio's bundled JBR, so no system JDK is required.
3. `OTHER_LDFLAGS` links `-lsqlite3` (SQLDelight's native driver) and `-framework AVFoundation`.

Notes: keep Kotlin 2.2.20+ (native targets). If Xcode can't find the framework, confirm the
build phase references `:composeApp`.

Current iOS limitations (Android has neither): pronunciation audio is a no-op on iOS
(`NSData`-from-`ByteArray` interop pending), and iOS uses the system fonts instead of the bundled
Nunito/Zen Maru Gothic (font-from-bytes interop pending — the system CJK font still renders
Japanese fine). The core flow — flashcards, flip, SRS, stats, settings — works on both platforms.

## Code quality

ktlint + detekt are wired into both modules. Run `./gradlew ktlintFormat` to auto-format, and
`./gradlew ktlintCheck detekt` to verify. detekt config lives in `config/detekt/detekt.yml`.

## Technical notes

- **Versions**: Kotlin 2.2.20, AGP 9.2, compileSdk 36 — bleeding edge. All versions are
  centralized in `gradle/libs.versions.toml`. If Gradle Sync suggests tweaks, align them there.
- **Fonts**: bundled in `composeResources/files/fonts/` and applied via `MochiTheme` — Nunito
  for UI/Latin text, Zen Maru Gothic for Japanese (exposed through `LocalJapaneseFont`, used by
  the card). They're loaded from raw bytes with `Res.readBytes(...)` + `Font(identity, data, …)`
  rather than the generated `Res.font.*` accessors, which weren't reliably generated under the
  current AGP 9 KMP-library + Compose Resources setup. Both are OFL (open source); Zen Maru
  Gothic (a ~3.8 MB/weight CJK font) was subset with `fonttools` to the deck's glyphs
  (kanji used + full kana + ASCII), ~440 KB per weight.
- **Audio**: the word pronunciation MP3s (~17 MB, 1500 clips) are extracted from the deck and
  bundled in `composeResources/files/audio/` under clean hashed names; `deck.json` points each
  card at its file. Playback uses a cross-platform `AudioPlayer` (`expect`/`actual`): Android
  uses `MediaPlayer` with an in-memory data source; iOS is currently a no-op (Foundation
  `NSData`-from-bytes interop pending). Sentence audio (another ~55 MB) is intentionally left
  out for now to keep the app slim.
- **Celebration**: `SuccessAnimation.kt` is a pure Compose Canvas animation (a green
  circle pops in with a spring bounce, a halo ring expands and fades, and a checkmark is
  stroked on). Drawn on the GPU, so it renders identically on Android and iOS — no external
  renderer or asset needed. (We started with Lottie/Compottie but dropped it: the polished
  LottieFiles exports relied on expressions, text layers and nested precomps that the
  pure-Kotlin renderer doesn't support.)
- **Architecture (MVVM)**: `ReviewViewModel` (multiplatform `androidx.lifecycle.ViewModel`)
  owns the whole flow as a `ReviewUiState` state machine (Loading → Reviewing → Complete →
  CaughtUp) and exposes the actions (`answer`, `playCurrentAudio`, `startSession`, `finish`).
  `App` is a thin host that observes the state and routes to a screen; the screens are
  presentation-only (data in, callbacks out). There's one ViewModel for the review flow —
  the other screens are stateless, so they don't need their own.
- **Navigation**: a bottom `NavigationBar` with three tabs — Review, Stats, Settings — hosted
  by `App` (crossfade between tabs). Each tab is a presentation-only screen.
- **Settings**: `SettingsViewModel` holds the theme preference (System/Light/Dark) and the
  daily new-card limit (10/20/30/Unlimited), persisted in the `app_setting` key/value table via
  `SettingsStore`; `App` applies the theme to `MochiTheme`.
- **Home**: the Review tab opens on a landing screen (`HomeScreen`) showing how many cards
  are ready, with a "Start studying" button — so the app doesn't auto-play audio on launch
  before the user chooses to study. When nothing is due it shows an "all caught up" message.
- **SRS loop (Anki-style)**: each session is the day's queue — all due reviews
  (`selectDueReviews`) plus new cards (`selectNewCards`) up to the remaining daily limit.
  `DeckRepository.recordAnswer` updates the card's schedule (simplified SM-2) and writes a row
  to the `review_log` table. When the queue is empty, `CaughtUpScreen` is shown.
- **Stats**: `StatsViewModel` derives streak (consecutive days with a review), reviews today,
  and words learned from `review_log` (via `StatsStore`). The daily new-card limit also reads
  today's new count from this log. `todayEpochDay()` (expect/actual) gives the local day index.

## Regenerate the deck from another .apkg

The `convert_apkg.py` converter (kept outside the repo, in the study material) produces
`deck.json`, `deck.csv` and `seed.sql` from any `.apkg`. Just copy the new `deck.json` into
`composeApp/src/commonMain/composeResources/files/`.
