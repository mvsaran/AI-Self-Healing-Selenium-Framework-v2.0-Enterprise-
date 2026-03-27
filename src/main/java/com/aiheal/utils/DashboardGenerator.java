package com.aiheal.utils;

import com.aiheal.healing.LocatorPersistenceUtil;
import com.aiheal.model.SuggestedLocator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * DashboardGenerator — Transforms the persistent healed locators JSON file
 * into a highly readable standalone Web UI (test-output/dashboard.html).
 */
public class DashboardGenerator {

    private static final Logger log = LogManager.getLogger(DashboardGenerator.class);
    private static final String DASHBOARD_FILE = "test-output/dashboard.html";

    private DashboardGenerator() {}

    /**
     * Reads memory mappings and exports a stylized HTML file dynamically.
     */
    public static void generate() {
        Map<String, SuggestedLocator> saved = LocatorPersistenceUtil.getAllSaved();
        
        StringBuilder rows = new StringBuilder();
        if (saved.isEmpty()) {
            rows.append("<tr><td colspan='4' style='text-align:center;'>No healed locators detected yet.</td></tr>");
        } else {
            saved.forEach((key, sl) -> {
                String confidenceColor = sl.getConfidence() >= 0.9 ? "green" : 
                                         sl.getConfidence() >= 0.8 ? "orange" : "red";
                String percentage = String.format("%.0f%%", sl.getConfidence() * 100);

                rows.append("<tr>")
                    .append("<td>").append(escapeHtml(key)).append("</td>")
                    .append("<td><b>[").append(sl.getType()).append("]</b> ").append(escapeHtml(sl.getValue())).append("</td>")
                    .append(String.format("<td style='color:%s; font-weight:bold;'>%s</td>", confidenceColor, percentage))
                    .append("<td><small>").append(escapeHtml(sl.getReason())).append("</small></td>")
                    .append("</tr>");
            });
        }

        String template = String.format("""
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <title>AI Healing Dashboard</title>
                <meta charset="UTF-8">
                <style>
                    body { font-family: 'Segoe UI', system-ui, sans-serif; background: #0f172a; color: #f8fafc; padding: 2rem; }
                    h1 { color: #38bdf8; font-weight: 300; border-bottom: 1px solid #334155; padding-bottom: 1rem; }
                    .stats { margin-bottom: 2rem; color: #94a3b8; }
                    table { width: 100%%; border-collapse: collapse; background: #1e293b; border-radius: 8px; overflow: hidden; }
                    th, td { padding: 12px 16px; text-align: left; border-bottom: 1px solid #334155; }
                    th { background: #334155; color: #e2e8f0; text-transform: uppercase; font-size: 0.85rem; letter-spacing: 0.5px; }
                    tr:hover { background: #24354b; }
                    td { font-size: 0.95rem; }
                </style>
            </head>
            <body>
                <h1>🩺 AI Self-Healing Locators Dashboard</h1>
                <div class="stats">
                    Last updated: <strong>%s</strong> | Total Mapped Fragments: <strong>%d</strong>
                </div>
                <table>
                    <thead>
                        <tr>
                            <th>Original Faulty Locator</th>
                            <th>AI Replaced Strategy</th>
                            <th>Model Confidence</th>
                            <th>AI Reasoning</th>
                        </tr>
                    </thead>
                    <tbody>
                        %s
                    </tbody>
                </table>
            </body>
            </html>
            """, 
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
            saved.size(),
            rows.toString()
        );

        try {
            Files.createDirectories(Paths.get("test-output"));
            try (FileWriter fw = new FileWriter(DASHBOARD_FILE)) {
                fw.write(template);
            }
            log.info("📊 Local Interactive Dashboard built: {}", Paths.get(DASHBOARD_FILE).toAbsolutePath());
        } catch (IOException e) {
            log.warn("Failed to generate dashboard HTML: {}", e.getMessage());
        }
    }

    private static String escapeHtml(String input) {
        if (input == null) return "-";
        return input.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;");
    }
}
