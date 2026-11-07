@echo off 
if exist "%~dp0Xuat_Ban_Desktop\VeSoDoKhaoSat_ChayNgay\VeSoDoKhaoSat.exe" ( 
    start "" "%~dp0Xuat_Ban_Desktop\VeSoDoKhaoSat_ChayNgay\VeSoDoKhaoSat.exe" 
) else if exist "%~dp0composeApp\build\compose\binaries\main\app\VeSoDoKhaoSat\VeSoDoKhaoSat.exe" ( 
    start "" "%~dp0composeApp\build\compose\binaries\main\app\VeSoDoKhaoSat\VeSoDoKhaoSat.exe" 
) else ( 
    echo Chua tim thay ban build Vui long chay export_desktop.bat de dong goi ung dung. 
    pause 
) 
