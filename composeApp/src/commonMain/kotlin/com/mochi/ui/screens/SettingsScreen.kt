package com.mochi.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.mochi.reminder.ReminderTime
import com.mochi.settings.MotionPreference
import com.mochi.settings.ThemeMode
import com.mochi.ui.components.MochiLogo

private val NewCardOptions = listOf(10 to "10", 20 to "20", 30 to "30", 0 to "Unlimited")

private enum class SettingsDialog {
    THEME,
    MOTION,
    NEW_CARDS,
}

private data class Choice<T>(
    val value: T,
    val label: String,
    val supportingText: String? = null,
    val testTag: String,
)

/** Settings tab grouped into compact, state-hoisted preference cards. */
@Composable
fun SettingsScreen(
    themeMode: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    motionPreference: MotionPreference,
    onMotionPreferenceChange: (MotionPreference) -> Unit,
    newCardLimit: Int,
    onNewCardLimitChange: (Int) -> Unit,
    reminderEnabled: Boolean,
    onReminderEnabledChange: (Boolean) -> Unit,
    reminderTime: ReminderTime,
    onReminderTimeChange: (Int, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var openDialog by remember { mutableStateOf<SettingsDialog?>(null) }
    var showTimeDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 20.dp),
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineSmall)
        Text(
            text = SETTINGS_SUBTITLE,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(20.dp))
        SettingsIntroCard()
        Spacer(Modifier.height(24.dp))

        SettingsSection(title = SETTINGS_LOOK_AND_FEEL) {
            PreferenceRow(
                icon = Icons.Filled.Palette,
                label = "Theme",
                supportingText = "Choose how Mochi looks",
                value = themeMode.label(),
                testTag = "theme-setting",
                iconBackground = MaterialTheme.colorScheme.secondaryContainer,
                onClick = { openDialog = SettingsDialog.THEME },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            PreferenceRow(
                icon = Icons.Filled.Animation,
                label = "Motion",
                supportingText = motionPreference.description(),
                value = motionPreference.label(),
                testTag = "motion-setting",
                iconBackground = MaterialTheme.colorScheme.primaryContainer,
                onClick = { openDialog = SettingsDialog.MOTION },
            )
        }

        Spacer(Modifier.height(24.dp))
        SettingsSection(title = SETTINGS_STUDY_RHYTHM) {
            PreferenceRow(
                icon = Icons.Filled.Style,
                label = "New cards",
                supportingText = "Introduced each day",
                value = newCardLimit.label(),
                testTag = "new-cards-setting",
                iconBackground = MaterialTheme.colorScheme.secondaryContainer,
                onClick = { openDialog = SettingsDialog.NEW_CARDS },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            ReminderToggleRow(
                enabled = reminderEnabled,
                onChange = onReminderEnabledChange,
            )
            if (shouldShowReminderTime(reminderEnabled)) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                ReminderTimeRow(
                    time = reminderTime,
                    onClick = { showTimeDialog = true },
                    modifier = Modifier.testTag("reminder-time"),
                )
            }
        }
        Spacer(Modifier.height(24.dp))
    }

    when (openDialog) {
        SettingsDialog.THEME -> ChoiceDialog(
            title = "Theme",
            selected = themeMode,
            choices = ThemeMode.entries.map { mode ->
                Choice(mode, mode.label(), testTag = "theme-option-${mode.name}")
            },
            onSelect = onThemeChange,
            onDismiss = { openDialog = null },
        )

        SettingsDialog.MOTION -> ChoiceDialog(
            title = "Motion",
            selected = motionPreference,
            choices = MotionPreference.entries.map { preference ->
                Choice(
                    value = preference,
                    label = preference.label(),
                    supportingText = preference.description(),
                    testTag = "motion-option-${preference.name}",
                )
            },
            onSelect = onMotionPreferenceChange,
            onDismiss = { openDialog = null },
        )

        SettingsDialog.NEW_CARDS -> ChoiceDialog(
            title = "New cards per day",
            selected = newCardLimit,
            choices = NewCardOptions.map { (value, label) ->
                Choice(value, label, testTag = "new-cards-option-$value")
            },
            onSelect = onNewCardLimitChange,
            onDismiss = { openDialog = null },
        )

        null -> Unit
    }

    if (showTimeDialog) {
        ReminderTimeDialog(
            initial = reminderTime,
            onConfirm = { hour, minute ->
                onReminderTimeChange(hour, minute)
                showTimeDialog = false
            },
            onDismiss = { showTimeDialog = false },
        )
    }
}

@Composable
private fun SettingsIntroCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MochiLogo(
                animateEntry = false,
                interactive = false,
                modifier = Modifier
                    .size(58.dp)
                    .clearAndSetSemantics {},
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(SETTINGS_INTRO_TITLE, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "Tune the look, motion, and rhythm anytime.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f),
                )
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
    )
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        shadowElevation = 2.dp,
    ) {
        Column(content = content)
    }
}

@Composable
private fun PreferenceRow(
    icon: ImageVector,
    label: String,
    supportingText: String,
    value: String,
    testTag: String,
    iconBackground: Color,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .testTag(testTag)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingsIcon(icon = icon, background = iconBackground)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = supportingText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SettingsIcon(icon: ImageVector, background: Color) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(background)
            .clearAndSetSemantics {},
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun ReminderToggleRow(enabled: Boolean, onChange: (Boolean) -> Unit) {
    val haptics = LocalHapticFeedback.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .clickable(role = Role.Switch) {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onChange(!enabled)
            }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingsIcon(
            icon = Icons.Filled.Notifications,
            background = MaterialTheme.colorScheme.primaryContainer,
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("Daily reminder", style = MaterialTheme.typography.bodyLarge)
            Text(
                text = "A gentle nudge to keep studying",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = enabled, onCheckedChange = null)
    }
}

@Composable
private fun ReminderTimeRow(
    time: ReminderTime,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingsIcon(
            icon = Icons.Filled.Schedule,
            background = MaterialTheme.colorScheme.secondaryContainer,
        )
        Spacer(Modifier.width(12.dp))
        Text("Time", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(
            text = time.formatted(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun <T> ChoiceDialog(
    title: String,
    selected: T,
    choices: List<Choice<T>>,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                choices.forEach { choice ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .clickable(role = Role.RadioButton) {
                                onSelect(choice.value)
                                onDismiss()
                            }
                            .testTag(choice.testTag)
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = choice.value == selected, onClick = null)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(choice.label, style = MaterialTheme.typography.bodyLarge)
                            choice.supportingText?.let { supportingText ->
                                Text(
                                    text = supportingText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderTimeDialog(
    initial: ReminderTime,
    onConfirm: (Int, Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val state = rememberTimePickerState(
        initialHour = initial.hour,
        initialMinute = initial.minute,
        is24Hour = true,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reminder time") },
        text = { TimePicker(state = state) },
        confirmButton = {
            TextButton(onClick = { onConfirm(state.hour, state.minute) }) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
