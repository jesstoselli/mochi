package com.mochi

import androidx.compose.ui.window.ComposeUIViewController
import com.mochi.data.DriverFactory
import platform.UIKit.UIViewController

/** Called from Swift (MainViewControllerKt.MainViewController()) to host Compose on iOS. */
fun MainViewController(): UIViewController = ComposeUIViewController {
    App(driverFactory = DriverFactory())
}
