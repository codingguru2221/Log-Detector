package com.darkeye.util;

import com.darkeye.model.LogEntry;
import com.darkeye.model.SecurityAlert;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Service for exporting logs and alerts to CSV and PDF formats
 */
public class ExportService {
    
    private static final Logger logger = LoggerFactory.getLogger(ExportService.class);
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    /**
     * Export log entries to CSV file
     */
    public void exportLogsToCSV(List<LogEntry> logEntries, String filePath) throws IOException {
        try (FileWriter writer = new FileWriter(filePath);
             CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT
                 .withHeader("ID", "Timestamp", "Source", "Host", "Severity", "Event Type", 
                           "Source IP", "Destination IP", "Username", "Message"))) {
            
            for (LogEntry entry : logEntries) {
                csvPrinter.printRecord(
                    entry.getId(),
                    entry.getTimestamp() != null ? entry.getTimestamp().format(DATE_FORMATTER) : "",
                    entry.getSource(),
                    entry.getHost(),
                    entry.getSeverity(),
                    entry.getEventType(),
                    entry.getSrcIp(),
                    entry.getDstIp(),
                    entry.getUsername(),
                    entry.getMessage()
                );
            }
            
            csvPrinter.flush();
        }
        
        logger.info("Exported {} log entries to CSV: {}", logEntries.size(), filePath);
    }
    
    /**
     * Export security alerts to CSV file
     */
    public void exportAlertsToCSV(List<SecurityAlert> alerts, String filePath) throws IOException {
        try (FileWriter writer = new FileWriter(filePath);
             CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT
                 .withHeader("ID", "Timestamp", "Severity", "Title", "Description", 
                           "Rule Name", "Log Entry ID", "Acknowledged"))) {
            
            for (SecurityAlert alert : alerts) {
                csvPrinter.printRecord(
                    alert.getId(),
                    alert.getTimestamp() != null ? alert.getTimestamp().format(DATE_FORMATTER) : "",
                    alert.getSeverity(),
                    alert.getTitle(),
                    alert.getDescription(),
                    alert.getRuleName(),
                    alert.getLogEntryId(),
                    alert.isAcknowledged()
                );
            }
            
            csvPrinter.flush();
        }
        
        logger.info("Exported {} security alerts to CSV: {}", alerts.size(), filePath);
    }
    
    /**
     * Export log entries to PDF report
     */
    public void exportLogsToPDF(List<LogEntry> logEntries, String filePath, String title) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                // Set up fonts
                PDType1Font titleFont = PDType1Font.HELVETICA_BOLD;
                PDType1Font headerFont = PDType1Font.HELVETICA_BOLD;
                PDType1Font bodyFont = PDType1Font.HELVETICA;
                
                float yPosition = 750;
                float leftMargin = 50;
                float rightMargin = 550;
                
                // Title
                contentStream.beginText();
                contentStream.setFont(titleFont, 16);
                contentStream.newLineAtOffset(leftMargin, yPosition);
                contentStream.showText(title);
                contentStream.endText();
                
                yPosition -= 30;
                
                // Report metadata
                contentStream.beginText();
                contentStream.setFont(bodyFont, 10);
                contentStream.newLineAtOffset(leftMargin, yPosition);
                contentStream.showText("Generated on: " + LocalDateTime.now().format(DATE_FORMATTER));
                contentStream.endText();
                
                yPosition -= 15;
                
                contentStream.beginText();
                contentStream.setFont(bodyFont, 10);
                contentStream.newLineAtOffset(leftMargin, yPosition);
                contentStream.showText("Total entries: " + logEntries.size());
                contentStream.endText();
                
                yPosition -= 40;
                
                // Table headers
                contentStream.beginText();
                contentStream.setFont(headerFont, 10);
                contentStream.newLineAtOffset(leftMargin, yPosition);
                contentStream.showText("Timestamp");
                contentStream.endText();
                
                contentStream.beginText();
                contentStream.setFont(headerFont, 10);
                contentStream.newLineAtOffset(leftMargin + 120, yPosition);
                contentStream.showText("Source");
                contentStream.endText();
                
                contentStream.beginText();
                contentStream.setFont(headerFont, 10);
                contentStream.newLineAtOffset(leftMargin + 200, yPosition);
                contentStream.showText("Severity");
                contentStream.endText();
                
                contentStream.beginText();
                contentStream.setFont(headerFont, 10);
                contentStream.newLineAtOffset(leftMargin + 260, yPosition);
                contentStream.showText("Message");
                contentStream.endText();
                
                yPosition -= 20;
                
                // Draw line under headers
                contentStream.moveTo(leftMargin, yPosition);
                contentStream.lineTo(rightMargin, yPosition);
                contentStream.stroke();
                
                yPosition -= 10;
                
                // Log entries
                contentStream.setFont(bodyFont, 8);
                for (LogEntry entry : logEntries) {
                    if (yPosition < 50) {
                        // Start new page
                        contentStream.close();
                        page = new PDPage();
                        document.addPage(page);
                        PDPageContentStream newContentStream = new PDPageContentStream(document, page);
                        newContentStream.setFont(bodyFont, 8);
                        contentStream = newContentStream;
                        yPosition = 750;
                    }
                    
                    // Timestamp
                    contentStream.beginText();
                    contentStream.newLineAtOffset(leftMargin, yPosition);
                    String timestamp = entry.getTimestamp() != null ? 
                        entry.getTimestamp().format(DATE_FORMATTER) : "";
                    contentStream.showText(truncateText(timestamp, 15));
                    contentStream.endText();
                    
                    // Source
                    contentStream.beginText();
                    contentStream.newLineAtOffset(leftMargin + 120, yPosition);
                    contentStream.showText(truncateText(entry.getSource(), 15));
                    contentStream.endText();
                    
                    // Severity
                    contentStream.beginText();
                    contentStream.newLineAtOffset(leftMargin + 200, yPosition);
                    contentStream.showText(truncateText(entry.getSeverity(), 10));
                    contentStream.endText();
                    
                    // Message
                    contentStream.beginText();
                    contentStream.newLineAtOffset(leftMargin + 260, yPosition);
                    contentStream.showText(truncateText(entry.getMessage(), 35));
                    contentStream.endText();
                    
                    yPosition -= 15;
                }
            }
            
            document.save(filePath);
        }
        
        logger.info("Exported {} log entries to PDF: {}", logEntries.size(), filePath);
    }
    
    /**
     * Export security alerts to PDF report
     */
    public void exportAlertsToPDF(List<SecurityAlert> alerts, String filePath, String title) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                // Set up fonts
                PDType1Font titleFont = PDType1Font.HELVETICA_BOLD;
                PDType1Font headerFont = PDType1Font.HELVETICA_BOLD;
                PDType1Font bodyFont = PDType1Font.HELVETICA;
                
                float yPosition = 750;
                float leftMargin = 50;
                float rightMargin = 550;
                
                // Title
                contentStream.beginText();
                contentStream.setFont(titleFont, 16);
                contentStream.newLineAtOffset(leftMargin, yPosition);
                contentStream.showText(title);
                contentStream.endText();
                
                yPosition -= 30;
                
                // Report metadata
                contentStream.beginText();
                contentStream.setFont(bodyFont, 10);
                contentStream.newLineAtOffset(leftMargin, yPosition);
                contentStream.showText("Generated on: " + LocalDateTime.now().format(DATE_FORMATTER));
                contentStream.endText();
                
                yPosition -= 15;
                
                contentStream.beginText();
                contentStream.setFont(bodyFont, 10);
                contentStream.newLineAtOffset(leftMargin, yPosition);
                contentStream.showText("Total alerts: " + alerts.size());
                contentStream.endText();
                
                yPosition -= 40;
                
                // Table headers
                contentStream.beginText();
                contentStream.setFont(headerFont, 10);
                contentStream.newLineAtOffset(leftMargin, yPosition);
                contentStream.showText("Timestamp");
                contentStream.endText();
                
                contentStream.beginText();
                contentStream.setFont(headerFont, 10);
                contentStream.newLineAtOffset(leftMargin + 120, yPosition);
                contentStream.showText("Severity");
                contentStream.endText();
                
                contentStream.beginText();
                contentStream.setFont(headerFont, 10);
                contentStream.newLineAtOffset(leftMargin + 180, yPosition);
                contentStream.showText("Title");
                contentStream.endText();
                
                contentStream.beginText();
                contentStream.setFont(headerFont, 10);
                contentStream.newLineAtOffset(leftMargin + 350, yPosition);
                contentStream.showText("Rule");
                contentStream.endText();
                
                yPosition -= 20;
                
                // Draw line under headers
                contentStream.moveTo(leftMargin, yPosition);
                contentStream.lineTo(rightMargin, yPosition);
                contentStream.stroke();
                
                yPosition -= 10;
                
                // Alerts
                contentStream.setFont(bodyFont, 8);
                for (SecurityAlert alert : alerts) {
                    if (yPosition < 50) {
                        // Start new page
                        contentStream.close();
                        page = new PDPage();
                        document.addPage(page);
                        PDPageContentStream newContentStream = new PDPageContentStream(document, page);
                        newContentStream.setFont(bodyFont, 8);
                        contentStream = newContentStream;
                        yPosition = 750;
                    }
                    
                    // Timestamp
                    contentStream.beginText();
                    contentStream.newLineAtOffset(leftMargin, yPosition);
                    String timestamp = alert.getTimestamp() != null ? 
                        alert.getTimestamp().format(DATE_FORMATTER) : "";
                    contentStream.showText(truncateText(timestamp, 15));
                    contentStream.endText();
                    
                    // Severity
                    contentStream.beginText();
                    contentStream.newLineAtOffset(leftMargin + 120, yPosition);
                    contentStream.showText(truncateText(alert.getSeverity(), 10));
                    contentStream.endText();
                    
                    // Title
                    contentStream.beginText();
                    contentStream.newLineAtOffset(leftMargin + 180, yPosition);
                    contentStream.showText(truncateText(alert.getTitle(), 25));
                    contentStream.endText();
                    
                    // Rule
                    contentStream.beginText();
                    contentStream.newLineAtOffset(leftMargin + 350, yPosition);
                    contentStream.showText(truncateText(alert.getRuleName(), 15));
                    contentStream.endText();
                    
                    yPosition -= 15;
                }
            }
            
            document.save(filePath);
        }
        
        logger.info("Exported {} security alerts to PDF: {}", alerts.size(), filePath);
    }
    
    /**
     * Export system statistics to PDF report
     */
    public void exportStatisticsToPDF(Map<String, Object> statistics, String filePath) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                // Set up fonts
                PDType1Font titleFont = PDType1Font.HELVETICA_BOLD;
                PDType1Font headerFont = PDType1Font.HELVETICA_BOLD;
                PDType1Font bodyFont = PDType1Font.HELVETICA;
                
                float yPosition = 750;
                float leftMargin = 50;
                
                // Title
                contentStream.beginText();
                contentStream.setFont(titleFont, 16);
                contentStream.newLineAtOffset(leftMargin, yPosition);
                contentStream.showText("DarkEye System Statistics Report");
                contentStream.endText();
                
                yPosition -= 30;
                
                // Report metadata
                contentStream.beginText();
                contentStream.setFont(bodyFont, 10);
                contentStream.newLineAtOffset(leftMargin, yPosition);
                contentStream.showText("Generated on: " + LocalDateTime.now().format(DATE_FORMATTER));
                contentStream.endText();
                
                yPosition -= 40;
                
                // Statistics
                contentStream.setFont(headerFont, 12);
                contentStream.beginText();
                contentStream.newLineAtOffset(leftMargin, yPosition);
                contentStream.showText("System Statistics:");
                contentStream.endText();
                
                yPosition -= 30;
                contentStream.setFont(bodyFont, 10);
                
                for (Map.Entry<String, Object> entry : statistics.entrySet()) {
                    contentStream.beginText();
                    contentStream.newLineAtOffset(leftMargin, yPosition);
                    contentStream.showText(entry.getKey() + ": " + entry.getValue());
                    contentStream.endText();
                    yPosition -= 20;
                }
            }
            
            document.save(filePath);
        }
        
        logger.info("Exported system statistics to PDF: {}", filePath);
    }
    
    /**
     * Truncate text to fit in PDF column
     */
    private String truncateText(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        
        if (text.length() <= maxLength) {
            return text;
        }
        
        return text.substring(0, maxLength - 3) + "...";
    }
}
