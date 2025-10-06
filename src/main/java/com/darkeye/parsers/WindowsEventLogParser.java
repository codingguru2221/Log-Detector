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
 * Windows Event Log parser for security, system, and application logs
 */
public class WindowsEventLogParser implements LogParser {
    
    // Windows Event Log format: Date Time Level Source EventID Task Category Message
    private static final Pattern WINDOWS_EVENT_PATTERN = Pattern.compile(
        "^(\\d{4}-\\d{2}-\\d{2})\\s+(\\d{2}:\\d{2}:\\d{2})\\s+(\\w+)\\s+(\\S+)\\s+(\\d+)\\s+(\\S+)\\s+(\\S+)\\s+(.*)$"
    );
    
    // Alternative format with more details
    private static final Pattern WINDOWS_EVENT_DETAILED_PATTERN = Pattern.compile(
        "^(\\d{4}-\\d{2}-\\d{2})\\s+(\\d{2}:\\d{2}:\\d{2})\\s+(\\w+)\\s+(\\S+)\\s+(\\d+)\\s+(\\S+)\\s+(\\S+)\\s+(\\S+)\\s+(.*)$"
    );
    
    private static final DateTimeFormatter WINDOWS_DATE_FORMAT = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    @Override
    public LogEntry parseLine(String line, String source, int lineNumber) {
        if (line == null || line.trim().isEmpty()) {
            return null;
        }
        
        Matcher detailedMatcher = WINDOWS_EVENT_DETAILED_PATTERN.matcher(line);
        Matcher standardMatcher = WINDOWS_EVENT_PATTERN.matcher(line);
        
        if (detailedMatcher.matches()) {
            return parseDetailedEvent(detailedMatcher, source, lineNumber);
        } else if (standardMatcher.matches()) {
            return parseStandardEvent(standardMatcher, source, lineNumber);
        }
        
        return null;
    }
    
    private LogEntry parseStandardEvent(Matcher matcher, String source, int lineNumber) {
        LogEntry entry = new LogEntry();
        entry.setId(UUID.randomUUID().toString());
        
        // Extract fields
        String dateStr = matcher.group(1);
        String timeStr = matcher.group(2);
        String level = matcher.group(3);
        String eventSource = matcher.group(4);
        String eventIdStr = matcher.group(5);
        String task = matcher.group(6);
        String category = matcher.group(7);
        String message = matcher.group(8);
        
        // Parse timestamp
        try {
            LocalDateTime timestamp = LocalDateTime.parse(dateStr + " " + timeStr, WINDOWS_DATE_FORMAT);
            entry.setTimestamp(timestamp);
        } catch (Exception e) {
            entry.setTimestamp(LocalDateTime.now());
        }
        
        // Set basic fields
        entry.setSource(source);
        entry.setHost("windows");
        entry.setSeverity(level.toUpperCase());
        entry.setEventType("WINDOWS_EVENT");
        entry.setMessage(message);
        
        // Add metadata
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("eventSource", eventSource);
        metadata.put("eventId", eventIdStr);
        metadata.put("task", task);
        metadata.put("category", category);
        metadata.put("lineNumber", lineNumber);
        entry.setMetadata(metadata);
        
        return entry;
    }
    
    private LogEntry parseDetailedEvent(Matcher matcher, String source, int lineNumber) {
        LogEntry entry = new LogEntry();
        entry.setId(UUID.randomUUID().toString());
        
        // Extract fields
        String dateStr = matcher.group(1);
        String timeStr = matcher.group(2);
        String level = matcher.group(3);
        String eventSource = matcher.group(4);
        String eventIdStr = matcher.group(5);
        String task = matcher.group(6);
        String category = matcher.group(7);
        String keywords = matcher.group(8);
        String message = matcher.group(9);
        
        // Parse timestamp
        try {
            LocalDateTime timestamp = LocalDateTime.parse(dateStr + " " + timeStr, WINDOWS_DATE_FORMAT);
            entry.setTimestamp(timestamp);
        } catch (Exception e) {
            entry.setTimestamp(LocalDateTime.now());
        }
        
        // Set basic fields
        entry.setSource(source);
        entry.setHost("windows");
        entry.setSeverity(level.toUpperCase());
        entry.setEventType("WINDOWS_EVENT");
        entry.setMessage(message);
        
        // Add metadata
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("eventSource", eventSource);
        metadata.put("eventId", eventIdStr);
        metadata.put("task", task);
        metadata.put("category", category);
        metadata.put("keywords", keywords);
        metadata.put("lineNumber", lineNumber);
        entry.setMetadata(metadata);
        
        return entry;
    }
    
    @Override
    public boolean canParse(String sampleLine) {
        if (sampleLine == null || sampleLine.trim().isEmpty()) {
            return false;
        }
        
        return WINDOWS_EVENT_DETAILED_PATTERN.matcher(sampleLine).matches() || 
               WINDOWS_EVENT_PATTERN.matcher(sampleLine).matches();
    }
    
    @Override
    public String getParserName() {
        return "Windows Event Log Parser";
    }
}
