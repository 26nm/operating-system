package com.example;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.StreamHandler;

public class LoggingSetup {

    private static boolean initialized = false; // To ensure we initialize only once.
    private static Logger rootLogger;

    public static void initializeLogger() {
        if (initialized) {
            return;
        }
        initialized = true;
        rootLogger = Logger.getLogger("");
        rootLogger.setLevel(Level.ALL); // Change the default root logger level. Set to ALL to get all messages
        Handler[] handlers = rootLogger.getHandlers();
        if (handlers.length > 0) {
            rootLogger.removeHandler(handlers[0]);
        }

        StreamHandler handler = new StreamHandler(System.out, new MillisecondFormatter());
        rootLogger.addHandler(handler); // Add the handler to the root logger.
        rootLogger.setLevel(Level.FINE); // Set the logger level here.
        handler.setLevel(Level.ALL); // Set handler level too.
        handler.flush(); // Flush the handler after you have added the handler
    }
        public static void logMessage(Level level, String format, Object... params) {
             if (rootLogger.isLoggable(level)) { // check if logging is enabled
                rootLogger.log(level, String.format(format, params));
            }
        }

        private static class MillisecondFormatter extends Formatter {
        private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

        @Override
        public String format(LogRecord record) {
            Date date = new Date(record.getMillis());
            return String.format("%s %s [%s]: %s%n", dateFormat.format(date), record.getSourceMethodName(),record.getLevel(),
                    record.getMessage());
        }
    }
}