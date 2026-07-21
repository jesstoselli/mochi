package com.mochi

import androidx.compose.ui.window.ComposeUIViewController
import com.mochi.data.DriverFactory
import com.mochi.reminder.IosReminderScheduler
import platform.UIKit.UIViewController

/**
 * Called from Swift (MainViewControllerKt.MainViewController()) to host Compose on iOS.
 * PascalCase is required by that Swift interop name, so the naming checks are suppressed.
 */
@Suppress("ktlint:standard:function-naming", "FunctionNaming")
fun MainViewController(): UIViewController = ComposeUIViewController {
    App(
        driverFactory = DriverFactory(),
        reminderScheduler = IosReminderScheduler(),
    )
}
