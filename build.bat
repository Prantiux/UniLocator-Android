@echo off
echo Setting up UniLocator Android App...
echo.

echo Checking if Android Studio is installed...
where /q android.bat
if %ERRORLEVEL% NEQ 0 (
    echo Android Studio command line tools not found in PATH
    echo Please install Android Studio and add platform-tools to your PATH
    pause
    exit /b 1
)

echo.
echo ========================================
echo IMPORTANT: Firebase Setup Required
echo ========================================
echo.
echo Before building the app, you need to:
echo 1. Create a Firebase project at https://console.firebase.google.com/
echo 2. Add an Android app with package name: com.unilocator.app
echo 3. Download google-services.json
echo 4. Replace the placeholder google-services.json in app/ folder
echo 5. Enable Email/Password authentication in Firebase Console
echo.
echo ========================================
echo.

set /p continue="Have you completed Firebase setup? (y/N): "
if /i NOT "%continue%"=="y" (
    echo.
    echo Please complete Firebase setup first, then run this script again.
    echo Detailed instructions are in README.md
    pause
    exit /b 1
)

echo.
echo Checking Gradle wrapper...
if not exist "gradlew.bat" (
    echo Gradle wrapper not found. Creating...
    gradle wrapper
)

echo.
echo Building the project...
call gradlew.bat assembleDebug

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ========================================
    echo Build completed successfully!
    echo ========================================
    echo.
    echo APK location: app\build\outputs\apk\debug\app-debug.apk
    echo.
    echo To install on connected device, run:
    echo gradlew.bat installDebug
    echo.
) else (
    echo.
    echo ========================================
    echo Build failed!
    echo ========================================
    echo.
    echo Please check the error messages above.
    echo Common issues:
    echo - Missing or incorrect google-services.json
    echo - Internet connection required for dependencies
    echo - Android SDK not properly configured
    echo.
)

pause
