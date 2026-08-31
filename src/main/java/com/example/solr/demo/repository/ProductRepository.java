package com.example.solr.demo.repository;

import com.example.solr.demo.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA Repository for the full {@link Product} entity.
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findBySku(String sku);

    List<Product> findByCategoryEnIgnoreCase(String categoryEn);

    List<Product> findByCategoryArIgnoreCase(String categoryAr);

    List<Product> findByBrandIgnoreCase(String brand);

    @Query("SELECT DISTINCT p.categoryEn FROM Product p WHERE p.categoryEn IS NOT NULL ORDER BY p.categoryEn")
    List<String> findDistinctCategoriesEn();

    @Query("SELECT DISTINCT p.brand FROM Product p WHERE p.brand IS NOT NULL ORDER BY p.brand")
    List<String> findDistinctBrands();
}
