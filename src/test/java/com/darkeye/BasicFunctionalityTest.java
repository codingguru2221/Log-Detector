package com.darkeye;

import com.darkeye.collectors.FileCollector;
import com.darkeye.detection.SimpleDetectionEngine;
import com.darkeye.model.SecurityAlert;
import com.darkeye.model.LogEntry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Basic functionality tests for DarkEye core components
 */
class BasicFunctionalityTest {
    
    private FileCollector fileCollector;
    private SimpleDetectionEngine detectionEngine;
    private List<LogEntry> collectedLogs;
    private List<SecurityAlert> generatedAlerts;
    
    @BeforeEach
    void setUp() {
        fileCollector = new FileCollector();
        detectionEngine = new SimpleDetectionEngine();
        collectedLogs = new ArrayList<>();
        generatedAlerts = new ArrayList<>();
        
        // Set up handlers
        fileCollector.setLogHandler(collectedLogs::add);
        detectionEngine.addAlertHandler(generatedAlerts::add);
    }
    
    @Test
    void testFileCollectorBasicFunctionality() throws Exception {
        // Create a temporary test log file
        Path tempFile = Files.createTempFile("test-log", ".log");
        Files.write(tempFile, List.of(
            "2024-10-04 10:30:15 ERROR Authentication failed for user admin",
            "2024-10-04 10:30:16 INFO User login successful",
            "2024-10-04 10:30:17 WARN Suspicious activity detected"
        ));
        
        // Collect logs
        fileCollector.start(tempFile.toString());
        
        // Wait a bit for processing
        Thread.sleep(100);
        
        // Verify logs were collected
        assertFalse(collectedLogs.isEmpty(), "Should have collected some logs");
        assertEquals(3, collectedLogs.size(), "Should have collected 3 log entries");
        
        // Verify log entry properties
        LogEntry firstLog = collectedLogs.get(0);
        assertNotNull(firstLog.getId());
        assertNotNull(firstLog.getTimestamp());
        assertNotNull(firstLog.getMessage());
        assertEquals("ERROR", firstLog.getSeverity());
        
        // Clean up
        Files.deleteIfExists(tempFile);
    }
    
    @Test
    void testDetectionEngineSuspiciousKeywords() {
        // Create a log entry with suspicious keyword
        LogEntry logEntry = new LogEntry();
        logEntry.setId("test-1");
        logEntry.setMessage("Security breach attempt detected");
        logEntry.setSrcIp("192.168.1.100");
        
        // Process through detection engine
        detectionEngine.processLogEntry(logEntry);
        
        // Verify alert was generated
        assertFalse(generatedAlerts.isEmpty(), "Should have generated an alert");
        SecurityAlert alert = generatedAlerts.get(0);
        assertEquals("SUSPICIOUS_KEYWORD", alert.getRuleName());
        assertEquals("MEDIUM", alert.getSeverity());
        assertTrue(alert.getTitle().contains("Suspicious keyword"));
    }
    
    @Test
    void testDetectionEngineBlacklistedIP() {
        // Create a log entry from blacklisted IP
        LogEntry logEntry = new LogEntry();
        logEntry.setId("test-2");
        logEntry.setMessage("Normal log entry");
        logEntry.setSrcIp("192.168.1.100"); // This IP is in the blacklist
        
        // Process through detection engine
        detectionEngine.processLogEntry(logEntry);
        
        // Verify alert was generated
        assertFalse(generatedAlerts.isEmpty(), "Should have generated an alert");
        SecurityAlert alert = generatedAlerts.get(0);
        assertEquals("BLACKLISTED_IP", alert.getRuleName());
        assertEquals("HIGH", alert.getSeverity());
        assertTrue(alert.getTitle().contains("Blacklisted IP"));
    }
    
    @Test
    void testDetectionEngineBruteForce() {
        // Simulate multiple failed login attempts from same IP
        for (int i = 0; i < 6; i++) {
            LogEntry logEntry = new LogEntry();
            logEntry.setId("test-" + i);
            logEntry.setMessage("Failed login attempt");
            logEntry.setSrcIp("192.168.1.200");
            
            detectionEngine.processLogEntry(logEntry);
        }
        
        // Verify brute force alert was generated
        assertFalse(generatedAlerts.isEmpty(), "Should have generated brute force alert");
        boolean foundBruteForceAlert = generatedAlerts.stream()
            .anyMatch(alert -> "BRUTE_FORCE".equals(alert.getRuleName()));
        assertTrue(foundBruteForceAlert, "Should have found brute force alert");
    }
    
    @Test
    void testLogEntryModel() {
        LogEntry logEntry = new LogEntry();
        logEntry.setId("test-id");
        logEntry.setSeverity("ERROR");
        logEntry.setMessage("Test message");
        logEntry.setSrcIp("192.168.1.1");
        
        assertEquals("test-id", logEntry.getId());
        assertEquals("ERROR", logEntry.getSeverity());
        assertEquals("Test message", logEntry.getMessage());
        assertEquals("192.168.1.1", logEntry.getSrcIp());
        
        assertNotNull(logEntry.toString());
    }
    
    @Test
    void testAlertModel() {
        SecurityAlert alert = new SecurityAlert();
        alert.setId("alert-1");
        alert.setSeverity("HIGH");
        alert.setTitle("Test Alert");
        alert.setDescription("This is a test alert");
        alert.setRuleName("TEST_RULE");
        
        assertEquals("alert-1", alert.getId());
        assertEquals("HIGH", alert.getSeverity());
        assertEquals("Test Alert", alert.getTitle());
        assertEquals("This is a test alert", alert.getDescription());
        assertEquals("TEST_RULE", alert.getRuleName());
        assertFalse(alert.isAcknowledged());
        
        assertNotNull(alert.toString());
    }
}
