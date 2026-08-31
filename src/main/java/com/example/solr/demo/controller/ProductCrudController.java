package com.example.solr.demo.controller;

import com.example.solr.demo.dto.ProductDetailDto;
import com.example.solr.demo.service.DataSeederService;
import com.example.solr.demo.service.ProductService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST Controller managing Product database CRUD operations and Index Lifecycle maintenance.
 * <p>
 * Demonstrates:
 * 1. Standard relational DB operations (Create, Read, Update, Delete).
 * 2. Automatic Solr index synchronization (Dual-Write pattern).
 * 3. Administrative operations (Bulk Seeding 5000+ items, Full Reindexing).
 */
@RestController
@RequestMapping("/api/products")
@CrossOrigin(origins = "*")
public class ProductCrudController {

    private final ProductService productService;
    private final DataSeederService seederService;

    public ProductCrudController(ProductService productService, DataSeederService seederService) {
        this.productService = productService;
        this.seederService = seederService;
    }

    /**
     * Lists paginated products from the relational database master store.
     */
    @GetMapping
    public ResponseEntity<Page<ProductDetailDto>> getAllProducts(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        Page<ProductDetailDto> products = productService.findAll(page, size);
        return ResponseEntity.ok(products);
    }

    /**
     * Retrieves full product details by database primary key.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProductDetailDto> getProductById(@PathVariable("id") Long id) {
        return productService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Creates a new product in the database and automatically indexes it in Apache Solr.
     */
    @PostMapping
    public ResponseEntity<ProductDetailDto> createProduct(@RequestBody ProductDetailDto dto) {
        ProductDetailDto created = productService.createProduct(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Updates an existing product in the database and refreshes its Solr index document.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ProductDetailDto> updateProduct(
            @PathVariable("id") Long id,
            @RequestBody ProductDetailDto dto) {
        return productService.updateProduct(id, dto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Deletes a product from the database and removes it from the Solr index.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteProduct(@PathVariable("id") Long id) {
        boolean deleted = productService.deleteProduct(id);
        if (deleted) {
            return ResponseEntity.ok(Map.of("success", true, "message", "Product deleted from DB and Solr index."));
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * Generates and seeds 5000+ fake products into both Database and Solr.
     */
    @PostMapping("/seed")
    public ResponseEntity<Map<String, Object>> seedDatabase(
            @RequestParam(name = "count", defaultValue = "5000") int count) {
        long seeded = seederService.seedProducts(count);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Successfully generated and indexed products into DB and Solr.",
                "totalSeeded", seeded
        ));
    }

    /**
     * Performs a full rebuild/re-index of all database products into Apache Solr.
     */
    @PostMapping("/reindex")
    public ResponseEntity<Map<String, Object>> reindexAll() {
        long reindexed = productService.reindexAll();
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Successfully re-indexed all database products into Solr core.",
                "totalIndexed", reindexed
        ));
    }

    /**
     * Refreshes the Apache Solr core index searcher and flushes in-flight document commits,
     * ensuring immediate real-time search visibility.
     */
    @RequestMapping(value = "/refresh", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<Map<String, Object>> refreshIndex() {
        boolean refreshed = productService.refreshSolrCore();
        return ResponseEntity.ok(Map.of(
                "success", refreshed,
                "message", refreshed ? "Solr core index searcher refreshed successfully." : "Solr refresh failed."
        ));
    }
}
