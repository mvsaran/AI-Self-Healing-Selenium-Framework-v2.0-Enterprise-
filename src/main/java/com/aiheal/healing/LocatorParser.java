package com.aiheal.healing;

import com.aiheal.model.SuggestedLocator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;

import java.util.ArrayList;
import java.util.List;

/**
 * LocatorParser — Parses the AI's JSON array response into a List of {@link SuggestedLocator}
 * and converts the suggested locator into a Selenium {@link By}.
 */
public class LocatorParser {

    private static final Logger log = LogManager.getLogger(LocatorParser.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private LocatorParser() {}

    /**
     * Parses the raw AI JSON array string into a List of SuggestedLocators.
     * Strips any accidental markdown fences before parsing.
     */
    public static List<SuggestedLocator> parseArray(String rawAiResponse) {
        List<SuggestedLocator> locators = new ArrayList<>();
        try {
            String json = stripMarkdownFences(rawAiResponse);
            JsonNode root = MAPPER.readTree(json);

            if (root.isArray()) {
                for (JsonNode node : root) {
                    SuggestedLocator sl = new SuggestedLocator(
                            node.path("suggestedLocatorType").asText("").trim(),
                            node.path("suggestedLocatorValue").asText("").trim(),
                            node.path("confidence").asDouble(0.0),
                            node.path("reason").asText("No reason provided.").trim()
                    );
                    locators.add(sl);
                }
            } else {
                log.warn("AI returned a JSON Object instead of an Array. Parsing as single locator...");
                SuggestedLocator sl = new SuggestedLocator(
                        root.path("suggestedLocatorType").asText("").trim(),
                        root.path("suggestedLocatorValue").asText("").trim(),
                        root.path("confidence").asDouble(0.0),
                        root.path("reason").asText("No reason provided.").trim()
                );
                locators.add(sl);
            }

            for (SuggestedLocator sl : locators) {
                String confidencePct = String.format("%.0f%%", sl.getConfidence() * 100);
                log.info("AI suggested locator: [{}] '{}' (confidence: {})",
                        sl.getType(), sl.getValue(), confidencePct);
            }

        } catch (Exception e) {
            log.error("Failed to parse AI response as JSON Array. Raw response:\n{}", rawAiResponse);
            log.error("Parse error: {}", e.getMessage());
        }
        return locators;
    }

    /**
     * Converts a SuggestedLocator into a Selenium By.
     */
    public static By toSeleniumBy(SuggestedLocator locator) {
        if (locator == null) return null;
        return toSeleniumBy(locator.getType(), locator.getValue());
    }

    public static By toSeleniumBy(String type, String value) {
        if (type == null || type.isBlank() || value == null || value.isBlank()) {
            log.warn("Cannot convert empty locator type/value to Selenium By.");
            return null;
        }

        return switch (type.toLowerCase()) {
            case "id"              -> By.id(value);
            case "cssselector"     -> By.cssSelector(value);
            case "xpath"           -> By.xpath(value);
            case "name"            -> By.name(value);
            case "classname"       -> By.className(value);
            case "linktext"        -> By.linkText(value);
            case "partiallinktext" -> By.partialLinkText(value);
            case "tagname"         -> By.tagName(value);
            default -> {
                log.warn("Unsupported locator type from AI: '{}'", type);
                yield null;
            }
        };
    }

    private static String stripMarkdownFences(String raw) {
        if (raw == null) return "";
        return raw.replaceAll("(?s)^```[\\w]*\\s*", "")
                  .replaceAll("(?s)```\\s*$", "")
                  .trim();
    }
}
