@echo off
echo DarkEye - Simple Log Analysis System
echo ====================================
echo.

REM Create output directory
if not exist "target\classes" mkdir "target\classes"

echo Compiling core components...
javac -d "target\classes" "src\main\java\com\darkeye\model\*.java"
javac -d "target\classes" -cp "target\classes" "src\main\java\com\darkeye\collectors\*.java"
javac -d "target\classes" -cp "target\classes" "src\main\java\com\darkeye\detection\*.java"
javac -d "target\classes" -cp "target\classes" "src\main\java\com\darkeye\ui\ConsoleMainApp.java"

echo.
echo ✅ Compilation complete!
echo.
echo 🚀 Starting DarkEye...
echo.
echo Workflow:
echo 1. Authentication: admin123
echo 2. Background monitoring starts automatically
echo 3. Real-time alerts and logs
echo 4. Interactive menu
echo.

java -cp "target\classes" com.darkeye.ui.ConsoleMainApp

echo.
echo 👋 DarkEye finished.
pause
