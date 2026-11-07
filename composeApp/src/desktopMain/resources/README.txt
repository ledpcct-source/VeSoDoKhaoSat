Thư mục này dùng để chứa icon khi đóng gói ứng dụng Desktop (nativeDistributions):
- icon.ico: Dùng cho Windows Installer (.exe / .msi) và shortcut desktop.
- icon.icns: Dùng cho macOS Installer (.dmg / .pkg).
- icon.png: Dùng cho Linux package (.deb / .rpm).

File cấu hình tương ứng trong composeApp/build.gradle.kts:
windows {
    iconFile.set(project.file("src/desktopMain/resources/icon.ico"))
}

