package com.vesodo.khaosat

import androidx.compose.ui.window.ComposeUIViewController
import com.vesodo.khaosat.ui.VeSoDoScreen
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController = ComposeUIViewController {
    VeSoDoScreen()
}

