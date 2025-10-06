package com.darkeye.util;

import com.darkeye.model.LogEntry;
import com.darkeye.model.SecurityAlert;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Simple export service for logs and alerts to CSV format
 * Works without external dependencies
 */
public class SimpleExportService {
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    /**
     * Export log entries to CSV file
     */
    public void exportLogsToCSV(List<LogEntry> logEntries, String filePath) throws IOException {
        try (FileWriter writer = new FileWriter(filePath)) {
            // Write CSV header
            writer.write("ID,Timestamp,Source,Host,Severity,Event Type,Source IP,Destination IP,Username,Message\n");
            
            // Write log entries
            for (LogEntry entry : logEntries) {
                writer.write(escapeCSV(entry.getId()) + ",");
                writer.write(escapeCSV(entry.getTimestamp() != null ? entry.getTimestamp().format(DATE_FORMATTER) : "") + ",");
                writer.write(escapeCSV(entry.getSource()) + ",");
                writer.write(escapeCSV(entry.getHost()) + ",");
                writer.write(escapeCSV(entry.getSeverity()) + ",");
                writer.write(escapeCSV(entry.getEventType()) + ",");
                writer.write(escapeCSV(entry.getSrcIp()) + ",");
                writer.write(escapeCSV(entry.getDstIp()) + ",");
                writer.write(escapeCSV(entry.getUsername()) + ",");
                writer.write(escapeCSV(entry.getMessage()) + "\n");
            }
            
            writer.flush();
        }
        
        System.out.println("✅ Exported " + logEntries.size() + " log entries to CSV: " + filePath);
    }
    
    /**
     * Export security alerts to CSV file
     */
    public void exportAlertsToCSV(List<SecurityAlert> alerts, String filePath) throws IOException {
        try (FileWriter writer = new FileWriter(filePath)) {
            // Write CSV header
            writer.write("ID,Timestamp,Severity,Title,Description,Rule Name,Log Entry ID,Acknowledged\n");
            
            // Write alerts
            for (SecurityAlert alert : alerts) {
                writer.write(escapeCSV(alert.getId()) + ",");
                writer.write(escapeCSV(alert.getTimestamp() != null ? alert.getTimestamp().format(DATE_FORMATTER) : "") + ",");
                writer.write(escapeCSV(alert.getSeverity()) + ",");
                writer.write(escapeCSV(alert.getTitle()) + ",");
                writer.write(escapeCSV(alert.getDescription()) + ",");
                writer.write(escapeCSV(alert.getRuleName()) + ",");
                writer.write(escapeCSV(alert.getLogEntryId()) + ",");
                writer.write(escapeCSV(String.valueOf(alert.isAcknowledged())) + "\n");
            }
            
            writer.flush();
        }
        
        System.out.println("✅ Exported " + alerts.size() + " security alerts to CSV: " + filePath);
    }
    
    /**
     * Export log entries to text report
     */
    public void exportLogsToText(List<LogEntry> logEntries, String filePath, String title) throws IOException {
        try (FileWriter writer = new FileWriter(filePath)) {
            // Write header
            writer.write("=".repeat(80) + "\n");
            writer.write(title + "\n");
            writer.write("=".repeat(80) + "\n");
            writer.write("Generated on: " + LocalDateTime.now().format(DATE_FORMATTER) + "\n");
            writer.write("Total entries: " + logEntries.size() + "\n");
            writer.write("=".repeat(80) + "\n\n");
            
            // Write log entries
            for (LogEntry entry : logEntries) {
                writer.write("ID: " + entry.getId() + "\n");
                writer.write("Timestamp: " + (entry.getTimestamp() != null ? entry.getTimestamp().format(DATE_FORMATTER) : "N/A") + "\n");
                writer.write("Source: " + entry.getSource() + "\n");
                writer.write("Host: " + entry.getHost() + "\n");
                writer.write("Severity: " + entry.getSeverity() + "\n");
                writer.write("Event Type: " + entry.getEventType() + "\n");
                writer.write("Source IP: " + entry.getSrcIp() + "\n");
                writer.write("Destination IP: " + entry.getDstIp() + "\n");
                writer.write("Username: " + entry.getUsername() + "\n");
                writer.write("Message: " + entry.getMessage() + "\n");
                writer.write("-".repeat(80) + "\n\n");
            }
            
            writer.flush();
        }
        
        System.out.println("✅ Exported " + logEntries.size() + " log entries to text: " + filePath);
    }
    
    /**
     * Export security alerts to text report
     */
    public void exportAlertsToText(List<SecurityAlert> alerts, String filePath, String title) throws IOException {
        try (FileWriter writer = new FileWriter(filePath)) {
            // Write header
            writer.write("=".repeat(80) + "\n");
            writer.write(title + "\n");
            writer.write("=".repeat(80) + "\n");
            writer.write("Generated on: " + LocalDateTime.now().format(DATE_FORMATTER) + "\n");
            writer.write("Total alerts: " + alerts.size() + "\n");
            writer.write("=".repeat(80) + "\n\n");
            
            // Write alerts
            for (SecurityAlert alert : alerts) {
                writer.write("ID: " + alert.getId() + "\n");
                writer.write("Timestamp: " + (alert.getTimestamp() != null ? alert.getTimestamp().format(DATE_FORMATTER) : "N/A") + "\n");
                writer.write("Severity: " + alert.getSeverity() + "\n");
                writer.write("Title: " + alert.getTitle() + "\n");
                writer.write("Description: " + alert.getDescription() + "\n");
                writer.write("Rule Name: " + alert.getRuleName() + "\n");
                writer.write("Log Entry ID: " + alert.getLogEntryId() + "\n");
                writer.write("Acknowledged: " + alert.isAcknowledged() + "\n");
                writer.write("-".repeat(80) + "\n\n");
            }
            
            writer.flush();
        }
        
        System.out.println("✅ Exported " + alerts.size() + " security alerts to text: " + filePath);
    }
    
    /**
     * Export system statistics to text report
     */
    public void exportStatisticsToText(Map<String, Object> statistics, String filePath) throws IOException {
        try (FileWriter writer = new FileWriter(filePath)) {
            // Write header
            writer.write("=".repeat(80) + "\n");
            writer.write("DarkEye System Statistics Report\n");
            writer.write("=".repeat(80) + "\n");
            writer.write("Generated on: " + LocalDateTime.now().format(DATE_FORMATTER) + "\n");
            writer.write("=".repeat(80) + "\n\n");
            
            // Write statistics
            writer.write("System Statistics:\n");
            writer.write("-".repeat(40) + "\n");
            
            for (Map.Entry<String, Object> entry : statistics.entrySet()) {
                writer.write(entry.getKey() + ": " + entry.getValue() + "\n");
            }
            
            writer.flush();
        }
        
        System.out.println("✅ Exported system statistics to text: " + filePath);
    }
    
    /**
     * Export summary report
     */
    public void exportSummaryReport(List<LogEntry> logEntries, List<SecurityAlert> alerts, 
                                   Map<String, Object> statistics, String filePath) throws IOException {
        try (FileWriter writer = new FileWriter(filePath)) {
            // Write header
            writer.write("=".repeat(80) + "\n");
            writer.write("DarkEye Security Summary Report\n");
            writer.write("=".repeat(80) + "\n");
            writer.write("Generated on: " + LocalDateTime.now().format(DATE_FORMATTER) + "\n");
            writer.write("=".repeat(80) + "\n\n");
            
            // Write statistics
            writer.write("SUMMARY STATISTICS\n");
            writer.write("-".repeat(40) + "\n");
            writer.write("Total Logs Processed: " + logEntries.size() + "\n");
            writer.write("Total Alerts Generated: " + alerts.size() + "\n");
            
            // Count alerts by severity
            long highAlerts = alerts.stream().filter(a -> "HIGH".equals(a.getSeverity())).count();
            long mediumAlerts = alerts.stream().filter(a -> "MEDIUM".equals(a.getSeverity())).count();
            long lowAlerts = alerts.stream().filter(a -> "LOW".equals(a.getSeverity())).count();
            
            writer.write("High Severity Alerts: " + highAlerts + "\n");
            writer.write("Medium Severity Alerts: " + mediumAlerts + "\n");
            writer.write("Low Severity Alerts: " + lowAlerts + "\n");
            writer.write("\n");
            
            // Write additional statistics
            for (Map.Entry<String, Object> entry : statistics.entrySet()) {
                writer.write(entry.getKey() + ": " + entry.getValue() + "\n");
            }
            
            writer.write("\n");
            
            // Write recent high-severity alerts
            writer.write("RECENT HIGH-SEVERITY ALERTS\n");
            writer.write("-".repeat(40) + "\n");
            
            List<SecurityAlert> highSeverityAlerts = alerts.stream()
                .filter(a -> "HIGH".equals(a.getSeverity()))
                .limit(10)
                .toList();
            
            if (highSeverityAlerts.isEmpty()) {
                writer.write("No high-severity alerts found.\n");
            } else {
                for (SecurityAlert alert : highSeverityAlerts) {
                    writer.write("• " + alert.getTitle() + " (" + 
                        (alert.getTimestamp() != null ? alert.getTimestamp().format(DATE_FORMATTER) : "N/A") + ")\n");
                }
            }
            
            writer.write("\n");
            
            // Write recent suspicious activities
            writer.write("RECENT SUSPICIOUS ACTIVITIES\n");
            writer.write("-".repeat(40) + "\n");
            
            List<LogEntry> suspiciousLogs = logEntries.stream()
                .filter(log -> log.getSeverity() != null && 
                    (log.getSeverity().equals("ERROR") || log.getSeverity().equals("WARN")))
                .limit(10)
                .toList();
            
            if (suspiciousLogs.isEmpty()) {
                writer.write("No suspicious activities found.\n");
            } else {
                for (LogEntry log : suspiciousLogs) {
                    writer.write("• " + log.getMessage() + " (" + 
                        (log.getTimestamp() != null ? log.getTimestamp().format(DATE_FORMATTER) : "N/A") + ")\n");
                }
            }
            
            writer.flush();
        }
        
        System.out.println("✅ Exported summary report: " + filePath);
    }
    
    /**
     * Escape CSV values
     */
    private String escapeCSV(String value) {
        if (value == null) {
            return "";
        }
        
        // If value contains comma, quote, or newline, wrap in quotes and escape quotes
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        
        return value;
    }
}
