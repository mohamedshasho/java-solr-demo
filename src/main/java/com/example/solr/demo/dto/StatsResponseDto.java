package com.example.solr.demo.dto;

import java.util.List;

/**
 * System and search engine statistics DTO.
 */
public class StatsResponseDto {

    private long databaseProductCount;
    private long solrIndexedDocCount;
    private boolean solrConnected;
    private String solrCoreName;
    private String solrBaseUrl;
    private List<String> categories;
    private List<String> brands;

    public StatsResponseDto() {
    }

    // Getters and Setters

    public long getDatabaseProductCount() {
        return databaseProductCount;
    }

    public void setDatabaseProductCount(long databaseProductCount) {
        this.databaseProductCount = databaseProductCount;
    }

    public long getSolrIndexedDocCount() {
        return solrIndexedDocCount;
    }

    public void setSolrIndexedDocCount(long solrIndexedDocCount) {
        this.solrIndexedDocCount = solrIndexedDocCount;
    }

    public boolean isSolrConnected() {
        return solrConnected;
    }

    public void setSolrConnected(boolean solrConnected) {
        this.solrConnected = solrConnected;
    }

    public String getSolrCoreName() {
        return solrCoreName;
    }

    public void setSolrCoreName(String solrCoreName) {
        this.solrCoreName = solrCoreName;
    }

    public String getSolrBaseUrl() {
        return solrBaseUrl;
    }

    public void setSolrBaseUrl(String solrBaseUrl) {
        this.solrBaseUrl = solrBaseUrl;
    }

    public List<String> getCategories() {
        return categories;
    }

    public void setCategories(List<String> categories) {
        this.categories = categories;
    }

    public List<String> getBrands() {
        return brands;
    }

    public void setBrands(List<String> brands) {
        this.brands = brands;
    }
}
