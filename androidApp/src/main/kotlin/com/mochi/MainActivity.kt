package com.mochi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.mochi.data.DriverFactory
import com.mochi.reminder.AndroidReminderScheduler

class MainActivity : ComponentActivity() {

    // Android 13+ runtime permission for posting the daily reminder notification.
    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Show the themed splash, then hand off to Compose (must run before super.onCreate).
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val reminderScheduler = AndroidReminderScheduler(applicationContext) { permission ->
            notificationPermission.launch(permission)
        }
        setContent {
            App(
                driverFactory = DriverFactory(applicationContext),
                reminderScheduler = reminderScheduler,
            )
        }
    }
}
