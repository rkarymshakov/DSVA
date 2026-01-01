package cz.ctu.fee.dsv.semwork;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Logger {
    private final long nodeId;
    private final FileWriter logWriter;
    private static final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    public Logger(long nodeId, FileWriter logWriter) {
        this.nodeId = nodeId;
        this.logWriter = logWriter;
    }

    public void logInfo(String message, int logicalClock) {
        log(message, logicalClock, false);
    }

    public void logError(String message, int logicalClock) {
        log(message, logicalClock, true);
    }

    private void log(String message, int logicalClock, boolean isError) {
        String timestamp = LocalDateTime.now().format(timeFormatter);
        String logLine = String.format("[%s][LC=%d][Node %d] %s", timestamp, logicalClock, nodeId, message);

        if (isError) System.err.println(logLine);
        else System.out.println(logLine);

        if (logWriter != null) {
            try {
                logWriter.write(logLine + "\n");
                logWriter.flush();
            } catch (IOException ignored) {}
        }
    }

    public void close() {
        try { if (logWriter != null) logWriter.close(); } catch (Exception ignored) {}
    }
}