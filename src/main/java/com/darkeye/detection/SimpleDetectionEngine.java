package com.darkeye.detection;

import com.darkeye.model.SecurityAlert;
import com.darkeye.model.LogEntry;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * Simple detection engine for basic threat detection
 */
public class SimpleDetectionEngine {
    private final List<Consumer<SecurityAlert>> alertHandlers = new CopyOnWriteArrayList<>();
    private final Map<String, Integer> ipCounts = new HashMap<>();
    private final Map<String, Integer> userCounts = new HashMap<>();
    private final List<String> suspiciousKeywords = Arrays.asList(
        "failed", "denied", "unauthorized", "attack", "malware", "virus", 
        "hack", "breach", "intrusion", "exploit", "vulnerability"
    );
    
    // Simple blacklist of suspicious IPs (in real app, this would be loaded from file)
    private final Set<String> blacklistedIPs = new HashSet<>(Arrays.asList(
        "192.168.1.100", "10.0.0.50" // Example blacklisted IPs
    ));
    
    public SimpleDetectionEngine() {}
    
    /**
     * Add an alert handler
     */
    public void addAlertHandler(Consumer<SecurityAlert> handler) {
        alertHandlers.add(handler);
    }
    
    /**
     * Process a log entry and check for threats
     */
    public void processLogEntry(LogEntry logEntry) {
        if (logEntry == null) return;
        
        // Check for blacklisted IPs
        checkBlacklistedIPs(logEntry);
        
        // Check for suspicious keywords
        checkSuspiciousKeywords(logEntry);
        
        // Check for brute force attempts
        checkBruteForceAttempts(logEntry);
        
        // Check for unusual activity patterns
        checkUnusualActivity(logEntry);
    }
    
    private void checkBlacklistedIPs(LogEntry logEntry) {
        if (logEntry.getSrcIp() != null && blacklistedIPs.contains(logEntry.getSrcIp())) {
            createAlert("BLACKLISTED_IP", "HIGH", 
                "Blacklisted IP detected: " + logEntry.getSrcIp(),
                "Source IP " + logEntry.getSrcIp() + " is on the blacklist and attempted access.",
                logEntry);
        }
    }
    
    private void checkSuspiciousKeywords(LogEntry logEntry) {
        String message = logEntry.getMessage().toLowerCase();
        for (String keyword : suspiciousKeywords) {
            if (message.contains(keyword)) {
                createAlert("SUSPICIOUS_KEYWORD", "MEDIUM",
                    "Suspicious keyword detected: " + keyword,
                    "Log entry contains suspicious keyword: " + keyword,
                    logEntry);
                break; // Only create one alert per log entry
            }
        }
    }
    
    private void checkBruteForceAttempts(LogEntry logEntry) {
        // Count failed login attempts by IP
        if (logEntry.getMessage().toLowerCase().contains("failed") && 
            logEntry.getMessage().toLowerCase().contains("login")) {
            
            String ip = logEntry.getSrcIp();
            if (ip != null) {
                ipCounts.put(ip, ipCounts.getOrDefault(ip, 0) + 1);
                
                // Alert if more than 5 failed attempts from same IP
                if (ipCounts.get(ip) > 5) {
                    createAlert("BRUTE_FORCE", "HIGH",
                        "Possible brute force attack from " + ip,
                        "Multiple failed login attempts (" + ipCounts.get(ip) + ") from IP: " + ip,
                        logEntry);
                }
            }
        }
    }
    
    private void checkUnusualActivity(LogEntry logEntry) {
        // Check for unusual user activity
        if (logEntry.getUsername() != null) {
            String username = logEntry.getUsername();
            userCounts.put(username, userCounts.getOrDefault(username, 0) + 1);
            
            // Alert if user has more than 100 log entries in short time (simplified)
            if (userCounts.get(username) > 100) {
                createAlert("UNUSUAL_USER_ACTIVITY", "MEDIUM",
                    "Unusual activity from user: " + username,
                    "User " + username + " has generated " + userCounts.get(username) + " log entries",
                    logEntry);
            }
        }
    }
    
    private void createAlert(String ruleName, String severity, String title, String description, LogEntry logEntry) {
        SecurityAlert alert = new SecurityAlert();
        alert.setId(UUID.randomUUID().toString());
        alert.setTimestamp(LocalDateTime.now());
        alert.setSeverity(severity);
        alert.setTitle(title);
        alert.setDescription(description);
        alert.setRuleName(ruleName);
        alert.setLogEntryId(logEntry.getId());
        
        // Add some details
        Map<String, Object> details = new HashMap<>();
        details.put("sourceIp", logEntry.getSrcIp());
        details.put("host", logEntry.getHost());
        details.put("source", logEntry.getSource());
        details.put("originalMessage", logEntry.getMessage());
        alert.setDetails(details);
        
        // Notify all handlers
        for (Consumer<SecurityAlert> handler : alertHandlers) {
            try {
                handler.accept(alert);
            } catch (Exception e) {
                System.err.println("Error in alert handler: " + e.getMessage());
            }
        }
    }
    
    /**
     * Reset counters (useful for testing or periodic cleanup)
     */
    public void resetCounters() {
        ipCounts.clear();
        userCounts.clear();
    }
    
    /**
     * Add IP to blacklist
     */
    public void addBlacklistedIP(String ip) {
        blacklistedIPs.add(ip);
    }
    
    /**
     * Remove IP from blacklist
     */
    public void removeBlacklistedIP(String ip) {
        blacklistedIPs.remove(ip);
    }
}
