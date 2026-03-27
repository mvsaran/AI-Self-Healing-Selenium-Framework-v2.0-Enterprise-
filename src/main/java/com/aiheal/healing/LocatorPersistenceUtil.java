package com.aiheal.healing;

import com.aiheal.model.SuggestedLocator;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles saving and retrieving healed locators sequentially to avoid redundant API hits.
 * Persists data to `test-output/healed-locators.json`.
 */
public class LocatorPersistenceUtil {

    private static final Logger log = LogManager.getLogger(LocatorPersistenceUtil.class);
    private static final String STORE_PATH = "test-output/healed-locators.json";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    // Map: Map<"id::login-btn", SuggestedLocator>
    private static final Map<String, SuggestedLocator> memory = new ConcurrentHashMap<>();

    // Static init loads any previous mappings from file
    static {
        load();
    }

    private LocatorPersistenceUtil() {}

    /**
     * Checks if a broken locator was already healed in a previous or current run.
     * 
     * @param original the broken By
     * @return the historically healed locator (By) or null if unknown
     */
    public static By getSavedLocator(By original) {
        String key = generateKey(original);
        SuggestedLocator saved = memory.get(key);
        if (saved != null) {
            log.info("🧠 Found persistent healed locator for {}: [{}] '{}'", 
                     key, saved.getType(), saved.getValue());
            return LocatorParser.toSeleniumBy(saved);
        }
        return null;
    }

    /**
     * Caches the winning locator block so future lookups instantly reuse it,
     * writing the outcome synchronously to JSON.
     */
    public static void save(By original, SuggestedLocator winningLocator) {
        String key = generateKey(original);
        memory.put(key, winningLocator);
        flushToDisk();
    }

    /**
     * Dumps in-memory store directly to the flat JSON cache.
     */
    private static synchronized void flushToDisk() {
        try {
            Files.createDirectories(Paths.get("test-output"));
            File file = new File(STORE_PATH);
            MAPPER.writerWithDefaultPrettyPrinter().writeValue(file, memory);
        } catch (IOException e) {
            log.warn("Failed to write persistent locators to file: {}", e.getMessage());
        }
    }

    private static void load() {
        File file = new File(STORE_PATH);
        if (!file.exists()) return;

        try {
            Map<String, SuggestedLocator> data = MAPPER.readValue(file, 
                    new TypeReference<Map<String, SuggestedLocator>>() {});
            memory.putAll(data);
            log.info("Loaded {} historical healed locators from cache.", data.size());
        } catch (IOException e) {
            log.warn("Failed to load persistent locators: {}. A new cache will be built.", e.getMessage());
        }
    }

    /** Generates a consistent map key string from a By object */
    private static String generateKey(By by) {
        return LocatorExtractor.extractType(by) + "::" + LocatorExtractor.extractValue(by);
    }
    
    /** Public getter to read mapping file contents directly for Dashboard Generation */
    public static Map<String, SuggestedLocator> getAllSaved() {
        return memory;
    }
}
