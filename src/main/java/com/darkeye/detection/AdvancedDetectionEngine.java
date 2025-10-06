package com.darkeye.detection;

import com.darkeye.model.LogEntry;
import com.darkeye.model.SecurityAlert;
import org.jeasy.rules.api.Facts;
import org.jeasy.rules.api.Rule;
import org.jeasy.rules.api.Rules;
import org.jeasy.rules.api.RulesEngine;
import org.jeasy.rules.core.DefaultRulesEngine;
import org.jeasy.rules.mvel.MVELRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Advanced detection engine using Drools-like rules engine for comprehensive threat detection
 */
public class AdvancedDetectionEngine {
    
    private static final Logger logger = LoggerFactory.getLogger(AdvancedDetectionEngine.class);
    
    private final List<Consumer<SecurityAlert>> alertHandlers = new CopyOnWriteArrayList<>();
    private final RulesEngine rulesEngine;
    private final Rules rules;
    private final Map<String, Integer> ipCounts = new ConcurrentHashMap<>();
    private final Map<String, Integer> userCounts = new ConcurrentHashMap<>();
    private final Map<String, List<LocalDateTime>> ipTimestamps = new ConcurrentHashMap<>();
    private final Map<String, List<LocalDateTime>> userTimestamps = new ConcurrentHashMap<>();
    
    // Blacklisted IPs and suspicious patterns
    private final Set<String> blacklistedIPs = ConcurrentHashMap.newKeySet();
    private final Set<String> suspiciousKeywords = ConcurrentHashMap.newKeySet();
    private final Set<String> suspiciousUserAgents = ConcurrentHashMap.newKeySet();
    
    public AdvancedDetectionEngine() {
        this.rulesEngine = new DefaultRulesEngine();
        this.rules = new Rules();
        initializeDefaultRules();
        initializeDefaultData();
    }
    
    /**
     * Initialize default detection rules using MVEL expressions
     */
    private void initializeDefaultRules() {
        // Rule 1: Blacklisted IP Detection
        Rule blacklistedIPRule = new MVELRule()
            .name("Blacklisted IP Detection")
            .description("Detect traffic from known malicious IPs")
            .when("logEntry.srcIp != null && blacklistedIPs.contains(logEntry.srcIp)")
            .then("createAlert('BLACKLISTED_IP', 'HIGH', 'Blacklisted IP detected: ' + logEntry.srcIp, 'Source IP ' + logEntry.srcIp + ' is on the blacklist and attempted access.', logEntry)");
        
        // Rule 2: Suspicious Keywords Detection
        Rule suspiciousKeywordsRule = new MVELRule()
            .name("Suspicious Keywords Detection")
            .description("Detect suspicious keywords in log messages")
            .when("logEntry.message != null && suspiciousKeywords.stream().anyMatch(keyword -> logEntry.message.toLowerCase().contains(keyword.toLowerCase()))")
            .then("String keyword = suspiciousKeywords.stream().filter(k -> logEntry.message.toLowerCase().contains(k.toLowerCase())).findFirst().orElse('unknown'); createAlert('SUSPICIOUS_KEYWORD', 'MEDIUM', 'Suspicious keyword detected: ' + keyword, 'Log entry contains suspicious keyword: ' + keyword, logEntry)");
        
        // Rule 3: Brute Force Attack Detection
        Rule bruteForceRule = new MVELRule()
            .name("Brute Force Attack Detection")
            .description("Detect multiple failed login attempts from same IP")
            .when("logEntry.message != null && logEntry.message.toLowerCase().contains('failed') && logEntry.message.toLowerCase().contains('login') && logEntry.srcIp != null")
            .then("incrementIPCount(logEntry.srcIp); if (ipCounts.get(logEntry.srcIp) > 5) { createAlert('BRUTE_FORCE', 'HIGH', 'Possible brute force attack from ' + logEntry.srcIp, 'Multiple failed login attempts (' + ipCounts.get(logEntry.srcIp) + ') from IP: ' + logEntry.srcIp, logEntry); }");
        
        // Rule 4: Unusual User Activity Detection
        Rule unusualUserActivityRule = new MVELRule()
            .name("Unusual User Activity Detection")
            .description("Detect users with excessive log entries")
            .when("logEntry.username != null")
            .then("incrementUserCount(logEntry.username); if (userCounts.get(logEntry.username) > 100) { createAlert('UNUSUAL_USER_ACTIVITY', 'MEDIUM', 'Unusual activity from user: ' + logEntry.username, 'User ' + logEntry.username + ' has generated ' + userCounts.get(logEntry.username) + ' log entries', logEntry); }");
        
        // Rule 5: SQL Injection Detection
        Rule sqlInjectionRule = new MVELRule()
            .name("SQL Injection Detection")
            .description("Detect potential SQL injection attempts")
            .when("logEntry.message != null && (logEntry.message.toLowerCase().contains('union select') || logEntry.message.toLowerCase().contains('drop table') || logEntry.message.toLowerCase().contains('insert into') || logEntry.message.toLowerCase().contains('delete from'))")
            .then("createAlert('SQL_INJECTION', 'HIGH', 'Potential SQL injection attempt detected', 'Log entry contains SQL injection patterns: ' + logEntry.message, logEntry)");
        
        // Rule 6: XSS Attack Detection
        Rule xssRule = new MVELRule()
            .name("XSS Attack Detection")
            .description("Detect potential XSS attack attempts")
            .when("logEntry.message != null && (logEntry.message.toLowerCase().contains('<script>') || logEntry.message.toLowerCase().contains('javascript:') || logEntry.message.toLowerCase().contains('onload=') || logEntry.message.toLowerCase().contains('onerror='))")
            .then("createAlert('XSS_ATTACK', 'HIGH', 'Potential XSS attack detected', 'Log entry contains XSS attack patterns: ' + logEntry.message, logEntry)");
        
        // Rule 7: Port Scan Detection
        Rule portScanRule = new MVELRule()
            .name("Port Scan Detection")
            .description("Detect potential port scanning activity")
            .when("logEntry.message != null && logEntry.message.toLowerCase().contains('connection refused') && logEntry.srcIp != null")
            .then("addIPTimestamp(logEntry.srcIp); if (getIPConnectionAttempts(logEntry.srcIp) > 10) { createAlert('PORT_SCAN', 'MEDIUM', 'Potential port scan from ' + logEntry.srcIp, 'Multiple connection attempts from IP: ' + logEntry.srcIp, logEntry); }");
        
        // Rule 8: Suspicious User Agent Detection
        Rule suspiciousUserAgentRule = new MVELRule()
            .name("Suspicious User Agent Detection")
            .description("Detect suspicious or malicious user agents")
            .when("logEntry.metadata != null && logEntry.metadata.get('userAgent') != null && suspiciousUserAgents.stream().anyMatch(ua -> logEntry.metadata.get('userAgent').toString().toLowerCase().contains(ua.toLowerCase()))")
            .then("String userAgent = logEntry.metadata.get('userAgent').toString(); createAlert('SUSPICIOUS_USER_AGENT', 'MEDIUM', 'Suspicious user agent detected', 'User agent: ' + userAgent, logEntry)");
        
        // Rule 9: High Severity Error Detection
        Rule highSeverityRule = new MVELRule()
            .name("High Severity Error Detection")
            .description("Detect high severity errors that might indicate attacks")
            .when("logEntry.severity != null && (logEntry.severity.equals('ERROR') || logEntry.severity.equals('CRITICAL') || logEntry.severity.equals('FATAL'))")
            .then("createAlert('HIGH_SEVERITY_ERROR', 'MEDIUM', 'High severity error detected', 'Severity: ' + logEntry.severity + ', Message: ' + logEntry.message, logEntry)");
        
        // Rule 10: Unusual Time Pattern Detection
        Rule unusualTimePatternRule = new MVELRule()
            .name("Unusual Time Pattern Detection")
            .description("Detect activity during unusual hours")
            .when("logEntry.timestamp != null && (logEntry.timestamp.getHour() < 6 || logEntry.timestamp.getHour() > 22)")
            .then("createAlert('UNUSUAL_TIME_PATTERN', 'LOW', 'Activity during unusual hours', 'Log entry at unusual time: ' + logEntry.timestamp, logEntry)");
        
        // Register all rules
        rules.register(blacklistedIPRule);
        rules.register(suspiciousKeywordsRule);
        rules.register(bruteForceRule);
        rules.register(unusualUserActivityRule);
        rules.register(sqlInjectionRule);
        rules.register(xssRule);
        rules.register(portScanRule);
        rules.register(suspiciousUserAgentRule);
        rules.register(highSeverityRule);
        rules.register(unusualTimePatternRule);
        
        logger.info("Initialized {} detection rules", rules.size());
    }
    
    /**
     * Initialize default blacklists and patterns
     */
    private void initializeDefaultData() {
        // Default blacklisted IPs
        blacklistedIPs.addAll(Arrays.asList(
            "192.168.1.100", "10.0.0.50", "172.16.0.100",
            "203.0.113.1", "198.51.100.1", "192.0.2.1"
        ));
        
        // Default suspicious keywords
        suspiciousKeywords.addAll(Arrays.asList(
            "failed", "denied", "unauthorized", "attack", "malware", "virus",
            "hack", "breach", "intrusion", "exploit", "vulnerability",
            "injection", "payload", "backdoor", "trojan", "rootkit",
            "phishing", "spam", "botnet", "ddos", "ransomware"
        ));
        
        // Default suspicious user agents
        suspiciousUserAgents.addAll(Arrays.asList(
            "sqlmap", "nikto", "nmap", "masscan", "zap",
            "burp", "w3af", "nessus", "openvas", "metasploit"
        ));
    }
    
    /**
     * Add an alert handler
     */
    public void addAlertHandler(Consumer<SecurityAlert> handler) {
        alertHandlers.add(handler);
    }
    
    /**
     * Process a log entry through the rules engine
     */
    public void processLogEntry(LogEntry logEntry) {
        if (logEntry == null) {
            return;
        }
        
        try {
            // Create facts for the rules engine
            Facts facts = new Facts();
            facts.put("logEntry", logEntry);
            facts.put("blacklistedIPs", blacklistedIPs);
            facts.put("suspiciousKeywords", suspiciousKeywords);
            facts.put("suspiciousUserAgents", suspiciousUserAgents);
            facts.put("ipCounts", ipCounts);
            facts.put("userCounts", userCounts);
            facts.put("ipTimestamps", ipTimestamps);
            facts.put("userTimestamps", userTimestamps);
            
            // Add helper methods to facts
            facts.put("createAlert", (AlertCreator) this::createAlert);
            facts.put("incrementIPCount", (IPCounter) this::incrementIPCount);
            facts.put("incrementUserCount", (UserCounter) this::incrementUserCount);
            facts.put("addIPTimestamp", (IPTimestampAdder) this::addIPTimestamp);
            facts.put("getIPConnectionAttempts", (IPConnectionCounter) this::getIPConnectionAttempts);
            
            // Fire rules
            rulesEngine.fire(rules, facts);
            
        } catch (Exception e) {
            logger.error("Error processing log entry through rules engine", e);
        }
    }
    
    /**
     * Create and dispatch a security alert
     */
    private void createAlert(String ruleName, String severity, String title, String description, LogEntry logEntry) {
        SecurityAlert alert = new SecurityAlert();
        alert.setId(UUID.randomUUID().toString());
        alert.setTimestamp(LocalDateTime.now());
        alert.setSeverity(severity);
        alert.setTitle(title);
        alert.setDescription(description);
        alert.setRuleName(ruleName);
        alert.setLogEntryId(logEntry.getId());
        
        // Add details
        Map<String, Object> details = new HashMap<>();
        details.put("sourceIp", logEntry.getSrcIp());
        details.put("host", logEntry.getHost());
        details.put("source", logEntry.getSource());
        details.put("originalMessage", logEntry.getMessage());
        details.put("username", logEntry.getUsername());
        alert.setDetails(details);
        
        // Notify all handlers
        for (Consumer<SecurityAlert> handler : alertHandlers) {
            try {
                handler.accept(alert);
            } catch (Exception e) {
                logger.error("Error in alert handler", e);
            }
        }
    }
    
    /**
     * Increment IP count for brute force detection
     */
    private void incrementIPCount(String ip) {
        ipCounts.put(ip, ipCounts.getOrDefault(ip, 0) + 1);
    }
    
    /**
     * Increment user count for unusual activity detection
     */
    private void incrementUserCount(String username) {
        userCounts.put(username, userCounts.getOrDefault(username, 0) + 1);
    }
    
    /**
     * Add timestamp for IP connection attempts
     */
    private void addIPTimestamp(String ip) {
        ipTimestamps.computeIfAbsent(ip, k -> new ArrayList<>()).add(LocalDateTime.now());
        
        // Clean old timestamps (older than 1 hour)
        List<LocalDateTime> timestamps = ipTimestamps.get(ip);
        timestamps.removeIf(timestamp -> timestamp.isBefore(LocalDateTime.now().minusHours(1)));
    }
    
    /**
     * Get connection attempts for an IP in the last hour
     */
    private int getIPConnectionAttempts(String ip) {
        List<LocalDateTime> timestamps = ipTimestamps.get(ip);
        if (timestamps == null) {
            return 0;
        }
        
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        return (int) timestamps.stream()
            .filter(timestamp -> timestamp.isAfter(oneHourAgo))
            .count();
    }
    
    /**
     * Add IP to blacklist
     */
    public void addBlacklistedIP(String ip) {
        blacklistedIPs.add(ip);
        logger.info("Added IP to blacklist: {}", ip);
    }
    
    /**
     * Remove IP from blacklist
     */
    public void removeBlacklistedIP(String ip) {
        blacklistedIPs.remove(ip);
        logger.info("Removed IP from blacklist: {}", ip);
    }
    
    /**
     * Add suspicious keyword
     */
    public void addSuspiciousKeyword(String keyword) {
        suspiciousKeywords.add(keyword);
        logger.info("Added suspicious keyword: {}", keyword);
    }
    
    /**
     * Add suspicious user agent
     */
    public void addSuspiciousUserAgent(String userAgent) {
        suspiciousUserAgents.add(userAgent);
        logger.info("Added suspicious user agent: {}", userAgent);
    }
    
    /**
     * Reset counters (useful for testing or periodic cleanup)
     */
    public void resetCounters() {
        ipCounts.clear();
        userCounts.clear();
        ipTimestamps.clear();
        userTimestamps.clear();
        logger.info("Reset all detection counters");
    }
    
    /**
     * Get current statistics
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("blacklistedIPs", blacklistedIPs.size());
        stats.put("suspiciousKeywords", suspiciousKeywords.size());
        stats.put("suspiciousUserAgents", suspiciousUserAgents.size());
        stats.put("activeIPCounts", ipCounts.size());
        stats.put("activeUserCounts", userCounts.size());
        stats.put("totalRules", rules.size());
        return stats;
    }
    
    // Functional interfaces for MVEL expressions
    @FunctionalInterface
    public interface AlertCreator {
        void createAlert(String ruleName, String severity, String title, String description, LogEntry logEntry);
    }
    
    @FunctionalInterface
    public interface IPCounter {
        void incrementIPCount(String ip);
    }
    
    @FunctionalInterface
    public interface UserCounter {
        void incrementUserCount(String username);
    }
    
    @FunctionalInterface
    public interface IPTimestampAdder {
        void addIPTimestamp(String ip);
    }
    
    @FunctionalInterface
    public interface IPConnectionCounter {
        int getIPConnectionAttempts(String ip);
    }
}
