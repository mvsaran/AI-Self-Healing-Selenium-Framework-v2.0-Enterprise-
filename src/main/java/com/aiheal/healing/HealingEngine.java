package com.aiheal.healing;

import com.aiheal.model.HealResult;
import com.aiheal.model.SuggestedLocator;
import com.aiheal.utils.ConfigReader;
import com.aiheal.utils.DashboardGenerator;
import com.aiheal.utils.ReportUtil;
import com.aiheal.utils.ScreenshotUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

/**
 * HealingEngine — The core orchestrator of the self-healing mechanism.
 *
 * Flow:
 * 1. Check persistent JSON for historically proven healed locators.
 * 2. Try the original (or persistent) locator.
 * 3. On failure, capture DOM & Base64 Screenshot.
 * 4. Call OpenAI multimodal AI client holding threshold limits.
 * 5. Parse returned array of SuggestedLocators (Top 3 rank).
 * 6. Iterate through Rank: try each until one works.
 * 7. If successful, cache it in LocatorPersistenceUtil and regenerate the DB Dashboard.
 * 8. Return result wrapped inside a WebElement proxy (StaleElementReferenceException defender).
 */
public class HealingEngine {

    private static final Logger log = LogManager.getLogger(HealingEngine.class);

    private HealingEngine() {}

    /**
     * Finds a WebElement using the given locator, activating AI self-healing
     * if the original locator fails with a {@link NoSuchElementException}.
     */
    public static WebElement findElement(WebDriver driver, By locator) {

        // ── Step 0: Check Persistent Store ───────────────────────────
        By cachedLocator = LocatorPersistenceUtil.getSavedLocator(locator);
        if (cachedLocator != null) {
            try {
                WebElement el = driver.findElement(cachedLocator);
                log.info("✅ Persistent AI heuristic found element securely.");
                return WebElementProxy.createProxy(driver, el, cachedLocator);
            } catch (NoSuchElementException ignored) {
                log.warn("Persistent heuristic became stale/invalid. Engaging active healing cycle.");
            }
        }

        // ── Step 1: Try the original locator ─────────────────────────
        try {
            WebElement element = driver.findElement(locator);
            log.info("✅ Element found with original locator: {}", locator);
            return WebElementProxy.createProxy(driver, element, locator);

        } catch (NoSuchElementException originalEx) {

            // Check if healing is enabled in config
            boolean healingEnabled = Boolean.parseBoolean(
                    ConfigReader.get("healing.enabled", "true"));

            if (!healingEnabled) {
                log.warn("⚠️  Healing is DISABLED in config. Re-throwing original exception.");
                throw originalEx;
            }

            log.warn("❌ Element NOT found with locator: {}", locator);
            log.warn("   Activating Multimodal AI Self-Healing...");

            return attemptHealing(driver, locator, originalEx);
        }
    }

    // ----------------------------------------------------------------
    // Private: full multimodal healing cascade
    // ----------------------------------------------------------------

    private static WebElement attemptHealing(WebDriver driver,
                                             By originalLocator,
                                             NoSuchElementException originalEx) {

        HealResult result = new HealResult();
        result.setOriginalLocatorType(LocatorExtractor.extractType(originalLocator));
        result.setOriginalLocatorValue(LocatorExtractor.extractValue(originalLocator));

        // ── Step 2: Capture Visual + DOM context ──────────────────────
        String domSnippet    = DOMCaptureUtil.captureDOM(driver);
        String base64Image   = ScreenshotUtil.captureBase64(driver);
        
        // Save physical screenshot strictly for test output folder artifacts
        ScreenshotUtil.capture(driver, "fault_trigger");

        // ── Step 3: Build the multimodal AI prompt ─────────────────────
        String prompt = PromptBuilder.build(
                result.getOriginalLocatorType(),
                result.getOriginalLocatorValue(),
                originalEx.getMessage(),
                domSnippet
        );

        // ── Step 4: Call OpenAI Multimodal Engine ─────────────────────
        String aiResponse;
        try {
            aiResponse = AIClient.callOpenAI(prompt, base64Image);
        } catch (Exception aiEx) {
            log.error("AI call failed: {}. Cannot heal. Re-throwing exception.", aiEx.getMessage());
            result.setErrorMessage("AI call failed: " + aiEx.getMessage());
            result.setHealedSuccessfully(false);
            throw originalEx;
        }

        // ── Step 5: Parse Top Tier Array & Apply Threshold ────────────
        List<SuggestedLocator> allSuggestions = LocatorParser.parseArray(aiResponse);
        result.setAllSuggestions(allSuggestions);
        
        double configThreshold = 0.85; 
        try {
            configThreshold = Double.parseDouble(ConfigReader.get("healing.confidence.threshold", "0.85"));
        } catch (NumberFormatException ignored) {}

        if (allSuggestions.isEmpty()) {
            throw new NoSuchElementException("AI parsed absolutely zero locators. " + originalEx.getMessage());
        }

        // ── Step 6: Loop ranked locators attempting execution ──────────
        log.info("Processing top {} AI suggestions (Confidence Threshold >= {})...", 
                 allSuggestions.size(), configThreshold);

        for (SuggestedLocator suggestion : allSuggestions) {
            
            if (suggestion.getConfidence() < configThreshold) {
                log.warn("Disregarding '{}' (Confidence: {}). Below required threshold.", 
                         suggestion.getValue(), suggestion.getConfidence());
                continue;
            }

            By healedBy = LocatorParser.toSeleniumBy(suggestion);
            if (healedBy == null) continue;

            result.setRetryAttempted(true);
            
            try {
                WebElement healedElement = driver.findElement(healedBy);

                // ── SUCCESS: Found ──
                log.info("🟢 Ranked Locator Succeeded! [{}] '{}'", suggestion.getType(), suggestion.getValue());
                result.setWinningLocator(suggestion);
                result.setHealedSuccessfully(true);
                
                // Synchronous Dashboard DB save
                LocatorPersistenceUtil.save(originalLocator, suggestion);
                DashboardGenerator.generate();
                
                // Output textual report
                ReportUtil.log(result);

                return WebElementProxy.createProxy(driver, healedElement, healedBy);

            } catch (NoSuchElementException ignored) {
                log.warn("Ranked locator failed execution: '{}'. Rolling to next priority...", suggestion.getValue());
            }
        }

        // ── Step 7: Total Extinction (All failed) ─────────────────
        result.setHealedSuccessfully(false);
        result.setErrorMessage("All AI suggested locators either failed to execute or fell below confidence minimums.");
        log.error("❌ HEALING FAILED. All AI locators ranked useless.");
        ReportUtil.log(result);

        String failDetails = "All multimodal top ranked locators failed lookup.%n  Original: [%s] '%s'%n";
        failDetails = String.format(failDetails, result.getOriginalLocatorType(), result.getOriginalLocatorValue());
        
        throw new NoSuchElementException(failDetails + " Reason: Exceeded fallback array limits.");
    }
}
