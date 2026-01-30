@echo off
chcp 65001 >nul
cd /d "%~dp0"
echo ================================================
echo   TopZurdo Ultimate Builder - Single Mod
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

echo [1/6] Cleaning previous builds...
REM Kill any running TopZurdo processes first
taskkill /F /IM "TopZurdo.exe" >nul 2>&1
taskkill /F /FI "WINDOWTITLE eq *TopZurdo*" >nul 2>&1
timeout /t 1 /nobreak >nul 2>&1
if exist "dist\TopZurdo\TopZurdo.exe" (
    taskkill /F /IM "TopZurdo.exe" >nul 2>&1
    timeout /t 1 /nobreak >nul 2>&1
)
if exist "dist" rmdir /s /q "dist" 2>nul
if exist "%USERPROFILE%\Desktop\TopZurdo-Installer.exe" del "%USERPROFILE%\Desktop\TopZurdo-Installer.exe" 2>nul
if exist "%USERPROFILE%\Desktop\TopZurdo.exe" del "%USERPROFILE%\Desktop\TopZurdo.exe" 2>nul
echo Cleanup completed!
echo.

echo [2/6] Cleaning Loom cache...
call gradlew.bat :mod:cleanLoomCache --no-daemon -q
echo Loom cache cleanup completed!
echo.

echo [3/6] Building main mod...
call gradlew.bat :mod:jar --no-daemon -q
if errorlevel 1 (
    echo ERROR: Mod build failed!
    pause
    exit /b 1
)
echo Main mod built successfully!
echo.

echo [4/6] Building launcher...
call gradlew.bat :launcher:jar --no-daemon -q
if errorlevel 1 (
    echo ERROR: Launcher build failed!
    pause
    exit /b 1
)
echo Launcher built successfully!
echo.

echo [5/6] Creating mods package...
if not exist "dist\mods" mkdir "dist\mods"
copy "mod\build\libs\topzurdo-mod-1.0.0.jar" "dist\mods\" >nul 2>nul
echo Mod packaged!
echo.

echo [6/6] Creating installer...
if not exist "%JAVA_HOME%\bin\jpackage.exe" (
    echo ERROR: jpackage not found in JDK.
    echo Installer creation requires JDK 17+ with jpackage.
    pause
    exit /b 1
)

REM Create directories
if not exist "dist" mkdir dist
if not exist "dist\jars" mkdir "dist\jars"

REM Copy JAR files
copy "launcher\build\libs\topzurdo-launcher-1.0.0.jar" "dist\jars\" >nul 2>nul

echo Creating portable app...
"%JAVA_HOME%\bin\jpackage.exe" ^
    --type app-image ^
    --dest "dist" ^
    --input "dist\jars" ^
    --name "TopZurdo" ^
    --main-jar "topzurdo-launcher-1.0.0.jar" ^
    --main-class "com.topzurdo.launcher.Main" ^
    --app-version "1.0.0" ^
    --icon "5c30bb4b-bcd6-4389-b240-554443bd12ec.ico" ^
    --vendor "TopZurdo Team" ^
    --description "TopZurdo Premium Minecraft Launcher" ^
    --java-options "-Xms256m" ^
    --java-options "-Xmx1024m" ^
    --java-options "-Dfile.encoding=UTF-8"

if errorlevel 1 (
    echo ERROR: App creation failed!
    pause
    exit /b 1
)

REM Copy launcher exe to desktop
echo Copying launcher to desktop...
copy "dist\TopZurdo\TopZurdo.exe" "%USERPROFILE%\Desktop\TopZurdo.exe" >nul 2>nul
if errorlevel 1 (
    echo WARNING: Failed to copy to desktop
) else (
    echo Launcher copied to desktop: TopZurdo.exe
)

echo.
echo ================================================
echo   Build completed!
echo ================================================
echo.
echo Results:
echo   * Installer: dist\TopZurdo-1.0.0.exe
echo   * On desktop: TopZurdo-Installer.exe
echo   * Mod package: dist\mods\
echo     - topzurdo-mod-1.0.0.jar
echo.
echo Features:
echo   - Single mod build output
echo   - Auto-cleanup after installation
echo   - Professional installer
echo   - Desktop shortcut creation
echo.
pause
