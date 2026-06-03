package com.mochi.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.mochi.reminder.ReminderTime
import com.mochi.settings.ThemeMode

private val NEW_CARD_OPTIONS = listOf(10 to "10", 20 to "20", 30 to "30", 0 to "Unlimited")

/** Settings tab: appearance (theme), the daily new-card limit and the daily reminder. */
@Composable
fun SettingsScreen(
    themeMode: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    newCardLimit: Int,
    onNewCardLimitChange: (Int) -> Unit,
    reminderEnabled: Boolean,
    onReminderEnabledChange: (Boolean) -> Unit,
    reminderTime: ReminderTime,
    onReminderTimeChange: (Int, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showTimeDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 16.dp),
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp),
        )

        SectionLabel("Appearance")
        ThemeMode.entries.forEach { mode ->
            OptionRow(selected = mode == themeMode, label = mode.label()) { onThemeChange(mode) }
        }

        Spacer(Modifier.height(16.dp))
        SectionLabel("New cards per day")
        NEW_CARD_OPTIONS.forEach { (value, label) ->
            OptionRow(selected = value == newCardLimit, label = label) { onNewCardLimitChange(value) }
        }

        Spacer(Modifier.height(16.dp))
        SectionLabel("Daily reminder")
        ReminderToggleRow(enabled = reminderEnabled, onChange = onReminderEnabledChange)
        if (reminderEnabled) {
            ReminderTimeRow(time = reminderTime, onClick = { showTimeDialog = true })
        }
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
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 16.dp, bottom = 4.dp),
    )
}

@Composable
private fun OptionRow(selected: Boolean, label: String, onClick: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (!selected) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(Modifier.width(12.dp))
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun ReminderToggleRow(enabled: Boolean, onChange: (Boolean) -> Unit) {
    val haptics = LocalHapticFeedback.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onChange(!enabled)
            }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("Remind me to study", style = MaterialTheme.typography.bodyLarge)
            Text(
                text = "A daily nudge to keep your streak going",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = enabled, onCheckedChange = null)
    }
}

@Composable
private fun ReminderTimeRow(time: ReminderTime, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Time",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = time.formatted(),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
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

private fun ThemeMode.label(): String = when (this) {
    ThemeMode.SYSTEM -> "System default"
    ThemeMode.LIGHT -> "Light"
    ThemeMode.DARK -> "Dark"
}
