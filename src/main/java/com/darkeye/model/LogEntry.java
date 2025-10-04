package com.darkeye.model;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Basic log entry model for DarkEye
 */
public class LogEntry {
    private String id;
    private LocalDateTime timestamp;
    private String source;
    private String host;
    private String severity;
    private String eventType;
    private String srcIp;
    private String dstIp;
    private String username;
    private String message;
    private Map<String, Object> metadata;
    
    public LogEntry() {}
    
    public LogEntry(String id, LocalDateTime timestamp, String source, String host, 
                   String severity, String eventType, String srcIp, String dstIp, 
                   String username, String message, Map<String, Object> metadata) {
        this.id = id;
        this.timestamp = timestamp;
        this.source = source;
        this.host = host;
        this.severity = severity;
        this.eventType = eventType;
        this.srcIp = srcIp;
        this.dstIp = dstIp;
        this.username = username;
        this.message = message;
        this.metadata = metadata;
    }
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    
    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }
    
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    
    public String getSrcIp() { return srcIp; }
    public void setSrcIp(String srcIp) { this.srcIp = srcIp; }
    
    public String getDstIp() { return dstIp; }
    public void setDstIp(String dstIp) { this.dstIp = dstIp; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    
    @Override
    public String toString() {
        return String.format("LogEntry{id='%s', timestamp=%s, source='%s', severity='%s', message='%s'}", 
                           id, timestamp, source, severity, message);
    }
}
