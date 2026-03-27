package com.aiheal.utils;

import com.aiheal.model.HealResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * ReportUtil — Logs healing attempts to console and appends to a local report file.
 *
 * Report file location: test-output/healing-report.txt
 */
public class ReportUtil {

    private static final Logger log = LogManager.getLogger(ReportUtil.class);

    private static final String REPORT_DIR  = "test-output";
    private static final String REPORT_FILE = REPORT_DIR + "/healing-report.txt";
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private ReportUtil() {}

    /**
     * Logs a HealResult to both console and the report file.
     *
     * @param result the completed HealResult to log
     */
    public static void log(HealResult result) {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        String icon      = result.isHealedSuccessfully() ? "✅" : "❌";
        String status    = result.isHealedSuccessfully() ? "SUCCESS" : "FAILED";

        // Build report with consistent line lengths — avoids box-drawing alignment issues
        // caused by variable-length field values stretching the border.
        String line = "+----------------------------------------------------+";
        String report = System.lineSeparator()
                + line                                                                    + System.lineSeparator()
                + "|        AI SELF-HEALING ATTEMPT REPORT              |"               + System.lineSeparator()
                + line                                                                    + System.lineSeparator()
                + "| Timestamp  : " + timestamp                                          + System.lineSeparator()
                + "| Status     : " + icon + " " + status                               + System.lineSeparator()
                + "| Original   : [" + result.getOriginalLocatorType()
                        + "] '" + result.getOriginalLocatorValue() + "'"                 + System.lineSeparator()
                + "| AI Suggest : [" + (result.getWinningLocator() != null ? safe(result.getWinningLocator().getType()) : "-")
                        + "] '" + (result.getWinningLocator() != null ? safe(result.getWinningLocator().getValue()) : "-") + "'"          + System.lineSeparator()
                + "| Confidence : " + (result.getWinningLocator() != null ? String.format("%.0f%%", result.getWinningLocator().getConfidence() * 100) : "-") + System.lineSeparator()
                + "| Reason     : " + (result.getWinningLocator() != null ? safe(result.getWinningLocator().getReason()) : "-")                           + System.lineSeparator()
                + line                                                                    + System.lineSeparator();

        // Print to console via logger
        if (result.isHealedSuccessfully()) {
            log.info(report);
        } else {
            log.error(report);
        }

        // Append to file report
        appendToFile(report);
    }

    // ----------------------------------------------------------------
    // Private helpers
    // ----------------------------------------------------------------

    /** Returns the value or a dash if null/blank — prevents "null" appearing in reports. */
    private static String safe(String s) {
        return (s == null || s.isBlank()) ? "-" : s;
    }

    private static void appendToFile(String content) {
        try {
            Files.createDirectories(Paths.get(REPORT_DIR));
            try (FileWriter fw = new FileWriter(REPORT_FILE, true)) {
                fw.write(content);
            }
        } catch (IOException e) {
            log.warn("Could not write healing report to file {}: {}", REPORT_FILE, e.getMessage());
        }
    }
}
