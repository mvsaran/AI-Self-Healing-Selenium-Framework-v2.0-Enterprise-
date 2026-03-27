package com.aiheal.healing;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;

/**
 * DOMCaptureUtil — Captures the current page's DOM (HTML source).
 *
 * For MVP we use driver.getPageSource() which returns the full HTML.
 * To keep the AI prompt concise (and reduce token cost), we trim the
 * source to a configurable character limit.
 */
public class DOMCaptureUtil {

    private static final Logger log = LogManager.getLogger(DOMCaptureUtil.class);

    /**
     * Maximum characters of page source to include in the AI prompt.
     * Keeps tokens under control while still giving the AI enough context.
     */
    private static final int MAX_DOM_CHARS = 6000;

    private DOMCaptureUtil() {}

    /**
     * Returns a trimmed version of the current page source, suitable for
     * embedding in a prompt.
     *
     * @param driver active WebDriver session
     * @return trimmed HTML string
     */
    public static String captureDOM(WebDriver driver) {
        try {
            String pageSource = driver.getPageSource();

            if (pageSource == null || pageSource.isBlank()) {
                log.warn("Page source is empty.");
                return "[Empty page source]";
            }

            if (pageSource.length() > MAX_DOM_CHARS) {
                log.debug("Page source too large ({} chars), trimming to {} chars.",
                        pageSource.length(), MAX_DOM_CHARS);
                return pageSource.substring(0, MAX_DOM_CHARS)
                        + "\n<!-- [DOM TRIMMED FOR BREVITY] -->";
            }

            return pageSource;

        } catch (Exception e) {
            log.error("Failed to capture page source: {}", e.getMessage());
            return "[DOM capture failed: " + e.getMessage() + "]";
        }
    }
}
