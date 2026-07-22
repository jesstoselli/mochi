# Reduced Motion and Settings Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a persisted Full/System/Reduced motion preference, honor Android and iOS accessibility settings, adapt every Mochi animation, and replace the flat Settings list with the approved grouped-card design.

**Architecture:** Persist only the user's `MotionPreference`; obtain the native setting through a lifecycle-safe `expect`/`actual` composable; resolve both inputs into a pure `MotionPolicy` provided at the app root. Animation components consume `LocalMotionPolicy`, while Settings remains state-hoisted and emits preference changes.

**Tech Stack:** Kotlin 2.2.20, Compose Multiplatform 1.10.3, Material 3, SQLDelight 2.2.1, kotlinx.coroutines, UIKit accessibility notifications, Android `Settings.Global`, kotlin.test, Compose Multiplatform UI tests.

## Global Constraints

- `Full` is the default for missing, invalid, or unreadable stored values.
- `System` follows Android animator duration scale and iOS Reduce Motion while the app is running.
- Reduced motion keeps finger-tracked gestures, haptics, and short fades.
- Reduced motion removes 3D rotation, bounce, particles, continuous waves, large spatial movement, and decorative travel.
- Review answers, thresholds, streak events, navigation destinations, and haptic timing must not change.
- Settings uses the approved Mochi cards layout and English product copy from the design spec.
- No database migration or new runtime dependency is required.
- App code, tests, documentation, and commit messages remain in English.
- Commits are granular and the assistant never pushes.

---

## File map

### New files

- `composeApp/src/commonMain/kotlin/com/mochi/settings/MotionPreference.kt` — persisted user choice.
- `composeApp/src/commonMain/kotlin/com/mochi/ui/motion/MotionPolicy.kt` — pure resolver, policy, and composition local.
- `composeApp/src/commonMain/kotlin/com/mochi/ui/motion/SystemMotionPreference.kt` — common `expect` declaration.
- `composeApp/src/androidMain/kotlin/com/mochi/ui/motion/SystemMotionPreference.android.kt` — Android observer.
- `composeApp/src/iosMain/kotlin/com/mochi/ui/motion/SystemMotionPreference.ios.kt` — UIKit observer.
- `composeApp/src/commonMain/kotlin/com/mochi/ui/motion/MotionValues.kt` — pure presentation decisions used by animated components.
- `composeApp/src/commonMain/kotlin/com/mochi/ui/motion/ReducedReward.kt` — static reward artwork with a short fade.
- `composeApp/src/commonTest/kotlin/com/mochi/settings/SettingsStoreTest.kt` — persistence behavior.
- `composeApp/src/commonTest/kotlin/com/mochi/settings/SettingsViewModelTest.kt` — hoisted state behavior.
- `composeApp/src/commonTest/kotlin/com/mochi/ui/motion/MotionPolicyTest.kt` — resolver matrix.
- `composeApp/src/commonTest/kotlin/com/mochi/ui/motion/MotionValuesTest.kt` — reduced/full presentation decisions.
- `composeApp/src/commonTest/kotlin/com/mochi/ui/screens/SettingsScreenTest.kt` — Settings structure and interactions.

### Modified files

- `composeApp/build.gradle.kts` — add Compose UI test support to `commonTest`.
- `composeApp/src/commonMain/kotlin/com/mochi/settings/SettingsStore.kt` — persist motion and expose a testable key/value boundary.
- `composeApp/src/commonMain/kotlin/com/mochi/settings/SettingsViewModel.kt` — expose and update motion state.
- `composeApp/src/commonMain/kotlin/com/mochi/App.kt` — provide policy and select reduced transitions.
- `composeApp/src/commonMain/kotlin/com/mochi/ui/screens/SettingsScreen.kt` — grouped-card redesign and dialogs.
- `composeApp/src/commonMain/kotlin/com/mochi/ui/screens/LibraryScreen.kt` — disable shared bounds in reduced mode.
- `composeApp/src/commonMain/kotlin/com/mochi/ui/screens/FlashcardScreen.kt` — reduced card-to-card transition and shared bounds.
- `composeApp/src/commonMain/kotlin/com/mochi/ui/motion/PressBounce.kt` — suppress spring scale.
- `composeApp/src/commonMain/kotlin/com/mochi/ui/motion/LiquidProgress.kt` — stop the infinite wave.
- `composeApp/src/commonMain/kotlin/com/mochi/ui/motion/AnimatedCounter.kt` — use crossfade instead of slide.
- `composeApp/src/commonMain/kotlin/com/mochi/ui/motion/SwipeToDismissCard.kt` — remove tilt/throw/bounce while preserving drag and threshold behavior.
- `composeApp/src/commonMain/kotlin/com/mochi/ui/motion/ConfettiBurst.kt` — select particles or static reward.
- `composeApp/src/commonMain/kotlin/com/mochi/ui/components/FlipCard.kt` — crossfade faces instead of rotating.
- `composeApp/src/commonMain/kotlin/com/mochi/ui/components/MochiLogo.kt` — static entry/tap response and decorative mode.
- `composeApp/src/commonMain/kotlin/com/mochi/ui/components/MochiMascot.kt` — in-place fade alternative.
- `composeApp/src/commonMain/kotlin/com/mochi/ui/components/SuccessAnimation.kt` — final-state check alternative.
- `README.md`, `docs/CONTEXT.md`, `docs/ROADMAP.md` — document the completed behavior.

---

### Task 1: Persist the motion preference

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/mochi/settings/MotionPreference.kt`
- Create: `composeApp/src/commonTest/kotlin/com/mochi/settings/SettingsStoreTest.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/mochi/settings/SettingsStore.kt`

**Interfaces:**
- Produces: `enum class MotionPreference { FULL, SYSTEM, REDUCED }`
- Produces: `SettingsStore.motionPreference(): MotionPreference`
- Produces: `SettingsStore.setMotionPreference(MotionPreference)`
- Produces: internal `SettingValues` seam used only by `SettingsStore` and common tests.

- [ ] **Step 1: Write the failing persistence tests**

Create `SettingsStoreTest.kt` with a map-backed `SettingValues` and these behaviors:

```kotlin
package com.mochi.settings

import kotlin.test.Test
import kotlin.test.assertEquals

class SettingsStoreTest {
    @Test
    fun missingMotionPreferenceDefaultsToFull() {
        val store = SettingsStore(FakeSettingValues())

        assertEquals(MotionPreference.FULL, store.motionPreference())
    }

    @Test
    fun invalidMotionPreferenceDefaultsToFull() {
        val store = SettingsStore(FakeSettingValues(mutableMapOf("motion_preference" to "BROKEN")))

        assertEquals(MotionPreference.FULL, store.motionPreference())
    }

    @Test
    fun validMotionPreferenceIsRead() {
        val store = SettingsStore(FakeSettingValues(mutableMapOf("motion_preference" to "SYSTEM")))

        assertEquals(MotionPreference.SYSTEM, store.motionPreference())
    }

    @Test
    fun settingMotionPreferencePersistsItsEnumName() {
        val values = FakeSettingValues()
        val store = SettingsStore(values)

        store.setMotionPreference(MotionPreference.REDUCED)

        assertEquals("REDUCED", values.entries["motion_preference"])
    }
}

internal class FakeSettingValues(
    val entries: MutableMap<String, String> = mutableMapOf(),
) : SettingValues {
    override fun read(key: String): String? = entries[key]

    override fun write(key: String, value: String) {
        entries[key] = value
    }
}
```

- [ ] **Step 2: Run the host tests and verify RED**

Run:

```bash
./gradlew :composeApp:testAndroidHostTest
```

Expected: compilation fails because `MotionPreference`, `SettingValues`, and the motion methods do not exist.

- [ ] **Step 3: Add the enum and key/value boundary**

Create `MotionPreference.kt`:

```kotlin
package com.mochi.settings

enum class MotionPreference {
    FULL,
    SYSTEM,
    REDUCED,
}
```

Refactor `SettingsStore.kt` so database access is behind this internal interface while preserving
the public `SettingsStore(AppDatabase)` constructor:

```kotlin
internal interface SettingValues {
    fun read(key: String): String?

    fun write(key: String, value: String)
}

private class DatabaseSettingValues(private val db: AppDatabase) : SettingValues {
    override fun read(key: String): String? =
        db.settingsQueries.selectSetting(key).executeAsOneOrNull()

    override fun write(key: String, value: String) {
        db.settingsQueries.upsertSetting(key, value)
    }
}

class SettingsStore internal constructor(
    private val values: SettingValues,
) : NewCardLimitSource {
    constructor(db: AppDatabase) : this(DatabaseSettingValues(db))

    fun motionPreference(): MotionPreference =
        values.read(KEY_MOTION)
            ?.let { runCatching { MotionPreference.valueOf(it) }.getOrNull() }
            ?: MotionPreference.FULL

    fun setMotionPreference(preference: MotionPreference) {
        values.write(KEY_MOTION, preference.name)
    }

    private companion object {
        const val KEY_MOTION = "motion_preference"
    }
}
```

Convert the existing theme, new-card-limit, reminder-enabled, and reminder-time methods from direct
SQLDelight calls to `values.read(key)` and `values.write(key, value)` without changing their keys,
defaults, or public signatures. Keep all existing constants in the single companion object.

- [ ] **Step 4: Run tests and verify GREEN**

Run:

```bash
./gradlew :composeApp:testAndroidHostTest
```

Expected: all host tests pass, including the four new persistence tests.

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/mochi/settings/MotionPreference.kt \
  composeApp/src/commonMain/kotlin/com/mochi/settings/SettingsStore.kt \
  composeApp/src/commonTest/kotlin/com/mochi/settings/SettingsStoreTest.kt
git commit -m "feat: persist motion preference"
```

---

### Task 2: Resolve app and system motion into one policy

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/mochi/ui/motion/MotionPolicy.kt`
- Create: `composeApp/src/commonMain/kotlin/com/mochi/ui/motion/SystemMotionPreference.kt`
- Create: `composeApp/src/androidMain/kotlin/com/mochi/ui/motion/SystemMotionPreference.android.kt`
- Create: `composeApp/src/iosMain/kotlin/com/mochi/ui/motion/SystemMotionPreference.ios.kt`
- Create: `composeApp/src/commonTest/kotlin/com/mochi/ui/motion/MotionPolicyTest.kt`

**Interfaces:**
- Consumes: `MotionPreference` from Task 1.
- Produces: `MotionPolicy`, `resolveMotionPolicy`, `LocalMotionPolicy`, and `MochiMotionProvider`.
- Produces: `@Composable expect fun rememberSystemReducedMotion(): Boolean`.

- [ ] **Step 1: Write the failing resolver matrix**

Create `MotionPolicyTest.kt`:

```kotlin
package com.mochi.ui.motion

import com.mochi.settings.MotionPreference
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MotionPolicyTest {
    @Test
    fun fullIgnoresTheSystemPreference() {
        assertFalse(resolveMotionPolicy(MotionPreference.FULL, systemReduced = false).reduced)
        assertFalse(resolveMotionPolicy(MotionPreference.FULL, systemReduced = true).reduced)
    }

    @Test
    fun reducedIgnoresTheSystemPreference() {
        assertTrue(resolveMotionPolicy(MotionPreference.REDUCED, systemReduced = false).reduced)
        assertTrue(resolveMotionPolicy(MotionPreference.REDUCED, systemReduced = true).reduced)
    }

    @Test
    fun systemTracksTheNativePreference() {
        assertFalse(resolveMotionPolicy(MotionPreference.SYSTEM, systemReduced = false).reduced)
        assertTrue(resolveMotionPolicy(MotionPreference.SYSTEM, systemReduced = true).reduced)
    }

    @Test
    fun reducedPolicyDisablesSpatialDecorativeAndInfiniteMotion() {
        val policy = MotionPolicy.Reduced

        assertFalse(policy.allowSpatialMotion)
        assertFalse(policy.allowDecorativeMotion)
        assertFalse(policy.allowInfiniteMotion)
        assertTrue(policy.allowShortFades)
    }
}
```

- [ ] **Step 2: Run the test and verify RED**

Run `./gradlew :composeApp:testAndroidHostTest`.

Expected: compilation fails because `MotionPolicy` and `resolveMotionPolicy` do not exist.

- [ ] **Step 3: Implement the pure policy and provider**

Create `MotionPolicy.kt`:

```kotlin
package com.mochi.ui.motion

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import com.mochi.settings.MotionPreference

@Immutable
data class MotionPolicy(val reduced: Boolean) {
    val allowSpatialMotion: Boolean get() = !reduced
    val allowDecorativeMotion: Boolean get() = !reduced
    val allowInfiniteMotion: Boolean get() = !reduced
    val allowShortFades: Boolean get() = true

    companion object {
        val Full = MotionPolicy(reduced = false)
        val Reduced = MotionPolicy(reduced = true)
    }
}

fun resolveMotionPolicy(
    preference: MotionPreference,
    systemReduced: Boolean,
): MotionPolicy = when (preference) {
    MotionPreference.FULL -> MotionPolicy.Full
    MotionPreference.SYSTEM -> if (systemReduced) MotionPolicy.Reduced else MotionPolicy.Full
    MotionPreference.REDUCED -> MotionPolicy.Reduced
}

val LocalMotionPolicy = staticCompositionLocalOf { MotionPolicy.Full }

@Composable
fun MochiMotionProvider(
    preference: MotionPreference,
    content: @Composable () -> Unit,
) {
    val systemReduced = rememberSystemReducedMotion()
    val policy = remember(preference, systemReduced) {
        resolveMotionPolicy(preference, systemReduced)
    }
    CompositionLocalProvider(LocalMotionPolicy provides policy, content = content)
}
```

Create the common declaration in `SystemMotionPreference.kt`:

```kotlin
package com.mochi.ui.motion

import androidx.compose.runtime.Composable

@Composable
expect fun rememberSystemReducedMotion(): Boolean
```

- [ ] **Step 4: Implement the Android observer**

Create `SystemMotionPreference.android.kt`:

```kotlin
package com.mochi.ui.motion

import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberSystemReducedMotion(): Boolean {
    val resolver = LocalContext.current.contentResolver
    fun read(): Boolean = runCatching {
        Settings.Global.getFloat(
            resolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) == 0f
    }.getOrDefault(false)

    var reduced by remember(resolver) { mutableStateOf(read()) }
    DisposableEffect(resolver) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                reduced = read()
            }
        }
        resolver.registerContentObserver(
            Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE),
            false,
            observer,
        )
        onDispose { resolver.unregisterContentObserver(observer) }
    }
    return reduced
}
```

- [ ] **Step 5: Implement the iOS observer**

Create `SystemMotionPreference.ios.kt` using the Kotlin/Native UIKit symbols verified in the local
2.2.x platform metadata:

```kotlin
package com.mochi.ui.motion

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.UIKit.UIAccessibilityIsReduceMotionEnabled
import platform.UIKit.UIAccessibilityReduceMotionStatusDidChangeNotification

@Composable
actual fun rememberSystemReducedMotion(): Boolean {
    val center = remember { NSNotificationCenter.defaultCenter }
    var reduced by remember { mutableStateOf(UIAccessibilityIsReduceMotionEnabled()) }

    DisposableEffect(center) {
        val observer = center.addObserverForName(
            name = UIAccessibilityReduceMotionStatusDidChangeNotification,
            `object` = null,
            queue = NSOperationQueue.mainQueue,
        ) {
            reduced = UIAccessibilityIsReduceMotionEnabled()
        }
        onDispose { center.removeObserver(observer) }
    }
    return reduced
}
```

- [ ] **Step 6: Verify tests and both platform compilations**

Run:

```bash
./gradlew :composeApp:testAndroidHostTest \
  :composeApp:compileAndroidMain \
  :composeApp:compileKotlinIosSimulatorArm64
```

Expected: all tasks succeed. The UIKit notification name is nullable in Kotlin/Native metadata and
is accepted directly by `addObserverForName`.

- [ ] **Step 7: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/mochi/ui/motion/MotionPolicy.kt \
  composeApp/src/commonMain/kotlin/com/mochi/ui/motion/SystemMotionPreference.kt \
  composeApp/src/androidMain/kotlin/com/mochi/ui/motion/SystemMotionPreference.android.kt \
  composeApp/src/iosMain/kotlin/com/mochi/ui/motion/SystemMotionPreference.ios.kt \
  composeApp/src/commonTest/kotlin/com/mochi/ui/motion/MotionPolicyTest.kt
git commit -m "feat: resolve platform reduced motion preference"
```

---

### Task 3: Hoist motion state into the app

**Files:**
- Create: `composeApp/src/commonTest/kotlin/com/mochi/settings/SettingsViewModelTest.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/mochi/settings/SettingsViewModel.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/mochi/App.kt`

**Interfaces:**
- Consumes: `SettingsStore.motionPreference()` and `MochiMotionProvider`.
- Produces: `SettingsViewModel.motionPreference: StateFlow<MotionPreference>`.
- Produces: `SettingsViewModel.setMotionPreference(MotionPreference)`.

- [ ] **Step 1: Write the failing ViewModel tests**

Create `SettingsViewModelTest.kt`:

```kotlin
package com.mochi.settings

import com.mochi.reminder.ReminderScheduler
import kotlin.test.Test
import kotlin.test.assertEquals

class SettingsViewModelTest {
    @Test
    fun startsWithTheStoredMotionPreference() {
        val values = FakeSettingValues(mutableMapOf("motion_preference" to "SYSTEM"))
        val viewModel = SettingsViewModel(SettingsStore(values), FakeReminderScheduler())

        assertEquals(MotionPreference.SYSTEM, viewModel.motionPreference.value)
    }

    @Test
    fun changingMotionUpdatesStateAndStorageImmediately() {
        val values = FakeSettingValues()
        val viewModel = SettingsViewModel(SettingsStore(values), FakeReminderScheduler())

        viewModel.setMotionPreference(MotionPreference.REDUCED)

        assertEquals(MotionPreference.REDUCED, viewModel.motionPreference.value)
        assertEquals("REDUCED", values.entries["motion_preference"])
    }
}

private class FakeReminderScheduler : ReminderScheduler {
    override fun schedule(hour: Int, minute: Int) = Unit

    override fun cancel() = Unit
}
```

- [ ] **Step 2: Run tests and verify RED**

Run `./gradlew :composeApp:testAndroidHostTest`.

Expected: compilation fails because the ViewModel motion state and setter do not exist.

- [ ] **Step 3: Add motion state to SettingsViewModel**

Add alongside the theme state:

```kotlin
private val _motionPreference = MutableStateFlow(store.motionPreference())
val motionPreference: StateFlow<MotionPreference> = _motionPreference.asStateFlow()

fun setMotionPreference(preference: MotionPreference) {
    store.setMotionPreference(preference)
    _motionPreference.value = preference
}
```

Update the class comment to include motion without changing reminder startup behavior.

- [ ] **Step 4: Provide the policy from App**

In `App.kt`, collect the new state:

```kotlin
val motionPreference by settingsViewModel.motionPreference.collectAsState()
```

Inside `MochiTheme`, add `MochiMotionProvider` immediately before the current
`SystemBarsEffect(darkTheme)` call:

```kotlin
MochiMotionProvider(preference = motionPreference) {
    SystemBarsEffect(darkTheme)
}
```

Move the closing brace shown directly above to immediately after the current `Scaffold` content
lambda. Keep the entire existing `Scaffold`, `Surface`, `AnimatedContent`, and `when` branches
verbatim inside the provider. Pass these new arguments to the existing `SettingsScreen` branch:

```kotlin
motionPreference = motionPreference,
onMotionPreferenceChange = settingsViewModel::setMotionPreference,
```

- [ ] **Step 5: Verify GREEN and compile**

Run:

```bash
./gradlew :composeApp:testAndroidHostTest :composeApp:compileAndroidMain
```

Expected: tests and compilation pass. Add the two SettingsScreen parameters in this task and retain
them without rendering until Task 4, so this commit remains independently green.

- [ ] **Step 6: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/mochi/settings/SettingsViewModel.kt \
  composeApp/src/commonMain/kotlin/com/mochi/App.kt \
  composeApp/src/commonMain/kotlin/com/mochi/ui/screens/SettingsScreen.kt \
  composeApp/src/commonTest/kotlin/com/mochi/settings/SettingsViewModelTest.kt
git commit -m "feat: hoist effective motion settings"
```

---

### Task 4: Redesign Settings as grouped Mochi cards

**Files:**
- Modify: `composeApp/build.gradle.kts`
- Modify: `composeApp/src/commonMain/kotlin/com/mochi/ui/screens/SettingsScreen.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/mochi/ui/components/MochiLogo.kt`
- Create: `composeApp/src/commonTest/kotlin/com/mochi/ui/screens/SettingsScreenTest.kt`

**Interfaces:**
- Consumes: hoisted theme, motion, new-card, reminder, and time state.
- Produces: state-hoisted grouped rows and focused single-choice dialogs.
- Extends: `MochiLogo(modifier: Modifier, animateEntry: Boolean, interactive: Boolean)` for decorative use.

- [ ] **Step 1: Enable common Compose UI tests**

Add to `commonTest.dependencies` in `composeApp/build.gradle.kts`:

```kotlin
implementation(compose.uiTest)
```

- [ ] **Step 2: Write failing layout and interaction tests**

Create `SettingsScreenTest.kt`. Use the actual `MochiTheme` and force full motion through the
composition local. The helper renders Settings with stable defaults:

```kotlin
package com.mochi.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.mochi.reminder.ReminderTime
import com.mochi.settings.MotionPreference
import com.mochi.settings.ThemeMode
import com.mochi.ui.motion.LocalMotionPolicy
import com.mochi.ui.motion.MotionPolicy
import com.mochi.ui.theme.MochiTheme
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class SettingsScreenTest {
    @Test
    fun showsApprovedGroupedSectionsAndCopy() = runComposeUiTest {
        setContent { settingsContent() }

        onNodeWithText("Make Mochi feel right for you.").assertExists()
        onNodeWithText("Study your way").assertExists()
        onNodeWithText("Look & feel").assertExists()
        onNodeWithText("Study rhythm").assertExists()
        onNodeWithText("Motion").assertExists()
    }

    @Test
    fun motionDialogEmitsTheSelectedPreference() = runComposeUiTest {
        var selected = MotionPreference.FULL
        setContent { settingsContent(onMotionChange = { selected = it }) }

        onNodeWithTag("motion-setting").performClick()
        onNodeWithTag("motion-option-REDUCED").performClick()

        assertEquals(MotionPreference.REDUCED, selected)
    }

    @Test
    fun reminderTimeIsHiddenWhileReminderIsDisabled() = runComposeUiTest {
        setContent { settingsContent(reminderEnabled = false) }

        onNodeWithTag("reminder-time").assertDoesNotExist()
    }

    @Composable
    private fun settingsContent(
        reminderEnabled: Boolean = true,
        onMotionChange: (MotionPreference) -> Unit = {},
    ) {
        MochiTheme {
            CompositionLocalProvider(LocalMotionPolicy provides MotionPolicy.Full) {
                SettingsScreen(
                    themeMode = ThemeMode.SYSTEM,
                    onThemeChange = {},
                    motionPreference = MotionPreference.FULL,
                    onMotionPreferenceChange = onMotionChange,
                    newCardLimit = 20,
                    onNewCardLimitChange = {},
                    reminderEnabled = reminderEnabled,
                    onReminderEnabledChange = {},
                    reminderTime = ReminderTime(20, 0),
                    onReminderTimeChange = { _, _ -> },
                )
            }
        }
    }
}
```

Import `assertDoesNotExist` when creating the file.

- [ ] **Step 3: Run tests and verify RED**

Run `./gradlew :composeApp:testAndroidHostTest`.

Expected: tests fail because the approved copy, tags, grouped hierarchy, and dialog do not exist.

- [ ] **Step 4: Build the grouped page structure**

Replace the flat radio lists with this composition hierarchy:

```kotlin
Column(
    modifier = modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp, vertical = 20.dp),
) {
    Text("Settings", style = MaterialTheme.typography.headlineSmall)
    Text(
        text = "Make Mochi feel right for you.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(20.dp))
    SettingsIntroCard()
    Spacer(Modifier.height(24.dp))
    SettingsSection(title = "Look & feel") {
        PreferenceRow(
            icon = Icons.Filled.Palette,
            label = "Theme",
            supportingText = "Choose how Mochi looks",
            value = themeMode.label(),
            testTag = "theme-setting",
            onClick = { openDialog = SettingsDialog.THEME },
        )
        PreferenceRow(
            icon = Icons.Filled.Animation,
            label = "Motion",
            supportingText = motionPreference.description(),
            value = motionPreference.label(),
            testTag = "motion-setting",
            onClick = { openDialog = SettingsDialog.MOTION },
        )
    }
    Spacer(Modifier.height(24.dp))
    SettingsSection(title = "Study rhythm") {
        PreferenceRow(
            icon = Icons.Filled.Style,
            label = "New cards",
            supportingText = "Introduced each day",
            value = if (newCardLimit == 0) "Unlimited" else newCardLimit.toString(),
            testTag = "new-cards-setting",
            onClick = { openDialog = SettingsDialog.NEW_CARDS },
        )
        ReminderToggleRow(enabled = reminderEnabled, onChange = onReminderEnabledChange)
        if (reminderEnabled) {
            ReminderTimeRow(
                time = reminderTime,
                onClick = { showTimeDialog = true },
                modifier = Modifier.testTag("reminder-time"),
            )
        }
    }
}
```

Implement `SettingsSection` with a label and one rounded `Surface`; implement `PreferenceRow` with
a 40 dp pastel icon tile, wrapping label/supporting text, trailing value, and chevron. Set the row
minimum height to 64 dp and the full row as the only click action. Keep 20 dp horizontal page
padding and existing Mochi theme tokens.

Use these exact helper signatures:

```kotlin
@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
)

@Composable
private fun PreferenceRow(
    icon: ImageVector,
    label: String,
    supportingText: String,
    value: String,
    testTag: String,
    onClick: () -> Unit,
)

@Composable
private fun ReminderTimeRow(
    time: ReminderTime,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
)
```

Add `SettingsIntroCard` with exact copy:

```kotlin
Text("Study your way", style = MaterialTheme.typography.titleMedium)
Text("Tune the look, motion, and rhythm anytime.")
MochiLogo(
    animateEntry = false,
    interactive = false,
    modifier = Modifier.size(56.dp).clearAndSetSemantics {},
)
```

Add `interactive: Boolean = true` to `MochiLogo`. Apply its existing `clickable` modifier only when
`interactive` is true; decorative mode must not emit click semantics or trigger audio/haptics.

- [ ] **Step 5: Add focused choice dialogs**

Use this local state and enum:

```kotlin
private enum class SettingsDialog {
    THEME,
    MOTION,
    NEW_CARDS,
}

var openDialog by remember { mutableStateOf<SettingsDialog?>(null) }
```

Create a reusable `ChoiceDialog` whose rows contain a radio button, label, optional supporting text,
and `Modifier.testTag`. Selecting a row calls its callback and dismisses immediately. Use it for:

```kotlin
private data class Choice<T>(
    val value: T,
    val label: String,
    val supportingText: String? = null,
    val testTag: String,
)

@Composable
private fun <T> ChoiceDialog(
    title: String,
    selected: T,
    choices: List<Choice<T>>,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
)
```

Render `ChoiceDialog` with Material 3 `AlertDialog`; each full-width choice row owns the click
action, the nested `RadioButton` has `onClick = null`, and the dismiss button is labeled `Cancel`.
Use it for:

```kotlin
MotionPreference.entries.map { preference ->
    Choice(
        value = preference,
        label = preference.label(),
        supportingText = preference.description(),
        testTag = "motion-option-${preference.name}",
    )
}
```

The exact Motion descriptions are:

```kotlin
private fun MotionPreference.description(): String = when (this) {
    MotionPreference.FULL -> "Play all Mochi animations."
    MotionPreference.SYSTEM -> "Follow your device accessibility setting."
    MotionPreference.REDUCED -> "Use fades and simpler transitions."
}
```

Theme retains `System default`, `Light`, and `Dark`. New-card choices retain 10, 20, 30, and
Unlimited. Keep the existing Material 3 `TimePicker` dialog unchanged.

- [ ] **Step 6: Run UI tests, quality checks, and compile**

Run:

```bash
./gradlew :composeApp:testAndroidHostTest \
  :composeApp:ktlintCheck \
  :composeApp:detekt \
  :composeApp:compileAndroidMain
```

Expected: all tasks succeed.

- [ ] **Step 7: Commit**

```bash
git add composeApp/build.gradle.kts \
  composeApp/src/commonMain/kotlin/com/mochi/ui/screens/SettingsScreen.kt \
  composeApp/src/commonMain/kotlin/com/mochi/ui/components/MochiLogo.kt \
  composeApp/src/commonTest/kotlin/com/mochi/ui/screens/SettingsScreenTest.kt
git commit -m "feat: redesign settings with grouped preference cards"
```

---

### Task 5: Adapt reusable motion primitives

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/mochi/ui/motion/MotionValues.kt`
- Create: `composeApp/src/commonTest/kotlin/com/mochi/ui/motion/MotionValuesTest.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/mochi/ui/motion/PressBounce.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/mochi/ui/motion/LiquidProgress.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/mochi/ui/motion/AnimatedCounter.kt`

**Interfaces:**
- Consumes: `LocalMotionPolicy`.
- Produces: pure press scale, wave amplitude, card tilt, and release-presentation decisions.

- [ ] **Step 1: Write failing presentation-decision tests**

Create `MotionValuesTest.kt`:

```kotlin
package com.mochi.ui.motion

import kotlin.test.Test
import kotlin.test.assertEquals

class MotionValuesTest {
    @Test
    fun reducedPressNeverScales() {
        assertEquals(1f, MotionPolicy.Reduced.pressScale(pressed = true, pressedScale = 0.9f))
        assertEquals(0.9f, MotionPolicy.Full.pressScale(pressed = true, pressedScale = 0.9f))
    }

    @Test
    fun reducedLiquidProgressHasNoWaveAmplitude() {
        assertEquals(0f, MotionPolicy.Reduced.waveAmplitude(8f))
        assertEquals(8f, MotionPolicy.Full.waveAmplitude(8f))
    }

    @Test
    fun reducedCardsNeverTilt() {
        assertEquals(0f, MotionPolicy.Reduced.cardTilt(progress = 0.5f, maxTilt = 12f))
        assertEquals(6f, MotionPolicy.Full.cardTilt(progress = 0.5f, maxTilt = 12f))
    }

    @Test
    fun swipeReleaseChoosesReducedAndFullPresentations() {
        assertEquals(SwipeRelease.FADE, swipeRelease(passed = true, reduced = true))
        assertEquals(SwipeRelease.THROW, swipeRelease(passed = true, reduced = false))
        assertEquals(SwipeRelease.SETTLE, swipeRelease(passed = false, reduced = true))
        assertEquals(SwipeRelease.SPRING, swipeRelease(passed = false, reduced = false))
    }
}
```

- [ ] **Step 2: Run tests and verify RED**

Run `./gradlew :composeApp:testAndroidHostTest`.

Expected: compilation fails because the motion value helpers do not exist.

- [ ] **Step 3: Implement MotionValues**

Create `MotionValues.kt`:

```kotlin
package com.mochi.ui.motion

internal enum class SwipeRelease {
    THROW,
    FADE,
    SPRING,
    SETTLE,
}

internal fun MotionPolicy.pressScale(pressed: Boolean, pressedScale: Float): Float =
    if (allowSpatialMotion && pressed) pressedScale else 1f

internal fun MotionPolicy.waveAmplitude(requested: Float): Float =
    if (allowInfiniteMotion) requested else 0f

internal fun MotionPolicy.cardTilt(progress: Float, maxTilt: Float): Float =
    if (allowSpatialMotion) progress.coerceIn(-1f, 1f) * maxTilt else 0f

internal fun swipeRelease(passed: Boolean, reduced: Boolean): SwipeRelease = when {
    passed && reduced -> SwipeRelease.FADE
    passed -> SwipeRelease.THROW
    reduced -> SwipeRelease.SETTLE
    else -> SwipeRelease.SPRING
}
```

- [ ] **Step 4: Make press, liquid, and counter policy-aware**

In `PressBounce.kt`, read `LocalMotionPolicy.current`. Under reduced motion return scale 1f directly;
under full motion retain the existing `animateFloatAsState` spring and use `policy.pressScale`
as its target.

In `LiquidProgress.kt`, do not create `rememberInfiniteTransition` when
`policy.allowInfiniteMotion` is false. Extract the current animated phase calculation into a
private `animatedLiquidPhase()` composable and render reduced mode with `phase = 0f` and
`waveHeight = policy.waveAmplitude(waveHeight)`. This prevents an invisible infinite frame loop.

In `AnimatedCounter.kt`, choose the transition from the policy:

```kotlin
val policy = LocalMotionPolicy.current
val transition = if (policy.reduced) {
    fadeIn(tween(durationMillis = 120)) togetherWith
        fadeOut(tween(durationMillis = 120))
} else {
    val goingUp = targetState > initialState
    val enter = slideInVertically { height -> if (goingUp) height else -height } + fadeIn()
    val exit = slideOutVertically { height -> if (goingUp) -height else height } + fadeOut()
    (enter togetherWith exit).using(SizeTransform(clip = false))
}
transition
```

- [ ] **Step 5: Verify GREEN and commit**

Run:

```bash
./gradlew :composeApp:testAndroidHostTest :composeApp:ktlintCheck :composeApp:detekt
```

Expected: all tasks succeed.

Commit:

```bash
git add composeApp/src/commonMain/kotlin/com/mochi/ui/motion/MotionValues.kt \
  composeApp/src/commonMain/kotlin/com/mochi/ui/motion/PressBounce.kt \
  composeApp/src/commonMain/kotlin/com/mochi/ui/motion/LiquidProgress.kt \
  composeApp/src/commonMain/kotlin/com/mochi/ui/motion/AnimatedCounter.kt \
  composeApp/src/commonTest/kotlin/com/mochi/ui/motion/MotionValuesTest.kt
git commit -m "feat: add reduced variants for motion primitives"
```

---

### Task 6: Adapt card gestures and navigation transitions

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/mochi/ui/motion/MotionValues.kt`
- Modify: `composeApp/src/commonTest/kotlin/com/mochi/ui/motion/MotionValuesTest.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/mochi/ui/motion/SwipeToDismissCard.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/mochi/ui/components/FlipCard.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/mochi/ui/screens/FlashcardScreen.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/mochi/ui/screens/LibraryScreen.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/mochi/App.kt`

**Interfaces:**
- Consumes: `LocalMotionPolicy`, `SwipeRelease`, and existing card callbacks.
- Preserves: one-shot threshold haptic and right/left answer semantics.

- [ ] **Step 1: Extend the failing value tests for flip and transition timing**

Add to `MotionValuesTest.kt`:

```kotlin
@Test
fun reducedFlipUsesNoRotation() {
    assertEquals(0f, MotionPolicy.Reduced.cardRotation(isFlipped = true))
    assertEquals(180f, MotionPolicy.Full.cardRotation(isFlipped = true))
}

@Test
fun reducedNavigationUsesTheShortFadeDuration() {
    assertEquals(120, MotionPolicy.Reduced.navigationFadeMillis())
    assertEquals(300, MotionPolicy.Full.navigationFadeMillis())
}
```

Run `./gradlew :composeApp:testAndroidHostTest` and expect compilation failure for the two missing
helpers. Then add to `MotionValues.kt`:

```kotlin
internal fun MotionPolicy.cardRotation(isFlipped: Boolean): Float =
    if (allowSpatialMotion && isFlipped) 180f else 0f

internal fun MotionPolicy.navigationFadeMillis(): Int = if (reduced) 120 else 300
```

- [ ] **Step 2: Implement reduced swipe release without changing gesture recognition**

In `SwipeToDismissCard.kt`:

- read `LocalMotionPolicy.current` in the `composed` block and retain it with
  `rememberUpdatedState`;
- add `val alpha = remember { Animatable(1f) }`;
- continue tracking both axes under the finger and continue calling `updateSwipeThreshold` exactly
  as today;
- choose `swipeRelease(passed, policy.reduced)` on end/cancel;
- for `THROW`, keep the existing off-screen spring;
- for `FADE`, animate alpha to 0f with `tween(120)`, then call `onDismiss`;
- for `SPRING`, keep the current medium-bouncy return;
- for `SETTLE`, animate offset to zero with `tween(100)` and no overshoot;
- always reset `thresholdOutside` after a cancelled release;
- set `rotationZ` through `policy.cardTilt(progress, MAX_TILT)` and set layer alpha from the new
  Animatable.

Do not change `DISMISS_THRESHOLD`, `LOCKED_RESISTANCE`, `CardHaptics`, or callback ordering.

- [ ] **Step 3: Crossfade card faces in reduced mode**

In `FlipCard.kt`, read `LocalMotionPolicy.current` and split face rendering into a private
`CardFace(front, reading, meaning, showBack)` composable. Full mode keeps the current rotation,
camera distance, lift, and mirrored back face. Reduced mode keeps rotation and press scale at their
final neutral values and renders:

```kotlin
AnimatedContent(
    targetState = isFlipped,
    transitionSpec = {
        fadeIn(tween(durationMillis = 120)) togetherWith
            fadeOut(tween(durationMillis = 120))
    },
    label = "reducedFlip",
) { showBack ->
    CardFace(
        front = front,
        reading = reading,
        meaning = meaning,
        showBack = showBack,
    )
}
```

Keep the existing `onFlip` call site so `VirtualKey` haptics fire in both modes.

- [ ] **Step 4: Remove spatial screen transitions in reduced mode**

In `FlashcardScreen.kt`, select a fade-only 120 ms transition for the next card when reduced;
otherwise retain slide plus fade. Apply `sharedBounds` only when
`LocalMotionPolicy.current.allowSpatialMotion` is true.

In `LibraryScreen.kt`, apply the same conditional shared-bounds modifier to each unit card. Keep
the key unchanged so switching back to Full restores the existing transition.

In `App.kt`, use policy-aware transitions for both tab `AnimatedContent` and review-state
`AnimatedContent`:

```kotlin
val duration = LocalMotionPolicy.current.navigationFadeMillis()
fadeIn(tween(durationMillis = duration)) togetherWith
    fadeOut(tween(durationMillis = duration))
```

The absence of shared-bounds modifiers in reduced mode makes the existing review-state host fall
back to this crossfade.

- [ ] **Step 5: Verify cards and navigation**

Run:

```bash
./gradlew :composeApp:testAndroidHostTest \
  :composeApp:ktlintCheck \
  :composeApp:detekt \
  :composeApp:compileAndroidMain \
  :composeApp:compileKotlinIosSimulatorArm64
```

Expected: all tasks succeed. Manually confirm that the existing threshold tests still pass and no
swipe intent pills reappear.

- [ ] **Step 6: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/mochi/App.kt \
  composeApp/src/commonMain/kotlin/com/mochi/ui/components/FlipCard.kt \
  composeApp/src/commonMain/kotlin/com/mochi/ui/motion/MotionValues.kt \
  composeApp/src/commonMain/kotlin/com/mochi/ui/motion/SwipeToDismissCard.kt \
  composeApp/src/commonMain/kotlin/com/mochi/ui/screens/FlashcardScreen.kt \
  composeApp/src/commonMain/kotlin/com/mochi/ui/screens/LibraryScreen.kt \
  composeApp/src/commonTest/kotlin/com/mochi/ui/motion/MotionValuesTest.kt
git commit -m "feat: reduce card and navigation motion"
```

---

### Task 7: Add reduced reward, mascot, and completion presentations

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/mochi/ui/motion/ReducedReward.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/mochi/ui/motion/MotionValues.kt`
- Modify: `composeApp/src/commonTest/kotlin/com/mochi/ui/motion/MotionValuesTest.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/mochi/ui/motion/ConfettiBurst.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/mochi/ui/components/MochiLogo.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/mochi/ui/components/MochiMascot.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/mochi/ui/components/SuccessAnimation.kt`

**Interfaces:**
- Produces: `RewardPresentation.PARTICLES` or `RewardPresentation.STATIC_GLOW`.
- Preserves: reward triggers and mascot haptic timing.

- [ ] **Step 1: Write the failing reward decision test**

Add to `MotionValuesTest.kt`:

```kotlin
@Test
fun rewardPresentationMatchesTheMotionPolicy() {
    assertEquals(RewardPresentation.PARTICLES, MotionPolicy.Full.rewardPresentation())
    assertEquals(RewardPresentation.STATIC_GLOW, MotionPolicy.Reduced.rewardPresentation())
}
```

Run `./gradlew :composeApp:testAndroidHostTest` and verify it fails because the reward decision does
not exist.

- [ ] **Step 2: Implement the reward decision and static artwork**

Add to `MotionValues.kt`:

```kotlin
internal enum class RewardPresentation {
    PARTICLES,
    STATIC_GLOW,
}

internal fun MotionPolicy.rewardPresentation(): RewardPresentation =
    if (allowDecorativeMotion) RewardPresentation.PARTICLES else RewardPresentation.STATIC_GLOW
```

Create `ReducedReward.kt`. It must:

- react only to a new non-null trigger;
- fade a static themed halo and four fixed sparkles in over 120 ms;
- hold for 700 ms;
- fade out over 120 ms;
- use Canvas without a frame loop or moving geometry;
- draw behind no interactive semantics.

Use `AnimatedVisibility`, `fadeIn(tween(120))`, `fadeOut(tween(120))`, and a `LaunchedEffect(trigger)`
that toggles a Boolean around `delay(700)`.

In `ConfettiBurst`, select by `LocalMotionPolicy.current.rewardPresentation()`. Keep the current
particle implementation unchanged in a private `ParticleBurst`; call `ReducedReward` for the
static branch.

- [ ] **Step 3: Adapt MochiLogo and MochiMascot**

In `MochiLogo`, read `LocalMotionPolicy.current`. Run entry/tap `bounce()` only when
`allowSpatialMotion`; interactive taps still emit their haptic and click sound in reduced mode.
When motion becomes reduced during a bounce, snap `drop` to zero in a keyed `LaunchedEffect`.

In `MochiMascot`, keep the same greet/react triggers and haptic. Change `popUp` by policy:

```kotlin
if (policy.reduced) {
    reveal.snapTo(1f)
    delay(holdMs)
    reveal.animateTo(0f, tween(durationMillis = 120))
} else {
    reveal.animateTo(1f, spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow,
    ))
    delay(holdMs)
    reveal.animateTo(HOP_PEAK, tween(durationMillis = HOP_DURATION_MS))
    reveal.animateTo(0f, tween(durationMillis = OUT_DURATION_MS))
}
```

Set mascot `translationY` to zero when reduced and use `reveal` only as alpha. Preserve the current
1.8 second greeting and 1 second reaction holds.

- [ ] **Step 4: Render the final completion check in reduced mode**

In `SuccessAnimation`, initialize or snap `circleScale`, `ringProgress`, and `checkProgress` to 1f
when reduced. Do not launch the spring, expanding ring, delayed drawing, or path animation. Wrap
the final Canvas in a 120 ms alpha fade so the check appears gently without scale or travel.

Full mode must retain the current circle spring, 900 ms halo, 180 ms delay, and 360 ms check draw.

- [ ] **Step 5: Verify GREEN and commit**

Run:

```bash
./gradlew :composeApp:testAndroidHostTest \
  :composeApp:ktlintCheck \
  :composeApp:detekt \
  :composeApp:compileAndroidMain \
  :composeApp:compileKotlinIosSimulatorArm64
```

Expected: all tasks succeed.

Commit:

```bash
git add composeApp/src/commonMain/kotlin/com/mochi/ui/components/MochiLogo.kt \
  composeApp/src/commonMain/kotlin/com/mochi/ui/components/MochiMascot.kt \
  composeApp/src/commonMain/kotlin/com/mochi/ui/components/SuccessAnimation.kt \
  composeApp/src/commonMain/kotlin/com/mochi/ui/motion/ConfettiBurst.kt \
  composeApp/src/commonMain/kotlin/com/mochi/ui/motion/MotionValues.kt \
  composeApp/src/commonMain/kotlin/com/mochi/ui/motion/ReducedReward.kt \
  composeApp/src/commonTest/kotlin/com/mochi/ui/motion/MotionValuesTest.kt
git commit -m "feat: add reduced reward and mascot feedback"
```

---

### Task 8: Verify the complete accessibility matrix

**Files:**
- Modify only if a check exposes a defect in an earlier task; commit each correction separately.

**Interfaces:**
- Verifies all production interfaces from Tasks 1–7.

- [ ] **Step 1: Run the complete automated suite**

Run:

```bash
./gradlew :composeApp:testAndroidHostTest \
  :composeApp:iosSimulatorArm64Test \
  :composeApp:ktlintCheck \
  :composeApp:detekt \
  :composeApp:compileAndroidMain \
  :composeApp:compileKotlinIosSimulatorArm64 \
  :composeApp:linkDebugFrameworkIosSimulatorArm64
```

Expected: every task succeeds with no new warnings. If the iOS simulator test cannot launch because
the matching simulator runtime is unavailable, record that environmental limitation and require
the iOS compile and framework link tasks to remain green.

- [ ] **Step 2: Exercise the preference precedence matrix**

On Android and iOS, confirm:

- `Full` plays full motion regardless of the native setting;
- `Reduced` uses reduced presentation regardless of the native setting;
- `System` changes immediately when the native setting changes;
- returning to `Full` restores all existing animations;
- force-closing and reopening preserves the selected value;
- a fresh install starts at `Full`.

- [ ] **Step 3: Exercise every animation pair**

Compare Full and Reduced for:

- card flip, pre-flip resisted drag, post-flip drag, threshold haptic, cancelled swipe, and
  confirmed left/right swipe;
- next card, library-to-session, back to library, and bottom-tab transitions;
- liquid unit progress, stats/session counters, and button presses;
- ten-correct reward, Mochi greeting/reaction, session-complete reward, and success check.

Confirm no hidden infinite Canvas loop remains in reduced mode and no answer callback fires twice.

- [ ] **Step 4: Audit Settings accessibility and layout**

Check both themes and both platforms with normal and enlarged system text:

- no label or selected value clips;
- cards remain scrollable;
- rows are at least 48 dp tall;
- decorative Mochi and icons do not add duplicate focus stops;
- each row exposes one action;
- selection state is announced independently of color;
- reminder Time appears only when enabled;
- changing Motion while a dialog is open closes safely into the selected mode.

- [ ] **Step 5: Commit only verification fixes**

For each defect found, first add or tighten a failing automated test where possible, then make the
smallest correction and use a scoped commit such as:

```bash
git commit -m "fix: settle active motion when reduction is enabled"
```

Do not create an empty verification commit.

---

### Task 9: Update project documentation

**Files:**
- Modify: `README.md`
- Modify: `docs/CONTEXT.md`
- Modify: `docs/ROADMAP.md`

**Interfaces:**
- Documents the shipped preference, platform behavior, Settings design, and verification status.

- [ ] **Step 1: Update README**

Add reduced-motion support to Features and Motion system. State that Settings offers Full, System,
and Reduced; Full is the default; System follows Android/iOS; and custom Canvas/gesture animations
provide explicit alternatives rather than relying only on duration scaling. Preserve exactly two
media slots.

- [ ] **Step 2: Update CONTEXT**

Document:

- `MotionPreference`, `MotionPolicy`, `LocalMotionPolicy`, and the platform observer files;
- the redesigned grouped Settings page;
- the full/reduced behavior summary;
- the new Settings, policy, and presentation tests;
- exact successful verification commands;
- the recent granular commits.

- [ ] **Step 3: Mark roadmap phase 1 complete**

Add `**Status: Complete**` under `## 1. Reduced-motion accessibility`. Do not alter the order or
scope of later phases.

- [ ] **Step 4: Check and commit documentation**

Run:

```bash
git diff --check
rg -n "Full|System|Reduced|reduced motion" README.md docs/CONTEXT.md docs/ROADMAP.md
```

Expected: no whitespace errors and all three documents describe the same default and behavior.

Commit:

```bash
git add README.md docs/CONTEXT.md docs/ROADMAP.md
git commit -m "docs: document reduced motion accessibility"
```

---

## Final completion gate

Before claiming completion:

- run `git status --short --branch` and confirm only expected commits exist;
- run the complete command from Task 8 again after documentation changes;
- inspect `git log --oneline` and confirm every commit is logically separated;
- invoke `superpowers:verification-before-completion`;
- invoke `toutbox-claude-skills:architecture-review` because the user-level instructions require it
  after code creation, even though Mochi is not a ToutBox repository;
- invoke `superpowers:requesting-code-review` before any merge decision;
- do not push.
