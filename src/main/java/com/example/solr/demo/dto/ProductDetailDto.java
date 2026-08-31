package com.example.solr.demo.dto;

import com.example.solr.demo.entity.Product;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Full Product details DTO for viewing and creating/updating records in DB + Solr.
 */
public class ProductDetailDto {

    private Long id;
    private String sku;
    private String titleEn;
    private String titleAr;
    private String shortDescriptionEn;
    private String shortDescriptionAr;
    private String fullDescriptionEn;
    private String fullDescriptionAr;
    private String categoryEn;
    private String categoryAr;
    private String brand;
    private BigDecimal price;
    private String currency;
    private Integer stockQuantity;
    private Boolean inStock;
    private Double rating;
    private Integer reviewsCount;
    private String imageUrl;
    private String tags;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ProductDetailDto() {
    }

    public static ProductDetailDto fromEntity(Product product) {
        if (product == null) return null;
        ProductDetailDto dto = new ProductDetailDto();
        dto.setId(product.getId());
        dto.setSku(product.getSku());
        dto.setTitleEn(product.getTitleEn());
        dto.setTitleAr(product.getTitleAr());
        dto.setShortDescriptionEn(product.getShortDescriptionEn());
        dto.setShortDescriptionAr(product.getShortDescriptionAr());
        dto.setFullDescriptionEn(product.getFullDescriptionEn());
        dto.setFullDescriptionAr(product.getFullDescriptionAr());
        dto.setCategoryEn(product.getCategoryEn());
        dto.setCategoryAr(product.getCategoryAr());
        dto.setBrand(product.getBrand());
        dto.setPrice(product.getPrice());
        dto.setCurrency(product.getCurrency());
        dto.setStockQuantity(product.getStockQuantity());
        dto.setInStock(product.getInStock());
        dto.setRating(product.getRating());
        dto.setReviewsCount(product.getReviewsCount());
        dto.setImageUrl(product.getImageUrl());
        dto.setTags(product.getTags());
        dto.setCreatedAt(product.getCreatedAt());
        dto.setUpdatedAt(product.getUpdatedAt());
        return dto;
    }

    public Product toEntity() {
        Product product = new Product();
        product.setId(this.id);
        product.setSku(this.sku);
        product.setTitleEn(this.titleEn);
        product.setTitleAr(this.titleAr);
        product.setShortDescriptionEn(this.shortDescriptionEn);
        product.setShortDescriptionAr(this.shortDescriptionAr);
        product.setFullDescriptionEn(this.fullDescriptionEn);
        product.setFullDescriptionAr(this.fullDescriptionAr);
        product.setCategoryEn(this.categoryEn);
        product.setCategoryAr(this.categoryAr);
        product.setBrand(this.brand);
        product.setPrice(this.price);
        product.setCurrency(this.currency != null ? this.currency : "SAR");
        product.setStockQuantity(this.stockQuantity);
        product.setInStock(this.inStock);
        product.setRating(this.rating);
        product.setReviewsCount(this.reviewsCount);
        product.setImageUrl(this.imageUrl);
        product.setTags(this.tags);
        return product;
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

    public String getFullDescriptionEn() {
        return fullDescriptionEn;
    }

    public void setFullDescriptionEn(String fullDescriptionEn) {
        this.fullDescriptionEn = fullDescriptionEn;
    }

    public String getFullDescriptionAr() {
        return fullDescriptionAr;
    }

    public void setFullDescriptionAr(String fullDescriptionAr) {
        this.fullDescriptionAr = fullDescriptionAr;
    }

    public String getCategoryEn() {
        return categoryEn;
    }

    public void setCategoryEn(String categoryEn) {
        this.categoryEn = categoryEn;
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
