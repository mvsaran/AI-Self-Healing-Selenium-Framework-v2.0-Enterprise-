package com.aiheal.model;

import java.util.ArrayList;
import java.util.List;

/**
 * HealResult — Plain data object that carries the full context of a healing attempt.
 */
public class HealResult {

    // ---- Original (broken) locator details ----
    private String originalLocatorType;
    private String originalLocatorValue;

    // ---- New locator details (The chosen winner) ----
    private SuggestedLocator winningLocator;

    // ---- All Suggestions returned by AI ----
    private List<SuggestedLocator> allSuggestions = new ArrayList<>();

    // ---- Healing outcome ----
    private boolean healedSuccessfully;
    private boolean retryAttempted;
    private String  errorMessage;

    public HealResult() {}

    public String getOriginalLocatorType() { return originalLocatorType; }
    public void setOriginalLocatorType(String v) { this.originalLocatorType = v; }

    public String getOriginalLocatorValue() { return originalLocatorValue; }
    public void setOriginalLocatorValue(String v) { this.originalLocatorValue = v; }

    public SuggestedLocator getWinningLocator() { return winningLocator; }
    public void setWinningLocator(SuggestedLocator winningLocator) { this.winningLocator = winningLocator; }

    public List<SuggestedLocator> getAllSuggestions() { return allSuggestions; }
    public void setAllSuggestions(List<SuggestedLocator> allSuggestions) { this.allSuggestions = allSuggestions; }

    public boolean isHealedSuccessfully() { return healedSuccessfully; }
    public void setHealedSuccessfully(boolean v) { this.healedSuccessfully = v; }

    public boolean isRetryAttempted() { return retryAttempted; }
    public void setRetryAttempted(boolean v) { this.retryAttempted = v; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String v) { this.errorMessage = v; }

    @Override
    public String toString() {
        return String.format(
            "HealResult{original=[%s:'%s'], winningLocator=%s, allSuggestions=%d, healed=%b}",
            originalLocatorType, originalLocatorValue,
            winningLocator != null ? winningLocator : "none",
            allSuggestions.size(),
            healedSuccessfully
        );
    }
}
