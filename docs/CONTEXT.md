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
- **Reactive data (SQLDelight Flow).** `LearningViewModel` and `StatsViewModel` expose
  `StateFlow`s built from `query.asFlow().mapToList(Dispatchers.Default)` → `map` →
  `stateIn(viewModelScope, WhileSubscribed(5s), initial)`. UI updates itself; no manual refresh.
  `ReviewViewModel` is a state machine (Loading → Home → Reviewing → Complete), still driven
  imperatively (it's a session, not a data view).
- **Persistence.** SQLDelight: `flashcard`, `app_setting` (key/value), `review_log`. Migrations
  are `*.sqm` files (`1.sqm`, `2.sqm`, `3.sqm`); the `.sq` `CREATE` statements are the current
  schema and must stay in sync with migrations. Schema version = migration count + 1.
- **Drawn, not imported.** The mascot (`MochiLogo`) and success checkmark are pure Compose
  `Canvas` vector drawing via `PathParser` — no image assets, identical on both platforms.

## Key packages (in composeApp/commonMain)

- `com.mochi.App` — host + bottom nav + tab routing.
- `com.mochi.review.ReviewViewModel` — SRS session state machine.
- `com.mochi.learning.{LearningStore, LearningViewModel}` — "Still learning" list (reactive).
- `com.mochi.stats.{StatsStore, StatsViewModel}` — streak/reviews/7-day chart (reactive).
- `com.mochi.settings.{SettingsStore, SettingsViewModel, ThemeMode}` — prefs.
- `com.mochi.reminder.{ReminderScheduler, ReminderTime}` — reminder contract (expect-like).
- `com.mochi.data.{DeckRepository, DatabaseFactory, Seed}` — data layer.
- `com.mochi.ui.screens.*` — Home, Flashcard, SessionComplete, Learning, Stats, Settings.
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
- **Practice ("Practice anyway")** logs answers with `practice = 1` (so the list updates and
  relearning works) but does **not** reschedule the card and is **excluded** from streak/daily
  stats/new-card limit (those queries filter `practice = 0`).
- **Daily new-card limit** (10/20/30/Unlimited=0) caps new cards per day; the day's queue is due
  reviews + new cards up to the remaining limit.

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

## UX touches added this session

- Mascot on Home: taps above the title; bounces in on launch and again on tap (spring), with a
  soft "pop" sound (`files/click.wav`) + haptic. Hiragana もち sits above "Mochi".
- Haptics on all `BouncyButton`s and on Settings option rows (fires only on actual change).
- Reminder settings: switch + Material 3 `TimePicker` dialog (24h).

## Conventions

- **All app code + commit messages in English** (portfolio for international market). Chat is PT-BR.
- **Commits are authored as** `Jessyca Toselli <toselli.jess@gmail.com>` (git history was rewritten
  earlier to fix author/committer; local git config set accordingly).
- Keep ktlint/detekt clean: imports sorted (case-sensitive, uppercase before lowercase), lines
  ≤120, detekt `MagicNumber` is off, `FunctionNaming` ignores `@Composable`.
- Screenshots referenced by README live in `docs/screenshots/` (not committed yet).

## Pending / follow-ups

- **`git push --force`** is still pending to publish the rewritten commit history (author fix).
  All work is committed locally; nothing pushed by the assistant.
- **iOS build not yet verified on device** for: reminder (UserNotifications interop), and it still
  has no audio and uses system fonts. Confirm it builds in Xcode.
- **iOS audio** (NSData/AVAudioPlayer) and **iOS bundled fonts** remain deferred.
- **Screenshots**: drop PNGs into `docs/screenshots/` (home, review, session-complete, learning,
  stats, settings) — README already links them.
- Possible niceties discussed but not done: shuffle the re-entry position of a missed last card;
  make Stats reactive was done; a "Still learning (N)" card on Stats was declined.

## Recent commits (latest first)

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
