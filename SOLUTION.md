# DarkEye JavaFX Runtime Issue - Solution

## Problem
When running DarkEye, you get the error:
```
Error: JavaFX runtime components are missing, and are required to run this application
```

## Root Cause
JavaFX is not included in the standard Java runtime since Java 11. The application needs JavaFX dependencies to run.

## Solutions

### Option 1: Use Maven (Recommended)
1. **Install Maven** from https://maven.apache.org/download.cgi
2. **Add Maven to PATH** environment variable
3. **Build and run**:
   ```bash
   mvn clean package
   java -jar target/darkeye-1.0.0-SNAPSHOT.jar
   ```

### Option 2: Use IDE (Easiest)
1. **Open project in IntelliJ IDEA** or Eclipse
2. **Configure JavaFX SDK** in project settings
3. **Run directly from IDE** - it handles JavaFX automatically

### Option 3: Manual JavaFX Setup
1. **Download JavaFX SDK** from https://openjfx.io/
2. **Extract to a folder** (e.g., `C:\javafx-sdk-21.0.1`)
3. **Run with module path**:
   ```bash
   java --module-path "C:\javafx-sdk-21.0.1\lib" --add-modules javafx.controls,javafx.fxml -jar target/darkeye-1.0.0-SNAPSHOT.jar
   ```

## Quick Test
Run the provided batch files:
- `run-darkeye.bat` - Uses Maven (if available)
- `run-jar.bat` - Runs pre-built JAR

## Project Status
✅ **Core functionality complete**:
- Log collection and parsing
- Threat detection engine
- JavaFX UI with tables and alerts
- Runtime popup notifications
- Sample test data

The application is fully functional once JavaFX dependencies are resolved.
