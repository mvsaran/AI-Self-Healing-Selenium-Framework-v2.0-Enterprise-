package com.aiheal.driver;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

/**
 * DriverFactory — Responsible for creating and managing the ChromeDriver instance.
 *
 * Kept simple for MVP. Uses WebDriverManager to auto-download the correct
 * ChromeDriver version so there's no manual setup needed.
 */
public class DriverFactory {

    private static final Logger log = LogManager.getLogger(DriverFactory.class);

    // Thread-local allows parallel test execution in future without sharing driver
    private static final ThreadLocal<WebDriver> driverThread = new ThreadLocal<>();

    private DriverFactory() {
        // Utility class — no instantiation
    }

    /**
     * Initializes ChromeDriver using WebDriverManager and stores it in ThreadLocal.
     * Call this once per test (from BaseTest.setUp).
     */
    public static WebDriver initDriver() {
        log.info("Initializing ChromeDriver via WebDriverManager...");

        // WebDriverManager auto-downloads the correct chromedriver binary
        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();
        // Allow running on CI or headless environments — comment this out for headed local demo
        // options.addArguments("--headless=new");
        options.addArguments("--start-maximized");
        options.addArguments("--disable-notifications");
        options.addArguments("--remote-allow-origins=*");

        WebDriver driver = new ChromeDriver(options);
        driverThread.set(driver);

        log.info("ChromeDriver initialized successfully.");
        return driver;
    }

    /**
     * Returns the WebDriver for the current thread.
     */
    public static WebDriver getDriver() {
        return driverThread.get();
    }

    /**
     * Quits the driver and removes it from ThreadLocal to prevent memory leaks.
     */
    public static void quitDriver() {
        WebDriver driver = driverThread.get();
        if (driver != null) {
            log.info("Quitting ChromeDriver...");
            driver.quit();
            driverThread.remove();
        }
    }
}
