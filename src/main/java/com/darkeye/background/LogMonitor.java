package com.darkeye.background;

import com.darkeye.model.LogEntry;

import java.io.File;
import java.nio.file.Files;
import java.util.function.Consumer;

/**
 * Very small log monitor used by BackgroundDetectorService.
 * This is intentionally lightweight: it scans files under a directory and emits LogEntry objects.
 */
public class LogMonitor {

    private Consumer<LogEntry> logHandler;
    private volatile boolean running = false;

    public void setLogHandler(Consumer<LogEntry> handler) {
        this.logHandler = handler;
    }

    public void start(String path) {
        running = true;
        try {
            File dir = new File(path);
            if (!dir.exists() || !dir.isDirectory()) {
                System.out.println("[LogMonitor] log directory missing: " + path);
                return;
            }

            // Simple loop - read files periodically (placeholder)
            while (running) {
                File[] files = dir.listFiles();
                if (files != null) {
                    for (File f : files) {
                        if (!f.isFile()) continue;
                        try {
                            String content = new String(Files.readAllBytes(f.toPath()));
                            LogEntry entry = new LogEntry();
                            entry.setId(f.getName());
                            entry.setMessage(content);
                            entry.setSeverity(content.contains("ERROR") ? "HIGH" : "LOW");
                            if (logHandler != null) logHandler.accept(entry);
                        } catch (Exception e) {
                            // ignore reading problems
                        }
                    }
                }

                Thread.sleep(2000);
            }
        } catch (InterruptedException e) {
            // stop requested
        }
    }

    public void stop() {
        running = false;
    }
}
