package com.aiheal.model;

/**
 * SuggestedLocator — Represents a single locator suggestion returned by the AI.
 */
public class SuggestedLocator {

    private String type;
    private String value;
    private double confidence;
    private String reason;

    public SuggestedLocator() {}

    public SuggestedLocator(String type, String value, double confidence, String reason) {
        this.type = type;
        this.value = value;
        this.confidence = confidence;
        this.reason = reason;
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }

    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    @Override
    public String toString() {
        return String.format("[%s] '%s' (Confidence: %.2f)", type, value, confidence);
    }
}
