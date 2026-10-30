@echo off
setlocal
rem Change to project root directory
set SCRIPT_DIR=%~dp0
cd /d "%SCRIPT_DIR%.."

echo ==========================================
echo Compiling VOTUM Java Application...
echo ==========================================

if not exist "bin" mkdir bin
rem Ensure source directory only contains .java files (remove any stray bytecode)
del /s /q src\*.class >nul 2>&1
dir /s /B src\*.java > sources.txt
javac -Xlint:all -d bin @sources.txt
set COMPILE_STATUS=%ERRORLEVEL%
del sources.txt

if %COMPILE_STATUS% equ 0 (
    echo Compilation successful! Class files generated in bin/
) else (
    echo Compilation failed. Please check errors above.
    pause
    exit /b %COMPILE_STATUS%
)

