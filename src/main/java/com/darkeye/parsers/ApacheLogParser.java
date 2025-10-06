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
 * Apache access log parser supporting Common Log Format and Combined Log Format
 */
public class ApacheLogParser implements LogParser {
    
    // Common Log Format: IP - - [timestamp] "method path protocol" status size
    private static final Pattern COMMON_LOG_PATTERN = Pattern.compile(
        "^(\\S+) (\\S+) (\\S+) \\[([^\\]]+)\\] \"(\\S+) ([^\"]*)\" (\\d+) (\\d+|-)"
    );
    
    // Combined Log Format: adds referer and user-agent
    private static final Pattern COMBINED_LOG_PATTERN = Pattern.compile(
        "^(\\S+) (\\S+) (\\S+) \\[([^\\]]+)\\] \"(\\S+) ([^\"]*)\" (\\d+) (\\d+|-) \"([^\"]*)\" \"([^\"]*)\""
    );
    
    private static final DateTimeFormatter APACHE_DATE_FORMAT = 
        DateTimeFormatter.ofPattern("dd/MMM/yyyy:HH:mm:ss Z");
    
    @Override
    public LogEntry parseLine(String line, String source, int lineNumber) {
        if (line == null || line.trim().isEmpty()) {
            return null;
        }
        
        Matcher combinedMatcher = COMBINED_LOG_PATTERN.matcher(line);
        Matcher commonMatcher = COMMON_LOG_PATTERN.matcher(line);
        
        if (combinedMatcher.matches()) {
            return parseCombinedLog(combinedMatcher, source, lineNumber);
        } else if (commonMatcher.matches()) {
            return parseCommonLog(commonMatcher, source, lineNumber);
        }
        
        return null;
    }
    
    private LogEntry parseCombinedLog(Matcher matcher, String source, int lineNumber) {
        LogEntry entry = new LogEntry();
        entry.setId(UUID.randomUUID().toString());
        
        // Extract fields
        String clientIp = matcher.group(1);
        String ident = matcher.group(2);
        String authUser = matcher.group(3);
        String timestampStr = matcher.group(4);
        String method = matcher.group(5);
        String path = matcher.group(6);
        String statusCode = matcher.group(7);
        String size = matcher.group(8);
        String referer = matcher.group(9);
        String userAgent = matcher.group(10);
        
        // Parse timestamp
        try {
            LocalDateTime timestamp = LocalDateTime.parse(timestampStr, APACHE_DATE_FORMAT);
            entry.setTimestamp(timestamp);
        } catch (Exception e) {
            entry.setTimestamp(LocalDateTime.now());
        }
        
        // Set basic fields
        entry.setSource(source);
        entry.setHost("apache");
        entry.setSrcIp(clientIp);
        entry.setUsername(!"-".equals(authUser) ? authUser : null);
        
        // Determine severity based on status code
        int status = Integer.parseInt(statusCode);
        if (status >= 400 && status < 500) {
            entry.setSeverity("WARN");
        } else if (status >= 500) {
            entry.setSeverity("ERROR");
        } else {
            entry.setSeverity("INFO");
        }
        
        // Set event type
        entry.setEventType("HTTP_REQUEST");
        
        // Create message
        String message = String.format("%s %s - %s %s", method, path, statusCode, size);
        entry.setMessage(message);
        
        // Add metadata
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("method", method);
        metadata.put("path", path);
        metadata.put("statusCode", statusCode);
        metadata.put("size", size);
        metadata.put("referer", referer);
        metadata.put("userAgent", userAgent);
        metadata.put("ident", ident);
        metadata.put("lineNumber", lineNumber);
        entry.setMetadata(metadata);
        
        return entry;
    }
    
    private LogEntry parseCommonLog(Matcher matcher, String source, int lineNumber) {
        LogEntry entry = new LogEntry();
        entry.setId(UUID.randomUUID().toString());
        
        // Extract fields
        String clientIp = matcher.group(1);
        String ident = matcher.group(2);
        String authUser = matcher.group(3);
        String timestampStr = matcher.group(4);
        String method = matcher.group(5);
        String path = matcher.group(6);
        String statusCode = matcher.group(7);
        String size = matcher.group(8);
        
        // Parse timestamp
        try {
            LocalDateTime timestamp = LocalDateTime.parse(timestampStr, APACHE_DATE_FORMAT);
            entry.setTimestamp(timestamp);
        } catch (Exception e) {
            entry.setTimestamp(LocalDateTime.now());
        }
        
        // Set basic fields
        entry.setSource(source);
        entry.setHost("apache");
        entry.setSrcIp(clientIp);
        entry.setUsername(!"-".equals(authUser) ? authUser : null);
        
        // Determine severity based on status code
        int status = Integer.parseInt(statusCode);
        if (status >= 400 && status < 500) {
            entry.setSeverity("WARN");
        } else if (status >= 500) {
            entry.setSeverity("ERROR");
        } else {
            entry.setSeverity("INFO");
        }
        
        // Set event type
        entry.setEventType("HTTP_REQUEST");
        
        // Create message
        String message = String.format("%s %s - %s %s", method, path, statusCode, size);
        entry.setMessage(message);
        
        // Add metadata
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("method", method);
        metadata.put("path", path);
        metadata.put("statusCode", statusCode);
        metadata.put("size", size);
        metadata.put("ident", ident);
        metadata.put("lineNumber", lineNumber);
        entry.setMetadata(metadata);
        
        return entry;
    }
    
    @Override
    public boolean canParse(String sampleLine) {
        if (sampleLine == null || sampleLine.trim().isEmpty()) {
            return false;
        }
        
        return COMBINED_LOG_PATTERN.matcher(sampleLine).matches() || 
               COMMON_LOG_PATTERN.matcher(sampleLine).matches();
    }
    
    @Override
    public String getParserName() {
        return "Apache Log Parser";
    }
}
