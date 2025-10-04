package com.darkeye.collectors;

import com.darkeye.model.LogEntry;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Simple file collector that reads log files and converts them to LogEntry objects
 */
public class FileCollector {
    private final AtomicBoolean running = new AtomicBoolean(false);
    private Consumer<LogEntry> logHandler;
    
    public FileCollector() {}
    
    /**
     * Set the handler for processed log entries
     */
    public void setLogHandler(Consumer<LogEntry> logHandler) {
        this.logHandler = logHandler;
    }
    
    /**
     * Start collecting from a file or directory
     */
    public void start(String path) {
        if (running.get()) {
            return;
        }
        
        running.set(true);
        
        try {
            Path filePath = Paths.get(path);
            if (Files.isDirectory(filePath)) {
                collectFromDirectory(filePath);
            } else {
                collectFromFile(filePath);
            }
        } catch (Exception e) {
            System.err.println("Error collecting logs: " + e.getMessage());
        } finally {
            running.set(false);
        }
    }
    
    /**
     * Stop collecting
     */
    public void stop() {
        running.set(false);
    }
    
    /**
     * Check if collector is running
     */
    public boolean isRunning() {
        return running.get();
    }
    
    private void collectFromDirectory(Path directory) throws IOException {
        Files.walk(directory)
            .filter(Files::isRegularFile)
            .filter(path -> path.toString().toLowerCase().endsWith(".log") || 
                           path.toString().toLowerCase().endsWith(".txt"))
            .forEach(this::collectFromFile);
    }
    
    private void collectFromFile(Path filePath) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath.toFile()))) {
            String line;
            int lineNumber = 0;
            
            while ((line = reader.readLine()) != null && running.get()) {
                lineNumber++;
                LogEntry logEntry = parseLogLine(line, filePath.toString(), lineNumber);
                if (logEntry != null && logHandler != null) {
                    logHandler.accept(logEntry);
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading file " + filePath + ": " + e.getMessage());
        }
    }
    
    /**
     * Simple log line parser - can be enhanced later
     */
    private LogEntry parseLogLine(String line, String source, int lineNumber) {
        if (line == null || line.trim().isEmpty()) {
            return null;
        }
        
        // Create a basic log entry
        LogEntry entry = new LogEntry();
        entry.setId(UUID.randomUUID().toString());
        entry.setTimestamp(LocalDateTime.now());
        entry.setSource(source);
        entry.setHost("localhost");
        entry.setMessage(line);
        
        // Try to detect log type and parse accordingly
        if (line.contains("ERROR") || line.contains("error")) {
            entry.setSeverity("ERROR");
        } else if (line.contains("WARN") || line.contains("warning")) {
            entry.setSeverity("WARN");
        } else if (line.contains("INFO") || line.contains("info")) {
            entry.setSeverity("INFO");
        } else {
            entry.setSeverity("UNKNOWN");
        }
        
        // Try to extract IP addresses (simple regex)
        String[] parts = line.split("\\s+");
        for (String part : parts) {
            if (part.matches("\\b(?:[0-9]{1,3}\\.){3}[0-9]{1,3}\\b")) {
                if (entry.getSrcIp() == null) {
                    entry.setSrcIp(part);
                } else if (entry.getDstIp() == null) {
                    entry.setDstIp(part);
                }
            }
        }
        
        // Try to extract username (look for common patterns)
        for (String part : parts) {
            if (part.contains("user=") || part.contains("username=")) {
                String[] userParts = part.split("=");
                if (userParts.length > 1) {
                    entry.setUsername(userParts[1]);
                }
            }
        }
        
        return entry;
    }
}
