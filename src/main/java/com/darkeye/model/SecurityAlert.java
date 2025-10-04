package com.darkeye.model;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * SecurityAlert model for detected threats or anomalies
 */
public class SecurityAlert {
    private String id;
    private LocalDateTime timestamp;
    private String severity;
    private String title;
    private String description;
    private String ruleName;
    private String logEntryId;
    private Map<String, Object> details;
    private boolean acknowledged;
    
    public SecurityAlert() {}
    
    public SecurityAlert(String id, LocalDateTime timestamp, String severity, String title, 
                String description, String ruleName, String logEntryId, Map<String, Object> details) {
        this.id = id;
        this.timestamp = timestamp;
        this.severity = severity;
        this.title = title;
        this.description = description;
        this.ruleName = ruleName;
        this.logEntryId = logEntryId;
        this.details = details;
        this.acknowledged = false;
    }
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getRuleName() { return ruleName; }
    public void setRuleName(String ruleName) { this.ruleName = ruleName; }
    
    public String getLogEntryId() { return logEntryId; }
    public void setLogEntryId(String logEntryId) { this.logEntryId = logEntryId; }
    
    public Map<String, Object> getDetails() { return details; }
    public void setDetails(Map<String, Object> details) { this.details = details; }
    
    public boolean isAcknowledged() { return acknowledged; }
    public void setAcknowledged(boolean acknowledged) { this.acknowledged = acknowledged; }
    
    @Override
    public String toString() {
        return String.format("SecurityAlert{id='%s', timestamp=%s, severity='%s', title='%s'}", 
                           id, timestamp, severity, title);
    }
}