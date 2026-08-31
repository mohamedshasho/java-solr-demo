package com.example.solr.demo.document;

import org.apache.solr.client.solrj.beans.Field;

/**
 * Solr Document mapping for Apache Solr indexing.
 * <p>
 * Specific Constraint from Requirements:
 * --------------------------------------
 * The Solr document index stores ONLY the 6 search-critical fields:
 * 1. id                    (Unique Document Identifier)
 * 2. sku                   (Stock Keeping Unit for exact/prefix code searches)
 * 3. title_en              (English product title with English stemming & analyzer)
 * 4. title_ar              (Arabic product title with Arabic normalization & stemmer)
 * 5. short_description_en  (English concise description)
 * 6. short_description_ar  (Arabic concise description)
 * <p>
 * Keeping Solr documents lean maximizes in-memory caching efficiency, minimizes segment size,
 * and speeds up query execution.
 */
public class ProductSolrDoc {

    @Field("id")
    private String id;

    @Field("sku")
    private String sku;

    @Field("title_en")
    private String titleEn;

    @Field("title_ar")
    private String titleAr;

    @Field("short_description_en")
    private String shortDescriptionEn;

    @Field("short_description_ar")
    private String shortDescriptionAr;

    public ProductSolrDoc() {
    }

    public ProductSolrDoc(String id, String sku, String titleEn, String titleAr, String shortDescriptionEn, String shortDescriptionAr) {
        this.id = id;
        this.sku = sku;
        this.titleEn = titleEn;
        this.titleAr = titleAr;
        this.shortDescriptionEn = shortDescriptionEn;
        this.shortDescriptionAr = shortDescriptionAr;
    }

    // Getters and Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getTitleEn() {
        return titleEn;
    }

    public void setTitleEn(String titleEn) {
        this.titleEn = titleEn;
    }

    public String getTitleAr() {
        return titleAr;
    }

    public void setTitleAr(String titleAr) {
        this.titleAr = titleAr;
    }

    public String getShortDescriptionEn() {
        return shortDescriptionEn;
    }

    public void setShortDescriptionEn(String shortDescriptionEn) {
        this.shortDescriptionEn = shortDescriptionEn;
    }

    public String getShortDescriptionAr() {
        return shortDescriptionAr;
    }

    public void setShortDescriptionAr(String shortDescriptionAr) {
        this.shortDescriptionAr = shortDescriptionAr;
    }
}
