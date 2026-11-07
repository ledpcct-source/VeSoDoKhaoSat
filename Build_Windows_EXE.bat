@echo off
setlocal enabledelayedexpansion
chcp 65001 >nul
title Dong Goi Bo Cai Dat Windows (.exe) - VeSoDoKhaoSat

echo ========================================================
echo   DONG GOI BO CAI DAT WINDOWS (.EXE) - VeSoDoKhaoSat
echo ========================================================
echo.

rem Kiem tra JAVA_HOME hien tai
if not "%JAVA_HOME%"=="" (
    if exist "%JAVA_HOME%\bin\java.exe" (
        goto :check_java_version
    )
)

rem Tu dong tim Android Studio JBR
set "FOUND_JDK="
if exist "C:\Program Files\Android\Android Studio\jbr\bin\java.exe" (
    set "FOUND_JDK=C:\Program Files\Android\Android Studio\jbr"
) else if exist "%LOCALAPPDATA%\Programs\Android Studio\jbr\bin\java.exe" (
    set "FOUND_JDK=%LOCALAPPDATA%\Programs\Android Studio\jbr"
) else if exist "C:\Program Files\Java\jdk-21\bin\java.exe" (
    set "FOUND_JDK=C:\Program Files\Java\jdk-21"
) else if exist "C:\Program Files\Java\jdk-17\bin\java.exe" (
    set "FOUND_JDK=C:\Program Files\Java\jdk-17"
)

if not "%FOUND_JDK%"=="" (
    set "JAVA_HOME=%FOUND_JDK%"
    set "PATH=%FOUND_JDK%\bin;%PATH%"
    echo [OK] Su dung JDK tai: %JAVA_HOME%
    goto :run_build
)

:check_java_version
echo [OK] JAVA_HOME: %JAVA_HOME%

:run_build
echo.
echo Dang dong goi bo cai dat Windows (.exe)...
echo Tien trinh se tu dong nhung san Runtime Java, gan Icon va tao file Setup .exe hoan chinh.
echo Vui long doi vai giay...
echo.

cd /d "%~dp0"
call gradlew.bat :composeApp:packageExe

if errorlevel 1 (
    echo.
    echo ========================================================
    echo [LOI] Dong goi that bai. Vui long kiem tra lai thong bao tren.
    echo ========================================================
    pause
    exit /b 1
)

set "EXE_FILE="
for /f "delims=" %%F in ('dir /b /o:-d "%~dp0composeApp\build\windows_dist\main\exe\VeSoDoKhaoSat-*.exe" 2^>nul') do (
    set "EXE_FILE=%~dp0composeApp\build\windows_dist\main\exe\%%F"
    goto :found_exe
)
for /f "delims=" %%F in ('dir /b /o:-d "%~dp0composeApp\build\compose\binaries\main\exe\VeSoDoKhaoSat-*.exe" 2^>nul') do (
    set "EXE_FILE=%~dp0composeApp\build\compose\binaries\main\exe\%%F"
    goto :found_exe
)
:found_exe

if exist "%EXE_FILE%" (
    copy /y "%EXE_FILE%" "%~dp0" >nul
    echo.
    echo ========================================================
    echo   [THANH CONG] File cai dat Windows Setup .EXE da tao xong tai:
    echo   %EXE_FILE%
    echo   (Da tu dong sao chep ra thu muc goc: %~dp0)
    echo ========================================================
    echo.
    echo Dang mo thu muc chua file cai dat va bôi xanh file...
    explorer.exe /select,"%EXE_FILE%"
) else (
    echo.
    echo Da chay xong nhung khong tim thay file %EXE_FILE%.
    echo Dang mo thu muc binaries...
    start explorer.exe "%~dp0composeApp\build\compose\binaries\main"
)

echo.
pause

