package com.aiheal.healing;

import com.aiheal.utils.ConfigReader;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * AIClient — Sends the healing prompt and base64 screenshot to OpenAI 
 * and returns the raw JSON response containing the suggested locators array.
 */
public class AIClient {

    private static final Logger log = LogManager.getLogger(AIClient.class);

    // OpenAI chat completions endpoint
    private static final String OPENAI_URL =
            "https://api.openai.com/v1/chat/completions";

    private static final MediaType JSON_MEDIA_TYPE =
            MediaType.parse("application/json; charset=utf-8");

    // Shared OkHttp client
    private static final OkHttpClient HTTP_CLIENT = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private AIClient() {}

    /**
     * Sends the prompt and visual context (base64 screenshot) to OpenAI.
     *
     * @param prompt      fully constructed prompt string
     * @param base64Image base64 screenshot of the browser (can be null)
     * @return the raw text content of OpenAI's response
     */
    public static String callOpenAI(String prompt, String base64Image) {
        String apiKey = resolveApiKey();
        String model  = ConfigReader.get("openai.model", "gpt-4o-mini");

        log.info("Calling OpenAI model '{}' for locator healing...", model);
        if (base64Image != null) {
            log.info("Visual context (screenshot base64) attached to AI prompt.");
        }

        String requestBody = buildRequestBody(model, prompt, base64Image);

        Request request = new Request.Builder()
                .url(OPENAI_URL)
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(requestBody, JSON_MEDIA_TYPE))
                .build();

        try (Response response = HTTP_CLIENT.newCall(request).execute()) {
            ResponseBody body = response.body();
            String responseBody = (body != null) ? body.string() : "";

            if (!response.isSuccessful()) {
                log.error("OpenAI API returned HTTP {}: {}", response.code(), responseBody);
                throw new RuntimeException(
                        "OpenAI API error [HTTP " + response.code() + "]: " + responseBody);
            }

            JsonNode root    = MAPPER.readTree(responseBody);
            String   content = root.path("choices")
                                   .path(0)
                                   .path("message")
                                   .path("content")
                                   .asText("");

            if (content.isBlank()) {
                log.warn("OpenAI returned an empty content string. Full response: {}", responseBody);
                throw new RuntimeException("OpenAI returned empty content.");
            }

            log.debug("OpenAI raw response content: {}", content);
            return content.trim();

        } catch (IOException e) {
            log.error("Network error calling OpenAI: {}", e.getMessage());
            throw new RuntimeException("Network error during OpenAI call", e);
        }
    }

    private static String buildRequestBody(String model, String prompt, String base64Image) {
        try {
            var bodyNode    = MAPPER.createObjectNode();
            var messagesArr = MAPPER.createArrayNode();
            var userMsg     = MAPPER.createObjectNode();

            userMsg.put("role", "user");

            if (base64Image != null && !base64Image.isBlank()) {
                // Formatting for multimodal (Vision) request
                var contentArr = MAPPER.createArrayNode();

                var textContent = MAPPER.createObjectNode();
                textContent.put("type", "text");
                textContent.put("text", prompt);
                contentArr.add(textContent);

                var imageContent = MAPPER.createObjectNode();
                imageContent.put("type", "image_url");

                var imageUrlNode = MAPPER.createObjectNode();
                imageUrlNode.put("url", "data:image/png;base64," + base64Image);
                // "low" detail saves substantial tokens if we just need layout understanding
                imageUrlNode.put("detail", "low"); 
                
                imageContent.set("image_url", imageUrlNode);
                contentArr.add(imageContent);

                userMsg.set("content", contentArr);
            } else {
                // Text-only request
                userMsg.put("content", prompt);
            }

            messagesArr.add(userMsg);

            bodyNode.put("model",       model);
            bodyNode.set("messages",    messagesArr);
            bodyNode.put("temperature", 0.1); // lower temperature for more deterministic locators
            bodyNode.put("max_tokens",  1000);

            return MAPPER.writeValueAsString(bodyNode);

        } catch (Exception e) {
            throw new RuntimeException("Failed to build OpenAI request body", e);
        }
    }

    private static String resolveApiKey() {
        String envKey = System.getenv("OPENAI_API_KEY");
        if (envKey != null && !envKey.isBlank()) return envKey;

        String sysProp = System.getProperty("OPENAI_API_KEY");
        if (sysProp != null && !sysProp.isBlank()) return sysProp;

        String configKey = ConfigReader.get("openai.apiKey");
        if (configKey != null && !configKey.isBlank() && !configKey.startsWith("${")) return configKey;

        throw new IllegalStateException("OpenAI API key not found in env var OPENAI_API_KEY or config.properties.");
    }
}
