@echo off
chcp 65001 >nul
cd /d "%~dp0"
echo ================================================
echo   TopZurdo Builder - Mod Only
echo ================================================
echo.

REM Find Java 17
set JAVA_HOME=
if exist "C:\Program Files\Java\jdk-17.0.16+8" set JAVA_HOME=C:\Program Files\Java\jdk-17.0.16+8
if exist "C:\Program Files\Eclipse Adoptium\jdk-17.0.15.6-hotspot" set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.15.6-hotspot

if "%JAVA_HOME%"=="" (
    echo Searching for Java 17...
    for /d %%i in ("C:\Program Files\Java\jdk-17*") do set JAVA_HOME=%%i
)

if "%JAVA_HOME%"=="" (
    for /d %%i in ("C:\Program Files\Eclipse Adoptium\jdk-17*") do set JAVA_HOME=%%i
)

if "%JAVA_HOME%"=="" (
    echo ERROR: JDK 17 not found!
    echo Install JDK 17 from https://adoptium.net/
    pause
    exit /b 1
)

echo Using Java: %JAVA_HOME%
set "PATH=%JAVA_HOME%\bin;%PATH%"
echo.

echo [1/3] Cleaning previous builds...
if exist "dist" rmdir /s /q "dist" 2>nul
echo Cleanup completed!
echo.

echo [2/3] Building mod...
call gradlew.bat :mod:jar --no-daemon -q
if errorlevel 1 (
    echo ERROR: Mod build failed!
    pause
    exit /b 1
)
echo Mod built successfully!
echo.

echo [3/3] Creating mods package...
if not exist "dist\mods" mkdir "dist\mods"
copy "mod\build\libs\topzurdo-mod-1.0.0.jar" "dist\mods\" >nul 2>nul
echo Mod packaged!
echo.

echo ================================================
echo   Build completed!
echo ================================================
echo.
echo Result: dist\mods\topzurdo-mod-1.0.0.jar
echo.
pause
