package com.aiheal.base;

import com.aiheal.driver.DriverFactory;
import com.aiheal.utils.ConfigReader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

/**
 * BaseTest — Parent class for all test classes.
 *
 * Handles:
 * - WebDriver lifecycle (setup before test, teardown after test)
 * - Making driver and base URL accessible to child test classes
 */
public class BaseTest {

    private static final Logger log = LogManager.getLogger(BaseTest.class);

    // Accessible to child tests
    protected WebDriver driver;
    protected String baseUrl;

    /**
     * Initializes the WebDriver and navigates to the baseUrl before each test method.
     */
    @BeforeMethod
    public void setUp() {
        log.info("============================");
        log.info("  TEST SETUP STARTED");
        log.info("============================");

        driver  = DriverFactory.initDriver();
        baseUrl = ConfigReader.get("baseUrl");

        if (baseUrl != null && !baseUrl.isBlank()) {
            log.info("Navigating to base URL: {}", baseUrl);
            driver.get(baseUrl);
        }
    }

    /**
     * Quits the WebDriver after each test method completes (pass or fail).
     */
    @AfterMethod
    public void tearDown() {
        log.info("============================");
        log.info("  TEST TEARDOWN STARTED");
        log.info("============================");
        DriverFactory.quitDriver();
    }
}
