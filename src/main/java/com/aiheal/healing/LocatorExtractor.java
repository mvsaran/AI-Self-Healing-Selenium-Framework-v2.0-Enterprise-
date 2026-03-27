package com.aiheal.healing;

import org.openqa.selenium.By;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * LocatorExtractor — Breaks down a Selenium {@link By} object into
 * its human-readable type and value strings.
 *
 * Example:
 *   By.id("login-btn")            → type="id",          value="login-btn"
 *   By.cssSelector(".submit-btn") → type="cssSelector",  value=".submit-btn"
 *   By.xpath("//button")          → type="xpath",         value="//button"
 *
 * Selenium's By.toString() produces strings like "By.id: login-btn".
 *
 * IMPORTANT — Selenium 4 changed cssSelector's toString output from
 *   "By.cssSelector: .x"   (Selenium 3)
 * to
 *   "By.css selector: .x"  (Selenium 4, with a space)
 *
 * The regex below handles both forms and normalises the type back to the
 * camelCase form used everywhere else in this framework.
 */
public class LocatorExtractor {

    /**
     * Matches "By.<type>: <value>" where type can include a space (e.g. "css selector").
     * The [\\w ]+ (word-chars plus space) captures both single-word and two-word type names.
     * The trailing ? makes the group non-greedy so it stops before the colon.
     */
    private static final Pattern BY_PATTERN =
            Pattern.compile("^By\\.([\\w ]+?):\\s*(.+)$", Pattern.DOTALL);

    /**
     * Maps Selenium 4 repr strings (lowercase, may contain spaces) back to the
     * camelCase form used in LocatorParser and reports.
     */
    private static final Map<String, String> TYPE_ALIASES = Map.of(
            "css selector", "cssSelector",  // Selenium 4 format
            "cssselector",  "cssSelector",  // Selenium 3 format (lowercase)
            "id",           "id",
            "xpath",        "xpath",
            "name",         "name",
            "class name",   "className",
            "link text",    "linkText",
            "tag name",     "tagName"
    );

    private LocatorExtractor() {}

    /**
     * Extracts the locator type from a Selenium By object.
     *
     * @param by Selenium By locator
     * @return normalised type string such as "id", "cssSelector", "xpath"
     */
    public static String extractType(By by) {
        return parse(by)[0];
    }

    /**
     * Extracts the locator value from a Selenium By object.
     *
     * @param by Selenium By locator
     * @return raw value string e.g. "login-btn", ".submit-btn", "//button"
     */
    public static String extractValue(By by) {
        return parse(by)[1];
    }

    // ----------------------------------------------------------------
    // Internal helper
    // ----------------------------------------------------------------

    private static String[] parse(By by) {
        String raw = by.toString().trim();
        Matcher m  = BY_PATTERN.matcher(raw);

        if (m.matches()) {
            String rawType        = m.group(1).trim().toLowerCase();
            // Normalise to canonical camelCase (e.g. "css selector" → "cssSelector")
            String normalisedType = TYPE_ALIASES.getOrDefault(rawType, rawType);
            return new String[]{normalisedType, m.group(2).trim()};
        }

        // Fallback: unrecognised format — return whole string as "unknown"
        return new String[]{"unknown", raw};
    }
}
