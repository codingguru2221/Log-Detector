package com.darkeye.ui;

import com.darkeye.collectors.FileCollector;
import com.darkeye.detection.SimpleDetectionEngine;
import com.darkeye.model.SecurityAlert;
import com.darkeye.model.LogEntry;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;
import java.util.UUID;

/**
 * Main controller for DarkEye UI
 */
public class MainController implements Initializable {
    
    @FXML private TableView<LogEntry> logTable;
    @FXML private TableColumn<LogEntry, String> timestampColumn;
    @FXML private TableColumn<LogEntry, String> severityColumn;
    @FXML private TableColumn<LogEntry, String> sourceColumn;
    @FXML private TableColumn<LogEntry, String> messageColumn;
    @FXML private TableColumn<LogEntry, String> srcIpColumn;
    
    @FXML private TableView<SecurityAlert> alertTable;
    @FXML private TableColumn<SecurityAlert, String> alertTimestampColumn;
    @FXML private TableColumn<SecurityAlert, String> alertSeverityColumn;
    @FXML private TableColumn<SecurityAlert, String> alertTitleColumn;
    @FXML private TableColumn<SecurityAlert, String> alertRuleColumn;
    
    @FXML private Button startButton;
    @FXML private Button stopButton;
    @FXML private Button browseButton;
    @FXML private TextField pathField;
    @FXML private Label statusLabel;
    @FXML private Label alertCountLabel;
    @FXML private Label logCountLabel;
    
    private ObservableList<LogEntry> logEntries = FXCollections.observableArrayList();
    private ObservableList<SecurityAlert> alerts = FXCollections.observableArrayList();
    private FileCollector fileCollector;
    private SimpleDetectionEngine detectionEngine;
    private int alertCount = 0;
    private int logCount = 0;
    // Popup suppression and rate-limiting for FXML controller
    private volatile boolean showPopupsForLocalIps = false;
    private final java.util.concurrent.ConcurrentMap<String, java.time.LocalDateTime> lastPopupTimes = new java.util.concurrent.ConcurrentHashMap<>();
    private static final java.time.Duration POPUP_MIN_INTERVAL = java.time.Duration.ofMinutes(1);
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTables();
        setupCollector();
        setupDetectionEngine();
        updateStatus("Ready to collect logs");
    }
    
    private void setupTables() {
        // Log table columns
        timestampColumn.setCellValueFactory(new PropertyValueFactory<>("timestamp"));
        severityColumn.setCellValueFactory(new PropertyValueFactory<>("severity"));
        sourceColumn.setCellValueFactory(new PropertyValueFactory<>("source"));
        messageColumn.setCellValueFactory(new PropertyValueFactory<>("message"));
        srcIpColumn.setCellValueFactory(new PropertyValueFactory<>("srcIp"));
        
        // Alert table columns
        alertTimestampColumn.setCellValueFactory(new PropertyValueFactory<>("timestamp"));
        alertSeverityColumn.setCellValueFactory(new PropertyValueFactory<>("severity"));
        alertTitleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        alertRuleColumn.setCellValueFactory(new PropertyValueFactory<>("ruleName"));
        
        // Set table data
        logTable.setItems(logEntries);
        alertTable.setItems(alerts);
        
        // Add context menu for log table
        ContextMenu logContextMenu = new ContextMenu();
        MenuItem viewDetailsItem = new MenuItem("View Details");
        viewDetailsItem.setOnAction(e -> viewLogDetails());
        logContextMenu.getItems().add(viewDetailsItem);
        logTable.setContextMenu(logContextMenu);
        
        // Add context menu for alert table
        ContextMenu alertContextMenu = new ContextMenu();
        MenuItem acknowledgeItem = new MenuItem("Acknowledge");
        acknowledgeItem.setOnAction(e -> acknowledgeAlert());
        alertContextMenu.getItems().add(acknowledgeItem);
        alertTable.setContextMenu(alertContextMenu);
    }
    
    private void setupCollector() {
        fileCollector = new FileCollector();
        fileCollector.setLogHandler(this::handleLogEntry);
    }
    
    private void setupDetectionEngine() {
        detectionEngine = new SimpleDetectionEngine();
        detectionEngine.addAlertHandler(this::handleAlert);
    }
    
    private void handleLogEntry(LogEntry logEntry) {
        Platform.runLater(() -> {
            logEntries.add(0, logEntry); // Add to beginning for newest first
            logCount++;
            logCountLabel.setText("Logs: " + logCount);
            
            // Keep only last 1000 entries to prevent memory issues
            if (logEntries.size() > 1000) {
                logEntries.remove(logEntries.size() - 1);
            }
        });
        
        // Process through detection engine
        detectionEngine.processLogEntry(logEntry);
    }
    
    private void handleAlert(SecurityAlert alert) {
        Platform.runLater(() -> {
            alerts.add(0, alert); // Add to beginning for newest first
            alertCount++;
            alertCountLabel.setText("Alerts: " + alertCount);
            
            // Show popup for high severity alerts
            if ("HIGH".equals(alert.getSeverity())) {
                String sourceIp = alert.getDetails() != null && alert.getDetails().get("sourceIp") != null
                    ? alert.getDetails().get("sourceIp").toString()
                    : "unknown";

                if (!showPopupsForLocalIps && isPrivateIp(sourceIp)) {
                    // suppress
                } else {
                    String key = alert.getRuleName() + "|" + sourceIp;
                    java.time.LocalDateTime last = lastPopupTimes.get(key);
                    java.time.LocalDateTime now = java.time.LocalDateTime.now();
                    if (last == null || java.time.Duration.between(last, now).compareTo(POPUP_MIN_INTERVAL) >= 0) {
                        lastPopupTimes.put(key, now);
                        showAlertPopup(alert);
                    }
                }
            }
            
            // Keep only last 100 alerts
            if (alerts.size() > 100) {
                alerts.remove(alerts.size() - 1);
            }
        });
    }
    
    private void showAlertPopup(SecurityAlert alert) {
        javafx.scene.control.Alert popup = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.WARNING);
        popup.setTitle("Security Alert");
        popup.setHeaderText(alert.getTitle());
        popup.setContentText(alert.getDescription() + "\n\nTime: " + 
                           alert.getTimestamp().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        popup.showAndWait();
    }
    
    @FXML
    private void browseForPath() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Log Directory");
        File selectedDirectory = directoryChooser.showDialog(getStage());
        
        if (selectedDirectory != null) {
            pathField.setText(selectedDirectory.getAbsolutePath());
        }
    }
    
    @FXML
    private void startCollection() {
        String path = pathField.getText().trim();
        if (path.isEmpty()) {
            showError("Please select a path to collect logs from");
            return;
        }
        
        File file = new File(path);
        if (!file.exists()) {
            showError("Selected path does not exist");
            return;
        }
        
        // Clear previous data
        logEntries.clear();
        alerts.clear();
        logCount = 0;
        alertCount = 0;
        logCountLabel.setText("Logs: 0");
        alertCountLabel.setText("Alerts: 0");
        
        // Start collection in background thread
        new Thread(() -> {
            Platform.runLater(() -> {
                startButton.setDisable(true);
                stopButton.setDisable(false);
                updateStatus("Collecting logs from: " + path);
            });
            
            fileCollector.start(path);
            
            Platform.runLater(() -> {
                startButton.setDisable(false);
                stopButton.setDisable(true);
                updateStatus("Collection stopped");
            });
        }).start();
    }
    
    @FXML
    private void stopCollection() {
        fileCollector.stop();
        startButton.setDisable(false);
        stopButton.setDisable(true);
        updateStatus("Collection stopped");
    }
    
    private void viewLogDetails() {
        LogEntry selected = logTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            showLogDetails(selected);
        }
    }
    
    private void acknowledgeAlert() {
        SecurityAlert selected = alertTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            selected.setAcknowledged(true);
            alertTable.refresh();
        }
    }
    
    private void showLogDetails(LogEntry logEntry) {
        Alert dialog = new Alert(Alert.AlertType.INFORMATION);
        dialog.setTitle("Log Entry Details");
        dialog.setHeaderText("Log Entry: " + logEntry.getId());
        
        StringBuilder content = new StringBuilder();
        content.append("Timestamp: ").append(logEntry.getTimestamp()).append("\n");
        content.append("Source: ").append(logEntry.getSource()).append("\n");
        content.append("Host: ").append(logEntry.getHost()).append("\n");
        content.append("Severity: ").append(logEntry.getSeverity()).append("\n");
        content.append("Event Type: ").append(logEntry.getEventType()).append("\n");
        content.append("Source IP: ").append(logEntry.getSrcIp()).append("\n");
        content.append("Destination IP: ").append(logEntry.getDstIp()).append("\n");
        content.append("Username: ").append(logEntry.getUsername()).append("\n");
        content.append("Message: ").append(logEntry.getMessage()).append("\n");
        
        dialog.setContentText(content.toString());
        dialog.showAndWait();
    }
    
    private void updateStatus(String status) {
        statusLabel.setText("Status: " + status);
    }
    
    private void showError(String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private Stage getStage() {
        return (Stage) logTable.getScene().getWindow();
    }

    private boolean isPrivateIp(String ip) {
        if (ip == null) return true;
        try {
            if (ip.startsWith("10.")) return true;
            if (ip.startsWith("192.168.")) return true;
            if (ip.startsWith("172.")) {
                String[] parts = ip.split("\\.");
                if (parts.length >= 2) {
                    int second = Integer.parseInt(parts[1]);
                    return second >= 16 && second <= 31;
                }
            }
        } catch (Exception e) {
            return true;
        }
        return false;
    }
}
