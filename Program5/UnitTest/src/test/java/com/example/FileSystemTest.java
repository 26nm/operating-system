package com.example;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.logging.Level;

import static org.junit.jupiter.api.Assertions.*;

public class FileSystemTest {
    // private FileSystem fs;

    // ------------ For debug logging only ------------
    private static final boolean LOGGING_ENABLED = true;

    static {
        LoggingSetup.initializeLogger();
    }

    private static void logMessage(Level level, String format, Object... params) {
         if(LOGGING_ENABLED) LoggingSetup.logMessage(level, format, params);
    }
    // ------------ For debug logging only ------------

    @BeforeEach
    public void setUp() throws IOException {
        // Initialize the disk before each test
        SysLib.initDisk();
        SysLib.clearCache(); // Ensure the cache is clear before each test
        // fs = new FileSystem(Constants.TEST_DISK_BLOCKS);
    }

    @AfterEach
    public void tearDown() throws IOException {
        SysLib.clearCache(); // Reset Cache after each test.
    }

    @Test
    public void testAsserts() {
        logMessage(Level.INFO, "Starting Test Asserts");
        assertTrue(true);
        logMessage(Level.INFO, "Finished Test Asserts");
    }

    @Test
    public void testLoggingSystem() {
        logMessage(Level.INFO, "Starting testLoggingSystem");
        logMessage(Level.FINE, "testFileSystemCreation: FINE logging check");
        logMessage(Level.WARNING, "TEST WARNING");
        logMessage(Level.SEVERE, "TEST SEVERE");
        logMessage(Level.CONFIG, "Test Config");
        logMessage(Level.ALL, "Test ALL");
        logMessage(Level.OFF, "Test OFF");
        logMessage(Level.INFO, "Finished testLoggingSystem");
    }
}