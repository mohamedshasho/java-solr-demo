package com.example.solr.demo.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * JPA Entity representing the full Product table in the relational database (H2).
 * <p>
 * Architectural Note for Presentation:
 * ------------------------------------
 * In modern e-commerce systems, the relational database acts as the single source of truth (master store)
 * containing rich, full relational attributes (pricing, stock levels, full descriptions, multi-language data,
 * audit timestamps, foreign keys).
 * <p>
 * In contrast, Apache Solr maintains a lean, optimized inverted index containing only search-critical fields
 * (id, title_en, title_ar, short_description_en, short_description_ar, sku).
 * When a user searches, Solr returns relevant document IDs and highlights in milliseconds, which can then be
 * joined or enriched with the full database records.
 */
@Entity
@Table(name = "products", indexes = {
        @Index(name = "idx_product_sku", columnList = "sku", unique = true),
        @Index(name = "idx_product_brand", columnList = "brand"),
        @Index(name = "idx_product_category", columnList = "categoryEn")
})
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String sku;

    // --- English Localization ---
    @Column(nullable = false, length = 255)
    private String titleEn;

    @Column(length = 1000)
    private String shortDescriptionEn;

    @Lob
    @Column(columnDefinition = "CLOB")
    private String fullDescriptionEn;

    @Column(length = 100)
    private String categoryEn;

    // --- Arabic Localization ---
    @Column(nullable = false, length = 255)
    private String titleAr;

    @Column(length = 1000)
    private String shortDescriptionAr;

    @Lob
    @Column(columnDefinition = "CLOB")
    private String fullDescriptionAr;

    @Column(length = 100)
    private String categoryAr;

    // --- Product Commercial & Inventory Attributes ---
    @Column(length = 100)
    private String brand;

    @Column(precision = 10, scale = 2)
    private BigDecimal price;

    @Column(length = 10)
    private String currency = "SAR";

    private Integer stockQuantity;

    private Boolean inStock;

    private Double rating;

    private Integer reviewsCount;

    @Column(length = 500)
    private String imageUrl;

    @Column(length = 255)
    private String tags;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public Product() {
    }

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.inStock == null && this.stockQuantity != null) {
            this.inStock = this.stockQuantity > 0;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
        if (this.stockQuantity != null) {
            this.inStock = this.stockQuantity > 0;
        }
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
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

    public String getShortDescriptionEn() {
        return shortDescriptionEn;
    }

    public void setShortDescriptionEn(String shortDescriptionEn) {
        this.shortDescriptionEn = shortDescriptionEn;
    }

    public String getFullDescriptionEn() {
        return fullDescriptionEn;
    }

    public void setFullDescriptionEn(String fullDescriptionEn) {
        this.fullDescriptionEn = fullDescriptionEn;
    }

    public String getCategoryEn() {
        return categoryEn;
    }

    public void setCategoryEn(String categoryEn) {
        this.categoryEn = categoryEn;
    }

    public String getTitleAr() {
        return titleAr;
    }

    public void setTitleAr(String titleAr) {
        this.titleAr = titleAr;
    }

    public String getShortDescriptionAr() {
        return shortDescriptionAr;
    }

    public void setShortDescriptionAr(String shortDescriptionAr) {
        this.shortDescriptionAr = shortDescriptionAr;
    }

    public String getFullDescriptionAr() {
        return fullDescriptionAr;
    }

    public void setFullDescriptionAr(String fullDescriptionAr) {
        this.fullDescriptionAr = fullDescriptionAr;
    }

    public String getCategoryAr() {
        return categoryAr;
    }

    public void setCategoryAr(String categoryAr) {
        this.categoryAr = categoryAr;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Integer getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(Integer stockQuantity) {
        this.stockQuantity = stockQuantity;
    }

    public Boolean getInStock() {
        return inStock;
    }

    public void setInStock(Boolean inStock) {
        this.inStock = inStock;
    }

    public Double getRating() {
        return rating;
    }

    public void setRating(Double rating) {
        this.rating = rating;
    }

    public Integer getReviewsCount() {
        return reviewsCount;
    }

    public void setReviewsCount(Integer reviewsCount) {
        this.reviewsCount = reviewsCount;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
