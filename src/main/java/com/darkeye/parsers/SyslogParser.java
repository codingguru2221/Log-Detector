package com.darkeye.parsers;

import com.darkeye.model.LogEntry;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Syslog parser supporting RFC 3164 and RFC 5424 formats
 */
public class SyslogParser implements LogParser {
    
    // RFC 3164 format: <priority>timestamp hostname tag: message
    private static final Pattern RFC3164_PATTERN = Pattern.compile(
        "^<(\\d+)>(\\w{3}\\s+\\d{1,2}\\s+\\d{2}:\\d{2}:\\d{2})\\s+(\\S+)\\s+(\\S+):\\s*(.*)$"
    );
    
    // RFC 5424 format: <priority>version timestamp hostname app-name procid msgid structured-data message
    private static final Pattern RFC5424_PATTERN = Pattern.compile(
        "^<(\\d+)>(\\d+)\\s+(\\S+)\\s+(\\S+)\\s+(\\S+)\\s+(\\S+)\\s+(\\S+)\\s+(\\S*)\\s*(.*)$"
    );
    
    private static final DateTimeFormatter SYSLOG_DATE_FORMAT = 
        DateTimeFormatter.ofPattern("MMM d HH:mm:ss");
    
    @Override
    public LogEntry parseLine(String line, String source, int lineNumber) {
        if (line == null || line.trim().isEmpty()) {
            return null;
        }
        
        Matcher rfc3164Matcher = RFC3164_PATTERN.matcher(line);
        Matcher rfc5424Matcher = RFC5424_PATTERN.matcher(line);
        
        if (rfc3164Matcher.matches()) {
            return parseRFC3164(rfc3164Matcher, source, lineNumber);
        } else if (rfc5424Matcher.matches()) {
            return parseRFC5424(rfc5424Matcher, source, lineNumber);
        }
        
        return null;
    }
    
    private LogEntry parseRFC3164(Matcher matcher, String source, int lineNumber) {
        LogEntry entry = new LogEntry();
        entry.setId(UUID.randomUUID().toString());
        
        // Extract fields
        String priorityStr = matcher.group(1);
        String timestampStr = matcher.group(2);
        String hostname = matcher.group(3);
        String tag = matcher.group(4);
        String message = matcher.group(5);
        
        // Parse priority
        int priority = Integer.parseInt(priorityStr);
        int facility = priority / 8;
        int severity = priority % 8;
        
        // Parse timestamp (add current year as syslog doesn't include it)
        try {
            String currentYear = String.valueOf(LocalDateTime.now().getYear());
            LocalDateTime timestamp = LocalDateTime.parse(currentYear + " " + timestampStr, 
                DateTimeFormatter.ofPattern("yyyy MMM d HH:mm:ss"));
            entry.setTimestamp(timestamp);
        } catch (Exception e) {
            entry.setTimestamp(LocalDateTime.now());
        }
        
        // Set basic fields
        entry.setSource(source);
        entry.setHost(hostname);
        entry.setSeverity(getSeverityName(severity));
        entry.setEventType("SYSLOG");
        entry.setMessage(message);
        
        // Add metadata
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("facility", facility);
        metadata.put("severity", severity);
        metadata.put("tag", tag);
        metadata.put("priority", priority);
        metadata.put("lineNumber", lineNumber);
        entry.setMetadata(metadata);
        
        return entry;
    }
    
    private LogEntry parseRFC5424(Matcher matcher, String source, int lineNumber) {
        LogEntry entry = new LogEntry();
        entry.setId(UUID.randomUUID().toString());
        
        // Extract fields
        String priorityStr = matcher.group(1);
        String version = matcher.group(2);
        String timestampStr = matcher.group(3);
        String hostname = matcher.group(4);
        String appName = matcher.group(5);
        String procId = matcher.group(6);
        String msgId = matcher.group(7);
        String structuredData = matcher.group(8);
        String message = matcher.group(9);
        
        // Parse priority
        int priority = Integer.parseInt(priorityStr);
        int facility = priority / 8;
        int severity = priority % 8;
        
        // Parse timestamp (RFC 5424 uses ISO 8601 format)
        try {
            entry.setTimestamp(LocalDateTime.parse(timestampStr));
        } catch (Exception e) {
            entry.setTimestamp(LocalDateTime.now());
        }
        
        // Set basic fields
        entry.setSource(source);
        entry.setHost(hostname);
        entry.setSeverity(getSeverityName(severity));
        entry.setEventType("SYSLOG");
        entry.setMessage(message);
        
        // Add metadata
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("facility", facility);
        metadata.put("severity", severity);
        metadata.put("version", version);
        metadata.put("appName", appName);
        metadata.put("procId", procId);
        metadata.put("msgId", msgId);
        metadata.put("structuredData", structuredData);
        metadata.put("priority", priority);
        metadata.put("lineNumber", lineNumber);
        entry.setMetadata(metadata);
        
        return entry;
    }
    
    private String getSeverityName(int severity) {
        switch (severity) {
            case 0: return "EMERGENCY";
            case 1: return "ALERT";
            case 2: return "CRITICAL";
            case 3: return "ERROR";
            case 4: return "WARNING";
            case 5: return "NOTICE";
            case 6: return "INFO";
            case 7: return "DEBUG";
            default: return "UNKNOWN";
        }
    }
    
    @Override
    public boolean canParse(String sampleLine) {
        if (sampleLine == null || sampleLine.trim().isEmpty()) {
            return false;
        }
        
        return RFC3164_PATTERN.matcher(sampleLine).matches() || 
               RFC5424_PATTERN.matcher(sampleLine).matches();
    }
    
    @Override
    public String getParserName() {
        return "Syslog Parser";
    }
}
