package com.darkeye.ui;

import com.darkeye.collectors.FileCollector;
import com.darkeye.detection.SimpleDetectionEngine;
import com.darkeye.model.LogEntry;
import com.darkeye.model.SecurityAlert;

import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Console version of DarkEye for systems without JavaFX
 */
public class ConsoleMainApp {
    
    private static FileCollector fileCollector;
    private static SimpleDetectionEngine detectionEngine;
    private static ScheduledExecutorService executor;
    private static boolean isMonitoring = false;
    private static int alertCount = 0;
    private static int logCount = 0;
    
    // Simple authentication
    private static final String ADMIN_PASSWORD = "admin123";
    
    public static void main(String[] args) {
        System.out.println("🚀 DarkEye - Console Log Analysis System");
        System.out.println("========================================");
        System.out.println();
        
        // Authentication
        if (!authenticate()) {
            System.out.println("❌ Authentication failed. Exiting...");
            return;
        }
        
        System.out.println("✅ Authentication successful!");
        System.out.println();
        
        // Start background monitoring
        startBackgroundMonitoring();
        
        // Show menu
        showMenu();
    }
    
    private static boolean authenticate() {
        Scanner scanner = new Scanner(System.in);
        System.out.print("🔐 Enter password to access DarkEye: ");
        if (scanner.hasNextLine()) {
            String password = scanner.nextLine();
            return ADMIN_PASSWORD.equals(password);
        }
        return false;
    }
    
    private static void startBackgroundMonitoring() {
        System.out.println("🔍 Starting background log monitoring...");
        
        // Initialize components
        fileCollector = new FileCollector();
        detectionEngine = new SimpleDetectionEngine();
        executor = Executors.newScheduledThreadPool(2);
        
        // Set up log handler
        fileCollector.setLogHandler(ConsoleMainApp::handleLogEntry);
        
        // Set up alert handler
        detectionEngine.addAlertHandler(ConsoleMainApp::handleAlert);
        
        // Start monitoring sample logs
        String logPath = "sample-logs";
        System.out.println("📁 Monitoring logs from: " + logPath);
        
        // Start collection in background
        executor.submit(() -> {
            fileCollector.start(logPath);
        });
        
        isMonitoring = true;
        System.out.println("✅ Background monitoring started successfully!");
        System.out.println();
    }
    
    private static void handleLogEntry(LogEntry logEntry) {
        logCount++;
        System.out.println("📝 [" + java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")) + 
                          "] Log #" + logCount + ": " + logEntry.getSeverity() + " - " + logEntry.getMessage());
    }
    
    private static void handleAlert(SecurityAlert alert) {
        alertCount++;
        System.out.println("🚨 [" + java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")) + 
                          "] ALERT #" + alertCount + ": " + alert.getSeverity() + " - " + alert.getTitle());
        System.out.println("   Description: " + alert.getDescription());
        
        if ("HIGH".equals(alert.getSeverity())) {
            System.out.println("   ⚠️  HIGH SEVERITY ALERT - Immediate attention required!");
        }
        System.out.println();
    }
    
    private static void showMenu() {
        Scanner scanner = new Scanner(System.in);
        
        while (true) {
            System.out.println("📊 DarkEye Dashboard");
            System.out.println("===================");
            System.out.println("🟢 Monitoring: " + (isMonitoring ? "ACTIVE" : "STOPPED"));
            System.out.println("📝 Total Logs: " + logCount);
            System.out.println("🚨 Total Alerts: " + alertCount);
            System.out.println();
            System.out.println("Commands:");
            System.out.println("1. View recent activity");
            System.out.println("2. Toggle monitoring");
            System.out.println("3. Add blacklisted IP");
            System.out.println("4. View statistics");
            System.out.println("5. Exit");
            System.out.println();
            System.out.print("Enter command (1-5): ");
            
            String input = scanner.nextLine().trim();
            
            switch (input) {
                case "1":
                    showRecentActivity();
                    break;
                case "2":
                    toggleMonitoring();
                    break;
                case "3":
                    addBlacklistedIP(scanner);
                    break;
                case "4":
                    showStatistics();
                    break;
                case "5":
                    System.out.println("👋 Shutting down DarkEye...");
                    shutdown();
                    return;
                default:
                    System.out.println("❌ Invalid command. Please enter 1-5.");
            }
            
            System.out.println();
            System.out.println("Press Enter to continue...");
            scanner.nextLine();
        }
    }
    
    private static void showRecentActivity() {
        System.out.println("📋 Recent Activity:");
        System.out.println("==================");
        System.out.println("✅ Background monitoring: " + (isMonitoring ? "Running" : "Stopped"));
        System.out.println("📁 Monitoring path: sample-logs");
        System.out.println("🔍 Detection engine: Active");
        System.out.println("🚨 Alert notifications: Enabled");
        System.out.println("📊 Logs processed: " + logCount);
        System.out.println("⚠️  Alerts generated: " + alertCount);
    }
    
    private static void toggleMonitoring() {
        if (isMonitoring) {
            fileCollector.stop();
            isMonitoring = false;
            System.out.println("⏹️  Monitoring stopped.");
        } else {
            startBackgroundMonitoring();
            System.out.println("▶️  Monitoring started.");
        }
    }
    
    private static void addBlacklistedIP(Scanner scanner) {
        System.out.print("Enter IP address to blacklist: ");
        String ip = scanner.nextLine().trim();
        detectionEngine.addBlacklistedIP(ip);
        System.out.println("✅ IP " + ip + " added to blacklist.");
    }
    
    private static void showStatistics() {
        System.out.println("📊 DarkEye Statistics:");
        System.out.println("=====================");
        System.out.println("🕐 Runtime: " + java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        System.out.println("📝 Logs processed: " + logCount);
        System.out.println("🚨 Alerts generated: " + alertCount);
        System.out.println("🔍 Monitoring status: " + (isMonitoring ? "Active" : "Inactive"));
        System.out.println("📁 Source: sample-logs directory");
    }
    
    private static void shutdown() {
        if (executor != null) {
            executor.shutdown();
        }
        if (fileCollector != null) {
            fileCollector.stop();
        }
        System.out.println("✅ DarkEye shutdown complete.");
    }
}
