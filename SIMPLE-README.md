# DarkEye - Simple Log Analysis System

## 🚀 Quick Start

**No Maven required!** Just run:

```bash
run-darkeye-final.bat
```

## 📋 Workflow

1. **Authentication**: Enter password `admin123`
2. **Background Monitoring**: Starts automatically monitoring `sample-logs` directory
3. **Real-time Alerts**: Shows popup notifications for threats
4. **Interactive Menu**: Manage monitoring and view statistics

## 🔍 Features

- ✅ **Background log collection** from files/directories
- ✅ **Real-time threat detection** (blacklisted IPs, suspicious keywords, brute force)
- ✅ **Authentication required** to access logs
- ✅ **Interactive console interface**
- ✅ **Sample data included** for immediate testing

## 🎯 Detection Rules

- **Blacklisted IPs**: Alerts on known malicious IPs
- **Suspicious Keywords**: Detects "attack", "breach", "malware", etc.
- **Brute Force**: Flags multiple failed login attempts
- **Unusual Activity**: Identifies excessive user activity

## 📁 Sample Data

The `sample-logs/` directory contains:
- `access.log` - Apache logs with suspicious activity
- `system.log` - System logs with various severity levels

## 🎮 Interactive Commands

1. **View recent activity** - Shows monitoring status
2. **Toggle monitoring** - Start/stop log collection
3. **Add blacklisted IP** - Add IPs to watchlist
4. **View statistics** - Show runtime stats
5. **Exit** - Shutdown system

## 🔧 Requirements

- Java 17+ (you have Java 24 ✅)
- No additional dependencies needed
- Works on Windows, Linux, macOS

## 🚨 Expected Alerts

When you run with sample data, you'll see:
- **Blacklisted IP alerts** (192.168.1.100, 10.0.0.50)
- **Brute force alerts** (6+ failed login attempts)
- **Suspicious keyword alerts** ("breach", "attack", etc.)

Perfect for testing the detection engine!
