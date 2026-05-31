# Mochi — Japanese Flashcards (Compose Multiplatform)

A Duolingo-inspired flashcard app built around the **Kaishi 1.5k** deck (1500 words),
focused on practicing **native animations** (flip + spring) and **Lottie** in
Compose Multiplatform (Android + iOS).

## Structure

```
Mochi/
├─ settings.gradle.kts            include(":composeApp")
├─ build.gradle.kts               project plugins (apply false)
├─ gradle/libs.versions.toml      centralized versions
├─ composeApp/                    Compose Multiplatform module
│  ├─ build.gradle.kts            android + iOS targets, SQLDelight, Compottie
│  └─ src/
│     ├─ commonMain/
│     │  ├─ kotlin/com/mochi/
│     │  │  ├─ App.kt             shared entry point (creates DB, seeds, loads deck)
│     │  │  ├─ ui/                FlipCard, NextButton, SuccessAnimation, FlashcardScreen
│     │  │  └─ data/              DriverFactory (expect), DeckRepository, Seed
│     │  ├─ sqldelight/com/mochi/db/Flashcard.sq   schema + queries
│     │  └─ composeResources/files/deck.json       1500 cards (generated from the .apkg)
│     ├─ androidMain/             MainActivity, DriverFactory (Android), res/, manifest
│     └─ iosMain/                 DriverFactory (iOS), MainViewController
└─ iosApp/                        SwiftUI app that hosts Compose (see the iOS note below)
```

## Run on Android

1. Open the `Mochi` folder in Android Studio.
2. Run **Gradle Sync** (the IDE downloads dependencies).
3. Run the `composeApp` configuration on an emulator/device.

On first launch the app seeds itself: it reads `deck.json` and populates SQLite
(`flashcards.db`). The screen shows one card at a time — tap to flip it, and use
**Next** (spring bounce) to advance.

## Run on iOS

The Kotlin iOS sources (`iosMain`) and the Swift sources (`iosApp/iosApp/`) are ready.
Only the **Xcode wrapper** (`iosApp.xcodeproj`) is missing, and it's safer to generate it
with tooling than by hand. Two options:

- Use the **Kotlin Multiplatform** plugin in Android Studio (Tools → KMP) to generate/open
  the `iosApp`; or
- Recreate the iOS skeleton with the **Kotlin Multiplatform Wizard** (kmp.jetbrains.com) and
  point it at this module's `ComposeApp` framework.

The Swift code already calls `MainViewControllerKt.MainViewController()`, so once the
`.xcodeproj` is linked to the framework, iOS runs the same Compose UI as Android.

## Technical notes

- **Versions**: Kotlin bumped to 2.2.20 (recommended for native/iOS targets). Compose
  Multiplatform, SQLDelight and Compottie are centralized in `gradle/libs.versions.toml`.
  We're on the bleeding edge (AGP 9.2, compileSdk 36); if Gradle Sync suggests version
  tweaks, align them there.
- **Audio**: `deck.json` carries each card's audio file name, but the MP3s live inside the
  `.apkg`. Playing audio is a future enhancement (extract and bundle the media).
- **Lottie**: `SuccessAnimation.kt` is ready but needs a `celebration.json`
  (from lottiefiles.com) in `composeResources/files/`. It isn't wired into the first screen yet.
- **SRS**: `DeckRepository.recordAnswer` has a spaced-repetition skeleton (SM-2) to grow the
  app from a plain flashcard app into a smart-review one.

## Regenerate the deck from another .apkg

The `convert_apkg.py` converter (kept outside the repo, in the study material) produces
`deck.json`, `deck.csv` and `seed.sql` from any `.apkg`. Just copy the new `deck.json` into
`composeApp/src/commonMain/composeResources/files/`.
