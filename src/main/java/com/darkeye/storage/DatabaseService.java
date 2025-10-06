package com.darkeye.storage;

import com.darkeye.model.LogEntry;
import com.darkeye.model.SecurityAlert;
import com.darkeye.security.EncryptionService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * SQLite database service with AES-256 encryption for log storage
 */
public class DatabaseService {
    
    private static final Logger logger = LoggerFactory.getLogger(DatabaseService.class);
    
    private static final String DB_URL = "jdbc:sqlite:darkeye.db";
    private static final String ENCRYPTION_KEY_FILE = "darkeye.key";
    
    private final EncryptionService encryptionService;
    private final ObjectMapper objectMapper;
    private Connection connection;
    
    public DatabaseService() {
        this.encryptionService = new EncryptionService();
        this.objectMapper = new ObjectMapper();
        initializeDatabase();
    }
    
    public DatabaseService(String encryptionKey) {
        this.encryptionService = new EncryptionService(encryptionKey);
        this.objectMapper = new ObjectMapper();
        initializeDatabase();
    }
    
    /**
     * Initialize the database and create tables
     */
    private void initializeDatabase() {
        try {
            connection = DriverManager.getConnection(DB_URL);
            createTables();
            logger.info("Database initialized successfully");
        } catch (SQLException e) {
            logger.error("Failed to initialize database", e);
            throw new RuntimeException("Database initialization failed", e);
        }
    }
    
    /**
     * Create necessary tables
     */
    private void createTables() throws SQLException {
        // Log entries table
        String createLogEntriesTable = """
            CREATE TABLE IF NOT EXISTS log_entries (
                id TEXT PRIMARY KEY,
                timestamp TEXT NOT NULL,
                source TEXT NOT NULL,
                host TEXT,
                severity TEXT,
                event_type TEXT,
                src_ip TEXT,
                dst_ip TEXT,
                username TEXT,
                message_encrypted TEXT NOT NULL,
                metadata_encrypted TEXT,
                created_at TEXT DEFAULT CURRENT_TIMESTAMP
            )
            """;
        
        // Security alerts table
        String createAlertsTable = """
            CREATE TABLE IF NOT EXISTS security_alerts (
                id TEXT PRIMARY KEY,
                timestamp TEXT NOT NULL,
                severity TEXT NOT NULL,
                title TEXT NOT NULL,
                description TEXT NOT NULL,
                rule_name TEXT NOT NULL,
                log_entry_id TEXT,
                details_encrypted TEXT,
                acknowledged BOOLEAN DEFAULT FALSE,
                created_at TEXT DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (log_entry_id) REFERENCES log_entries (id)
            )
            """;
        
        // System configuration table
        String createConfigTable = """
            CREATE TABLE IF NOT EXISTS system_config (
                key TEXT PRIMARY KEY,
                value_encrypted TEXT NOT NULL,
                updated_at TEXT DEFAULT CURRENT_TIMESTAMP
            )
            """;
        
        // Create indexes for better performance
        String createIndexes = """
            CREATE INDEX IF NOT EXISTS idx_log_entries_timestamp ON log_entries(timestamp);
            CREATE INDEX IF NOT EXISTS idx_log_entries_severity ON log_entries(severity);
            CREATE INDEX IF NOT EXISTS idx_log_entries_src_ip ON log_entries(src_ip);
            CREATE INDEX IF NOT EXISTS idx_alerts_timestamp ON security_alerts(timestamp);
            CREATE INDEX IF NOT EXISTS idx_alerts_severity ON security_alerts(severity);
            CREATE INDEX IF NOT EXISTS idx_alerts_acknowledged ON security_alerts(acknowledged);
            """;
        
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createLogEntriesTable);
            stmt.execute(createAlertsTable);
            stmt.execute(createConfigTable);
            stmt.execute(createIndexes);
        }
    }
    
    /**
     * Store a log entry in encrypted form
     */
    public void storeLogEntry(LogEntry logEntry) {
        String sql = """
            INSERT INTO log_entries 
            (id, timestamp, source, host, severity, event_type, src_ip, dst_ip, username, message_encrypted, metadata_encrypted)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, logEntry.getId());
            pstmt.setString(2, logEntry.getTimestamp().toString());
            pstmt.setString(3, logEntry.getSource());
            pstmt.setString(4, logEntry.getHost());
            pstmt.setString(5, logEntry.getSeverity());
            pstmt.setString(6, logEntry.getEventType());
            pstmt.setString(7, logEntry.getSrcIp());
            pstmt.setString(8, logEntry.getDstIp());
            pstmt.setString(9, logEntry.getUsername());
            
            // Encrypt sensitive data
            String encryptedMessage = encryptionService.encrypt(logEntry.getMessage());
            pstmt.setString(10, encryptedMessage);
            
            if (logEntry.getMetadata() != null) {
                String metadataJson = objectMapper.writeValueAsString(logEntry.getMetadata());
                String encryptedMetadata = encryptionService.encrypt(metadataJson);
                pstmt.setString(11, encryptedMetadata);
            } else {
                pstmt.setString(11, null);
            }
            
            pstmt.executeUpdate();
        } catch (SQLException | JsonProcessingException e) {
            logger.error("Failed to store log entry", e);
            throw new RuntimeException("Failed to store log entry", e);
        }
    }
    
    /**
     * Store a security alert
     */
    public void storeSecurityAlert(SecurityAlert alert) {
        String sql = """
            INSERT INTO security_alerts 
            (id, timestamp, severity, title, description, rule_name, log_entry_id, details_encrypted, acknowledged)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, alert.getId());
            pstmt.setString(2, alert.getTimestamp().toString());
            pstmt.setString(3, alert.getSeverity());
            pstmt.setString(4, alert.getTitle());
            pstmt.setString(5, alert.getDescription());
            pstmt.setString(6, alert.getRuleName());
            pstmt.setString(7, alert.getLogEntryId());
            
            if (alert.getDetails() != null) {
                String detailsJson = objectMapper.writeValueAsString(alert.getDetails());
                String encryptedDetails = encryptionService.encrypt(detailsJson);
                pstmt.setString(8, encryptedDetails);
            } else {
                pstmt.setString(8, null);
            }
            
            pstmt.setBoolean(9, alert.isAcknowledged());
            pstmt.executeUpdate();
        } catch (SQLException | JsonProcessingException e) {
            logger.error("Failed to store security alert", e);
            throw new RuntimeException("Failed to store security alert", e);
        }
    }
    
    /**
     * Retrieve log entries with optional filters
     */
    public List<LogEntry> getLogEntries(LocalDateTime from, LocalDateTime to, String severity, String source, int limit) {
        List<LogEntry> entries = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM log_entries WHERE 1=1");
        List<Object> params = new ArrayList<>();
        
        if (from != null) {
            sql.append(" AND timestamp >= ?");
            params.add(from.toString());
        }
        
        if (to != null) {
            sql.append(" AND timestamp <= ?");
            params.add(to.toString());
        }
        
        if (severity != null) {
            sql.append(" AND severity = ?");
            params.add(severity);
        }
        
        if (source != null) {
            sql.append(" AND source = ?");
            params.add(source);
        }
        
        sql.append(" ORDER BY timestamp DESC");
        
        if (limit > 0) {
            sql.append(" LIMIT ?");
            params.add(limit);
        }
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    LogEntry entry = mapResultSetToLogEntry(rs);
                    entries.add(entry);
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to retrieve log entries", e);
            throw new RuntimeException("Failed to retrieve log entries", e);
        }
        
        return entries;
    }
    
    /**
     * Retrieve security alerts
     */
    public List<SecurityAlert> getSecurityAlerts(LocalDateTime from, LocalDateTime to, String severity, boolean acknowledged, int limit) {
        List<SecurityAlert> alerts = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM security_alerts WHERE 1=1");
        List<Object> params = new ArrayList<>();
        
        if (from != null) {
            sql.append(" AND timestamp >= ?");
            params.add(from.toString());
        }
        
        if (to != null) {
            sql.append(" AND timestamp <= ?");
            params.add(to.toString());
        }
        
        if (severity != null) {
            sql.append(" AND severity = ?");
            params.add(severity);
        }
        
        sql.append(" AND acknowledged = ?");
        params.add(acknowledged);
        
        sql.append(" ORDER BY timestamp DESC");
        
        if (limit > 0) {
            sql.append(" LIMIT ?");
            params.add(limit);
        }
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    SecurityAlert alert = mapResultSetToSecurityAlert(rs);
                    alerts.add(alert);
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to retrieve security alerts", e);
            throw new RuntimeException("Failed to retrieve security alerts", e);
        }
        
        return alerts;
    }
    
    /**
     * Acknowledge a security alert
     */
    public boolean acknowledgeAlert(String alertId) {
        String sql = "UPDATE security_alerts SET acknowledged = TRUE WHERE id = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, alertId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Failed to acknowledge alert", e);
            return false;
        }
    }
    
    /**
     * Get statistics for dashboard
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            // Total log entries
            String totalLogsSql = "SELECT COUNT(*) as count FROM log_entries";
            try (PreparedStatement pstmt = connection.prepareStatement(totalLogsSql);
                 ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    stats.put("totalLogs", rs.getInt("count"));
                }
            }
            
            // Total alerts
            String totalAlertsSql = "SELECT COUNT(*) as count FROM security_alerts";
            try (PreparedStatement pstmt = connection.prepareStatement(totalAlertsSql);
                 ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    stats.put("totalAlerts", rs.getInt("count"));
                }
            }
            
            // Unacknowledged alerts
            String unackAlertsSql = "SELECT COUNT(*) as count FROM security_alerts WHERE acknowledged = FALSE";
            try (PreparedStatement pstmt = connection.prepareStatement(unackAlertsSql);
                 ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    stats.put("unacknowledgedAlerts", rs.getInt("count"));
                }
            }
            
            // Alerts by severity
            String severitySql = "SELECT severity, COUNT(*) as count FROM security_alerts GROUP BY severity";
            try (PreparedStatement pstmt = connection.prepareStatement(severitySql);
                 ResultSet rs = pstmt.executeQuery()) {
                Map<String, Integer> severityCounts = new HashMap<>();
                while (rs.next()) {
                    severityCounts.put(rs.getString("severity"), rs.getInt("count"));
                }
                stats.put("alertsBySeverity", severityCounts);
            }
            
        } catch (SQLException e) {
            logger.error("Failed to get statistics", e);
        }
        
        return stats;
    }
    
    /**
     * Map ResultSet to LogEntry
     */
    private LogEntry mapResultSetToLogEntry(ResultSet rs) throws SQLException {
        LogEntry entry = new LogEntry();
        entry.setId(rs.getString("id"));
        entry.setTimestamp(LocalDateTime.parse(rs.getString("timestamp")));
        entry.setSource(rs.getString("source"));
        entry.setHost(rs.getString("host"));
        entry.setSeverity(rs.getString("severity"));
        entry.setEventType(rs.getString("event_type"));
        entry.setSrcIp(rs.getString("src_ip"));
        entry.setDstIp(rs.getString("dst_ip"));
        entry.setUsername(rs.getString("username"));
        
        // Decrypt message
        String encryptedMessage = rs.getString("message_encrypted");
        if (encryptedMessage != null) {
            entry.setMessage(encryptionService.decrypt(encryptedMessage));
        }
        
        // Decrypt metadata
        String encryptedMetadata = rs.getString("metadata_encrypted");
        if (encryptedMetadata != null) {
            try {
                String metadataJson = encryptionService.decrypt(encryptedMetadata);
                Map<String, Object> metadata = objectMapper.readValue(metadataJson, Map.class);
                entry.setMetadata(metadata);
            } catch (Exception e) {
                logger.warn("Failed to decrypt metadata for log entry: " + entry.getId());
            }
        }
        
        return entry;
    }
    
    /**
     * Map ResultSet to SecurityAlert
     */
    private SecurityAlert mapResultSetToSecurityAlert(ResultSet rs) throws SQLException {
        SecurityAlert alert = new SecurityAlert();
        alert.setId(rs.getString("id"));
        alert.setTimestamp(LocalDateTime.parse(rs.getString("timestamp")));
        alert.setSeverity(rs.getString("severity"));
        alert.setTitle(rs.getString("title"));
        alert.setDescription(rs.getString("description"));
        alert.setRuleName(rs.getString("rule_name"));
        alert.setLogEntryId(rs.getString("log_entry_id"));
        alert.setAcknowledged(rs.getBoolean("acknowledged"));
        
        // Decrypt details
        String encryptedDetails = rs.getString("details_encrypted");
        if (encryptedDetails != null) {
            try {
                String detailsJson = encryptionService.decrypt(encryptedDetails);
                Map<String, Object> details = objectMapper.readValue(detailsJson, Map.class);
                alert.setDetails(details);
            } catch (Exception e) {
                logger.warn("Failed to decrypt details for alert: " + alert.getId());
            }
        }
        
        return alert;
    }
    
    /**
     * Close the database connection
     */
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            logger.error("Failed to close database connection", e);
        }
    }
    
    /**
     * Get the encryption key for backup
     */
    public String getEncryptionKey() {
        return encryptionService.getKeyAsBase64();
    }
}
