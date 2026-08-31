package com.example.solr.demo.dto;

import java.util.List;

/**
 * Spellcheck response containing correction suggestions and collations.
 */
public class SpellcheckResponseDto {

    private String originalQuery;
    private boolean correctlySpelled;
    private String didYouMean;
    private List<String> suggestions;
    private long qTimeMs;

    public SpellcheckResponseDto() {
    }

    public SpellcheckResponseDto(String originalQuery, boolean correctlySpelled, String didYouMean, List<String> suggestions, long qTimeMs) {
        this.originalQuery = originalQuery;
        this.correctlySpelled = correctlySpelled;
        this.didYouMean = didYouMean;
        this.suggestions = suggestions;
        this.qTimeMs = qTimeMs;
    }

    // Getters and Setters

    public String getOriginalQuery() {
        return originalQuery;
    }

    public void setOriginalQuery(String originalQuery) {
        this.originalQuery = originalQuery;
    }

    public boolean isCorrectlySpelled() {
        return correctlySpelled;
    }

    public void setCorrectlySpelled(boolean correctlySpelled) {
        this.correctlySpelled = correctlySpelled;
    }

    public String getDidYouMean() {
        return didYouMean;
    }

    public void setDidYouMean(String didYouMean) {
        this.didYouMean = didYouMean;
    }

    public List<String> getSuggestions() {
        return suggestions;
    }

    public void setSuggestions(List<String> suggestions) {
        this.suggestions = suggestions;
    }

    public long getQTimeMs() {
        return qTimeMs;
    }

    public void setQTimeMs(long qTimeMs) {
        this.qTimeMs = qTimeMs;
    }
}
