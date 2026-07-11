package com.hk.hkterminal;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * ============================================================================
 * HK-BOT MASTER ARCHITECTURE - MODULE 15: LOGGER ENGINE
 * Engine: HK-BOT Runtime Intelligence System
 * Author: HK Prashant Singh (Tech Wizard)
 * Architecture: Structured Logging • Event Driven • Persistent 
 * ============================================================================
 */
public class HKLogger {

    // Master Directory and Log Files
    private static final String BASE_LOG_DIR = "/sdcard/hk/logs/";
    private static final String MASTER_LOG = "hk.log";
    private static final String CRASH_LOG = "crash.log";
    private static final String REPAIR_LOG = "repair.log";
    
    // Unique Session ID for tracking process flow
    private static final String SESSION_ID = "HK-" + System.currentTimeMillis();

    /**
     * Initializes the logging directory structure.
     * Zero-Trust: Never assumes the directory exists.
     */
    private static void initializeSystem() {
        File dir = new File(BASE_LOG_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    /**
     * Standard Execution Logger
     */
    public static void log(String module, String action) {
        writeCore(MASTER_LOG, module, "READY", action);
    }

    /**
     * Warning and Event Logger
     */
    public static void logEvent(String module, String eventName, String details) {
        writeCore(MASTER_LOG, module, "WARNING", eventName + " -> " + details);
    }

    /**
     * Repair Pipeline Logger
     */
    public static void logRepair(String targetPackage, String action, String status) {
        writeCore(REPAIR_LOG, "MODULE-13", status, "Target: " + targetPackage + " | " + action);
        // Also append to master log for central tracking
        writeCore(MASTER_LOG, "HEAL-ENGINE", status, action);
    }

    /**
     * Critical System Crash Logger
     */
    public static void logCrash(String module, String errorData) {
        writeCore(CRASH_LOG, module, "FAILED", "CRITICAL ERROR: " + errorData);
        writeCore(MASTER_LOG, module, "FAILED", "[SYSTEM CRASH] -> See crash.log");
    }

    /**
     * Core Writing Mechanism
     * Format: [Timestamp] [SessionID] [Module] [Status] [Action/Result]
     */
    private static void writeCore(String fileName, String module, String status, String action) {
        initializeSystem();
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
        String logEntry = String.format("[%s] [%s] [%s] [%s] %s\n", timestamp, SESSION_ID, module, status, action);

        try (FileWriter fw = new FileWriter(BASE_LOG_DIR + fileName, true)) {
            fw.write(logEntry);
        } catch (IOException e) {
            // Failsafe: If SD card access fails, print to system console
            System.err.println("HK-LOGGER FATAL: Unable to write to " + BASE_LOG_DIR);
            e.printStackTrace();
        }
    }
}
