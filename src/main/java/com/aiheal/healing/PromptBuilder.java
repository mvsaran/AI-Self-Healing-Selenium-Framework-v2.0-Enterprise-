package com.aiheal.healing;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * PromptBuilder — Loads the prompt template and injects runtime values.
 *
 * The template lives at: prompts/heal-locator-prompt.txt (on the classpath).
 * Placeholders use {{PLACEHOLDER}} syntax and are replaced at runtime.
 */
public class PromptBuilder {

    private static final Logger log = LogManager.getLogger(PromptBuilder.class);

    // Classpath location of the prompt template
    private static final String TEMPLATE_PATH = "/prompts/heal-locator-prompt.txt";

    // Placeholders defined in the template file
    private static final String PH_LOCATOR_TYPE  = "{{LOCATOR_TYPE}}";
    private static final String PH_LOCATOR_VALUE = "{{LOCATOR_VALUE}}";
    private static final String PH_EXCEPTION_MSG = "{{EXCEPTION_MESSAGE}}";
    private static final String PH_DOM_SNIPPET   = "{{DOM_SNIPPET}}";

    private PromptBuilder() {}

    /**
     * Builds the final prompt string by injecting the runtime values
     * into the loaded template.
     *
     * @param locatorType  e.g. "id"
     * @param locatorValue e.g. "login-btn"
     * @param exceptionMsg the NoSuchElementException message
     * @param domSnippet   trimmed page source
     * @return fully constructed prompt ready to send to OpenAI
     */
    public static String build(String locatorType,
                               String locatorValue,
                               String exceptionMsg,
                               String domSnippet) {

        String template = loadTemplate();

        return template
                .replace(PH_LOCATOR_TYPE,  safe(locatorType))
                .replace(PH_LOCATOR_VALUE, safe(locatorValue))
                .replace(PH_EXCEPTION_MSG, safe(exceptionMsg))
                .replace(PH_DOM_SNIPPET,   safe(domSnippet));
    }

    // ----------------------------------------------------------------
    // Internal helpers
    // ----------------------------------------------------------------

    /**
     * Reads the prompt template from the classpath.
     */
    private static String loadTemplate() {
        try (InputStream is = PromptBuilder.class.getResourceAsStream(TEMPLATE_PATH)) {
            if (is == null) {
                throw new IllegalStateException(
                        "Prompt template not found on classpath: " + TEMPLATE_PATH);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Failed to load prompt template from {}: {}", TEMPLATE_PATH, e.getMessage());
            throw new RuntimeException("Could not load prompt template", e);
        }
    }

    /** Returns a safe non-null string to prevent literal 'null' in prompts. */
    private static String safe(String s) {
        return s == null ? "" : s;
    }
}
