package com.darkeye.ui;

import com.darkeye.collectors.FileCollector;
import com.darkeye.collectors.NetworkMonitorCollector;
import com.darkeye.collectors.SystemMonitorCollector;
import com.darkeye.detection.AdvancedDetectionEngine;
import com.darkeye.model.LogEntry;
import com.darkeye.model.SecurityAlert;
import com.darkeye.parsers.LogParserFactory;
import com.darkeye.security.AuthenticationService;
import com.darkeye.security.EncryptionService;
import com.darkeye.storage.DatabaseService;
import com.darkeye.util.ExportService;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.time.LocalDateTime;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Comprehensive DarkEye application implementing the complete workflow
 * - System Initialization & Log Collection
 * - Threat Detection & Alert Response  
 * - Dashboard & Threat Management
 */
public class ComprehensiveMainApp extends Application {
    
    private static final Logger logger = LoggerFactory.getLogger(ComprehensiveMainApp.class);
    
    // Core components
    private AuthenticationService authService;
    private DatabaseService databaseService;
    private AdvancedDetectionEngine detectionEngine;
    private ExportService exportService;
    
    // Collectors
    private FileCollector fileCollector;
    private SystemMonitorCollector systemMonitor;
    private NetworkMonitorCollector networkMonitor;
    
    // UI Components
    private Stage primaryStage;
    private Stage alertStage;
    private String currentUserToken;
    
    // Monitoring
    private ScheduledExecutorService executor;
    private boolean isMonitoring = false;
    private int alertCount = 0;
    private int logCount = 0;
    // Suppress popups for private/local IPs by default (toggleable in Tools menu)
    private volatile boolean showPopupsForLocalIps = false;
    // Rate-limit duplicate popups: track last popup time per (rule|sourceIp)
    private final ConcurrentMap<String, LocalDateTime> lastPopupTimes = new ConcurrentHashMap<>();
    private static final Duration POPUP_MIN_INTERVAL = Duration.ofMinutes(1);
    
    // UI Elements
    private Label statusLabel;
    private Label alertCountLabel;
    private Label logCountLabel;
    private TextArea logArea;
    private ListView<String> alertList;
    private ProgressBar monitoringProgress;
    
    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        
        // Initialize core services
        initializeServices();
        
        // Show authentication dialog first
        showAuthenticationDialog();
    }
    
    /**
     * Initialize all core services
     */
    private void initializeServices() {
        try {
            logger.info("Initializing DarkEye services...");
            
            // Initialize authentication service
            authService = new AuthenticationService();
            
            // Initialize encryption service
            EncryptionService encryptionService = new EncryptionService();
            
            // Initialize database service
            databaseService = new DatabaseService(encryptionService.getKeyAsBase64());
            
            // Initialize detection engine
            detectionEngine = new AdvancedDetectionEngine();
            detectionEngine.addAlertHandler(this::handleSecurityAlert);
            
            // Initialize export service
            exportService = new ExportService();
            
            // Initialize collectors
            fileCollector = new FileCollector();
            fileCollector.setLogHandler(this::handleLogEntry);
            
            systemMonitor = new SystemMonitorCollector();
            systemMonitor.setLogHandler(this::handleLogEntry);
            
            networkMonitor = new NetworkMonitorCollector();
            networkMonitor.setLogHandler(this::handleLogEntry);
            
            // Initialize executor for background tasks
            executor = Executors.newScheduledThreadPool(4);
            
            logger.info("All services initialized successfully");
            
        } catch (Exception e) {
            logger.error("Failed to initialize services", e);
            showErrorDialog("Initialization Error", "Failed to initialize DarkEye services: " + e.getMessage());
        }
    }
    
    /**
     * Show authentication dialog
     */
    private void showAuthenticationDialog() {
        Stage authStage = new Stage();
        authStage.setTitle("DarkEye Authentication");
        authStage.setResizable(false);
        
        VBox authLayout = new VBox(15);
        authLayout.setPadding(new Insets(20));
        authLayout.setAlignment(Pos.CENTER);
        
        // Title
        Label titleLabel = new Label("DarkEye Security System");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        titleLabel.setTextFill(Color.DARKBLUE);
        
        // Username field
        Label userLabel = new Label("Username:");
        TextField usernameField = new TextField();
        usernameField.setPromptText("Enter username");
        usernameField.setPrefWidth(200);
        
        // Password field
        Label passLabel = new Label("Password:");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Enter password");
        passwordField.setPrefWidth(200);
        
        // Login button
        Button loginButton = new Button("Login");
        loginButton.setPrefWidth(100);
        loginButton.setOnAction(e -> {
            String username = usernameField.getText();
            String password = passwordField.getText();
            
            String token = authService.authenticate(username, password);
            if (token != null) {
                currentUserToken = token;
                authStage.close();
                showMainDashboard();
            } else {
                showErrorDialog("Authentication Failed", "Invalid username or password");
            }
        });
        
    // Default credentials info
    Label infoLabel = new Label("Default: admin/Codex, analyst/analyst123, viewer/viewer123");
        infoLabel.setFont(Font.font("Arial", 10));
        infoLabel.setTextFill(Color.GRAY);
        
        authLayout.getChildren().addAll(titleLabel, userLabel, usernameField, 
                                      passLabel, passwordField, loginButton, infoLabel);
        
        Scene authScene = new Scene(authLayout, 300, 250);
        authStage.setScene(authScene);
        authStage.showAndWait();
    }
    
    /**
     * Show main dashboard
     */
    private void showMainDashboard() {
        primaryStage.setTitle("DarkEye - Comprehensive Security Dashboard");
        primaryStage.setMinWidth(1200);
        primaryStage.setMinHeight(800);
        
        // Create main layout
        BorderPane mainLayout = new BorderPane();
        
        // Top menu bar
        mainLayout.setTop(createMenuBar());
        
        // Center content
        mainLayout.setCenter(createMainContent());
        
        // Bottom status bar
        mainLayout.setBottom(createStatusBar());
        
        Scene scene = new Scene(mainLayout, 1200, 800);
        primaryStage.setScene(scene);
        primaryStage.show();
        
        // Start background monitoring
        startBackgroundMonitoring();
    }
    
    /**
     * Create menu bar
     */
    private MenuBar createMenuBar() {
        MenuBar menuBar = new MenuBar();
        
        // File menu
        Menu fileMenu = new Menu("File");
        MenuItem exportLogsItem = new MenuItem("Export Logs...");
        exportLogsItem.setOnAction(e -> exportLogs());
        
        MenuItem exportAlertsItem = new MenuItem("Export Alerts...");
        exportAlertsItem.setOnAction(e -> exportAlerts());
        
        MenuItem exitItem = new MenuItem("Exit");
        exitItem.setOnAction(e -> Platform.exit());
        
        fileMenu.getItems().addAll(exportLogsItem, exportAlertsItem, new SeparatorMenuItem(), exitItem);
        
        // Monitoring menu
        Menu monitoringMenu = new Menu("Monitoring");
        MenuItem startMonitoringItem = new MenuItem("Start Monitoring");
        startMonitoringItem.setOnAction(e -> startMonitoring());
        
        MenuItem stopMonitoringItem = new MenuItem("Stop Monitoring");
        stopMonitoringItem.setOnAction(e -> stopMonitoring());
        
        monitoringMenu.getItems().addAll(startMonitoringItem, stopMonitoringItem);
        
        // Tools menu
        Menu toolsMenu = new Menu("Tools");
        MenuItem addBlacklistItem = new MenuItem("Add IP to Blacklist");
        addBlacklistItem.setOnAction(e -> addIPToBlacklist());
        
        MenuItem viewStatsItem = new MenuItem("View Statistics");
        viewStatsItem.setOnAction(e -> showStatistics());

        // Toggle: Show popups for local/private IPs
        CheckMenuItem toggleLocalPopupItem = new CheckMenuItem("Show popups for local/private IPs");
        toggleLocalPopupItem.setSelected(showPopupsForLocalIps);
        toggleLocalPopupItem.setOnAction(e -> {
            showPopupsForLocalIps = toggleLocalPopupItem.isSelected();
            updateStatus("Show popups for local IPs: " + showPopupsForLocalIps);
        });
        
        toolsMenu.getItems().addAll(addBlacklistItem, viewStatsItem, new SeparatorMenuItem(), toggleLocalPopupItem);
        
        menuBar.getMenus().addAll(fileMenu, monitoringMenu, toolsMenu);
        return menuBar;
    }
    
    /**
     * Create main content area
     */
    private VBox createMainContent() {
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        
        // Control panel
        HBox controlPanel = new HBox(10);
        controlPanel.setAlignment(Pos.CENTER_LEFT);
        
        Button startButton = new Button("Start Monitoring");
        startButton.setOnAction(e -> startMonitoring());
        
        Button stopButton = new Button("Stop Monitoring");
        stopButton.setOnAction(e -> stopMonitoring());
        
        Button browseButton = new Button("Browse Log Files");
        browseButton.setOnAction(e -> browseLogFiles());
        
        controlPanel.getChildren().addAll(startButton, stopButton, browseButton);
        
        // Monitoring progress
        monitoringProgress = new ProgressBar();
        monitoringProgress.setPrefWidth(200);
        monitoringProgress.setProgress(0);
        
        HBox progressBox = new HBox(10);
        progressBox.setAlignment(Pos.CENTER_LEFT);
        progressBox.getChildren().addAll(new Label("Monitoring Status:"), monitoringProgress);
        
        // Main content area
        HBox mainArea = new HBox(10);
        
        // Log display area
        VBox logAreaBox = new VBox(5);
        logAreaBox.setPrefWidth(600);
        
        Label logLabel = new Label("Recent Log Entries:");
        logLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        
        logArea = new TextArea();
        logArea.setPrefRowCount(20);
        logArea.setEditable(false);
        logArea.setWrapText(true);
        
        logAreaBox.getChildren().addAll(logLabel, logArea);
        
        // Alert display area
        VBox alertAreaBox = new VBox(5);
        alertAreaBox.setPrefWidth(400);
        
        Label alertLabel = new Label("Security Alerts:");
        alertLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        
        alertList = new ListView<>();
        alertList.setPrefHeight(400);
        
        alertAreaBox.getChildren().addAll(alertLabel, alertList);
        
        mainArea.getChildren().addAll(logAreaBox, alertAreaBox);
        
        content.getChildren().addAll(controlPanel, progressBox, mainArea);
        
        return content;
    }
    
    /**
     * Create status bar
     */
    private HBox createStatusBar() {
        HBox statusBar = new HBox(20);
        statusBar.setPadding(new Insets(5));
        statusBar.setStyle("-fx-background-color: #f0f0f0;");
        
        statusLabel = new Label("Ready");
        alertCountLabel = new Label("Alerts: 0");
        logCountLabel = new Label("Logs: 0");
        
        statusBar.getChildren().addAll(statusLabel, alertCountLabel, logCountLabel);
        
        return statusBar;
    }
    
    /**
     * Start background monitoring
     */
    private void startBackgroundMonitoring() {
        logger.info("Starting background monitoring...");
        
        // Start system monitoring
        systemMonitor.start();
        
        // Start network monitoring
        networkMonitor.start();
        
        // Schedule periodic statistics update
        executor.scheduleAtFixedRate(this::updateStatistics, 0, 30, TimeUnit.SECONDS);
        
        updateStatus("Background monitoring started");
    }
    
    /**
     * Start active monitoring
     */
    private void startMonitoring() {
        if (isMonitoring) {
            return;
        }
        
        isMonitoring = true;
        monitoringProgress.setProgress(-1); // Indeterminate progress
        
        // Start file collection from sample logs
        executor.submit(() -> {
            fileCollector.start("sample-logs");
        });
        
        updateStatus("Active monitoring started");
        logger.info("Active monitoring started");
    }
    
    /**
     * Stop monitoring
     */
    private void stopMonitoring() {
        isMonitoring = false;
        monitoringProgress.setProgress(0);
        
        fileCollector.stop();
        
        updateStatus("Monitoring stopped");
        logger.info("Monitoring stopped");
    }
    
    /**
     * Browse and select log files
     */
    private void browseLogFiles() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Log Files or Directory");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Log Files", "*.log", "*.txt"),
            new FileChooser.ExtensionFilter("All Files", "*.*")
        );
        
        File selectedFile = fileChooser.showOpenDialog(primaryStage);
        if (selectedFile != null) {
            executor.submit(() -> {
                fileCollector.start(selectedFile.getAbsolutePath());
            });
            updateStatus("Collecting from: " + selectedFile.getName());
        }
    }
    
    /**
     * Handle log entry processing
     */
    private void handleLogEntry(LogEntry logEntry) {
        if (logEntry == null) {
            return;
        }
        
        // Store in database
        databaseService.storeLogEntry(logEntry);
        
        // Process through detection engine
        detectionEngine.processLogEntry(logEntry);
        
        // Update UI
        Platform.runLater(() -> {
            logCount++;
            logCountLabel.setText("Logs: " + logCount);
            
            // Add to log display (keep last 100 entries)
            String logText = logArea.getText();
            String newEntry = String.format("[%s] %s: %s\n", 
                logEntry.getTimestamp().format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                logEntry.getSeverity(),
                logEntry.getMessage());
            
            if (logText.split("\n").length > 100) {
                String[] lines = logText.split("\n");
                StringBuilder sb = new StringBuilder();
                for (int i = 1; i < lines.length; i++) {
                    sb.append(lines[i]).append("\n");
                }
                logText = sb.toString();
            }
            
            logArea.setText(newEntry + logText);
        });
    }
    
    /**
     * Handle security alert
     */
    private void handleSecurityAlert(SecurityAlert alert) {
        if (alert == null) {
            return;
        }
        
        // Store in database
        databaseService.storeSecurityAlert(alert);
        
        // Update UI
        Platform.runLater(() -> {
            alertCount++;
            alertCountLabel.setText("Alerts: " + alertCount);
            
            // Add to alert list
            String alertText = String.format("[%s] %s: %s", 
                alert.getTimestamp().format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                alert.getSeverity(),
                alert.getTitle());
            alertList.getItems().add(0, alertText);
            
            // Keep only last 50 alerts
            if (alertList.getItems().size() > 50) {
                alertList.getItems().remove(alertList.getItems().size() - 1);
            }
            
            // Show popup for high severity alerts with suppression for local IPs and rate-limiting
            if ("HIGH".equals(alert.getSeverity())) {
                String sourceIp = alert.getDetails() != null && alert.getDetails().get("sourceIp") != null
                    ? alert.getDetails().get("sourceIp").toString()
                    : "unknown";

                // Suppress local/private IP popups unless user enabled them
                if (!showPopupsForLocalIps && isPrivateIp(sourceIp)) {
                    // Log suppressed popup
                    logger.info("Suppressed popup for private IP: {} - {}", sourceIp, alert.getTitle());
                } else {
                    // Rate-limit duplicates per rule+ip
                    String key = alert.getRuleName() + "|" + sourceIp;
                    LocalDateTime last = lastPopupTimes.get(key);
                    LocalDateTime now = LocalDateTime.now();
                    if (last == null || Duration.between(last, now).compareTo(POPUP_MIN_INTERVAL) >= 0) {
                        lastPopupTimes.put(key, now);
                        showAlertPopup(alert);
                    } else {
                        logger.info("Rate-limited duplicate popup for {} (last shown {})", key, last);
                    }
                }
            }
        });
    }
    
    /**
     * Show alert popup
     */
    private void showAlertPopup(SecurityAlert alert) {
        Platform.runLater(() -> {
            Alert alertDialog = new Alert(Alert.AlertType.WARNING);
            alertDialog.setTitle("Security Alert");
            alertDialog.setHeaderText(alert.getTitle());
            alertDialog.setContentText(alert.getDescription());
            alertDialog.showAndWait();
        });
    }

    /**
     * Returns true if the IP is in a private/local range (IPv4 simplified)
     */
    private boolean isPrivateIp(String ip) {
        if (ip == null) return true; // treat null as local/internal
        try {
            if (ip.startsWith("10.")) return true;
            if (ip.startsWith("192.168.")) return true;
            if (ip.startsWith("172.")) {
                // 172.16.0.0 - 172.31.255.255
                String[] parts = ip.split("\\.");
                if (parts.length >= 2) {
                    int second = Integer.parseInt(parts[1]);
                    return second >= 16 && second <= 31;
                }
            }
        } catch (Exception e) {
            // if parsing fails, err on the side of treating as private
            return true;
        }
        return false;
    }
    
    /**
     * Update statistics
     */
    private void updateStatistics() {
        try {
            Map<String, Object> stats = databaseService.getStatistics();
            // Update UI with statistics if needed
        } catch (Exception e) {
            logger.error("Error updating statistics", e);
        }
    }
    
    /**
     * Update status label
     */
    private void updateStatus(String status) {
        Platform.runLater(() -> {
            statusLabel.setText(status);
        });
    }
    
    /**
     * Export logs
     */
    private void exportLogs() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Logs");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("CSV Files", "*.csv"),
            new FileChooser.ExtensionFilter("PDF Files", "*.pdf")
        );
        
        File file = fileChooser.showSaveDialog(primaryStage);
        if (file != null) {
            executor.submit(() -> {
                try {
                    List<LogEntry> logs = databaseService.getLogEntries(null, null, null, null, 1000);
                    
                    if (file.getName().endsWith(".csv")) {
                        exportService.exportLogsToCSV(logs, file.getAbsolutePath());
                    } else if (file.getName().endsWith(".pdf")) {
                        exportService.exportLogsToPDF(logs, file.getAbsolutePath(), "DarkEye Log Export");
                    }
                    
                    Platform.runLater(() -> {
                        showInfoDialog("Export Complete", "Logs exported successfully to: " + file.getName());
                    });
                } catch (Exception e) {
                    logger.error("Error exporting logs", e);
                    Platform.runLater(() -> {
                        showErrorDialog("Export Error", "Failed to export logs: " + e.getMessage());
                    });
                }
            });
        }
    }
    
    /**
     * Export alerts
     */
    private void exportAlerts() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Alerts");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("CSV Files", "*.csv"),
            new FileChooser.ExtensionFilter("PDF Files", "*.pdf")
        );
        
        File file = fileChooser.showSaveDialog(primaryStage);
        if (file != null) {
            executor.submit(() -> {
                try {
                    List<SecurityAlert> alerts = databaseService.getSecurityAlerts(null, null, null, false, 1000);
                    
                    if (file.getName().endsWith(".csv")) {
                        exportService.exportAlertsToCSV(alerts, file.getAbsolutePath());
                    } else if (file.getName().endsWith(".pdf")) {
                        exportService.exportAlertsToPDF(alerts, file.getAbsolutePath(), "DarkEye Security Alerts");
                    }
                    
                    Platform.runLater(() -> {
                        showInfoDialog("Export Complete", "Alerts exported successfully to: " + file.getName());
                    });
                } catch (Exception e) {
                    logger.error("Error exporting alerts", e);
                    Platform.runLater(() -> {
                        showErrorDialog("Export Error", "Failed to export alerts: " + e.getMessage());
                    });
                }
            });
        }
    }
    
    /**
     * Add IP to blacklist
     */
    private void addIPToBlacklist() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Add IP to Blacklist");
        dialog.setHeaderText("Enter IP address to blacklist:");
        dialog.setContentText("IP Address:");
        
        dialog.showAndWait().ifPresent(ip -> {
            detectionEngine.addBlacklistedIP(ip);
            showInfoDialog("IP Blacklisted", "IP " + ip + " has been added to the blacklist");
        });
    }
    
    /**
     * Show statistics
     */
    private void showStatistics() {
        executor.submit(() -> {
            try {
                Map<String, Object> stats = databaseService.getStatistics();
                Map<String, Object> detectionStats = detectionEngine.getStatistics();
                
                StringBuilder statsText = new StringBuilder();
                statsText.append("Database Statistics:\n");
                for (Map.Entry<String, Object> entry : stats.entrySet()) {
                    statsText.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
                }

                statsText.append("\nDetection Engine Statistics:\n");
                for (Map.Entry<String, Object> entry : detectionStats.entrySet()) {
                    statsText.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
                }
                
                Platform.runLater(() -> {
                    Alert statsDialog = new Alert(Alert.AlertType.INFORMATION);
                    statsDialog.setTitle("System Statistics");
                    statsDialog.setHeaderText("DarkEye System Statistics");
                    statsDialog.setContentText(statsText.toString());
                    statsDialog.showAndWait();
                });
            } catch (Exception e) {
                logger.error("Error getting statistics", e);
            }
        });
    }
    
    /**
     * Show error dialog
     */
    private void showErrorDialog(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    /**
     * Show info dialog
     */
    private void showInfoDialog(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    @Override
    public void stop() {
        logger.info("Shutting down DarkEye...");
        
        // Stop monitoring
        if (isMonitoring) {
            stopMonitoring();
        }
        
        // Stop collectors
        systemMonitor.stop();
        networkMonitor.stop();
        
        // Shutdown executor
        if (executor != null) {
            executor.shutdown();
        }
        
        // Close database
        if (databaseService != null) {
            databaseService.close();
        }
        
        logger.info("DarkEye shutdown complete");
    }
    
    /**
     * Main method
     */
    public static void main(String[] args) {
        logger.info("Starting DarkEye Comprehensive Security System...");
        launch(args);
    }
}
