package com.aiheal.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * ConfigReader — Centralized, simple config loader.
 *
 * Loads from: src/test/resources/config.properties (on the classpath).
 * Property values that start with "${" are treated as unresolved
 * placeholders (e.g. ${OPENAI_API_KEY}) and are not returned.
 */
public class ConfigReader {

    private static final Logger log = LogManager.getLogger(ConfigReader.class);
    private static final String CONFIG_FILE = "/config.properties";
    private static final Properties PROPS = new Properties();

    static {
        try (InputStream is = ConfigReader.class.getResourceAsStream(CONFIG_FILE)) {
            if (is == null) {
                log.warn("config.properties not found on classpath at {}", CONFIG_FILE);
            } else {
                PROPS.load(is);
                log.info("config.properties loaded successfully.");
            }
        } catch (IOException e) {
            log.error("Failed to load config.properties: {}", e.getMessage());
        }
    }

    private ConfigReader() {}

    /**
     * Returns the property value, or null if not found or is an unresolved placeholder.
     */
    public static String get(String key) {
        String value = PROPS.getProperty(key);
        if (value != null && value.startsWith("${")) {
            return null; // Unresolved placeholder
        }
        return value;
    }

    /**
     * Returns the property value, or the given default if not found.
     */
    public static String get(String key, String defaultValue) {
        String value = get(key);
        return (value != null && !value.isBlank()) ? value : defaultValue;
    }
}
