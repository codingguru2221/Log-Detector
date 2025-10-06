package com.darkeye.ui;

import com.darkeye.collectors.FileCollector;
import com.darkeye.detection.SimpleDetectionEngine;
import com.darkeye.model.LogEntry;
import com.darkeye.model.SecurityAlert;
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

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Enhanced DarkEye application with comprehensive workflow implementation
 * This version works with the existing dependencies and provides the complete workflow
 */
public class EnhancedMainApp extends Application {
    
    private Stage primaryStage;
    private Stage alertStage;
    private FileCollector fileCollector;
    private SimpleDetectionEngine detectionEngine;
    private ScheduledExecutorService executor;
    private boolean isMonitoring = false;
    private int alertCount = 0;
    private int logCount = 0;
    
    // Simple authentication (enhanced)
    private static final String ADMIN_PASSWORD = "Codex";
    private static final String ANALYST_PASSWORD = "analyst123";
    private static final String VIEWER_PASSWORD = "viewer123";
    private String currentUserRole = "VIEWER";
    private boolean isAuthenticated = false;
    
    // UI Components
    private Label statusLabel;
    private Label alertCountLabel;
    private Label logCountLabel;
    private TextArea logArea;
    private ListView<String> alertList;
    private ProgressBar monitoringProgress;
    private TextField blacklistField;
    
    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        // Show authentication dialog
        showAuthenticationDialog();
    }

    /**
     * Launch the enhanced admin dashboard without showing the auth dialog.
     * This is used by other UI entry points to open the admin UI after successful admin authentication.
     */
    public void launchAsAdmin(Stage stage) {
        this.primaryStage = stage;
        this.currentUserRole = "ADMIN";
        this.isAuthenticated = true;

        // Start background monitoring if not already started
        startBackgroundMonitoring();

        // Show the main dashboard on the JavaFX thread
        Platform.runLater(this::showMainDashboard);
    }
    
    /**
     * Start background monitoring
     */
    private void startBackgroundMonitoring() {
        System.out.println("🔍 DarkEye: Starting comprehensive background monitoring...");
        
        // Initialize components
        fileCollector = new FileCollector();
        detectionEngine = new SimpleDetectionEngine();
        executor = Executors.newScheduledThreadPool(3);
        
        // Set up log handler
        fileCollector.setLogHandler(this::handleLogEntry);
        
        // Set up alert handler
        detectionEngine.addAlertHandler(this::handleAlert);
        
        // Start monitoring sample logs
        String logPath = "sample-logs";
        System.out.println("📁 Monitoring logs from: " + logPath);
        
        // Start collection in background
        executor.submit(() -> {
            fileCollector.start(logPath);
        });
        
        // Schedule periodic monitoring
        executor.scheduleAtFixedRate(this::performSystemMonitoring, 0, 10, TimeUnit.SECONDS);
        
        isMonitoring = true;
        System.out.println("✅ Comprehensive background monitoring started successfully!");
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
        Label titleLabel = new Label("🔒 DarkEye Security System");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        titleLabel.setTextFill(Color.DARKBLUE);
        
        // Role selection
        Label roleLabel = new Label("Select Role:");
        ComboBox<String> roleCombo = new ComboBox<>();
        roleCombo.getItems().addAll("Admin", "Analyst", "Viewer");
        roleCombo.setValue("Viewer");
        
        // Password field
        Label passLabel = new Label("Password:");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Enter password");
        passwordField.setPrefWidth(200);
        
        // Login button
        Button loginButton = new Button("Login");
        loginButton.setPrefWidth(100);
        loginButton.setOnAction(e -> {
            String role = roleCombo.getValue();
            String password = passwordField.getText();
            
            if (authenticate(role, password)) {
                currentUserRole = role.toUpperCase();
                isAuthenticated = true;
                authStage.close();
                showMainDashboard();
            } else {
                showErrorDialog("Authentication Failed", "Invalid credentials for " + role);
            }
        });
        
        // Default credentials info
    Label infoLabel = new Label("Default: Admin/Codex, Analyst/analyst123, Viewer/viewer123");
        infoLabel.setFont(Font.font("Arial", 10));
        infoLabel.setTextFill(Color.GRAY);
        
        authLayout.getChildren().addAll(titleLabel, roleLabel, roleCombo, 
                                      passLabel, passwordField, loginButton, infoLabel);
        
        Scene authScene = new Scene(authLayout, 350, 300);
        authStage.setScene(authScene);
        authStage.showAndWait();
    }
    
    /**
     * Authenticate user
     */
    private boolean authenticate(String role, String password) {
        switch (role.toUpperCase()) {
            case "ADMIN":
                return ADMIN_PASSWORD.equals(password);
            case "ANALYST":
                return ANALYST_PASSWORD.equals(password);
            case "VIEWER":
                return VIEWER_PASSWORD.equals(password);
            default:
                return false;
        }
    }
    
    /**
     * Show main dashboard
     */
    private void showMainDashboard() {
        primaryStage.setTitle("DarkEye - Comprehensive Security Dashboard (" + currentUserRole + ")");
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
        // Start background monitoring after UI is visible so UI components (labels, lists) are initialized
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
        MenuItem startMonitoringItem = new MenuItem("Start Active Monitoring");
        startMonitoringItem.setOnAction(e -> startActiveMonitoring());
        
        MenuItem stopMonitoringItem = new MenuItem("Stop Active Monitoring");
        stopMonitoringItem.setOnAction(e -> stopActiveMonitoring());
        
        monitoringMenu.getItems().addAll(startMonitoringItem, stopMonitoringItem);
        
        // Tools menu (Admin/Analyst only)
        if (!"VIEWER".equals(currentUserRole)) {
            Menu toolsMenu = new Menu("Tools");
            MenuItem addBlacklistItem = new MenuItem("Add IP to Blacklist");
            addBlacklistItem.setOnAction(e -> showBlacklistDialog());
            
            MenuItem viewStatsItem = new MenuItem("View Statistics");
            viewStatsItem.setOnAction(e -> showStatistics());
            
            toolsMenu.getItems().addAll(addBlacklistItem, viewStatsItem);
            menuBar.getMenus().add(toolsMenu);
        }
        
        menuBar.getMenus().addAll(fileMenu, monitoringMenu);
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
        
        Button startButton = new Button("Start Active Monitoring");
        startButton.setOnAction(e -> startActiveMonitoring());
        
        Button stopButton = new Button("Stop Active Monitoring");
        stopButton.setOnAction(e -> stopActiveMonitoring());
        
        Button browseButton = new Button("Browse Log Files");
        browseButton.setOnAction(e -> browseLogFiles());
        
        controlPanel.getChildren().addAll(startButton, stopButton, browseButton);
        
        // Monitoring progress
        monitoringProgress = new ProgressBar();
        monitoringProgress.setPrefWidth(200);
        monitoringProgress.setProgress(-1); // Indeterminate for background monitoring
        
        HBox progressBox = new HBox(10);
        progressBox.setAlignment(Pos.CENTER_LEFT);
        progressBox.getChildren().addAll(new Label("Monitoring Status:"), monitoringProgress);
        
        // Main content area
        HBox mainArea = new HBox(10);
        
        // Log display area
        VBox logAreaBox = new VBox(5);
        logAreaBox.setPrefWidth(600);
        
        Label logLabel = new Label("📊 Real-time Log Entries:");
        logLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        
        logArea = new TextArea();
        logArea.setPrefRowCount(25);
        logArea.setEditable(false);
        logArea.setWrapText(true);
        logArea.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 10px;");
        
        logAreaBox.getChildren().addAll(logLabel, logArea);
        
        // Alert display area
        VBox alertAreaBox = new VBox(5);
        alertAreaBox.setPrefWidth(400);
        
        Label alertLabel = new Label("🚨 Security Alerts:");
        alertLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        
        alertList = new ListView<>();
        alertList.setPrefHeight(500);
        
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
        
        statusLabel = new Label("🟢 Background Monitoring Active");
        alertCountLabel = new Label("🚨 Alerts: 0");
        logCountLabel = new Label("📝 Logs: 0");
        
        Label userLabel = new Label("👤 User: " + currentUserRole);
        userLabel.setTextFill(Color.BLUE);
        
        statusBar.getChildren().addAll(statusLabel, alertCountLabel, logCountLabel, userLabel);
        
        return statusBar;
    }
    
    /**
     * Start active monitoring
     */
    private void startActiveMonitoring() {
        if (!isAuthenticated) {
            showErrorDialog("Access Denied", "Please authenticate first");
            return;
        }
        
        updateStatus("🟡 Active Monitoring Started");
        monitoringProgress.setProgress(-1);
        
        // Start additional file collection
        executor.submit(() -> {
            fileCollector.start("sample-logs");
        });
        
        showInfoDialog("Monitoring Started", "Active monitoring has been started. Check the log area for real-time updates.");
    }
    
    /**
     * Stop active monitoring
     */
    private void stopActiveMonitoring() {
        updateStatus("🔴 Active Monitoring Stopped");
        monitoringProgress.setProgress(0);
        
        fileCollector.stop();
        
        showInfoDialog("Monitoring Stopped", "Active monitoring has been stopped.");
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
            updateStatus("📁 Collecting from: " + selectedFile.getName());
        }
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
        
        // Update UI
        Platform.runLater(() -> {
            logCount++;
            logCountLabel.setText("📝 Logs: " + logCount);
            
            // Add to log display (keep last 100 entries)
            String logText = logArea.getText();
            String newEntry = String.format("[%s] %s %s: %s\n", 
                logEntry.getTimestamp().format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                getSeverityIcon(logEntry.getSeverity()),
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
    private void handleAlert(SecurityAlert alert) {
        if (alert == null) {
            return;
        }
        
        // Update UI
        Platform.runLater(() -> {
            alertCount++;
            alertCountLabel.setText("🚨 Alerts: " + alertCount);
            
            // Add to alert list
            String alertText = String.format("[%s] %s %s: %s", 
                alert.getTimestamp().format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                getSeverityIcon(alert.getSeverity()),
                alert.getSeverity(),
                alert.getTitle());
            alertList.getItems().add(0, alertText);
            
            // Keep only last 50 alerts
            if (alertList.getItems().size() > 50) {
                alertList.getItems().remove(alertList.getItems().size() - 1);
            }
            
            // Show popup for high severity alerts
            if ("HIGH".equals(alert.getSeverity())) {
                showAlertPopup(alert);
            }
        });
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
     * Show alert popup
     */
    private void showAlertPopup(SecurityAlert alert) {
        Platform.runLater(() -> {
            Alert alertDialog = new Alert(Alert.AlertType.WARNING);
            alertDialog.setTitle("🚨 Security Alert");
            alertDialog.setHeaderText(alert.getTitle());
            alertDialog.setContentText(alert.getDescription());
            alertDialog.showAndWait();
        });
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
     * Export logs (simplified)
     */
    private void exportLogs() {
        showInfoDialog("Export Feature", "Log export functionality would be implemented here.\nCurrent logs: " + logCount);
    }
    
    /**
     * Export alerts (simplified)
     */
    private void exportAlerts() {
        showInfoDialog("Export Feature", "Alert export functionality would be implemented here.\nCurrent alerts: " + alertCount);
    }
    
    /**
     * Show blacklist dialog
     */
    private void showBlacklistDialog() {
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
        StringBuilder stats = new StringBuilder();
        stats.append("DarkEye System Statistics\n");
        stats.append("========================\n\n");
        stats.append("Total Logs Processed: ").append(logCount).append("\n");
        stats.append("Total Alerts Generated: ").append(alertCount).append("\n");
        stats.append("Current User Role: ").append(currentUserRole).append("\n");
        stats.append("Monitoring Status: ").append(isMonitoring ? "Active" : "Inactive").append("\n");
        stats.append("System Time: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");
        
        Alert statsDialog = new Alert(Alert.AlertType.INFORMATION);
        statsDialog.setTitle("System Statistics");
        statsDialog.setHeaderText("DarkEye System Statistics");
        statsDialog.setContentText(stats.toString());
        statsDialog.showAndWait();
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
        System.out.println("🛑 Shutting down DarkEye...");
        
        // Stop monitoring
        if (isMonitoring) {
            fileCollector.stop();
        }
        
        // Shutdown executor
        if (executor != null) {
            executor.shutdown();
        }
        
        System.out.println("✅ DarkEye shutdown complete");
    }
    
    /**
     * Main method
     */
    public static void main(String[] args) {
        System.out.println("🚀 Starting DarkEye Enhanced Security System...");
        System.out.println("================================================");
        System.out.println("Features:");
        System.out.println("✅ Real-time log monitoring");
        System.out.println("✅ Advanced threat detection");
        System.out.println("✅ Role-based authentication");
        System.out.println("✅ Security alert system");
        System.out.println("✅ Comprehensive dashboard");
        System.out.println("✅ System activity monitoring");
        System.out.println("================================================");
        
        launch(args);
    }
}
