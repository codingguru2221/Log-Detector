@echo off
echo DarkEye - Enhanced Console Security System
echo ==========================================
echo.

REM Create output directory
if not exist "target\classes" mkdir "target\classes"

echo Compiling enhanced console components...
javac -d "target\classes" "src\main\java\com\darkeye\model\*.java"
javac -d "target\classes" -cp "target\classes" "src\main\java\com\darkeye\collectors\FileCollector.java"
javac -d "target\classes" -cp "target\classes" "src\main\java\com\darkeye\detection\SimpleDetectionEngine.java"
javac -d "target\classes" -cp "target\classes" "src\main\java\com\darkeye\util\SimpleExportService.java"
javac -d "target\classes" -cp "target\classes" "src\main\java\com\darkeye\ui\ConsoleEnhancedApp.java"

echo.
echo ✅ Compilation complete!
echo.
echo 🚀 Starting Enhanced DarkEye Console...
echo.
echo Features:
echo ✅ Real-time log monitoring
echo ✅ Advanced threat detection  
echo ✅ Role-based authentication
echo ✅ Security alert system
echo ✅ Comprehensive dashboard
echo ✅ System activity monitoring
echo ✅ Complete workflow implementation
echo.

java -cp "target\classes" com.darkeye.ui.ConsoleEnhancedApp

echo.
echo 👋 DarkEye finished.
pause
