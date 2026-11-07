package com.vesodo.khaosat

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.vesodo.khaosat.config.AppConfig
import org.jetbrains.compose.resources.painterResource
import vesodokhaosat.composeapp.generated.resources.Res
import vesodokhaosat.composeapp.generated.resources.app_icon

fun main() {
    // Bắt lỗi toàn cục: Ngăn ngừa app bị crash/đóng đột ngột, hiển thị dialog lỗi
    Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
        System.err.println("Phát hiện lỗi trên luồng [${thread.name}]: ${throwable.message}")
        throwable.printStackTrace()
        AppConfig.reportError(throwable, "Luồng: ${thread.name}")
    }

    application {
        val windowState = rememberWindowState(width = 1280.dp, height = 800.dp)
        Window(
            onCloseRequest = ::exitApplication,
            state = windowState,
            title = "Vẽ Sơ Đồ Khảo Sát - Trần Hữu Thiện - Phòng KHCNCĐS PC Cần Thơ",
            icon = painterResource(Res.drawable.app_icon)
        ) {
            App()
        }
    }
}
