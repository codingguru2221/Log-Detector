package com.darkeye.background;

import com.darkeye.model.LogEntry;
import com.darkeye.model.SecurityAlert;
import com.darkeye.auth.USBAuthManager;
import javafx.application.Platform;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * BackgroundDetectorService
 * - Runs (simulated) at system startup
 * - Monitors logs using a LogMonitor
 * - Emits SecurityAlert objects through a simple listener API
 */
public class BackgroundDetectorService {

    private static BackgroundDetectorService instance;

    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);
    private final List<AlertListener> listeners = new ArrayList<>();
    private final ConcurrentLinkedQueue<SecurityAlert> alertQueue = new ConcurrentLinkedQueue<>();
    private LogMonitor monitor;
    private volatile boolean running = false;

    public interface AlertListener {
        void onAlert(SecurityAlert alert);
    }

    private BackgroundDetectorService() {
        // singleton
    }

    public static synchronized BackgroundDetectorService getInstance() {
        if (instance == null) {
            instance = new BackgroundDetectorService();
        }
        return instance;
    }

    /** Start service (non-blocking) */
    public void start() {
        if (running) return;
        running = true;

        monitor = new LogMonitor();
        monitor.setLogHandler(this::handleLogEntry);

        // start monitoring in background
        executor.submit(() -> monitor.start("sample-logs"));

        // periodic heartbeat (placeholder)
        executor.scheduleAtFixedRate(() -> {
            // placeholder: health check or metrics
        }, 1, 30, TimeUnit.SECONDS);
    }

    public void stop() {
        running = false;
        if (monitor != null) {
            monitor.stop();
        }
        executor.shutdownNow();
    }

    private void handleLogEntry(LogEntry entry) {
        // placeholder detection logic: if severity equals "HIGH" create alert
        if (entry == null) return;
        if ("HIGH".equalsIgnoreCase(entry.getSeverity()) || entry.getMessage().toLowerCase().contains("unauthorized")) {
            SecurityAlert alert = new SecurityAlert(UUID.randomUUID().toString(), LocalDateTime.now(), "HIGH",
                    "Suspicious activity detected", entry.getMessage(), "placeholder-rule", entry.getId(), null);

            alertQueue.add(alert);

            // notify listeners
            for (AlertListener l : listeners) {
                try { l.onAlert(alert); } catch (Exception ignored) {}
            }

            // show quick popup (UI should handle auth decisions)
            Platform.runLater(() -> {
                // Use AlertPopup to show choices
                AlertPopup.showForAlert(alert, this);
            });
        }
    }

    // Listener API
    public void registerListener(AlertListener listener) {
        listeners.add(listener);
    }

    public void unregisterListener(AlertListener listener) {
        listeners.remove(listener);
    }

    public List<SecurityAlert> drainPendingAlerts() {
        List<SecurityAlert> drained = new ArrayList<>();
        SecurityAlert a;
        while ((a = alertQueue.poll()) != null) {
            drained.add(a);
        }
        return drained;
    }

    /**
     * Simulate service auto-start at system boot. In a real application this would be
     * registered with OS startup mechanisms (Windows Service, systemd, launchd, etc.).
     */
    public static void simulateAutoStartOnBoot() {
        BackgroundDetectorService svc = BackgroundDetectorService.getInstance();
        svc.start();
        System.out.println("[BackgroundDetectorService] Simulated auto-start completed.");
    }

    // Helper actions invoked by popup buttons
    public void handleIgnore(SecurityAlert alert) {
        // mark acknowledged locally
        if (alert != null) alert.setAcknowledged(true);
        System.out.println("[BackgroundDetectorService] Alert ignored: " + alert);
    }

    public void handleAddAsDevice(SecurityAlert alert, Consumer<Boolean> resultCallback) {
        // requires user authentication (USB)
        boolean ok = USBAuthManager.requestUserKey();
        if (ok) {
            // placeholder: mark device as known
            alert.setAcknowledged(true);
            System.out.println("[BackgroundDetectorService] Device added after user auth: " + alert);
        }
        if (resultCallback != null) resultCallback.accept(ok);
    }

    public void handleResolve(SecurityAlert alert, Consumer<Boolean> resultCallback) {
        // requires admin authentication (USB)
        boolean ok = USBAuthManager.requestAdminKey();
        if (ok) {
            alert.setAcknowledged(true);
            System.out.println("[BackgroundDetectorService] Alert resolved by admin: " + alert);
        }
        if (resultCallback != null) resultCallback.accept(ok);
    }
}
