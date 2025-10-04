# DarkEye - Offline Log Analysis Desktop Application

DarkEye is a cross-platform Java desktop application for offline log analysis, designed for air-gapped environments where network connectivity is limited or prohibited.

## 🚀 Quick Start (Basic Version)

The basic version is now ready with core functionality:

### Features Implemented
- ✅ **JavaFX UI**: Modern desktop interface with log table and alerts
- ✅ **File Collector**: Reads log files from directories
- ✅ **Basic Detection**: Detects suspicious keywords, blacklisted IPs, and brute force attacks
- ✅ **Real-time Alerts**: Popup notifications for high-severity threats
- ✅ **Log Parsing**: Basic parsing of common log formats

### Running the Application

**⚠️ JavaFX Runtime Issue**: If you get "JavaFX runtime components are missing", see [SOLUTION.md](SOLUTION.md)

**Option 1 - Maven (Recommended)**:
1. **Prerequisites**: Java 17+ and Maven
2. **Build**: `mvn clean package`
3. **Run**: `java -jar target/darkeye-1.0.0-SNAPSHOT.jar`

**Option 2 - IDE (Easiest)**:
1. Open project in IntelliJ IDEA or Eclipse
2. Configure JavaFX SDK in project settings
3. Run directly from IDE

**Option 3 - Quick Test**:
- Use `run-darkeye.bat` (requires Maven)
- Use `run-jar.bat` (runs pre-built JAR)

### Testing with Sample Data

1. Run the application
2. Click "Browse" and select the `sample-logs` directory
3. Click "Start" to begin log collection
4. Watch for alerts in the alerts table and popup notifications

## 🔍 Detection Rules

The basic detection engine includes:

- **Blacklisted IPs**: Alerts when traffic comes from known malicious IPs
- **Suspicious Keywords**: Detects words like "attack", "breach", "malware"
- **Brute Force**: Identifies multiple failed login attempts from same IP
- **Unusual Activity**: Flags users with excessive log entries

## 🎯 Planned Features (Full Version)

- **Encrypted Storage**: AES-256-GCM encryption for all sensitive data
- **Advanced Parsing**: Apache, Syslog, Windows Event Logs
- **ML Detection**: Weka-based anomaly detection
- **Secure Authentication**: BCrypt password hashing with roles
- **Export Capabilities**: CSV and PDF report generation
- **Key Management**: PBKDF2-derived encryption keys

## 📁 Project Structure

```
src/main/java/com/darkeye/
├── ui/           # JavaFX interface
├── collectors/   # Log collection (FileCollector)
├── detection/    # Threat detection engine
├── model/        # LogEntry, Alert models
└── util/         # Utilities
```

## 🧪 Testing

Run tests with: `mvn test`

The basic functionality tests verify:
- File collection works
- Detection rules trigger correctly
- Models function properly

## 📝 Sample Logs

The `sample-logs/` directory contains example log files to test with:
- `access.log` - Apache access logs with suspicious activity
- `system.log` - System logs with various severity levels

## License

This project is licensed under the MIT License - see the LICENSE file for details.
