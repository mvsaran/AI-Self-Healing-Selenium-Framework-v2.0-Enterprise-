package com.aiheal.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * ScreenshotUtil — Captures browser screenshots on locator failure.
 *
 * Screenshots saved to: test-output/screenshots/<timestamp>_<name>.png
 */
public class ScreenshotUtil {

    private static final Logger log = LogManager.getLogger(ScreenshotUtil.class);

    private static final String SCREENSHOT_DIR = "test-output/screenshots";
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private ScreenshotUtil() {}

    /**
     * Takes a screenshot and saves it to the screenshots folder.
     *
     * @param driver active WebDriver
     * @param name   descriptive name appended to the filename
     * @return absolute path of the saved screenshot, or null if capture failed
     */
    public static String capture(WebDriver driver, String name) {
        try {
            // Ensure the screenshot directory exists
            Path screenshotPath = Paths.get(SCREENSHOT_DIR);
            Files.createDirectories(screenshotPath);

            // Build the filename
            String timestamp = LocalDateTime.now().format(FORMATTER);
            String filename  = timestamp + "_" + name + ".png";
            Path   filePath  = screenshotPath.resolve(filename);

            // Capture and save
            File srcFile = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            Files.copy(srcFile.toPath(), filePath);

            log.info("Screenshot saved: {}", filePath.toAbsolutePath());
            return filePath.toAbsolutePath().toString();

        } catch (IOException e) {
            log.warn("Failed to save screenshot: {}", e.getMessage());
            return null;
        } catch (Exception e) {
            log.warn("Screenshot capture failed (driver may not support it): {}", e.getMessage());
            return null;
        }
    }

    /**
     * Captures the screen as a Base64 string to be sent directly to AI APIs.
     *
     * @param driver active WebDriver
     * @return base64 string, or null if capture failed
     */
    public static String captureBase64(WebDriver driver) {
        try {
            return ((TakesScreenshot) driver).getScreenshotAs(OutputType.BASE64);
        } catch (Exception e) {
            log.warn("Base64 screenshot capture failed: {}", e.getMessage());
            return null;
        }
    }
}
