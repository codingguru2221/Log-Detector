package com.darkeye.ui;

import com.darkeye.collectors.FileCollector;
import com.darkeye.detection.SimpleDetectionEngine;
import com.darkeye.model.LogEntry;
import com.darkeye.model.SecurityAlert;
import com.darkeye.util.SimpleExportService;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Console-based Enhanced DarkEye application implementing the complete workflow
 * - System Initialization & Log Collection
 * - Threat Detection & Alert Response  
 * - Dashboard & Threat Management
 */
public class ConsoleEnhancedApp {
    
    // Core components
    private FileCollector fileCollector;
    private SimpleDetectionEngine detectionEngine;
    private SimpleExportService exportService;
    private ScheduledExecutorService executor;
    
    // Authentication
    private static final String ADMIN_PASSWORD = "Codex";
    private static final String ANALYST_PASSWORD = "Codex";
    private static final String VIEWER_PASSWORD = "Codex";
    private String currentUserRole = "VIEWER";
    private boolean isAuthenticated = false;
    
    // Monitoring state
    private boolean isMonitoring = false;
    private int alertCount = 0;
    private int logCount = 0;
    private List<String> recentLogs = new ArrayList<>();
    private List<String> recentAlerts = new ArrayList<>();
    
    // Scanner for user input
    private Scanner scanner = new Scanner(System.in);
    
    public static void main(String[] args) {
        System.out.println("🚀 DarkEye Enhanced Security System");
        System.out.println("====================================");
        System.out.println("Features:");
        System.out.println("✅ Real-time log monitoring");
        System.out.println("✅ Advanced threat detection");
        System.out.println("✅ Role-based authentication");
        System.out.println("✅ Security alert system");
        System.out.println("✅ Comprehensive dashboard");
        System.out.println("✅ System activity monitoring");
        System.out.println("====================================");
        System.out.println();
        
        ConsoleEnhancedApp app = new ConsoleEnhancedApp();
        app.run();
    }
    
    /**
     * Main application loop
     */
    public void run() {
        // Authentication
        if (!authenticate()) {
            System.out.println("❌ Authentication failed. Exiting...");
            return;
        }
        
        System.out.println("✅ Authentication successful! Welcome, " + currentUserRole);
        System.out.println();
        
        // Initialize services
        initializeServices();
        
        // Start background monitoring
        startBackgroundMonitoring();
        
        // Show main menu
        showMainMenu();
    }
    
    /**
     * Authenticate user
     */
    private boolean authenticate() {
        System.out.println("🔐 DarkEye Authentication");
        System.out.println("=========================");
        System.out.println("Available roles:");
        System.out.println("1. Admin (Full system access)");
        System.out.println("2. Analyst (View and analyze logs)");
        System.out.println("3. Viewer (Read-only access)");
        System.out.println();
        
        System.out.print("Select role (1-3): ");
        String roleChoice = scanner.nextLine();
        
        System.out.print("Enter password: ");
        String password = scanner.nextLine();
        
        switch (roleChoice) {
            case "1":
                if (ADMIN_PASSWORD.equals(password)) {
                    currentUserRole = "ADMIN";
                    return true;
                }
                break;
            case "2":
                if (ANALYST_PASSWORD.equals(password)) {
                    currentUserRole = "ANALYST";
                    return true;
                }
                break;
            case "3":
                if (VIEWER_PASSWORD.equals(password)) {
                    currentUserRole = "VIEWER";
                    return true;
                }
                break;
        }
        
        return false;
    }
    
    /**
     * Initialize core services
     */
    private void initializeServices() {
        System.out.println("🔧 Initializing DarkEye services...");
        
        // Initialize file collector
        fileCollector = new FileCollector();
        fileCollector.setLogHandler(this::handleLogEntry);
        
        // Initialize detection engine
        detectionEngine = new SimpleDetectionEngine();
        detectionEngine.addAlertHandler(this::handleAlert);
        
        // Initialize export service
        exportService = new SimpleExportService();
        
        // Initialize executor
        executor = Executors.newScheduledThreadPool(3);
        
        System.out.println("✅ Services initialized successfully!");
        System.out.println();
    }
    
    /**
     * Start background monitoring
     */
    private void startBackgroundMonitoring() {
        System.out.println("🔍 Starting background monitoring...");
        
        // Start monitoring sample logs
        executor.submit(() -> {
            fileCollector.start("sample-logs");
        });
        
        // Schedule periodic system monitoring
        executor.scheduleAtFixedRate(this::performSystemMonitoring, 0, 10, TimeUnit.SECONDS);
        
        isMonitoring = true;
        System.out.println("✅ Background monitoring started!");
        System.out.println();
    }
    
    /**
     * Perform system monitoring tasks
     */
    private void performSystemMonitoring() {
        try {
            // Simulate system monitoring
            LogEntry systemLog = new LogEntry();
            systemLog.setId(java.util.UUID.randomUUID().toString());
            systemLog.setTimestamp(LocalDateTime.now());
            systemLog.setSource("SYSTEM_MONITOR");
            systemLog.setHost(System.getProperty("user.name"));
            systemLog.setSeverity("INFO");
            systemLog.setEventType("SYSTEM_CHECK");
            systemLog.setMessage("System monitoring check - " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
            
            handleLogEntry(systemLog);
            
            // Simulate network monitoring
            if (Math.random() < 0.1) { // 10% chance
                LogEntry networkLog = new LogEntry();
                networkLog.setId(java.util.UUID.randomUUID().toString());
                networkLog.setTimestamp(LocalDateTime.now());
                networkLog.setSource("NETWORK_MONITOR");
                networkLog.setHost(System.getProperty("user.name"));
                networkLog.setSeverity("INFO");
                networkLog.setEventType("NETWORK_CHECK");
                networkLog.setMessage("Network connection check - Active connections: " + (int)(Math.random() * 10));
                
                handleLogEntry(networkLog);
            }
            
        } catch (Exception e) {
            System.err.println("Error in system monitoring: " + e.getMessage());
        }
    }
    
    /**
     * Show main menu
     */
    private void showMainMenu() {
        while (true) {
            System.out.println("📊 DarkEye Dashboard - " + currentUserRole);
            System.out.println("=====================================");
            System.out.println("Status: " + (isMonitoring ? "🟢 Monitoring Active" : "🔴 Monitoring Stopped"));
            System.out.println("Logs Processed: " + logCount);
            System.out.println("Alerts Generated: " + alertCount);
            System.out.println();
            
            System.out.println("Menu Options:");
            System.out.println("1. View Recent Logs");
            System.out.println("2. View Security Alerts");
            System.out.println("3. Start Active Monitoring");
            System.out.println("4. Stop Active Monitoring");
            System.out.println("5. Browse Log Files");
            
            if (!"VIEWER".equals(currentUserRole)) {
                System.out.println("6. Add IP to Blacklist");
                System.out.println("7. View System Statistics");
            }
            
            System.out.println("8. Export Data");
            System.out.println("9. Launch GUI ");
            System.out.println("0. Exit");
            System.out.println();
            
            System.out.print("Select option: ");
            String choice = scanner.nextLine();
            
            // Allow launching a JavaFX UI from the console menu
            if ("9".equals(choice)) {
                launchGUI();
                continue;
            }
            
            switch (choice) {
                case "1":
                    viewRecentLogs();
                    break;
                case "2":
                    viewSecurityAlerts();
                    break;
                case "3":
                    startActiveMonitoring();
                    break;
                case "4":
                    stopActiveMonitoring();
                    break;
                case "5":
                    browseLogFiles();
                    break;
                case "6":
                    if (!"VIEWER".equals(currentUserRole)) {
                        addIPToBlacklist();
                    } else {
                        System.out.println("❌ Access denied. Viewer role cannot modify blacklist.");
                    }
                    break;
                case "7":
                    if (!"VIEWER".equals(currentUserRole)) {
                        viewSystemStatistics();
                    } else {
                        System.out.println("❌ Access denied. Viewer role cannot view statistics.");
                    }
                    break;
                case "8":
                    exportData();
                    break;
                case "0":
                    System.out.println("👋 Shutting down DarkEye...");
                    shutdown();
                    return;
                default:
                    System.out.println("❌ Invalid option. Please try again.");
            }
            
            System.out.println();
            System.out.println("Press Enter to continue...");
            scanner.nextLine();
        }
    }

    /**
     * Launch the JavaFX GUI (SimpleMainApp) in a background thread so the console remains usable.
     */
    private void launchGUI() {
        System.out.println("Launching JavaFX GUI...");
        Thread fxThread = new Thread(() -> {
            try {
                boolean jfxAvailable = true;
                try {
                    Class<?> jfxPanelCls = Class.forName("javafx.embed.swing.JFXPanel");
                    jfxPanelCls.getConstructor().newInstance();
                } catch (ClassNotFoundException cnf) {
                    jfxAvailable = false;
                } catch (Throwable t) {
                    System.err.println("Warning: JavaFX toolkit init via JFXPanel failed: " + t.getMessage());
                }

                if (jfxAvailable) {
                    System.out.println("JavaFX detected -> launching full JavaFX EnhancedMainApp UI...");
                    try {
                        Class<?> appClass = Class.forName("com.darkeye.ui.EnhancedMainApp");
                        java.lang.reflect.Method mainMethod = appClass.getMethod("main", String[].class);
                        String[] args = new String[0];
                        mainMethod.invoke(null, (Object) args);
                        return;
                    } catch (ClassNotFoundException cnf) {
                        System.err.println("EnhancedMainApp class not found: " + cnf.getMessage());
                    } catch (Throwable e) {
                        System.err.println("Failed to invoke EnhancedMainApp.main: " + e.getMessage());
                        e.printStackTrace();
                    }
                } else {
                    try {
                        // Use the richer Swing dashboard fallback
                        com.darkeye.ui.SwingDashboardApp.main(new String[0]);
                    } catch (Throwable t) {
                        System.err.println("Failed to launch Swing dashboard UI: " + t.getMessage());
                    }
                }

            } catch (Throwable e) {
                System.err.println("Failed to launch GUI: " + e.getMessage());
                e.printStackTrace();
            }
        }, "DarkEye-JavaFX-Launcher");
        fxThread.setDaemon(true);
        fxThread.start();
    }
    
    /**
     * View recent logs
     */
    private void viewRecentLogs() {
        System.out.println("📝 Recent Log Entries");
        System.out.println("=====================");
        
        if (recentLogs.isEmpty()) {
            System.out.println("No logs available yet.");
            return;
        }
        
        for (int i = Math.max(0, recentLogs.size() - 20); i < recentLogs.size(); i++) {
            System.out.println(recentLogs.get(i));
        }
    }
    
    /**
     * View security alerts
     */
    private void viewSecurityAlerts() {
        System.out.println("🚨 Security Alerts");
        System.out.println("==================");
        
        if (recentAlerts.isEmpty()) {
            System.out.println("No alerts generated yet.");
            return;
        }
        
        for (int i = Math.max(0, recentAlerts.size() - 10); i < recentAlerts.size(); i++) {
            System.out.println(recentAlerts.get(i));
        }
    }
    
    /**
     * Start active monitoring
     */
    private void startActiveMonitoring() {
        if (isMonitoring) {
            System.out.println("⚠️ Monitoring is already active.");
            return;
        }
        
        System.out.println("🟡 Starting active monitoring...");
        executor.submit(() -> {
            fileCollector.start("sample-logs");
        });
        
        isMonitoring = true;
        System.out.println("✅ Active monitoring started!");
    }
    
    /**
     * Stop active monitoring
     */
    private void stopActiveMonitoring() {
        if (!isMonitoring) {
            System.out.println("⚠️ Monitoring is not active.");
            return;
        }
        
        System.out.println("🔴 Stopping active monitoring...");
        fileCollector.stop();
        isMonitoring = false;
        System.out.println("✅ Active monitoring stopped!");
    }
    
    /**
     * Browse log files
     */
    private void browseLogFiles() {
        System.out.println("📁 Browse Log Files");
        System.out.println("===================");
        System.out.println("Enter path to log file or directory:");
        System.out.print("Path: ");
        String path = scanner.nextLine();
        
        File file = new File(path);
        if (!file.exists()) {
            System.out.println("❌ Path does not exist: " + path);
            return;
        }
        
        System.out.println("🔄 Collecting logs from: " + path);
        executor.submit(() -> {
            fileCollector.start(path);
        });
        
        System.out.println("✅ Log collection started!");
    }
    
    /**
     * Add IP to blacklist
     */
    private void addIPToBlacklist() {
        System.out.println("🚫 Add IP to Blacklist");
        System.out.println("======================");
        System.out.print("Enter IP address to blacklist: ");
        String ip = scanner.nextLine();
        
        detectionEngine.addBlacklistedIP(ip);
        System.out.println("✅ IP " + ip + " has been added to the blacklist!");
    }
    
    /**
     * View system statistics
     */
    private void viewSystemStatistics() {
        System.out.println("📊 System Statistics");
        System.out.println("====================");
        System.out.println("Total Logs Processed: " + logCount);
        System.out.println("Total Alerts Generated: " + alertCount);
        System.out.println("Current User Role: " + currentUserRole);
        System.out.println("Monitoring Status: " + (isMonitoring ? "Active" : "Inactive"));
        System.out.println("System Time: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        System.out.println("Java Version: " + System.getProperty("java.version"));
        System.out.println("OS: " + System.getProperty("os.name") + " " + System.getProperty("os.version"));
        System.out.println("User: " + System.getProperty("user.name"));
    }
    
    /**
     * Export data
     */
    private void exportData() {
        System.out.println("📤 Export Data");
        System.out.println("==============");
        System.out.println("Export options:");
        System.out.println("1. Export Recent Logs (CSV)");
        System.out.println("2. Export Security Alerts (CSV)");
        System.out.println("3. Export Recent Logs (Text)");
        System.out.println("4. Export Security Alerts (Text)");
        System.out.println("5. Export Summary Report");
        System.out.print("Select option: ");
        
        String choice = scanner.nextLine();
        
        try {
            switch (choice) {
                case "1":
                    System.out.print("Enter filename (e.g., logs.csv): ");
                    String csvLogFile = scanner.nextLine();
                    exportService.exportLogsToCSV(convertLogsToEntries(), csvLogFile);
                    break;
                case "2":
                    System.out.print("Enter filename (e.g., alerts.csv): ");
                    String csvAlertFile = scanner.nextLine();
                    exportService.exportAlertsToCSV(convertAlertsToEntries(), csvAlertFile);
                    break;
                case "3":
                    System.out.print("Enter filename (e.g., logs.txt): ");
                    String txtLogFile = scanner.nextLine();
                    exportService.exportLogsToText(convertLogsToEntries(), txtLogFile, "DarkEye Log Export");
                    break;
                case "4":
                    System.out.print("Enter filename (e.g., alerts.txt): ");
                    String txtAlertFile = scanner.nextLine();
                    exportService.exportAlertsToText(convertAlertsToEntries(), txtAlertFile, "DarkEye Security Alerts");
                    break;
                case "5":
                    System.out.print("Enter filename (e.g., summary.txt): ");
                    String summaryFile = scanner.nextLine();
                    java.util.Map<String, Object> stats = new java.util.HashMap<>();
                    stats.put("Total Logs", logCount);
                    stats.put("Total Alerts", alertCount);
                    stats.put("User Role", currentUserRole);
                    stats.put("Monitoring Status", isMonitoring ? "Active" : "Inactive");
                    exportService.exportSummaryReport(convertLogsToEntries(), convertAlertsToEntries(), stats, summaryFile);
                    break;
                default:
                    System.out.println("❌ Invalid option.");
            }
        } catch (Exception e) {
            System.out.println("❌ Export failed: " + e.getMessage());
        }
    }
    
    /**
     * Convert recent logs to LogEntry objects (simplified)
     */
    private java.util.List<LogEntry> convertLogsToEntries() {
        java.util.List<LogEntry> entries = new java.util.ArrayList<>();
        // For demonstration, create sample entries based on recent logs
        for (int i = 0; i < Math.min(10, recentLogs.size()); i++) {
            LogEntry entry = new LogEntry();
            entry.setId("log-" + i);
            entry.setTimestamp(LocalDateTime.now().minusMinutes(i));
            entry.setSource("SYSTEM");
            entry.setHost("localhost");
            entry.setSeverity("INFO");
            entry.setMessage("Sample log entry " + i);
            entries.add(entry);
        }
        return entries;
    }
    
    /**
     * Convert recent alerts to SecurityAlert objects (simplified)
     */
    private java.util.List<SecurityAlert> convertAlertsToEntries() {
        java.util.List<SecurityAlert> alerts = new java.util.ArrayList<>();
        // For demonstration, create sample alerts based on recent alerts
        for (int i = 0; i < Math.min(10, recentAlerts.size()); i++) {
            SecurityAlert alert = new SecurityAlert();
            alert.setId("alert-" + i);
            alert.setTimestamp(LocalDateTime.now().minusMinutes(i));
            alert.setSeverity("MEDIUM");
            alert.setTitle("Sample alert " + i);
            alert.setDescription("Sample alert description " + i);
            alert.setRuleName("SAMPLE_RULE");
            alerts.add(alert);
        }
        return alerts;
    }
    
    /**
     * Handle log entry processing
     */
    private void handleLogEntry(LogEntry logEntry) {
        if (logEntry == null) {
            return;
        }
        
        // Process through detection engine
        detectionEngine.processLogEntry(logEntry);
        
        // Update counters
        logCount++;
        
        // Add to recent logs
        String logText = String.format("[%s] %s %s: %s", 
            logEntry.getTimestamp().format(DateTimeFormatter.ofPattern("HH:mm:ss")),
            getSeverityIcon(logEntry.getSeverity()),
            logEntry.getSeverity(),
            logEntry.getMessage());
        
        recentLogs.add(logText);
        
        // Keep only last 100 logs
        if (recentLogs.size() > 100) {
            recentLogs.remove(0);
        }
        
        // Print to console for real-time monitoring
        System.out.println("📝 " + logText);
    }
    
    /**
     * Handle security alert
     */
    private void handleAlert(SecurityAlert alert) {
        if (alert == null) {
            return;
        }
        
        // Update counters
        alertCount++;
        
        // Add to recent alerts
        String alertText = String.format("[%s] %s %s: %s", 
            alert.getTimestamp().format(DateTimeFormatter.ofPattern("HH:mm:ss")),
            getSeverityIcon(alert.getSeverity()),
            alert.getSeverity(),
            alert.getTitle());
        
        recentAlerts.add(alertText);
        
        // Keep only last 50 alerts
        if (recentAlerts.size() > 50) {
            recentAlerts.remove(0);
        }
        
        // Print to console for real-time alerts
        System.out.println("🚨 ALERT: " + alertText);
        
        // Show popup for high severity alerts
        if ("HIGH".equals(alert.getSeverity())) {
            System.out.println("⚠️ HIGH SEVERITY ALERT: " + alert.getDescription());
        }
    }
    
    /**
     * Get severity icon
     */
    private String getSeverityIcon(String severity) {
        switch (severity) {
            case "HIGH": return "🔴";
            case "MEDIUM": return "🟡";
            case "LOW": return "🟢";
            case "ERROR": return "❌";
            case "WARN": return "⚠️";
            case "INFO": return "ℹ️";
            default: return "📝";
        }
    }
    
    /**
     * Shutdown application
     */
    private void shutdown() {
        System.out.println("🛑 Shutting down DarkEye services...");
        
        // Stop monitoring
        if (isMonitoring) {
            fileCollector.stop();
        }
        
        // Shutdown executor
        if (executor != null) {
            executor.shutdown();
        }
        
        // Close scanner
        if (scanner != null) {
            scanner.close();
        }
        
        System.out.println("✅ DarkEye shutdown complete!");
    }
}
