package com.aiheal.tests;

import com.aiheal.base.BaseTest;
import com.aiheal.healing.HealingEngine;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.net.URL;

/**
 * LoginTest — Demonstrates the AI Self-Healing mechanism.
 *
 * Scenario:
 *   The test intentionally uses a BROKEN locator: By.id("login-btn")
 *   The actual HTML element is: <button data-testid="signin-btn">Sign In</button>
 *
 * Expected healing:
 *   The AI should suggest: By.cssSelector("[data-testid='signin-btn']")
 *   or                     By.xpath("//button[text()='Sign In']")
 *   and the click should succeed after healing.
 *
 * The self-contained demo page is served from: src/test/resources/demo-login.html
 * We load it as a local file:// URL so no internet connection is needed for the page.
 */
public class LoginTest extends BaseTest {

    private static final Logger log = LogManager.getLogger(LoginTest.class);

    @Test(description = "Verifies AI self-healing recovers from a broken id locator")
    public void testHealBrokenLocator() {

        // ── Load the self-contained local demo HTML page ──────────────
        String demoPageUrl = getLocalDemoPageUrl();
        log.info("Loading local demo page: {}", demoPageUrl);
        driver.get(demoPageUrl);

        log.info("==============================================");
        log.info("  INTENTIONALLY USING A BROKEN LOCATOR...");
        log.info("  By.id(\"login-btn\") [does NOT exist on page]");
        log.info("==============================================");

        // ── Use HealingEngine instead of raw driver.findElement ───────
        // The broken locator will trigger AI healing automatically
        WebElement signInButton = HealingEngine.findElement(driver, By.id("login-btn"));

        // ── Assertions ────────────────────────────────────────────────
        Assert.assertNotNull(signInButton, "Healed element should not be null");
        Assert.assertTrue(signInButton.isDisplayed(), "Healed button should be visible");

        log.info("Clicking the Sign In button (found via AI healing)...");
        signInButton.click();

        // After clicking Sign In, the demo page shows a success message
        WebElement successMsg = driver.findElement(By.id("success-message"));
        Assert.assertTrue(successMsg.isDisplayed(), "Success message should be visible after click");
        Assert.assertTrue(successMsg.getText().contains("Login Successful"),
                "Expected 'Login Successful' message after clicking Sign In");

        log.info("✅ TEST PASSED — AI self-healing successfully recovered the broken locator!");
    }

    // ----------------------------------------------------------------
    // Helper: Resolve the local demo HTML file as a file:// URL
    // ----------------------------------------------------------------

    private String getLocalDemoPageUrl() {
        // Try classpath first (works from Maven test resources)
        URL resource = getClass().getClassLoader().getResource("demo-login.html");
        if (resource != null) {
            return resource.toString();
        }

        // Fallback: resolve relative to working directory
        File file = new File("src/test/resources/demo-login.html");
        if (file.exists()) {
            return file.toURI().toString();
        }

        throw new IllegalStateException(
                "demo-login.html not found. Expected at src/test/resources/demo-login.html");
    }
}
