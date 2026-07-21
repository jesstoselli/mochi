# Mochi — Session Context / Handoff

Working notes for continuing development (e.g. in the Code tab). For the product overview see
[README.md](../README.md); for build/run instructions see [SETUP.md](../SETUP.md).

## What Mochi is

A Duolingo-inspired Japanese flashcard app built with **Compose Multiplatform** (Android + iOS),
one shared Kotlin codebase, around the **Kaishi 1.5k** Anki deck (1500 words). Started as an
animation playground and grew into an Anki-style study app: SRS, pronunciation audio, stats,
a "Still learning" list, theming, haptics, a mascot, a daily reminder, and a splash screen.

## Tech stack & versions

- Kotlin 2.2.20, Compose Multiplatform 1.10.3, AGP 9.2.1, Gradle, compileSdk 36, minSdk 26, Java 11.
- SQLDelight 2.2.1 (with `coroutines-extensions` for reactive queries).
- `org.jetbrains.androidx.lifecycle` ViewModel 2.10.0 (multiplatform MVVM).
- kotlinx coroutines 1.10.2, kotlinx.serialization 1.9.0.
- androidx.core:core-splashscreen 1.0.1 (Android).
- ktlint 14.2.0 + detekt 2.0.0-alpha.2. Line length limit is **120** (`.editorconfig`).

## Module layout (AGP 9 split)

- `composeApp/` — shared KMP **library** (`com.android.kotlin.multiplatform.library` via
  `kotlin { android { namespace="com.mochi.shared"; ... } }`) + iOS framework `ComposeApp`
  (isStatic, linkerOpts `-lsqlite3`). Holds all cross-platform code.
- `androidApp/` — pure Android **application** (`com.android.application`), namespace `com.mochi`.
  Only the Android entry points live here (MainActivity, reminder alarm/notification, res).
- `iosApp/` — the SwiftUI host with `iosApp.xcodeproj` (wizard-generated).

Source sets: `commonMain` (UI + data + VMs), `androidMain` / `iosMain` for `expect`/`actual`
(DriverFactory, AudioPlayer, fonts, system bars, reminder scheduler).

## Architecture patterns

- **MVVM.** `App.kt` is a thin host: builds deps + ViewModels, applies theme, hosts a 4-tab
  bottom nav (Review · Learning · Stats · Settings). Screens are presentation-only (data in,
  callbacks out).
- **DI via interfaces** so ViewModels are unit-testable with fakes: `ReviewDeck`,
  `NewCardCounter`, `NewCardLimitSource`, `ReminderScheduler`.
- **Reactive data (SQLDelight Flow).** `LearningViewModel`, `StatsViewModel` and
  `LibraryViewModel` expose `StateFlow`s built from `query.asFlow().mapToList(Dispatchers.Default)`
  → `map` → `stateIn(viewModelScope, WhileSubscribed(5s), initial)`. UI updates itself; no manual
  refresh. `ReviewViewModel` is a state machine (Loading → Idle → Reviewing → Complete), still
  driven imperatively (it's a session, not a data view). `Idle` = the Library grid is shown.
- **Library & per-unit sessions.** The Review tab's landing is the **Library**: a grid of 30
  study **units** of 50 words, derived from frequency rank (no schema change — `LibraryStore`
  chunks the frequency-ordered cards). Tapping a unit opens a session scoped to that unit
  (`ReviewViewModel.openUnit(id)`): its due + new cards up to the remaining **global** daily
  new-card limit; if nothing is scheduled it falls back to a practice drill of that unit.
  Streak, Stats and the "Still learning" list stay **global** (not per-unit).
- **Animations (all `commonMain`, pure Canvas/graphicsLayer).** Reusable, data-agnostic
  modifiers/composables live in `com.mochi.ui.motion`: 3D flip (spring + mid-flip shadow) with
  physics `swipeToDismissCard` (drag resistance before flip, elastic throw after), `ConfettiBurst`
  particle system (fires on session complete + every 10-answer session streak), `LiquidProgress`
  (sine-wave fill on unit cards), `AnimatedCounter` (odometer for streak/stats), `pressBounce`.
  A `SharedTransitionLayout` morphs the tapped unit card into the study session.
- **Persistence.** SQLDelight: `flashcard`, `app_setting` (key/value), `review_log`. Migrations
  are `*.sqm` files (`1.sqm`, `2.sqm`, `3.sqm`); the `.sq` `CREATE` statements are the current
  schema and must stay in sync with migrations. Schema version = migration count + 1.
- **Drawn, not imported.** The mascot (`MochiLogo`), success checkmark, confetti and liquid
  progress are pure Compose `Canvas` — no image assets, identical on both platforms.

## Key packages (in composeApp/commonMain)

- `com.mochi.App` — host + bottom nav + tab routing; wraps the Review tab in `SharedTransitionLayout`.
- `com.mochi.review.{ReviewViewModel, ReviewUiState}` — SRS session state machine; `openUnit(id)`,
  session-streak + `streakMilestone` signal, `lastOpenedUnitId` (drives the shared-element key).
- `com.mochi.library.{LibraryStore, LibraryViewModel, UnitSummary}` — unit derivation
  (`toUnitSummaries`, pure/tested) + reactive `units()` Flow. `UNIT_SIZE = 50`.
- `com.mochi.learning.{LearningStore, LearningViewModel}` — "Still learning" list (reactive).
- `com.mochi.stats.{StatsStore, StatsViewModel}` — streak/reviews/7-day chart (reactive).
- `com.mochi.settings.{SettingsStore, SettingsViewModel, ThemeMode}` — prefs.
- `com.mochi.reminder.{ReminderScheduler, ReminderTime}` — reminder contract (expect-like).
- `com.mochi.data.{DeckRepository, DatabaseFactory, Seed}` — data layer; `ReviewDeck.cardsInUnit(id)`.
- `com.mochi.ui.screens.*` — Library, Flashcard, SessionComplete, Learning, Stats, Settings
  (Home was removed; the Library replaced it as the Review landing).
- `com.mochi.ui.motion.*` — reusable animation building blocks: `swipeToDismissCard`, `pressBounce`,
  `ConfettiBurst` (+`Particle`), `LiquidProgress`, `AnimatedCounter`.
- `com.mochi.ui.components.*` — MochiLogo, FlipCard, AnswerButtons, BouncyButton, SuccessAnimation.
- `com.mochi.ui.theme.*` — Mochi Box palette, typography (LocalJapaneseFont), shapes, SystemBars.

## Domain behaviors (important, non-obvious)

- **Answer rating is boolean.** "I knew it" → `correct = 1`; "Still learning" → `correct = 0`,
  logged in `review_log`.
- **SRS (simplified SM-2).** In `DeckRepository.recordAnswer`: correct → ease +0.1 and interval
  grows by ease; wrong → interval = 1 day, ease −0.2 (min 1.3). So a missed card comes back the
  next day and its future intervals grow slower.
- **In-session relearning (Anki-style).** In `ReviewViewModel.answer`, a missed card is appended
  to the end of the current session queue and reappears until answered correctly (progress total
  grows on a miss). `recordAnswer` returns the updated `Flashcard` so the requeued copy carries
  fresh SRS state.
- **"Still learning" list** = cards whose **most recent** `review_log` row has `correct = 0`
  (query `stillLearning` joins flashcard, latest row per card). A card leaves the list once its
  latest answer is correct. Because of in-session relearning, cards usually leave the list by the
  end of a session (last answer ends up correct).
- **Practice** logs answers with `practice = 1` (so the list updates and relearning works) but
  does **not** reschedule the card and is **excluded** from streak/daily stats/new-card limit
  (those queries filter `practice = 0`). A unit with nothing due opens as a practice drill.
- **Daily new-card limit** (10/20/30/Unlimited=0) caps new cards per day. It stays **global**
  even though sessions are per-unit: a unit's queue is its due reviews + its new cards up to the
  **remaining** global allowance, so once the daily budget is spent a fresh unit shows reviews only.
- **Session streak** (`ReviewViewModel`) counts consecutive correct answers, resets on a miss;
  crossing a multiple of 10 sets `streakMilestone` on exactly one `Reviewing` emission → confetti.
- **Units are derived, not stored.** Unit N = the cards ranked `[N*50, N*50+50)` by frequency.
  `learnedCount` = cards with `next_review != null` (matches the "words learned" stat);
  `dueCount` = cards with `next_review <= now` (new cards aren't counted as due but are still offered).

## Platform specifics

- **Audio** (`AudioPlayer` expect/actual): Android uses `MediaPlayer` with an in-memory data
  source; **iOS is a no-op** (Foundation `NSData`-from-bytes interop was deferred).
- **Fonts** (`rememberMochiFonts` expect/actual): Android loads bundled Nunito + Zen Maru Gothic
  from bytes; **iOS uses system fonts** (still renders Japanese fine).
- **Reminder** (`ReminderScheduler` expect-like, one impl per platform):
  - Android `AndroidReminderScheduler` (in androidApp): `AlarmManager.setAndAllowWhileIdle`
    (inexact), `ReminderReceiver` posts the notification + re-arms next day + handles
    `BOOT_COMPLETED`; prefs mirrored in SharedPreferences; requests `POST_NOTIFICATIONS` (API 33+).
    Status icon `ic_stat_mochi.xml`. Notification copy: "もち misses you! …".
  - iOS `IosReminderScheduler` (iosMain): `UNUserNotificationCenter` +
    `UNCalendarNotificationTrigger(repeats = true)`. Added `-framework UserNotifications` to Xcode
    `OTHER_LDFLAGS`.
- **Splash** (Android 12+ via core-splashscreen): `Theme.Mochi.Splash` shows the mochi on a cream
  badge over a theme-aware background (`values` vs `values-night` colors). `installSplashScreen()`
  in MainActivity before `super.onCreate`.
- **App icon**: adaptive (cream bg + mochi foreground PNG per density) + legacy mipmaps; iOS
  `AppIcon.appiconset` 1024. Source SVG kept at `docs/icon/mochi.svg`.

## Interactive animations (latest session)

- **Library grid** (`LibraryScreen`): 30 unit cards, distinct pastel per unit, a `LiquidProgress`
  wave fill for `learned/50`, and a "N due" badge. Confirmed working on device (user, both flows).
- **Flashcard**: spring flip with a mid-flip shadow "lift"; `swipeToDismissCard` (drag resists +
  springs back before flip, tilts and throws off-screen after flip → rates the card). Right = "I
  knew it", left = "Still learning"; the on-screen `AnswerButtons` still work in parallel.
  The intent-overlay pills were tried then **removed** (they cluttered the card during drag).
- **Confetti** (`ConfettiBurst`): on session complete and on each 10-answer session streak.
- **Odometer counters** (`AnimatedCounter`): Stats numbers and the in-session 🔥 streak HUD.
- **Shared-element transition**: the tapped unit card expands into the session and contracts back.
- Existing: haptics on `BouncyButton`s + Settings rows; reminder `TimePicker`.

## Conventions

- **All app code + commit messages in English** (portfolio for international market). Chat is PT-BR.
- **Commits are authored as** `Jessyca Toselli <toselli.jess@gmail.com>` (git history was rewritten
  earlier to fix author/committer; local git config set accordingly).
- **Commits: granular, one logical change each; never `git push` (user does that manually).**
- **ktlint/detekt are green** (`./gradlew :composeApp:ktlintCheck :composeApp:detekt`). The repo
  had pre-existing debt from ktlint 1.x's opinionated rules; those wrapping/signature rules are
  **disabled in `.editorconfig`** (the codebase uses a compact style), generated code is excluded,
  and detekt `LongMethod` ignores `@Composable`. Meaningful checks (indent, ≤120, trailing commas)
  stay on. Imports sorted (case-sensitive, uppercase before lowercase).
- **Tests:** `commonTest` runs on the JVM via `./gradlew :composeApp:testAndroidHostTest` (fast;
  `withHostTest {}` is enabled) or on iOS-sim via `:allTests`. Compile check: `:compileAndroidMain`
  (the `:compileDebugKotlinAndroid` task does NOT exist in this AGP-9 KMP-library module).
- Screenshots referenced by README live in `docs/screenshots/` (not committed yet).

## Pending / follow-ups

- **Push pending.** All work merged to `main` locally; `main` is ~21 commits ahead of `origin/main`
  (includes the earlier author-fix history rewrite → needs `git push --force-with-lease origin main`).
  Nothing pushed by the assistant.
- **Mascot dropped from the Review landing.** The bouncing mochi + "pop" sound lived on the old
  `HomeScreen`, which the Library replaced. If desired, re-home the mascot (e.g. a small tappable
  `MochiLogo` in the Library header) to keep that delight.
- **iOS**: common code compiles for iOS; animations are Compose-common so they render on both.
  Still deferred: **iOS audio** (NSData/AVAudioPlayer) and **iOS bundled fonts** (uses system fonts).
  Reminder (UserNotifications) still to confirm on a real device.
- **Screenshots**: drop PNGs into `docs/screenshots/` (now: library, flashcard, session-complete,
  learning, stats, settings) — README already links them.
- Possible niceties: shuffle the re-entry position of a missed last card; a "Still learning (N)"
  card on Stats was declined.

## Recent commits (latest first)

- build: make ktlint/detekt green (relax opinionated rules, exclude generated)
- feat: remove swipe intent overlay pills from the card
- feat: shared-element transition from library unit to session
- feat: odometer counters for stats and session streak
- feat: add liquid sine-wave progress to unit cards
- feat: celebrate session completion and 10-streaks with confetti
- feat: add Canvas confetti particle system
- feat: spring flip with dynamic shadow and physics swipe-to-rate
- feat: add swipeToDismissCard physics gesture modifier
- refactor: extract reusable Modifier.pressBounce
- feat: replace Home with a unit Library grid on the Review tab
- feat: scope review sessions to a unit and track session streak
- feat: derive study units of 50 from frequency rank (+ LibraryStore/VM, cardsInUnit)
- test: enable fast Android host tests for commonTest
- docs: add interactive animations spec + implementation plan
- Requeue missed cards within the session (Anki-style relearning)
- Add configurable daily study reminder notification (Android + iOS)
- Update README: still-learning list, mascot, reactivity, splash, haptics
- Add Android 12+ splash screen with the mochi (theme-aware)
- Make cold-start background follow system dark/light theme
- Add haptic feedback to settings option rows / to buttons
- Make stats reactive via SQLDelight Flow
- Make the Still-learning list reactive via SQLDelight Flow
- Log practice answers so the Still-learning list reflects practice too
- Add "Still learning" word list as a new bottom tab
- Add tap sound + haptic feedback to the mochi mascot
- Bounce the mochi mascot on tap / on entry; hiragana above the title
- Add Mochi app icon (cream background) for Android + iOS
