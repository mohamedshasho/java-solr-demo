package com.example.solr.demo.dto;

/**
 * Encapsulates a curated "Hard Search Test Case" for presentation demonstrations.
 * <p>
 * Demonstrates why enterprise search engines (Apache Solr) are required over standard SQL queries
 * when dealing with Arabic morphology, English stemming, typos, diacritics, and complex relevancy.
 */
public class HardExampleDto {

    private String id;
    private String title;
    private String category;
    private String query;
    private String language;
    private String solrMechanism;
    private String whySqlFails;
    private String expectedMatches;
    private String badgeColor;

    public HardExampleDto() {
    }

    public HardExampleDto(String id, String title, String category, String query, String language,
                          String solrMechanism, String whySqlFails, String expectedMatches, String badgeColor) {
        this.id = id;
        this.title = title;
        this.category = category;
        this.query = query;
        this.language = language;
        this.solrMechanism = solrMechanism;
        this.whySqlFails = whySqlFails;
        this.expectedMatches = expectedMatches;
        this.badgeColor = badgeColor;
    }

    // Getters and Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getSolrMechanism() {
        return solrMechanism;
    }

    public void setSolrMechanism(String solrMechanism) {
        this.solrMechanism = solrMechanism;
    }

    public String getWhySqlFails() {
        return whySqlFails;
    }

    public void setWhySqlFails(String whySqlFails) {
        this.whySqlFails = whySqlFails;
    }

    public String getExpectedMatches() {
        return expectedMatches;
    }

    public void setExpectedMatches(String expectedMatches) {
        this.expectedMatches = expectedMatches;
    }

    public String getBadgeColor() {
        return badgeColor;
    }

    public void setBadgeColor(String badgeColor) {
        this.badgeColor = badgeColor;
    }
}
