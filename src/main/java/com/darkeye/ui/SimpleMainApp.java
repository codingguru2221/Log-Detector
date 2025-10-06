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
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Simple DarkEye application with background monitoring and authentication
 */
public class SimpleMainApp extends Application {
    
    private Stage primaryStage;
    private Stage alertStage;
    private FileCollector fileCollector;
    private SimpleDetectionEngine detectionEngine;
    private ScheduledExecutorService executor;
    private boolean isMonitoring = false;
    private int alertCount = 0;
    private int logCount = 0;
    
    // Simple authentication (in real app, use proper auth)
    private static final String ADMIN_PASSWORD = "Codex";
    private boolean isAuthenticated = false;
    
    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        
        // Start background monitoring immediately
        startBackgroundMonitoring();
        
        // Show authentication dialog
        showAuthenticationDialog();
    }
    
    private void startBackgroundMonitoring() {
        System.out.println("🔍 DarkEye: Starting background log monitoring...");
        
        // Initialize components
        fileCollector = new FileCollector();
        detectionEngine = new SimpleDetectionEngine();
        executor = Executors.newScheduledThreadPool(2);
        
        // Set up log handler
        fileCollector.setLogHandler(this::handleLogEntry);
        
        // Set up alert handler
        detectionEngine.addAlertHandler(this::handleAlert);
        
        // Start monitoring sample logs (you can change this path)
        String logPath = "sample-logs";
        System.out.println("📁 Monitoring logs from: " + logPath);
        
        // Start collection in background
        executor.submit(() -> {
            fileCollector.start(logPath);
        });
        
        isMonitoring = true;
        System.out.println("✅ Background monitoring started successfully!");
    }
    
    private void handleLogEntry(LogEntry logEntry) {
        logCount++;
        System.out.println("📝 Log #" + logCount + ": " + logEntry.getSeverity() + " - " + logEntry.getMessage());
        
        // Process through detection engine
        detectionEngine.processLogEntry(logEntry);
    }
    
    private void handleAlert(SecurityAlert alert) {
        alertCount++;
        System.out.println("🚨 ALERT #" + alertCount + ": " + alert.getSeverity() + " - " + alert.getTitle());
        
        // Show popup notification for high severity alerts
        if ("HIGH".equals(alert.getSeverity())) {
            Platform.runLater(() -> showAlertNotification(alert));
        }
    }
    
    private void showAlertNotification(SecurityAlert alert) {
        if (alertStage != null) {
            alertStage.close();
        }
        
        alertStage = new Stage();
        alertStage.initStyle(StageStyle.UTILITY);
        alertStage.setTitle("🚨 Security Alert");
        alertStage.setAlwaysOnTop(true);
        
        VBox content = new VBox(10);
        content.setPadding(new Insets(20));
        content.setAlignment(Pos.CENTER);
        
        Label titleLabel = new Label("🚨 " + alert.getTitle());
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: red;");
        
        Label descLabel = new Label(alert.getDescription());
        descLabel.setWrapText(true);
        
        Label timeLabel = new Label("Time: " + alert.getTimestamp().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        timeLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: gray;");
        
        Button okButton = new Button("OK");
        okButton.setOnAction(e -> alertStage.close());
        
        content.getChildren().addAll(titleLabel, descLabel, timeLabel, okButton);
        
        Scene scene = new Scene(content, 400, 200);
        alertStage.setScene(scene);
        alertStage.show();
        
        // Auto-close after 10 seconds
        executor.schedule(() -> {
            Platform.runLater(() -> {
                if (alertStage != null) {
                    alertStage.close();
                }
            });
        }, 10, TimeUnit.SECONDS);
    }
    
    private void showAuthenticationDialog() {
        Stage authStage = new Stage();
        authStage.setTitle("DarkEye - Authentication Required");
        authStage.setResizable(false);
        
        VBox content = new VBox(20);
        content.setPadding(new Insets(30));
        content.setAlignment(Pos.CENTER);
        
        Label titleLabel = new Label("🔐 DarkEye Security");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        
        Label descLabel = new Label("Enter password to view logs and alerts:");
        descLabel.setStyle("-fx-font-size: 12px;");
        
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Enter password");
        passwordField.setMaxWidth(200);
        
        Label statusLabel = new Label();
        statusLabel.setStyle("-fx-text-fill: red;");
        
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER);
        
        Button loginButton = new Button("Login");
        Button exitButton = new Button("Exit");
        
        loginButton.setOnAction(e -> {
            String password = passwordField.getText();
            if (ADMIN_PASSWORD.equals(password)) {
                isAuthenticated = true;
                statusLabel.setText("✅ Authentication successful!");
                statusLabel.setStyle("-fx-text-fill: green;");
                
                // Show main UI after successful auth
                executor.schedule(() -> {
                    Platform.runLater(() -> {
                        authStage.close();
                        // If admin, launch enhanced admin dashboard
                        try {
                            EnhancedMainApp adminApp = new EnhancedMainApp();
                            adminApp.launchAsAdmin(primaryStage);
                        } catch (Exception ex) {
                            // Fallback to simple UI if enhanced fails
                            showMainUI();
                        }
                    });
                }, 1, TimeUnit.SECONDS);
            } else {
                statusLabel.setText("❌ Invalid password!");
                passwordField.clear();
            }
        });
        
        exitButton.setOnAction(e -> {
            System.exit(0);
        });
        
        // Enter key to login
        passwordField.setOnAction(e -> loginButton.fire());
        
        buttonBox.getChildren().addAll(loginButton, exitButton);
        content.getChildren().addAll(titleLabel, descLabel, passwordField, statusLabel, buttonBox);
        
        Scene scene = new Scene(content, 350, 250);
        authStage.setScene(scene);
        authStage.show();
        
        // Focus on password field
        passwordField.requestFocus();
    }
    
    private void showMainUI() {
        primaryStage.setTitle("DarkEye - Log Analysis Dashboard");
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(600);
        
        BorderPane root = new BorderPane();
        
        // Top status bar
        HBox statusBar = new HBox(20);
        statusBar.setPadding(new Insets(10));
        statusBar.setStyle("-fx-background-color: #f0f0f0;");
        
        Label monitoringStatus = new Label("🟢 Monitoring: ACTIVE");
        monitoringStatus.setStyle("-fx-font-weight: bold; -fx-text-fill: green;");
        
        Label logCountLabel = new Label("📝 Logs: " + logCount);
        Label alertCountLabel = new Label("🚨 Alerts: " + alertCount);
        
        Button refreshButton = new Button("🔄 Refresh");
        refreshButton.setOnAction(e -> {
            logCountLabel.setText("📝 Logs: " + logCount);
            alertCountLabel.setText("🚨 Alerts: " + alertCount);
        });
        
        Button stopButton = new Button("⏹️ Stop Monitoring");
        stopButton.setOnAction(e -> {
            if (isMonitoring) {
                fileCollector.stop();
                isMonitoring = false;
                monitoringStatus.setText("🔴 Monitoring: STOPPED");
                monitoringStatus.setStyle("-fx-font-weight: bold; -fx-text-fill: red;");
                stopButton.setText("▶️ Start Monitoring");
            } else {
                startBackgroundMonitoring();
                monitoringStatus.setText("🟢 Monitoring: ACTIVE");
                monitoringStatus.setStyle("-fx-font-weight: bold; -fx-text-fill: green;");
                stopButton.setText("⏹️ Stop Monitoring");
            }
        });
        
        statusBar.getChildren().addAll(monitoringStatus, logCountLabel, alertCountLabel, refreshButton, stopButton);
        root.setTop(statusBar);
        
        // Center content
        VBox centerContent = new VBox(20);
        centerContent.setPadding(new Insets(20));
        centerContent.setAlignment(Pos.CENTER);
        
        Label welcomeLabel = new Label("🔍 DarkEye Log Analysis System");
        welcomeLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");
        
        Label descLabel = new Label("Background monitoring is active. Check console for real-time logs and alerts.");
        descLabel.setStyle("-fx-font-size: 14px;");
        
        TextArea logArea = new TextArea();
        logArea.setPrefRowCount(15);
        logArea.setEditable(false);
        logArea.setText("📋 Recent Activity:\n" +
                       "✅ Background monitoring started\n" +
                       "📁 Monitoring: sample-logs directory\n" +
                       "🔍 Detection engine active\n" +
                       "🚨 Alert notifications enabled\n\n" +
                       "Check console output for real-time logs and alerts...");
        
        centerContent.getChildren().addAll(welcomeLabel, descLabel, logArea);
        root.setCenter(centerContent);
        
        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.show();
        
        // Update log area periodically
        executor.scheduleAtFixedRate(() -> {
            Platform.runLater(() -> {
                logCountLabel.setText("📝 Logs: " + logCount);
                alertCountLabel.setText("🚨 Alerts: " + alertCount);
            });
        }, 1, 1, TimeUnit.SECONDS);
    }
    
    @Override
    public void stop() {
        if (executor != null) {
            executor.shutdown();
        }
        if (fileCollector != null) {
            fileCollector.stop();
        }
    }
    
    public static void main(String[] args) {
        System.out.println("🚀 Starting DarkEye - Simple Log Analysis System");
        launch(args);
    }
}
