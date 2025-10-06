package com.darkeye.collectors;

import com.darkeye.model.LogEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Real-time system monitoring collector for Windows systems
 * Monitors system events, file changes, and process activities
 */
public class SystemMonitorCollector {
    
    private static final Logger logger = LoggerFactory.getLogger(SystemMonitorCollector.class);
    
    private final AtomicBoolean running = new AtomicBoolean(false);
    private Consumer<LogEntry> logHandler;
    private WatchService watchService;
    private Thread monitoringThread;
    
    public SystemMonitorCollector() {}
    
    /**
     * Set the handler for processed log entries
     */
    public void setLogHandler(Consumer<LogEntry> logHandler) {
        this.logHandler = logHandler;
    }
    
    /**
     * Start system monitoring
     */
    public void start() {
        if (running.get()) {
            return;
        }
        
        running.set(true);
        
        try {
            // Initialize watch service for file system monitoring
            watchService = FileSystems.getDefault().newWatchService();
            
            // Start monitoring thread
            monitoringThread = new Thread(this::monitorSystem);
            monitoringThread.setDaemon(true);
            monitoringThread.start();
            
            logger.info("System monitoring started");
        } catch (IOException e) {
            logger.error("Failed to start system monitoring", e);
            running.set(false);
        }
    }
    
    /**
     * Stop system monitoring
     */
    public void stop() {
        running.set(false);
        
        if (monitoringThread != null) {
            monitoringThread.interrupt();
        }
        
        if (watchService != null) {
            try {
                watchService.close();
            } catch (IOException e) {
                logger.error("Error closing watch service", e);
            }
        }
        
        logger.info("System monitoring stopped");
    }
    
    /**
     * Check if monitoring is running
     */
    public boolean isRunning() {
        return running.get();
    }
    
    /**
     * Main monitoring loop
     */
    private void monitorSystem() {
        while (running.get()) {
            try {
                // Monitor file system changes
                monitorFileSystemChanges();
                
                // Monitor system processes
                monitorSystemProcesses();
                
                // Monitor network connections
                monitorNetworkConnections();
                
                // Monitor system resources
                monitorSystemResources();
                
                // Sleep for a short interval
                Thread.sleep(5000); // 5 seconds
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                logger.error("Error in system monitoring", e);
            }
        }
    }
    
    /**
     * Monitor file system changes
     */
    private void monitorFileSystemChanges() {
        try {
            // Register watch for system directories
            Path systemDir = Paths.get("C:\\Windows\\System32");
            if (Files.exists(systemDir)) {
                systemDir.register(watchService, 
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_DELETE,
                    StandardWatchEventKinds.ENTRY_MODIFY);
            }
            
            // Check for watch events
            WatchKey key = watchService.poll();
            if (key != null) {
                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();
                    Path path = (Path) event.context();
                    
                    if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
                        createSystemLogEntry("FILE_CREATED", "System file created: " + path, "SYSTEM");
                    } else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
                        createSystemLogEntry("FILE_DELETED", "System file deleted: " + path, "SYSTEM");
                    } else if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                        createSystemLogEntry("FILE_MODIFIED", "System file modified: " + path, "SYSTEM");
                    }
                }
                key.reset();
            }
        } catch (Exception e) {
            logger.error("Error monitoring file system changes", e);
        }
    }
    
    /**
     * Monitor system processes
     */
    private void monitorSystemProcesses() {
        try {
            ProcessBuilder pb = new ProcessBuilder("tasklist", "/fo", "csv");
            Process process = pb.start();
            
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                int processCount = 0;
                while ((line = reader.readLine()) != null && processCount < 10) {
                    if (line.contains(".exe")) {
                        processCount++;
                        // Parse process information
                        String[] parts = line.split(",");
                        if (parts.length >= 2) {
                            String processName = parts[0].replace("\"", "");
                            String pid = parts[1].replace("\"", "");
                            
                            createSystemLogEntry("PROCESS_RUNNING", 
                                "Process: " + processName + " (PID: " + pid + ")", "PROCESS");
                        }
                    }
                }
            }
            
            process.waitFor();
        } catch (Exception e) {
            logger.error("Error monitoring system processes", e);
        }
    }
    
    /**
     * Monitor network connections
     */
    private void monitorNetworkConnections() {
        try {
            ProcessBuilder pb = new ProcessBuilder("netstat", "-an");
            Process process = pb.start();
            
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                int connectionCount = 0;
                while ((line = reader.readLine()) != null && connectionCount < 5) {
                    if (line.contains("ESTABLISHED") || line.contains("LISTENING")) {
                        connectionCount++;
                        createSystemLogEntry("NETWORK_CONNECTION", 
                            "Network connection: " + line.trim(), "NETWORK");
                    }
                }
            }
            
            process.waitFor();
        } catch (Exception e) {
            logger.error("Error monitoring network connections", e);
        }
    }
    
    /**
     * Monitor system resources
     */
    private void monitorSystemResources() {
        try {
            // Get system memory info
            Runtime runtime = Runtime.getRuntime();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            
            double memoryUsagePercent = (double) usedMemory / totalMemory * 100;
            
            if (memoryUsagePercent > 80) {
                createSystemLogEntry("HIGH_MEMORY_USAGE", 
                    "High memory usage: " + String.format("%.2f", memoryUsagePercent) + "%", "RESOURCE");
            }
            
            // Get CPU info (simplified)
            ProcessBuilder pb = new ProcessBuilder("wmic", "cpu", "get", "loadpercentage", "/value");
            Process process = pb.start();
            
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains("LoadPercentage=")) {
                        String cpuUsage = line.split("=")[1].trim();
                        if (!cpuUsage.isEmpty()) {
                            int cpuPercent = Integer.parseInt(cpuUsage);
                            if (cpuPercent > 80) {
                                createSystemLogEntry("HIGH_CPU_USAGE", 
                                    "High CPU usage: " + cpuPercent + "%", "RESOURCE");
                            }
                        }
                    }
                }
            }
            
            process.waitFor();
        } catch (Exception e) {
            logger.error("Error monitoring system resources", e);
        }
    }
    
    /**
     * Create a system log entry
     */
    private void createSystemLogEntry(String eventType, String message, String category) {
        if (logHandler == null) {
            return;
        }
        
        LogEntry logEntry = new LogEntry();
        logEntry.setId(UUID.randomUUID().toString());
        logEntry.setTimestamp(LocalDateTime.now());
        logEntry.setSource("SYSTEM_MONITOR");
        logEntry.setHost(System.getProperty("user.name"));
        logEntry.setSeverity("INFO");
        logEntry.setEventType(eventType);
        logEntry.setMessage(message);
        
        // Add metadata
        java.util.Map<String, Object> metadata = new java.util.HashMap<>();
        metadata.put("category", category);
        metadata.put("monitorType", "REAL_TIME");
        logEntry.setMetadata(metadata);
        
        logHandler.accept(logEntry);
    }
}
