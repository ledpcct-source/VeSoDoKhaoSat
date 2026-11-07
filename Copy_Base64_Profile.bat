@echo off
chcp 65001 >nul
echo =======================================================
echo     TOOL COPY BASE64 CUA FILE PROVISIONING PROFILE
echo =======================================================
echo.

setlocal enabledelayedexpansion
set PROFILE_FILE=
for %%F in (*.mobileprovision) do (
    set PROFILE_FILE=%%F
    goto :found
)

:found
if "%PROFILE_FILE%"=="" (
    echo [LOI] Khong tim thay file .mobileprovision nao trong thu muc nay!
    echo Vui long tai file Profile tu Apple Developer ve dat vao thu muc nay:
    echo %~dp0
    echo.
    pause
    exit /b
)

echo Tim thay file profile: %PROFILE_FILE%
powershell -Command "[Convert]::ToBase64String([IO.File]::ReadAllBytes('%PROFILE_FILE%')) | Set-Clipboard"

echo.
echo =======================================================
echo [THANH CONG] Da copy ma Base64 vao bo nho tam (Clipboard)!
echo Ban chi can sang GitHub va nhan Ctrl + V de dan vao:
echo Secret: APPLE_PROVISIONING_PROFILE_BASE64
echo =======================================================
echo.
pause
