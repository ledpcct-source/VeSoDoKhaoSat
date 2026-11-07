@echo off
setlocal enabledelayedexpansion
chcp 65001 >nul

echo =======================================================================
echo        XUAT BAN UNG DUNG DESKTOP WINDOWS (VeSoDoKhaoSat)
echo =======================================================================
echo.

rem 0. Tu dong dong ung dung cu neu dang mo de tranh bi Windows khoa file (File Lock)
tasklist /fi "imagename eq VeSoDoKhaoSat*" 2>nul | findstr /i "VeSoDoKhaoSat" >nul
if not errorlevel 1 (
    echo [THONG BAO] Phat hien ung dung hoac bo cai cu dang chay!
    echo Dang dong tien trinh de cap nhat de len phien ban cu...
    taskkill /F /IM VeSoDoKhaoSat* >nul 2>&1
    timeout /t 1 >nul
)

rem 1. Tim JDK 17/21 phu hop cho dong goi Compose Desktop (can co jpackage/javac)
set "FOUND_JDK="

rem Kiem tra Eclipse Adoptium Temurin
if exist "C:\Program Files\Eclipse Adoptium\jdk-21.0.10.7-hotspot\bin\jpackage.exe" (
    set "FOUND_JDK=C:\Program Files\Eclipse Adoptium\jdk-21.0.10.7-hotspot"
) else (
    for /d %%D in ("C:\Program Files\Eclipse Adoptium\jdk-*") do (
        if exist "%%D\bin\jpackage.exe" set "FOUND_JDK=%%D"
    )
)

rem Kiem tra Oracle / OpenJDK trong C:\Program Files\Java
if "!FOUND_JDK!"=="" (
    for /d %%D in ("C:\Program Files\Java\jdk-*") do (
        if exist "%%D\bin\jpackage.exe" set "FOUND_JDK=%%D"
    )
)

rem Kiem tra Android Studio JBR
if "!FOUND_JDK!"=="" (
    if exist "C:\Program Files\Android\Android Studio\jbr\bin\java.exe" (
        set "FOUND_JDK=C:\Program Files\Android\Android Studio\jbr"
    ) else if exist "C:\Program Files\Android\Android Studio1\jbr\bin\java.exe" (
        set "FOUND_JDK=C:\Program Files\Android\Android Studio1\jbr"
    )
)

rem Kiem tra JAVA_HOME neu khong phai Java 1.8
if "!FOUND_JDK!"=="" (
    if not "%JAVA_HOME%"=="" (
        echo %JAVA_HOME% | findstr /i "1.8 jre" >nul
        if errorlevel 1 (
            if exist "%JAVA_HOME%\bin\java.exe" set "FOUND_JDK=%JAVA_HOME%"
        )
    )
)

if not "!FOUND_JDK!"=="" (
    set "JAVA_HOME=!FOUND_JDK!"
    set "PATH=!FOUND_JDK!\bin;%PATH%"
    echo [OK] Da tim thay JDK phu hop: !JAVA_HOME!
) else (
    echo [CANH BAO] Khong tim thay JDK 17/21 phu hop, se thu chay voi cau hinh he thong...
)

echo.
echo Dang tien hanh dong goi ung dung Desktop...
echo  1. Ban chay truc tiep khong can cai dat (createDistributable)
echo  2. File cai dat Setup .EXE cho Windows (packageExe)
echo  3. File cai dat Setup .MSI cho Windows (packageMsi)
echo Vui long cho trong giay lat...
echo.

cd /d "%~dp0"
call "%~dp0gradlew.bat" :composeApp:createDistributable :composeApp:packageExe :composeApp:packageMsi

if errorlevel 1 (
    echo.
    echo =======================================================================
    echo [LOI] Qua trinh dong goi that bai! Vui long kiem tra lai thong bao loi o tren.
    echo =======================================================================
    echo.
    pause
    exit /b 1
)

set "DEST_DIR=%~dp0Xuat_Ban_Desktop"
if not exist "%DEST_DIR%" mkdir "%DEST_DIR%"

rem 1. Sao chep ban chay truc tiep (Portable)
set "APP_SRC=%~dp0composeApp\build\compose\binaries\main\app\VeSoDoKhaoSat"
if not exist "%APP_SRC%" set "APP_SRC=%~dp0composeApp\build\windows_dist\main\app\VeSoDoKhaoSat"

if exist "%APP_SRC%" (
    echo [1/4] Dang cap nhat ban chay truc tiep vao Xuat_Ban_Desktop...
    if not exist "%DEST_DIR%\VeSoDoKhaoSat_ChayNgay" mkdir "%DEST_DIR%\VeSoDoKhaoSat_ChayNgay"
    robocopy "%APP_SRC%" "%DEST_DIR%\VeSoDoKhaoSat_ChayNgay" /E /IS /IT /nfl /ndl /njh /njs /nc /ns /np >nul
)

rem 2. Tim va sao chep bo cai dat Setup .EXE moi nhat
set "EXE_SRC="
set "EXE_NAME="
for /f "delims=" %%F in ('dir /b /o:-d "%~dp0composeApp\build\compose\binaries\main\exe\VeSoDoKhaoSat-*.exe" 2^>nul') do (
    set "EXE_SRC=%~dp0composeApp\build\compose\binaries\main\exe\%%F"
    set "EXE_NAME=%%F"
    goto :copy_exe
)
for /f "delims=" %%F in ('dir /b /o:-d "%~dp0composeApp\build\windows_dist\main\exe\VeSoDoKhaoSat-*.exe" 2^>nul') do (
    set "EXE_SRC=%~dp0composeApp\build\windows_dist\main\exe\%%F"
    set "EXE_NAME=%%F"
    goto :copy_exe
)

:copy_exe
if exist "%EXE_SRC%" (
    echo [2/4] Dang sao chep bo cai dat Setup .EXE...
    copy /y "%EXE_SRC%" "%DEST_DIR%\!EXE_NAME!" >nul
    copy /y "%EXE_SRC%" "%DEST_DIR%\VeSoDoKhaoSat_Setup_MoiNhat.exe" >nul
    copy /y "%EXE_SRC%" "%~dp0!EXE_NAME!" >nul
)

rem 3. Tim va sao chep bo cai dat Setup .MSI moi nhat
set "MSI_SRC="
set "MSI_NAME="
for /f "delims=" %%F in ('dir /b /o:-d "%~dp0composeApp\build\compose\binaries\main\msi\VeSoDoKhaoSat-*.msi" 2^>nul') do (
    set "MSI_SRC=%~dp0composeApp\build\compose\binaries\main\msi\%%F"
    set "MSI_NAME=%%F"
    goto :copy_msi
)
for /f "delims=" %%F in ('dir /b /o:-d "%~dp0composeApp\build\windows_dist\main\msi\VeSoDoKhaoSat-*.msi" 2^>nul') do (
    set "MSI_SRC=%~dp0composeApp\build\windows_dist\main\msi\%%F"
    set "MSI_NAME=%%F"
    goto :copy_msi
)

:copy_msi
if exist "%MSI_SRC%" (
    echo [3/4] Dang sao chep bo cai dat Setup .MSI...
    copy /y "%MSI_SRC%" "%DEST_DIR%\!MSI_NAME!" >nul
    copy /y "%MSI_SRC%" "%DEST_DIR%\VeSoDoKhaoSat_Setup_MoiNhat.msi" >nul
    copy /y "%MSI_SRC%" "%~dp0!MSI_NAME!" >nul
)

rem 4. Tao file chay nhanh tien loi
echo [4/4] Tao phim tat khoi dong nhanh...
echo @echo off > "%DEST_DIR%\Chay_Ngay.bat"
echo start "" "%%~dp0VeSoDoKhaoSat_ChayNgay\VeSoDoKhaoSat.exe" >> "%DEST_DIR%\Chay_Ngay.bat"

echo @echo off > "%~dp0Chay_VeSoDo_Desktop.bat"
echo if exist "%%~dp0Xuat_Ban_Desktop\VeSoDoKhaoSat_ChayNgay\VeSoDoKhaoSat.exe" ( >> "%~dp0Chay_VeSoDo_Desktop.bat"
echo     start "" "%%~dp0Xuat_Ban_Desktop\VeSoDoKhaoSat_ChayNgay\VeSoDoKhaoSat.exe" >> "%~dp0Chay_VeSoDo_Desktop.bat"
echo ) else if exist "%%~dp0composeApp\build\compose\binaries\main\app\VeSoDoKhaoSat\VeSoDoKhaoSat.exe" ( >> "%~dp0Chay_VeSoDo_Desktop.bat"
echo     start "" "%%~dp0composeApp\build\compose\binaries\main\app\VeSoDoKhaoSat\VeSoDoKhaoSat.exe" >> "%~dp0Chay_VeSoDo_Desktop.bat"
echo ) else ( >> "%~dp0Chay_VeSoDo_Desktop.bat"
echo     echo Chua tim thay ban build! Vui long chay export_desktop.bat de dong goi ung dung. >> "%~dp0Chay_VeSoDo_Desktop.bat"
echo     pause >> "%~dp0Chay_VeSoDo_Desktop.bat"
echo ) >> "%~dp0Chay_VeSoDo_Desktop.bat"

rem Tao file huong dan su dung va nang cap
echo ======================================================================= > "%DEST_DIR%\HUONG_DAN_SU_DUNG.txt"
echo     HUONG DAN CAI DAT VA NANG CAP UNG DUNG VE SO DO KHAO SAT >> "%DEST_DIR%\HUONG_DAN_SU_DUNG.txt"
echo ======================================================================= >> "%DEST_DIR%\HUONG_DAN_SU_DUNG.txt"
echo. >> "%DEST_DIR%\HUONG_DAN_SU_DUNG.txt"
echo 1. NANG CAP DE LEN PHIEN BAN CU (UPGRADE): >> "%DEST_DIR%\HUONG_DAN_SU_DUNG.txt"
echo    - Khi co ban moi, chay file Setup .EXE hoac .MSI (hoac VeSoDoKhaoSat_Setup_MoiNhat.exe). >> "%DEST_DIR%\HUONG_DAN_SU_DUNG.txt"
echo    - Bo cai se tu dong nhan dien ban cu, go bo file cu va ghi de file moi vao. >> "%DEST_DIR%\HUONG_DAN_SU_DUNG.txt"
echo    - Toan bo du lieu ban ve, cau hinh cua ban se duoc giu nguyen ven. >> "%DEST_DIR%\HUONG_DAN_SU_DUNG.txt"
echo. >> "%DEST_DIR%\HUONG_DAN_SU_DUNG.txt"
echo 2. NEU UNG DUNG CU DANG MO (DANG CHAY): >> "%DEST_DIR%\HUONG_DAN_SU_DUNG.txt"
echo    - Vui long dong ung dung VeSoDoKhaoSat truoc khi chay bo cai hoac copy ban moi. >> "%DEST_DIR%\HUONG_DAN_SU_DUNG.txt"
echo    - Script export_desktop.bat se tu dong dong ung dung cu neu phat hien dang chay. >> "%DEST_DIR%\HUONG_DAN_SU_DUNG.txt"
echo. >> "%DEST_DIR%\HUONG_DAN_SU_DUNG.txt"
echo 3. DUNG BAN PORTABLE (KHONG CAN CAI DAT): >> "%DEST_DIR%\HUONG_DAN_SU_DUNG.txt"
echo    - Nhan dup file Chay_Ngay.bat trong thu muc nay de dung ngay. >> "%DEST_DIR%\HUONG_DAN_SU_DUNG.txt"
echo    - Muon dem sang may khac: Copy ca thu muc VeSoDoKhaoSat_ChayNgay la chay duoc moi may. >> "%DEST_DIR%\HUONG_DAN_SU_DUNG.txt"
echo ======================================================================= >> "%DEST_DIR%\HUONG_DAN_SU_DUNG.txt"

echo.
echo =======================================================================
echo   [THANH CONG] CAC FILE NANG CAP DA DUOC XUAT RA TAI:
echo   %DEST_DIR%
echo =======================================================================
echo.
echo   1. File bo cai dat nang cap .MSI:
echo      %DEST_DIR%\!MSI_NAME!
echo.
echo   2. File bo cai dat nang cap Setup .EXE:
echo      %DEST_DIR%\!EXE_NAME!
echo.
echo   3. Thu muc chay truc tiep khong can cai dat (Portable):
echo      %DEST_DIR%\VeSoDoKhaoSat_ChayNgay\VeSoDoKhaoSat.exe
echo      (Hoac double-click: %DEST_DIR%\Chay_Ngay.bat)
echo.
echo   4. Shortcut ngay thu muc goc:
echo      %~dp0Chay_VeSoDo_Desktop.bat
echo.
echo Dang mo thu muc Xuat_Ban_Desktop trong Windows Explorer...
start "" explorer.exe "%DEST_DIR%"

echo.
pause
