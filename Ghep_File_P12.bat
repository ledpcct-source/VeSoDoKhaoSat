@echo off
chcp 65001 >nul
echo =======================================================
echo          TOOL GHEP FILE CHUNG CHI APPLE .P12
echo =======================================================
echo.

if not exist "distribution.cer" (
    echo [LOI] Khong tim thay file "distribution.cer"!
    echo Vui long tai file "distribution.cer" tu trang Apple Developer ve va dat vao thu muc nay:
    echo %~dp0
    echo.
    pause
    exit /b
)

if not exist "ios_distribution.key" (
    echo [LOI] Khong tim thay file "ios_distribution.key"!
    pause
    exit /b
)

set OPENSSL="C:\Program Files\Git\usr\bin\openssl.exe"
set OPENSSL_CONF=C:\Program Files\Git\usr\ssl\openssl.cnf

echo 1. Dang chuyen doi distribution.cer sang PEM...
%OPENSSL% x509 -in "distribution.cer" -inform DER -out "distribution.pem" -outform PEM

echo 2. Dang ghep thanh file ios_distribution.p12...
echo.
echo === CHU Y QUAN TRONG ===
echo He thong se yeu cau nhap mat khau cho file .p12.
echo Hay nhap mat khau ban muon dat (vi du: 123456Aa@) va nhan Enter.
echo Sau do nhap lai mat khau mot lan nua.
echo ========================
echo.

%OPENSSL% pkcs12 -export -inkey "ios_distribution.key" -in "distribution.pem" -out "ios_distribution.p12"

if exist "ios_distribution.p12" (
    echo.
    echo =======================================================
    echo [THANH CONG] Da tao xong file: ios_distribution.p12!
    echo.
    echo Bay gio dang tu dong copy chuoi Base64 vao Clipboard...
    powershell -Command "[Convert]::ToBase64String([IO.File]::ReadAllBytes('ios_distribution.p12')) | Set-Clipboard"
    echo [XONG] Da copy ma Base64 vao bo nho tam (Clipboard)!
    echo Ban chi can sang GitHub va nhan Ctrl + V de dan vao:
    echo Secret: APPLE_CERTIFICATE_BASE64
    echo =======================================================
) else (
    echo [LOI] Tao file .p12 that bai. Vui long kiem tra lai.
)

echo.
pause
