@echo off
setlocal
rem Change to project root directory
set SCRIPT_DIR=%~dp0
cd /d "%SCRIPT_DIR%.."

echo ==========================================
echo Compiling VOTUM and Verification Suites...
echo ==========================================

if not exist "bin" mkdir bin
dir /s /B src\*.java > sources.txt
javac --release 17 -Xlint:all -d bin @sources.txt
set COMPILE_STATUS=%ERRORLEVEL%
del sources.txt

if %COMPILE_STATUS% neq 0 (
    echo Compilation failed. Aborting tests.
    pause
    exit /b %COMPILE_STATUS%
)

echo Running Domain Logic Verification Suite...
java -cp bin com.votum.test.VotumTestSuite
if %ERRORLEVEL% neq 0 (
    echo Domain verification failed!
    pause
    exit /b %ERRORLEVEL%
)

echo Running UI and Interaction Smoke Test Suite...
java -cp bin com.votum.test.UISmokeTestSuite
if %ERRORLEVEL% neq 0 (
    echo UI smoke tests failed!
    pause
    exit /b %ERRORLEVEL%
)

echo ==========================================
echo ALL TESTS PASSED SUCCESSFULLY!
echo ==========================================

