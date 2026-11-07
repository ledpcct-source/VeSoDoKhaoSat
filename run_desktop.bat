@echo off
setlocal
title Ve So Do Khao Sat - Desktop
cd /d "%~dp0"

echo ========================================================
echo   KHOI CHAY UNG DUNG VE SO DO KHAO SAT (DESKTOP)
echo ========================================================
echo.

rem Uu tien su dung JDK 17/21 tu Android Studio JBR de tuong thich Gradle va Compose Multiplatform
if exist "C:\Program Files\Android\Android Studio\jbr\bin\java.exe" (
    set "JAVA_HOME=C:\Program Files\Android\Android Studio\jbr"
) else if exist "%LOCALAPPDATA%\Programs\Android Studio\jbr\bin\java.exe" (
    set "JAVA_HOME=%LOCALAPPDATA%\Programs\Android Studio\jbr"
) else if exist "C:\Program Files\Android\Android Studio 1\jbr\bin\java.exe" (
    set "JAVA_HOME=C:\Program Files\Android\Android Studio 1\jbr"
) else if exist "C:\Program Files\Java\jdk-17\bin\java.exe" (
    set "JAVA_HOME=C:\Program Files\Java\jdk-17"
) else if exist "C:\Program Files\Java\jdk-21\bin\java.exe" (
    set "JAVA_HOME=C:\Program Files\Java\jdk-21"
)

if defined JAVA_HOME (
    set "PATH=%JAVA_HOME%\bin;%PATH%"
    echo [OK] JAVA_HOME: %JAVA_HOME%
) else (
    echo [CANH BAO] Khong tim thay Android Studio JBR, dung moi truong mac dinh.
)

echo.
echo Dang bien dich va khoi dong ung dung Desktop...
echo Vui long doi vai giay de cua so xuat hien...
echo.

call "%~dp0gradlew.bat" :composeApp:run

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ========================================================
    echo [LOI] Khong the khoi chay ung dung! Ma loi: %ERRORLEVEL%
    echo ========================================================
    echo.
    pause
)

endlocal
