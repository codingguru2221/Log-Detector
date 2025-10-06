package com.darkeye.collectors;

import com.darkeye.model.LogEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.time.LocalDateTime;
import java.util.Enumeration;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Network monitoring collector for real-time network activity detection
 */
public class NetworkMonitorCollector {
    
    private static final Logger logger = LoggerFactory.getLogger(NetworkMonitorCollector.class);
    
    private final AtomicBoolean running = new AtomicBoolean(false);
    private Consumer<LogEntry> logHandler;
    private Thread monitoringThread;
    
    // Patterns for network monitoring
    private static final Pattern IP_PATTERN = Pattern.compile("\\b(?:[0-9]{1,3}\\.){3}[0-9]{1,3}\\b");
    private static final Pattern PORT_PATTERN = Pattern.compile(":(\\d+)");
    
    public NetworkMonitorCollector() {}
    
    /**
     * Set the handler for processed log entries
     */
    public void setLogHandler(Consumer<LogEntry> logHandler) {
        this.logHandler = logHandler;
    }
    
    /**
     * Start network monitoring
     */
    public void start() {
        if (running.get()) {
            return;
        }
        
        running.set(true);
        
        // Start monitoring thread
        monitoringThread = new Thread(this::monitorNetwork);
        monitoringThread.setDaemon(true);
        monitoringThread.start();
        
        logger.info("Network monitoring started");
    }
    
    /**
     * Stop network monitoring
     */
    public void stop() {
        running.set(false);
        
        if (monitoringThread != null) {
            monitoringThread.interrupt();
        }
        
        logger.info("Network monitoring stopped");
    }
    
    /**
     * Check if monitoring is running
     */
    public boolean isRunning() {
        return running.get();
    }
    
    /**
     * Main network monitoring loop
     */
    private void monitorNetwork() {
        while (running.get()) {
            try {
                // Monitor active connections
                monitorActiveConnections();
                
                // Monitor network interfaces
                monitorNetworkInterfaces();
                
                // Monitor DNS queries (if possible)
                monitorDNSActivity();
                
                // Monitor network traffic patterns
                monitorTrafficPatterns();
                
                // Sleep for monitoring interval
                Thread.sleep(10000); // 10 seconds
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                logger.error("Error in network monitoring", e);
            }
        }
    }
    
    /**
     * Monitor active network connections
     */
    private void monitorActiveConnections() {
        try {
            ProcessBuilder pb = new ProcessBuilder("netstat", "-an");
            Process process = pb.start();
            
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains("ESTABLISHED")) {
                        analyzeConnection(line);
                    } else if (line.contains("LISTENING")) {
                        analyzeListeningPort(line);
                    }
                }
            }
            
            process.waitFor();
        } catch (Exception e) {
            logger.error("Error monitoring active connections", e);
        }
    }
    
    /**
     * Analyze established connection
     */
    private void analyzeConnection(String connectionLine) {
        Matcher ipMatcher = IP_PATTERN.matcher(connectionLine);
        Matcher portMatcher = PORT_PATTERN.matcher(connectionLine);
        
        String localIP = null;
        String remoteIP = null;
        String localPort = null;
        String remotePort = null;
        
        // Extract IPs and ports
        if (ipMatcher.find()) {
            localIP = ipMatcher.group();
            if (ipMatcher.find()) {
                remoteIP = ipMatcher.group();
            }
        }
        
        if (portMatcher.find()) {
            localPort = portMatcher.group(1);
            if (portMatcher.find()) {
                remotePort = portMatcher.group(1);
            }
        }
        
        // Check for suspicious connections
        if (remoteIP != null && isSuspiciousIP(remoteIP)) {
            createNetworkLogEntry("SUSPICIOUS_CONNECTION", 
                "Suspicious connection to " + remoteIP + ":" + remotePort, 
                "HIGH", remoteIP, remotePort);
        }
        
        // Check for connections to unusual ports
        if (remotePort != null && isSuspiciousPort(remotePort)) {
            createNetworkLogEntry("UNUSUAL_PORT_CONNECTION", 
                "Connection to unusual port " + remotePort + " on " + remoteIP, 
                "MEDIUM", remoteIP, remotePort);
        }
    }
    
    /**
     * Analyze listening port
     */
    private void analyzeListeningPort(String listeningLine) {
        Matcher ipMatcher = IP_PATTERN.matcher(listeningLine);
        Matcher portMatcher = PORT_PATTERN.matcher(listeningLine);
        
        String localIP = null;
        String localPort = null;
        
        if (ipMatcher.find()) {
            localIP = ipMatcher.group();
        }
        
        if (portMatcher.find()) {
            localPort = portMatcher.group(1);
        }
        
        // Check for services listening on unusual ports
        if (localPort != null && isSuspiciousPort(localPort)) {
            createNetworkLogEntry("UNUSUAL_LISTENING_PORT", 
                "Service listening on unusual port " + localPort, 
                "MEDIUM", localIP, localPort);
        }
    }
    
    /**
     * Monitor network interfaces
     */
    private void monitorNetworkInterfaces() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                
                if (networkInterface.isUp() && !networkInterface.isLoopback()) {
                    // Check for new network interfaces
                    if (isNewInterface(networkInterface)) {
                        createNetworkLogEntry("NEW_NETWORK_INTERFACE", 
                            "New network interface detected: " + networkInterface.getDisplayName(), 
                            "INFO", null, null);
                    }
                    
                    // Monitor interface statistics
                    monitorInterfaceStats(networkInterface);
                }
            }
        } catch (SocketException e) {
            logger.error("Error monitoring network interfaces", e);
        }
    }
    
    /**
     * Monitor DNS activity (simplified)
     */
    private void monitorDNSActivity() {
        try {
            // This is a simplified DNS monitoring - in a real implementation,
            // you would use packet capture or system APIs
            ProcessBuilder pb = new ProcessBuilder("nslookup", "google.com");
            Process process = pb.start();
            
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains("Address:")) {
                        createNetworkLogEntry("DNS_QUERY", 
                            "DNS query resolved: " + line.trim(), 
                            "INFO", null, null);
                        break;
                    }
                }
            }
            
            process.waitFor();
        } catch (Exception e) {
            // DNS monitoring is optional, so we don't log errors
        }
    }
    
    /**
     * Monitor traffic patterns
     */
    private void monitorTrafficPatterns() {
        try {
            // Monitor network statistics
            ProcessBuilder pb = new ProcessBuilder("netstat", "-s");
            Process process = pb.start();
            
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains("packets received") || line.contains("packets sent")) {
                        // Analyze packet statistics
                        createNetworkLogEntry("NETWORK_STATISTICS", 
                            "Network stats: " + line.trim(), 
                            "INFO", null, null);
                    }
                }
            }
            
            process.waitFor();
        } catch (Exception e) {
            logger.error("Error monitoring traffic patterns", e);
        }
    }
    
    /**
     * Check if IP is suspicious
     */
    private boolean isSuspiciousIP(String ip) {
        // Check against known suspicious IP ranges
        return ip.startsWith("10.0.0.") || 
               ip.startsWith("192.168.1.100") ||
               ip.startsWith("172.16.0.100");
    }
    
    /**
     * Check if port is suspicious
     */
    private boolean isSuspiciousPort(String port) {
        int portNum = Integer.parseInt(port);
        
        // Check for common suspicious ports
        return portNum == 22 ||    // SSH (if unexpected)
               portNum == 23 ||    // Telnet
               portNum == 135 ||   // RPC
               portNum == 139 ||   // NetBIOS
               portNum == 445 ||   // SMB
               portNum == 1433 ||  // SQL Server
               portNum == 3389 ||  // RDP
               portNum == 5432 ||  // PostgreSQL
               portNum == 5900 ||  // VNC
               portNum == 8080;    // HTTP Proxy
    }
    
    /**
     * Check if network interface is new
     */
    private boolean isNewInterface(NetworkInterface networkInterface) {
        // This is a simplified check - in a real implementation,
        // you would maintain a list of known interfaces
        return networkInterface.getDisplayName().contains("Virtual") ||
               networkInterface.getDisplayName().contains("VPN");
    }
    
    /**
     * Monitor interface statistics
     */
    private void monitorInterfaceStats(NetworkInterface networkInterface) {
        try {
            // Get interface statistics (simplified)
            if (networkInterface.getInetAddresses().hasMoreElements()) {
                InetAddress address = networkInterface.getInetAddresses().nextElement();
                createNetworkLogEntry("INTERFACE_ACTIVE", 
                    "Network interface active: " + networkInterface.getDisplayName() + 
                    " (" + address.getHostAddress() + ")", 
                    "INFO", address.getHostAddress(), null);
            }
        } catch (Exception e) {
            logger.error("Error monitoring interface stats", e);
        }
    }
    
    /**
     * Create a network log entry
     */
    private void createNetworkLogEntry(String eventType, String message, String severity, String srcIp, String port) {
        if (logHandler == null) {
            return;
        }
        
        LogEntry logEntry = new LogEntry();
        logEntry.setId(UUID.randomUUID().toString());
        logEntry.setTimestamp(LocalDateTime.now());
        logEntry.setSource("NETWORK_MONITOR");
        logEntry.setHost(System.getProperty("user.name"));
        logEntry.setSeverity(severity);
        logEntry.setEventType(eventType);
        logEntry.setSrcIp(srcIp);
        logEntry.setMessage(message);
        
        // Add metadata
        java.util.Map<String, Object> metadata = new java.util.HashMap<>();
        metadata.put("monitorType", "NETWORK");
        metadata.put("port", port);
        logEntry.setMetadata(metadata);
        
        logHandler.accept(logEntry);
    }
}
