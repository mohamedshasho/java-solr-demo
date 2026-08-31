package com.example.solr.demo.dto;

import java.util.Collections;
import java.util.List;

/**
 * Encapsulates full search response data for presentation and client consumption.
 */
public class SearchResponseDto {

    private String query;
    private long totalHits;
    private int page;
    private int size;
    private int totalPages;
    private long qTimeMs;
    private String parsedSolrQuery;
    private List<String> spellcheckSuggestions = Collections.emptyList();
    private String didYouMean;
    private List<SearchResultItemDto> items = Collections.emptyList();

    public SearchResponseDto() {
    }

    // Getters and Setters

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public long getTotalHits() {
        return totalHits;
    }

    public void setTotalHits(long totalHits) {
        this.totalHits = totalHits;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public long getQTimeMs() {
        return qTimeMs;
    }

    public void setQTimeMs(long qTimeMs) {
        this.qTimeMs = qTimeMs;
    }

    public String getParsedSolrQuery() {
        return parsedSolrQuery;
    }

    public void setParsedSolrQuery(String parsedSolrQuery) {
        this.parsedSolrQuery = parsedSolrQuery;
    }

    public List<String> getSpellcheckSuggestions() {
        return spellcheckSuggestions;
    }

    public void setSpellcheckSuggestions(List<String> spellcheckSuggestions) {
        this.spellcheckSuggestions = spellcheckSuggestions;
    }

    public String getDidYouMean() {
        return didYouMean;
    }

    public void setDidYouMean(String didYouMean) {
        this.didYouMean = didYouMean;
    }

    public List<SearchResultItemDto> getItems() {
        return items;
    }

    public void setItems(List<SearchResultItemDto> items) {
        this.items = items;
    }
}
