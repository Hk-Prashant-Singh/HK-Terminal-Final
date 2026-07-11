package com.hk.hkterminal;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * ============================================================================
 * HK-BOT MASTER ARCHITECTURE - MODULE 19: DATABASE ENGINE
 * Engine: HK-BOT Runtime Intelligence System (v4.0)
 * Author: HK Prashant (Tech Wizard)
 * Architecture: Persistent Storage • State Machine • Zero Data Loss (SECURE MATRIX)
 * ============================================================================
 */
public class HKDatabaseManager extends SQLiteOpenHelper {

    // Core Database Configuration
    private static final String DATABASE_NAME = "hk_packages.db";
    private static final int DATABASE_VERSION = 1;

    // Architecture Tables
    private static final String TABLE_PACKAGES = "Packages";
    private static final String TABLE_HEALTH = "Health";
    private static final String TABLE_HISTORY = "History";

    public HKDatabaseManager(Context context) {
        // [!] FIXED: Shifted to Native Android Secure Database path (Bypasses Scoped Storage block)
        // HK-Bot will now initialize the database in a secure, OS-protected directory.
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        HKLogger.logEvent("DB-ENGINE", "INITIALIZATION", "HK Database Engine Connected in Secure App Matrix.");
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        HKLogger.logEvent("DB-ENGINE", "SCHEMA_CREATE", "Building Core Tables...");

        // 1. Package Registry Table (Maintains Lifecycle)
        String createPackagesTable = "CREATE TABLE " + TABLE_PACKAGES + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "package_name TEXT UNIQUE, " +
                "version TEXT, " +
                "status TEXT, " +
                "install_date TEXT)";
        db.execSQL(createPackagesTable);

        // 2. Health Engine Table (Monitors Corruption)
        String createHealthTable = "CREATE TABLE " + TABLE_HEALTH + " (" +
                "package_name TEXT PRIMARY KEY, " +
                "health_score INTEGER, " +
                "last_scan TEXT, " +
                "is_corrupted INTEGER)";
        db.execSQL(createHealthTable);

        // 3. Command History Table
        String createHistoryTable = "CREATE TABLE " + TABLE_HISTORY + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "command_run TEXT, " +
                "timestamp TEXT)";
        db.execSQL(createHistoryTable);

        HKLogger.logEvent("DB-ENGINE", "SCHEMA_READY", "All tables locked and loaded.");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PACKAGES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_HEALTH);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_HISTORY);
        onCreate(db);
    }

    /**
     * LOGIC GATE 1: Package Registration (Used during Module 08 - Deployment)
     */
    public void registerPackage(String pkgName, String version) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("package_name", pkgName);
        values.put("version", version);
        values.put("status", "INSTALLING");
        values.put("install_date", String.valueOf(System.currentTimeMillis()));

        long result = db.insertWithOnConflict(TABLE_PACKAGES, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        if (result != -1) {
            HKLogger.logEvent("DB-ENGINE", "REGISTRY_UPDATE", "Package [" + pkgName + "] registered successfully.");
        }
    }

    /**
     * LOGIC GATE 2: The Package State Machine
     * Handles Transitions: EXTRACTING -> DEPLOYING -> VALIDATING -> READY / FAILED
     */
    public void updatePackageState(String pkgName, String state) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("status", state);

        int rows = db.update(TABLE_PACKAGES, values, "package_name=?", new String[]{pkgName});
        if (rows > 0) {
            HKLogger.logEvent("DB-ENGINE", "STATE_TRANSITION", pkgName + " is now -> " + state);
        }
    }

    /**
     * LOGIC GATE 3: Health Monitor Pipeline (Used by Module 16 & 13)
     */
    public void updateHealthScore(String pkgName, int score, boolean isCorrupted) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("package_name", pkgName);
        values.put("health_score", score);
        values.put("is_corrupted", isCorrupted ? 1 : 0);
        values.put("last_scan", String.valueOf(System.currentTimeMillis()));

        db.insertWithOnConflict(TABLE_HEALTH, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        
        if (isCorrupted) {
            updatePackageState(pkgName, "REPAIRABLE");
            HKLogger.logEvent("DB-ENGINE", "HEALTH_ALERT", pkgName + " flagged as corrupted. Triggering RepairQueue.");
        }
    }
}
