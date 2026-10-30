@echo off
setlocal
rem Change to project root directory
set SCRIPT_DIR=%~dp0
cd /d "%SCRIPT_DIR%.."

echo ==========================================
echo Starting VOTUM — Campus Election System...
echo ==========================================

if not exist "bin" mkdir bin
del /s /q src\*.class >nul 2>&1
dir /s /B src\*.java > sources.txt
javac --release 17 -d bin @sources.txt
set COMPILE_STATUS=%ERRORLEVEL%
del sources.txt

if %COMPILE_STATUS% equ 0 (
    java -cp bin com.votum.Main %*
) else (
    echo Compilation failed. Aborting run.
    pause
    exit /b %COMPILE_STATUS%
)

