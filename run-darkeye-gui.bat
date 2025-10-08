@echo off
rem Run DarkEye GUI (tries Maven JavaFX run first, then attempts to run the shaded jar with JavaFX jars from local m2 repo)
@echo off
setlocal enabledelayedexpansion

:: Change working dir to the directory containing this script
cd /d "%~dp0"

echo --------------------------------------------
echo DarkEye - GUI launcher
echo --------------------------------------------

:: If user passes --watch, start a development watch loop that rebuilds and restarts on changes
if "%1"=="--watch" (
  goto DEV_WATCH
)

:: 1) If Maven is available, prefer using it (it sets up module-path correctly)
where mvn >nul 2>&1 && goto RUN_WITH_MAVEN

:: 2) Try to assemble a module-path from JavaFX jars in local Maven repository
set M2=%USERPROFILE%\.m2\repository
set JFX_VER=21.0.1
set JFX_PLATFORM=win
set JFX_BASE=%M2%\org\openjfx\javafx-base\%JFX_VER%\javafx-base-%JFX_VER%-%JFX_PLATFORM%.jar
set JFX_GRAPHICS=%M2%\org\openjfx\javafx-graphics\%JFX_VER%\javafx-graphics-%JFX_VER%-%JFX_PLATFORM%.jar
set JFX_CONTROLS=%M2%\org\openjfx\javafx-controls\%JFX_VER%\javafx-controls-%JFX_VER%-%JFX_PLATFORM%.jar
set JFX_FXML=%M2%\org\openjfx\javafx-fxml\%JFX_VER%\javafx-fxml-%JFX_VER%-%JFX_PLATFORM%.jar

echo Checking for JavaFX jars in %M2% ...
echo DEBUG: M2=%M2%
echo DEBUG: JFX_BASE=%JFX_BASE%
echo DEBUG: JFX_GRAPHICS=%JFX_GRAPHICS%
echo DEBUG: JFX_CONTROLS=%JFX_CONTROLS%
echo DEBUG: JFX_FXML=%JFX_FXML%
if exist "%JFX_BASE%" (
  if exist "%JFX_GRAPHICS%" (
    if exist "%JFX_CONTROLS%" (
      if exist "%JFX_FXML%" (
        echo Found JavaFX jars in local m2; launching EnhancedMainApp with module-path...
        set "MODULE_PATH=%JFX_BASE%;%JFX_GRAPHICS%;%JFX_CONTROLS%;%JFX_FXML%"
        echo module-path: !MODULE_PATH!
        echo Running java --module-path "!MODULE_PATH!" --add-modules javafx.controls,javafx.fxml -cp "target\darkeye-1.0.0-SNAPSHOT-shaded.jar" com.darkeye.ui.EnhancedMainApp
        java --module-path "!MODULE_PATH!" --add-modules javafx.controls,javafx.fxml -cp "target\darkeye-1.0.0-SNAPSHOT-shaded.jar" com.darkeye.ui.EnhancedMainApp
        if ERRORLEVEL 1 (
          echo Java exited with code %ERRORLEVEL%.
        )
        pause
        goto :EOF
      )
    )
  )
)

:RUN_WITH_MAVEN
echo Found Maven on PATH - launching via javafx:run (this will compile/run the JavaFX app)...
mvn -DskipTests javafx:run
if ERRORLEVEL 1 (
  echo Maven run failed with exit code %ERRORLEVEL%. Will try other launch methods (local JavaFX jars or shaded jar)...
  rem fall through to other checks/fallbacks
) else (
  echo Maven run finished. Press any key to close this window...
  pause
  goto :EOF
)

:: 3) If we can't find JavaFX jars and Maven isn't available, run the console fallback jar if present
echo Could not find Maven or JavaFX runtime jars automatically.
echo Attempting to run the shaded jar (console fallback) instead...
if exist "target\darkeye-1.0.0-SNAPSHOT-shaded.jar" (
  java -jar target\darkeye-1.0.0-SNAPSHOT-shaded.jar
  if ERRORLEVEL 1 (
    echo The application exited with error code %ERRORLEVEL%.
    pause
  ) else (
    echo Application finished normally.
    pause
  )
  goto :EOF
)

echo ERROR: Unable to start the application.
echo - Install Maven and try again OR
echo - Make sure JavaFX jars are present under %USERPROFILE%\.m2\repository\org\openjfx (version %JFX_VER% with classifier %JFX_PLATFORM%) OR
echo - Build the project with Maven (mvn -DskipTests package) to create the shaded jar.
pause

:EOF
endlocal
exit /b

:DEV_WATCH
echo Development watch mode: building and restarting GUI on source changes.
echo Requires PowerShell (available on Windows). Press Ctrl-C to stop.

rem initial build/run
mvn -DskipTests package
if exist "target\darkeye-1.0.0-SNAPSHOT-shaded.jar" (
  start "DarkEye GUI" cmd /c "java -jar target\darkeye-1.0.0-SNAPSHOT-shaded.jar"
) else (
  echo Build failed or shaded jar not found; attempting mvn javafx:run
  start "DarkEye GUI (mvn)" cmd /c "mvn -DskipTests javafx:run"
)

rem Use PowerShell FileSystemWatcher to monitor src and resources
powershell -NoLogo -NoProfile -Command "
$filter = '*.java','*.fxml','*.xml','*.properties';
$paths = @('src\main\java','src\main\resources');
$watcher = New-Object System.IO.FileSystemWatcher;
$watcher.Path = $paths[0];
$watcher.IncludeSubdirectories = $true;
$watcher.Filter = '*.*';
$action = { Write-Host 'Change detected: rebuilding...' ; Start-Sleep -Seconds 1 ; cmd /c 'mvn -DskipTests package' | Out-Null ; Stop-Process -Name java -ErrorAction SilentlyContinue ; Start-Sleep -Milliseconds 500 ; if (Test-Path 'target\darkeye-1.0.0-SNAPSHOT-shaded.jar') { Start-Process -FilePath 'java' -ArgumentList '-jar','target\darkeye-1.0.0-SNAPSHOT-shaded.jar' } }
[System.IO.FileSystemWatcher]::new($paths[0], '*.*') | ForEach-Object { $_.IncludeSubdirectories = $true; Register-ObjectEvent -InputObject $_ -EventName Changed -Action $action | Out-Null }
Write-Host 'Watching for changes. Press Ctrl-C to exit.'; while ($true) { Start-Sleep -Seconds 1 }
"

goto :EOF
