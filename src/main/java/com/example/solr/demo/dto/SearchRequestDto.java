package com.example.solr.demo.dto;

/**
 * Encapsulates search request parameters.
 */
public class SearchRequestDto {

    private String q;
    private int page = 0;
    private int size = 20;
    private boolean fuzzy = false;
    private boolean highlight = true;
    private String lang = "all"; // "all", "en", "ar"
    private String sortBy = "score"; // "score", "price_asc", "price_desc"
    private String category;
    private String brand;
    private String op = "AND"; // "AND" or "OR"

    public SearchRequestDto() {
    }

    public SearchRequestDto(String q) {
        this.q = q;
    }

    // Getters and Setters

    public String getQ() {
        return q;
    }

    public void setQ(String q) {
        this.q = q;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = Math.max(0, page);
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = (size <= 0) ? 20 : Math.min(size, 100);
    }

    public boolean isFuzzy() {
        return fuzzy;
    }

    public void setFuzzy(boolean fuzzy) {
        this.fuzzy = fuzzy;
    }

    public boolean isHighlight() {
        return highlight;
    }

    public void setHighlight(boolean highlight) {
        this.highlight = highlight;
    }

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    public String getSortBy() {
        return sortBy;
    }

    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getOp() {
        return (op != null && !op.trim().isEmpty()) ? op.trim().toUpperCase() : "AND";
    }

    public void setOp(String op) {
        this.op = (op != null && "OR".equalsIgnoreCase(op.trim())) ? "OR" : "AND";
    }
}
